/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.jboss.jmx;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.plugin.jboss.JBossServerControlPlugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

public class ServerQuery extends JBossQuery {

    private static final Log log = LogFactory.getLog("JBossServerQuery");

    public static final String SERVER_NAME =
        "jboss.system:type=Server";

    private static final String SERVER_CONFIG_NAME =
        "jboss.system:type=ServerConfig";
    
    private static final String SERVER_INFO_NAME =
        "jboss.system:type=ServerInfo";

    private static final String ATTR_HOMEDIR     = "HomeDir";
    private static final String ATTR_SERVER_URL  = "ServerHomeURL";
    private static final String ATTR_SERVER_NAME = "ServerName";
    public static final String ATTR_VERSION     = "Version";

    private static final String[] ATTRS_SERVER = {
        ATTR_VERSION,
        "BuildDate",
        "VersionName",
    };

    private static final String[] ATTRS_SERVER_CONFIG = {
        ATTR_HOMEDIR,
        ATTR_SERVER_URL,
        ATTR_SERVER_NAME,
    };

    private static final String[] ATTRS_SERVER_INFO = {
        "JavaVersion",
        "JavaVendor"
    };

    private String installPath;
    private String version;
    private String type;
    private List services = new ArrayList();

    public ServerQuery(ServerDetector detector) {
        setServerDetector(detector);
    }

    public List getServiceQueries() {
        return this.services;
    }

    public void getAttributes(RMIAdaptor mServer)
        throws PluginException {
        ObjectName name;

        try {
            name = new ObjectName(SERVER_NAME);
        }
        catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        getAttributes(mServer, name, ATTRS_SERVER);

        try {
            name = new ObjectName(SERVER_CONFIG_NAME);
        }
        catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        getAttributes(mServer, name, ATTRS_SERVER_CONFIG);
        
        try {
            name = new ObjectName(SERVER_INFO_NAME);
        }
        catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        getAttributes(mServer, name, ATTRS_SERVER_INFO);

        //remove build id
        String fullVersion =
            getAttribute(ATTR_VERSION).substring(0, 5);
        setAttribute("version", fullVersion); //cprop defined in hq-plugin.xml
        String ver = fullVersion.substring(0, 3);
        setVersion(ver);

        setName(getAttribute(ATTR_SERVER_NAME));
        setInstallPath(getAttribute(ATTR_HOMEDIR));
    }

    public String getServerURL() {
        return getAttribute(ATTR_SERVER_URL);
    }

    public void findServices(RMIAdaptor mServer)
        throws PluginException {
        
        ServiceQuery[] queries = {
            new StatelessSessionBeanQuery(),
            new StatefulSessionBeanQuery(),
            new EntityBeanQuery(),
            new MessageDrivenBeanQuery(),
            new ConnectionPoolQuery(),
        };

        for (int i=0; i<queries.length; i++) {
            findServices(mServer, queries[i]);
        }

        Map servicePlugins = getServerDetector().getServiceInventoryPlugins();

        if (servicePlugins == null) {
            return;
        }

        for (Iterator it=servicePlugins.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            String type = (String)entry.getKey();
            String name = (String)entry.getValue();

            GenericServiceQuery query;
            if (name == null) {
                query = new GenericServiceQuery();
            }
            else {
                try {
                    Class plugin = Class.forName(name);
                    query = (GenericServiceQuery)plugin.newInstance();
                } catch (Exception e) {
                    log.error("Creating " + name + ": " + e, e);
                    continue;
                }
            }

            query.setType(type);
            query.setParent(this);
            try {
                findServices(mServer, query);
            } catch (IllegalArgumentException e) {
                String msg =
                    "Error running query for " + type + ": " +
                    e.getMessage();
                e.printStackTrace();
                System.out.println(msg);
            }
        }
    }

    private void findServices(RMIAdaptor mServer, ServiceQuery query)
        throws PluginException {

        query.initialize();

        Set services;
        ObjectName name;
        String mbeanClass = null;
        boolean isDebug = log.isDebugEnabled();

        try {
            name = new ObjectName(query.getQueryName());
            services = mServer.queryNames(name, null);
        } catch (MalformedObjectNameException e) {
            String msg = query.getQueryName() + ": " + e.getMessage();
            throw new IllegalArgumentException(msg);
        } catch (RemoteException e) {
            throw new PluginException("Cannot connect to JBoss", e);
        } catch (IOException e) {
            throw new PluginException("Cannot connecto to JBoss", e);
        }

        if (query instanceof GenericServiceQuery) {
            mbeanClass =
                ((GenericServiceQuery)query).getMBeanClass();
        }

        for (Iterator it=services.iterator(); it.hasNext();) {
            name = (ObjectName)it.next();
            if (!query.apply(name)) {
                continue;
            }

            if (mbeanClass != null) {
                try {
                    MBeanInfo info = mServer.getMBeanInfo(name);
                    if (!info.getClassName().matches(mbeanClass)) {
                        if (isDebug) {
                            log.debug("[" + name + "] " + info.getClassName() +
                                      " !instanceof " + mbeanClass);
                        }
                        continue;
                    }
                } catch (Exception e) {
                    log.error("mServer.getMBeanInfo(" + name + "): " + e);
                    continue;
                }
            }

            ServiceQuery service = query.cloneInstance();
            service.setObjectName(name);

            this.services.add(service);
            service.setParent(this);
            service.setServerQuery(this);
            service.getAttributes(mServer);
        }
    }

    public String getQualifiedName() {
        String fqdn =
            getServerDetector().getConfig(ProductPlugin.PROP_PLATFORM_FQDN);
        return
            fqdn + " " +
            getResourceType()   + " " +
            getName();
    }

    public String getResourceType() {
        return getServerDetector().getTypeInfo().getName();
    }

    public String getIdentifier() {
        return getInstallPath();
    }

    public Properties getControlConfig() {
        Properties config = new Properties();
        config.put(JBossServerControlPlugin.PROP_CONFIGSET,
                   getName());
        return config;
    }

    public String getInstallPath() {
        return this.installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

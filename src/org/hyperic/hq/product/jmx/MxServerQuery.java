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

package org.hyperic.hq.product.jmx;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.StringMatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MxServerQuery extends MxQuery {

    private static final Log log =
        LogFactory.getLog(MxServerQuery.class);

    private String installPath;
    private String version;
    private List services = new ArrayList();

    public MxServerQuery(ServerDetector detector) {
        setServerDetector(detector);
    }

    public List getServiceQueries() {
        return this.services;
    }

    public void findServices(MBeanServerConnection mServer)
        throws PluginException {

        Map servicePlugins = getServerDetector().getServiceInventoryPlugins();

        if (servicePlugins == null) {
            return;
        }

        for (Iterator it=servicePlugins.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            String type = (String)entry.getKey();
            String name = (String)entry.getValue();

            MxObjectNameQuery query;
            if (name == null) {
                query = new MxObjectNameQuery();
            }
            else {
                try {
                    Class plugin = Class.forName(name);
                    query = (MxObjectNameQuery)plugin.newInstance();
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

    private void findServices(MBeanServerConnection mServer, MxServiceQuery query)
        throws PluginException {

        boolean isDebug = log.isDebugEnabled();

        query.initialize();

        Set services;
        ObjectName name;
        String mbeanClass = query.getMBeanClass();

        String filter = query.getObjectNameFilter();
        StringMatcher matcher = null;
        if (filter != null) {
            matcher = new StringMatcher();
            if (filter.charAt(0) == '!') {
                matcher.setExcludes(filter.substring(1));
            }
            else {
                matcher.setIncludes(filter);
            }
        }

        try {
            name = new ObjectName(query.getQueryName());
            services = mServer.queryNames(name, null);
        } catch (MalformedObjectNameException e) {
            String msg = query.getQueryName() + ": " + e.getMessage();
            throw new IllegalArgumentException(msg);
        } catch (RemoteException e) {
            throw new PluginException("Cannot connect to server", e);
        } catch (IOException e) {
            throw new PluginException("Cannot connect to server", e);
        }

        for (Iterator it=services.iterator(); it.hasNext();) {
            name = (ObjectName)it.next();

            if ((matcher != null) &&
                !matcher.matches(name.toString()))
            {
                if (isDebug) {
                    log.debug("[" + name + "] !matches(" + matcher + ")");
                }
                continue;
            }

            if (!query.apply(name)) {
                continue;
            }

            if (mbeanClass != null) {
                try {
                    MBeanInfo info = mServer.getMBeanInfo(name);
                    if (!mbeanClass.equals(info.getClassName())) {
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

            MxServiceQuery service = query.cloneInstance();
            service.setObjectName(name);

            this.services.add(service);
            service.setParent(this);
            service.setServerQuery(this);
            service.getAttributes(mServer);
        }
    }

    public String getQualifiedName() {
        String name =
            ServerDetector.getPlatformName() + " " +
            getResourceType();

        if (getName() != null) {
            name += " " + getName();
        }

        return name;
    }

    public String getResourceType() {
        return getServerDetector().getTypeInfo().getName();
    }

    public String getIdentifier() {
        return getInstallPath();
    }

    public Properties getControlConfig() {
        Properties config = new Properties();
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

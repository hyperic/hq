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

package org.hyperic.hq.plugin.weblogic.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicUtil;

public class WeblogicDiscover {

    private static Log log = LogFactory.getLog("WeblogicDiscover");

    private String adminName = null;
    private String domain = null;
    private Properties props;

    private HashMap mbeanServers = new HashMap();
    private HashMap internalApps = new HashMap();

    private final WeblogicQuery[] SERVICE_QUERIES = {
        new ApplicationQuery(), //has webapp and ejb kids
        new JDBCPoolQuery(),
        new ExqQuery(),
        new JMSServerQuery(), //has JMSDestination kid
        new JTAResourceQuery(),
    };

    public WeblogicDiscover(Properties props) {
        this.props = props;
    }

    public WeblogicDiscover(String adminURL,
                            String username,
                            String password) {

        this.props = getProperties(adminURL, username, password);
    }

    public static Log getLog() {
        return log;
    }

    private Properties getProperties(String adminURL,
                                     String username,
                                     String password) {
        Properties props = new Properties();

        props.put(WeblogicMetric.PROP_ADMIN_URL,
                  adminURL);
        props.put(WeblogicMetric.PROP_ADMIN_USERNAME,
                  username);
        props.put(WeblogicMetric.PROP_ADMIN_PASSWORD,
                  password);

        return props;
    }

    public boolean isAdminSSL() {
        return getAdminURL().startsWith(ServerQuery.PROTOCOL_T3S);
    }

    public String getAdminURL() {
        return getAdminURL(this.props);
    }

    public String getUsername() {
        return getUsername(this.props);
    }

    public String getPassword() {
        return getPassword(this.props);
    }

    public String getAdminURL(Properties props) {
        return props.getProperty(WeblogicMetric.PROP_ADMIN_URL);
    }

    public String getUsername(Properties props) {
        return props.getProperty(WeblogicMetric.PROP_ADMIN_USERNAME);
    }

    public String getPassword(Properties props) {
        return props.getProperty(WeblogicMetric.PROP_ADMIN_PASSWORD, "");
    }

    public String getDomain() {
        return this.domain;
    }

    public String getAdminName() {
        return this.adminName;
    }

    public MBeanServer getMBeanServer()
        throws WeblogicDiscoverException {
        return getMBeanServer(this.props);
    }

    public MBeanServer getMBeanServer(String url)
        throws WeblogicDiscoverException {

        return getMBeanServer(url, getUsername(),  getPassword());
    }

    public MBeanServer getMBeanServer(String url, String user, String pass) 
        throws WeblogicDiscoverException {
        return getMBeanServer(getProperties(url, user, pass));
    }

    public MBeanServer getMBeanServer(Properties props) 
        throws WeblogicDiscoverException {

        String url = getAdminURL(props);

        MBeanServer server = (MBeanServer)mbeanServers.get(url);

        if (server != null) {
            return server;
        }

        try {
            server = WeblogicUtil.getMBeanServer(props);
        } catch (Exception e) {
            //WeblogicUtil fixes up the exception messages
            throw new WeblogicDiscoverException(e.getMessage(), e);
        }

        mbeanServers.put(url, server);

        return server;
    }

    public void find(MBeanServer mServer,
                     WeblogicQuery query,
                     List types)
        throws WeblogicDiscoverException {

        ObjectName scope;

        try {
            scope = new ObjectName(getDomain() + ":" +
                                   query.getScope() + ",*");
        } catch (MalformedObjectNameException e) {
            //wont happen
            throw new IllegalArgumentException(e.getMessage());
        }

        for (Iterator it = mServer.queryNames(scope, null).iterator();
             it.hasNext();) 
        {
            ObjectName obj = (ObjectName)it.next();
            String name = obj.getKeyProperty("Name");
            if (name.startsWith("__") || //e.g. __weblogic_admin_rmi_queue
               (name.indexOf("uuid-") != -1)) //wierdo 9.1 stuff
            {
                continue;
            }

            WeblogicQuery type = query.cloneInstance();

            if (type.getAttributes(mServer, obj)) {
                types.add(type);
            }
            else {
                continue;
            }

            WeblogicQuery[] childQueries = query.getChildQueries();

            for (int i=0; i<childQueries.length; i++) {
                WeblogicQuery childQuery = childQueries[i];
                childQuery.setParent(type);
                childQuery.setVersion(type.getVersion());
                find(mServer, childQuery, types);
            }
        }
    }

    public void init(MBeanServer mServer) 
        throws WeblogicDiscoverException {
        try {
            discoverInit(mServer);
        } catch (Exception e) {
            //XXX there are a handful of possible exceptions
            //most of which will never happen.
            throw new WeblogicDiscoverException(e.getMessage(), e);
        }
    }

    private void discoverInit(MBeanServer mServer)
        throws Exception {
        
        //only exists on the admin server
        final String scope = "*:Type=ApplicationConfig,*";

        for (Iterator it=mServer.queryNames(new ObjectName(scope), null).iterator();
             it.hasNext();)
        {
            ObjectName oName = (ObjectName)it.next();
            if (this.domain == null) {
                this.domain = oName.getDomain();
            }
            if (this.adminName == null) {
                this.adminName = oName.getKeyProperty("Location");
            }

            String name = oName.getKeyProperty("Name"); 

            //special case for console so we can control it
            if (name.equals("console")) {
                continue;
            }

            boolean isInternal =
                ((Boolean)mServer.getAttribute(oName, "InternalApp")).booleanValue();

            if (isInternal) {
                this.internalApps.put(name, Boolean.TRUE);
            }
        }
    }

    public boolean isInternalApp(String name) {
        return this.internalApps.get(name) == Boolean.TRUE;
    }

    public WeblogicQuery[] getServiceQueries() {
        return SERVICE_QUERIES;
    }
}

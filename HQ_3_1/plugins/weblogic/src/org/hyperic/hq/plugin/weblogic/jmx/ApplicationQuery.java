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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class ApplicationQuery extends ServiceQuery {

    public static final String MBEAN_TYPE = "ApplicationRuntime";

    public static final String MBEAN_TYPE_61 = "ApplicationConfig";

    private static final String ATTR_EAR = "EAR";

    private static final String[] ATTRS = {
        "Path", ATTR_NOTES,
    };

    private static final String[] EAR_ATTRS = { ATTR_EAR };

    private static final WeblogicQuery[] COMPONENTS = {
        new EntityBeanQuery(),
        new MessageDrivenBeanQuery(),
        new StatelessBeanQuery(),
        new StatefulBeanQuery(),
        new WebAppComponentQuery(),
    };

    private String mbeanName;

    public String[] getAttributeNames() {
        return ATTRS;
    }

    public WeblogicQuery[] getChildQueries() {
        return COMPONENTS;
    }

    public String getMBeanType() {
        if (isServer61()) {
            return MBEAN_TYPE_61;
        }
        else {
            return MBEAN_TYPE;
        }
    }

    public String getMBeanNameProperty() {
        return this.mbeanName;
    }

    public String getResourceType() {
        return WeblogicProductPlugin.APP_NAME;
    }

    public String getPropertyName() {
        return WeblogicMetric.PROP_APP;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {

        String appName = name.getKeyProperty("Name");

        ServerQuery server = (ServerQuery)getParent();
        String serverName = server.getName();

        //for later use in getScope to find children
        this.mbeanName = appName;

        if (appName.startsWith(serverName)) {
            appName = appName.substring(serverName.length() + 1);
        }

        if (server.getDiscover().isInternalApp(appName)) {
            return false;
        }

        if (isServer91()) {
            super.getAttributes(mServer, name, EAR_ATTRS);
            if ("false".equals(getAttribute(ATTR_EAR))) {
                //internal stuff and wierdo data source containers
                return false;
            }
        }
        
        setName(appName);

        String appMBeanName =
            name.getDomain() + ":" + 
            "Name=" + appName + "," +
            "Type=Application";

        ObjectName appMBean;
        try {
            appMBean = new ObjectName(appMBeanName);
        } catch (MalformedObjectNameException e) {
            //notgonnahappen
            WeblogicDiscover.getLog().error(e.getMessage(), e);
            return true;
        }

        super.getAttributes(mServer, appMBean, ATTRS);
        
        return true;
    }
}

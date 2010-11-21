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
import javax.management.AttributeNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class ApplicationQuery extends ServiceQuery {

    private static final Log log = LogFactory.getLog(ApplicationQuery.class);

    public static final String MBEAN_TYPE = "ApplicationRuntime";
    public static final String MBEAN_TYPE_61 = "ApplicationConfig";

    private static final String ATTR_EAR = "EAR";
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
        return EAR_ATTRS;
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

    public boolean isEAR(){
        return "true".equalsIgnoreCase(getAttribute(ATTR_EAR));
    }

    public boolean getAttributes(MBeanServer mServer,
            ObjectName name) {
        String appName = name.getKeyProperty("Name");

        ServerQuery server = (ServerQuery)getParent();
        String serverName = server.getName();

        //for later use in getScope to find children
        this.mbeanName = appName;

        if ((appName.startsWith(serverName)) && (appName.length() > (serverName.length() + 1))) {
            appName = appName.substring(serverName.length() + 1);
        }

        if ((appName.startsWith(serverName)) && (appName.length() > serverName.length())) {
            appName = appName.substring(serverName.length());
        }
        log.debug("[getAttributes] mbeanName = '" + this.mbeanName + "' => '" + appName+"'");

        if (server.getDiscover().isInternalApp(appName)) {
            log.debug(appName+" is a internal Application");
            return false;
        }

        setName(appName);
        super.getAttributes(mServer, name);

        return true;
    }
}

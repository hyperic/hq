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

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class JDBCDataSourceQuery extends ServiceQuery {
    public static final String MBEAN_TYPE = "JDBCDataSourceRuntime";

    private static final String[] CONFIG_ATTRS = {
        "VersionJDBCDriver", "DatabaseProductName", "DatabaseProductVersion",
        //XXX ATTR_NOTES
    };
    
    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    public String getResourceType() {
        return WeblogicProductPlugin.JDBC_DS_NAME;
    }

    public String getPropertyName() {
        return WeblogicMetric.PROP_JDBC_CONN;
    }


    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {
        //Avoiding other version to discover since jdbc connections 
    	//will be discoverd through jdbc connection pool service
        /*super.getAttributes(mServer, name);

        super.getAttributes(mServer, name, CONFIG_ATTRS);

        String app = name.getKeyProperty("ApplicationRuntime");
        if (app != null) {
            this.attrs.put(WeblogicMetric.PROP_APP, app);
        }*/

        return false;
    }
    
    public boolean getAttributes(MBeanServerConnection mServer,
            	ObjectName name) {

    	super.getAttributes(mServer, name);

    	super.getAttributes(mServer, name, CONFIG_ATTRS);

    	String app = name.getKeyProperty("ApplicationRuntime");
    	if (app != null) {
    		this.attrs.put(WeblogicMetric.PROP_APP, app);
    	}

    	return true;
    }

    public String[] getCustomPropertiesNames() {
        return CONFIG_ATTRS;
    }

    public Properties getResourceConfig() {
        Properties props = super.getResourceConfig();
        String app = this.getAttribute(WeblogicMetric.PROP_APP);
        if (app != null) {
            props.setProperty(WeblogicMetric.PROP_APP, app);
        }
        return props;
    }
}

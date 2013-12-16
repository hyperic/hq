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
package org.hyperic.hq.plugin.websphere.jmx;

import java.util.Properties;
import javax.management.ObjectName;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

public class JDBCProviderQuery extends WebSphereQuery {

    public static final String MBEAN_TYPE = "JDBCProvider";

    @Override
    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    @Override
    public String getResourceType() {
        return WebsphereProductPlugin.CONNPOOL_NAME;
    }

    @Override
    public void configure(Properties props) {
        String id = getObjectName().getKeyProperty("mbeanIdentifier");
        props.setProperty("mbeanIdentifier", id);
    }

    @Override
    public String[] getAttributeNames() {
        return new String[]{
                    "implementationClassName"
                };
    }

    @Override
    public String getFullName() {
        ObjectName oname = getObjectName();
        String server = oname.getKeyProperty("Server");
        String node = oname.getKeyProperty("node");
        String id = oname.getKeyProperty("mbeanIdentifier");
        String name = oname.getKeyProperty("name");
        return (id.contains("/" + node + "/") ? node + " " : "")
                + (id.contains("/" + server + "/") ? server + " " : "")
                + name;//+ " (" + super.getFullName() + ")";
    }
}
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

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class JTAResourceQuery extends ServiceQuery {
    public static final String MBEAN_TYPE = "TransactionResourceRuntime";

    private static final String PREFIX = "JTAResourceRuntime_";

    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    public String getResourceType() {
        return WeblogicProductPlugin.JTA_RES_NAME;
    }

    public String getPropertyName() {
        return WeblogicMetric.PROP_JTA_RES;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {

        String resource = name.getKeyProperty("Name");
        if (resource.startsWith(PREFIX)) {
            resource = resource.substring(PREFIX.length());
        }
        setName(resource);

        return true;
    }

    public boolean hasControl() {
        return false;
    }
}

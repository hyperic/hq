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
import javax.management.ObjectName;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;

public abstract class EJBComponentQuery extends ComponentQuery {
    public static final String MBEAN_TYPE = "EJBComponentRuntime";

    private String component;

    public abstract String getMBeanType();

    public abstract String getResourceType();

    public String getPropertyName() {
        return WeblogicMetric.PROP_EJB;
    }

    protected String getNamePrefix() {
        return this.ejbPrefix;
    }

    public boolean getAttributes(MBeanServer mServer, ObjectName name) {
        this.component = name.getKeyProperty(MBEAN_TYPE);
        return super.getAttributes(mServer, name);
    }

    public Properties getResourceConfig() {
        Properties props = super.getResourceConfig();
        if (this.component != null) {
            props.setProperty(WeblogicMetric.PROP_EJB_COMPONENT,
                              this.component);
        }
        return props;
    }
}

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
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

public class ThreadPoolQuery extends WebSphereQuery {

    public static final String MBEAN_TYPE = "ThreadPool";

    @Override
    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    @Override
    public String getResourceType() {
        return WebsphereProductPlugin.THRPOOL_NAME;
    }

    @Override
    public String getPropertyName() {
        return WebsphereProductPlugin.PROP_THRPOOL_NAME;
    }

    @Override
    public String[] getAttributeNames() {
        return new String[]{"maximumSize", "minimumSize"};
    }

    private String getMbeanIdentifier() {
        return getObjectName().getKeyProperty("mbeanIdentifier");
    }

    @Override
    public void configure(Properties props) {
        super.configure(props);
        props.setProperty("mbeanIdentifier", getMbeanIdentifier());
    }

    @Override
    public boolean apply() {
        String v = getObjectName().getKeyProperty("version");
        if (v.startsWith("8.5")) {
            if ("null".equalsIgnoreCase(getMbeanIdentifier())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getFullName() {
        String name = super.getName();
        return name;
    }
}

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

public class ComponentQuery extends ChildServiceQuery {

    protected String ejbPrefix = null;
    protected String ejbPostfix = null;
    protected String webappPrefix = null;
    private String serverName;

    protected String getNamePrefix() {
        return null;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {

        //we could get these values using mServer.getAttribute to dig
        //out the ComponentName attribute, but this is much lighter
        //to avoid the trips.
        if (this.ejbPrefix == null) {
            serverName = getParent().getParent().getName();

            this.ejbPostfix = "_" + getParent().getName();

            this.ejbPrefix = serverName + this.ejbPostfix + "_";
        }

        if (this.webappPrefix == null) {
            this.webappPrefix =
                serverName + "_" + this.ejbPrefix;
        }

        String prefix = getNamePrefix();
        String component = name.getKeyProperty("Name");
        if (component.startsWith(prefix)) {
            component = component.substring(prefix.length());
        }
        else if (isServer61()) {
            //6.1 does not include parent in the ObjectName
            //so we have to determine the parent by other means.
            if (prefix == this.webappPrefix) {
                return false;
            }
            else {
                //Name=%ejb%_%application%
                if (!component.endsWith(this.ejbPostfix)) {
                    return false;
                }
                int len = component.length() - this.ejbPostfix.length();
                component = component.substring(0, len);
            }
        }

        setName(component);

        return true;
    }

    public boolean skipParentScope() {
        if (isServer61()) {
            return true;
        }

        return false;
    }
}

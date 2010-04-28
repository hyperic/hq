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

package org.hyperic.hq.plugin.jboss.jmx;

import java.util.Properties;

import javax.management.ObjectName;

import org.hyperic.hq.plugin.jboss.JBossProductPlugin;

public abstract class BeanQuery extends ServiceQuery {

    private static final String PROP_EJB_JAR  = "ejb.jar";
    private static final String PROP_J2EE_APP = "j2ee.application";

    public String getQueryName() {
        return
            "jboss.management.local:j2eeType=" +
            getBeanQueryName() + ",*";
    }

    protected abstract String getBeanQueryName();
    
    protected String getPropertyName() {
        return "ejb.name";
    }
    
    public Properties getResourceConfig() {
        Properties props = super.getResourceConfig();

        props.setProperty(PROP_EJB_JAR,
                         this.objectName.getKeyProperty("EJBModule"));
        props.setProperty(PROP_J2EE_APP,
                         this.objectName.getKeyProperty("J2EEApplication"));
        
        return props;
    }

    public boolean hasControl() {
        return false;
    }

    protected boolean applyWithoutHashCode(ObjectName name) {
        String jndiName = name.getKeyProperty("name");

        if (containsHashCode(jndiName)) {
            return false;
        }
        else {
            return super.apply(name);
        }
    }

    protected boolean containsHashCode(String name) {
        if (JBossProductPlugin.ignoreHashCodes()) {
            return false;
        }

        int ix = name.indexOf('@');

        if (ix == -1) {
            return false;
        }
        //could probably just assume this is a hashCode
        //at this point, but we'll go the distance.
        String hashCode = name.substring(ix);
        for (int i=1; i<hashCode.length(); i++) {
            if (!Character.isDigit(hashCode.charAt(i))) {
                return false;
            }
        }
        //this bean has not configured <local-jndi-name>
        //and the name is generated w/ a hashCode that will
        //change when the server is restarted, skip it.
        return true;
    }
}

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

package org.hyperic.hq.events;

import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.ext.PropertyNotFoundException;

/**
 * This is not an interface to be implemented.  This contains the loaders for
 * the alert configurations.
 *
 */
public interface AlertConfigLoader {
    static ConfigClassesInitializer triggersInitializer =
        new ConfigClassesInitializer("triggers");
    
    static ConfigClassesInitializer actionsInitializer =
        new ConfigClassesInitializer("actions");

    class ConfigClassesInitializer {
        Log log = LogFactory.getLog(ConfigClassesInitializer.class);

        public ConfigClassesInitializer(String prop) {
            try {
                MBeanServer mServer = (MBeanServer) MBeanServerFactory
                        .findMBeanServer(null).iterator().next();
                
                ObjectName propName = new ObjectName(
                        "jboss:type=Service,name=AlertDefinitionsProperties");
                
                Object obj = mServer.invoke(
                                propName, "get", 
                                new Object[] { prop, null },
                                new String[] { String.class.getName(),
                                               String.class.getName() });
    
                if (obj == null) {
                    throw new PropertyNotFoundException(prop +
                                                        " list not found");
                }
    
                log.info(prop + " list: " + obj);
                
                StringTokenizer tok = new StringTokenizer((String) obj, ", ");
                while (tok.hasMoreTokens()) {
                    String className = tok.nextToken();
                    if (log.isDebugEnabled())
                        log.debug("Initialize class: " + className);
                    
                    try {
                        Class classObj = Class.forName(className);
                        classObj.newInstance();
                    } catch (ClassNotFoundException e) {
                        log.error("Class: " + className + " not found");
                    } catch (InstantiationException e) {
                        log.error("Error instantiating class: " + className);
                    } catch (IllegalAccessException e) {
                        log.error("Error instantiating class: " + className);
                    }
                }
            } catch (Exception e) {
                // Swallow all exceptions
                log.error("Encountered error initializing " + prop, e);
            }
        }
    }
}

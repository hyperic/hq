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

package org.hyperic.hq.events.server.session;

import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.PropertyNotFoundException;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;

public class EventsStartupListener 
    implements StartupListener
{
    private static final Log _log = 
        LogFactory.getLog(EventsStartupListener.class);
    private static final Object LOCK = new Object();
    private static TriggerChangeCallback _changeCallback;
    
    public void hqStarted() {
        // Make sure the escalation enumeration is loaded and registered so 
        // that the escalations run
        ClassicEscalationAlertType.class.getClass();
        AlertableRoleCalendarType.class.getClass();
        
        HQApp app = HQApp.getInstance();

        synchronized (LOCK) {
            _changeCallback = (TriggerChangeCallback)
                app.registerCallbackCaller(TriggerChangeCallback.class);
        }

        loadConfigProps("triggers");
        loadConfigProps("actions");
    }
    
    private void loadConfigProps(String prop) {
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

            _log.info(prop + " list: " + obj);
            
            StringTokenizer tok = new StringTokenizer((String) obj, ", ");
            while (tok.hasMoreTokens()) {
                String className = tok.nextToken();
                    _log.debug("Initialize class: " + className);
                
                try {
                    Class classObj = Class.forName(className);
                    classObj.newInstance();
                } catch (ClassNotFoundException e) {
                    _log.error("Class: " + className + " not found");
                } catch (InstantiationException e) {
                    _log.error("Error instantiating class: " + className);
                } catch (IllegalAccessException e) {
                    _log.error("Error instantiating class: " + className);
                }
            }
        } catch (Exception e) {
            // Swallow all exceptions
            _log.error("Encountered error initializing " + prop, e);
        }
    }
    
    static TriggerChangeCallback getChangedTriggerCallback() {
        synchronized (LOCK) {
            return _changeCallback;
        }
    }
}

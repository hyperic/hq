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

/*
 * Created on Jul 30, 2003
 *
 * RegisteredTriggerNotifier.java
 */
package org.hyperic.hq.events.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggerEvent;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

/**
 *
 * Utility class for handling notification of registered triggers changes
 */
public class RegisteredTriggerNotifier {
    private static Log log =
        LogFactory.getLog(RegisteredTriggerNotifier.class.getName());
    
    private static Messenger sender = new Messenger();
    private static Broadcaster broadcaster = null;

    public static void notify(int action, RegisteredTriggerValue val) {
        RegisteredTriggerEvent event = new RegisteredTriggerEvent(action, val);
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }
    
    public static void broadcast(int action, RegisteredTriggerValue val) {
        broadcast(action, new RegisteredTriggerValue[] { val });
    }

    public static void broadcast(int action, RegisteredTriggerValue[] vals) {
        if (broadcaster == null) {
            broadcaster = (Broadcaster) ProductProperties
            .   getPropertyInstance("hyperic.hq.events.broadcaster");
                
            
            if (broadcaster == null)
                broadcaster = new Broadcaster() {
                    public void broadcast(int action,
                                          RegisteredTriggerValue[] vals) {
                        for (int i = 0; i < vals.length; i++)
                            RegisteredTriggerNotifier.notify(action, vals[i]);
                    }
                };
        }
        
        broadcaster.broadcast(action, vals);
    }
    
    public interface Broadcaster {
        public void broadcast(int action, RegisteredTriggerValue[] vals);
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;

public class TriggerFiredEvent extends AbstractEvent
    implements java.io.Serializable {

    private static final long serialVersionUID = -2813854367589382845L;

    /** Holds value of property value. */
    private AbstractEvent[] events;
    
    /** Holds value of property message. */
    private String message;
    
    /** Creates a new instance of TriggerFiredEvent */
    public TriggerFiredEvent(Integer instanceId, AbstractEvent event) {
        setInstanceId(instanceId);
        setEvents(new AbstractEvent[] { event });
    }
    
    /** Creates a new instance of TriggerFiredEvent */
    public TriggerFiredEvent(Integer instanceId, AbstractEvent[] events) {
        setInstanceId(instanceId);
        setEvents(events);
    }
    
    /** Getter for property events.
     * @return Value of property events.
     *
     */
    public AbstractEvent[] getEvents() {
        return this.events;
    }
    
    /** Setter for property events.
     * @param events New value of property events.
     *
     */
    public void setEvents(AbstractEvent[] events) {
        this.events = events;
    }
    
    /** Traverse down nested events to a non TriggerFired event
     * @return The root event which caused the TriggerFiredEvent
     */

    public TriggerFiredEvent[] getRootEvents() {
        if (events.length == 1 && !(events[0] instanceof TriggerFiredEvent)) {
            return new TriggerFiredEvent[] { this };
        }
        else {
            // Go through the events and create an list of TriggerFiredEvents
            ArrayList list = new ArrayList();
            for (int i = 0; i < events.length; i++) {
                // Theoretically, they should all be TriggerFiredEvents
                if (events[i] instanceof TriggerFiredEvent) {
                    TriggerFiredEvent tfe = (TriggerFiredEvent) events[i];
                    list.addAll(Arrays.asList(tfe.getRootEvents()));
                }
                else {  // Not a valid case, but let's be safe
                    list.add(new TriggerFiredEvent(getInstanceId(), events[i]));
                }
            }

            // Now return the list as array
            return (TriggerFiredEvent[])
                list.toArray(new TriggerFiredEvent[list.size()]);
        }
    }

    /** Getter for property message.
     * @return Value of property message.
     *
     */
    public String getMessage() {
        return this.message;
    }
    
    /** Setter for property message.
     * @param message New value of property message.
     *
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        long timestamp = 0;
        for (int i = 0; i < events.length; i++) {
            timestamp = Math.max(timestamp, events[i].getTimestamp());
        }
        return timestamp;
    }

    public String toString() {
        if (events.length == 1) {
            return events[0].toString();
        }
        else if (this.message != null) {
            return this.message;
        }
        else {
            return super.toString();
        }
    }
}

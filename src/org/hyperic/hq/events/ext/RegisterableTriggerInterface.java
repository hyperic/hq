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

package org.hyperic.hq.events.ext;

import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigSchema;

/**
 * An interface for triggers which can be dispatched by the 
 * RegisteredDispatcher.  
 */

public interface RegisterableTriggerInterface {
    
    /**
     * Initialize the trigger with a value object.
     *
     * @param tval  Configuration data for the trigger
     * @throws InvalidTriggerDataException if the configuration data is invalid.
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException;

    /**
     * Get the event classes that the trigger is interested in
     * seeing.  This is an optimization, so that a trigger's
     * processEvent() method is called only when a valid event
     * occurs.
     *
     * @return an array of Class objects which implement
     *          the 'Event' interface
     */
    public Class[] getInterestedEventTypes();

    /**
     * Get a list of instance IDs specific to a class (as returned
     * by getInterestedEventTypes) which the trigger is interested
     * in seeing.  These values are specific to the event type, and
     * represent things such as specific measurements.
     *
     * @param c Class to get the interested event IDs for
     *
     * @return An array of integers representing the instance IDs
     *          for the specific event class
     */
    public Integer[] getInterestedInstanceIDs(Class c);
    
    public ConfigSchema getConfigSchema();
}

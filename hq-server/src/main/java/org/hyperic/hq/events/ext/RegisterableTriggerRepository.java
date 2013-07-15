/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.events.ext;

import java.util.Collection;

import org.hyperic.hq.events.AbstractEvent;

/**
 * Repository of in-memory representations of alert triggers
 * @author jhickey
 * 
 */
public interface RegisterableTriggerRepository {

    /**
     * 
     * @param trigger The trigger to add to the repository
     */
    void addTrigger(RegisterableTriggerInterface trigger);

    /**
     * 
     * @param eventClass The event class
     * @param instanceId The id of the source instance of the event
     * @return The {@link RegisterableTriggerInterface}s interested in the event
     */
    Collection<RegisterableTriggerInterface> getInterestedTriggers(AbstractEvent event,
                                                                   Integer instanceId);

    /**
     * 
     * @param triggerId The trigger ID
     * @return The {@link RegisterableTriggerInterface} with the specified ID or
     *         null if none exists
     */
    RegisterableTriggerInterface getTriggerById(Integer triggerId);

    /**
     * 
     * @param triggerId The trigger to remove from the repository
     */
    void removeTrigger(Integer triggerId);

    /**
     * Enables or disables triggers. Should be called when the alert definition
     * is enabled or disabled. Disabled triggers will not be returned from calls
     * to getInterestedTriggers, and therefore will not receive events from the
     * RegisteredDispatcher
     * @param triggerIds The triggers that should be enabled or disabled
     * @param enabled true if triggers should be enabled
     */
    void setTriggersEnabled(Collection<Integer> triggerIds, boolean enabled);

    /**
     * Initialize the contents of the repository
     */
    void init();

}

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
package org.hyperic.hq.events.shared;

import java.util.Collection;
import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.server.session.TriggersCreatedZevent;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Local interface for RegisteredTriggerManager.
 */
public interface RegisteredTriggerManager {
    /**
     * Processes {@link TriggerCreatedEvent}s that indicate that triggers should
     * be created
     */
    public void handleTriggerCreatedEvents(Collection<TriggersCreatedZevent> events);

    /**
     * Initialize the in-memory triggers and update the RegisteredTriggers
     * repository
     */
    public void initializeTriggers();

    /**
     * Enable or disable triggers associated with an alert definition
     */
    public void setAlertDefinitionTriggersEnabled(Integer alertDefId, boolean enabled);
    
    void setAlertDefinitionTriggersEnabled(List<Integer> alertDefIds, boolean enabled);

    /**
     * Finds a trigger by its ID, assuming existence
     * @param id The trigger ID
     * @return The trigger with the specified ID (exception will occur if
     *         trigger does not exist)
     */
    public RegisteredTrigger findById(Integer id);

    /**
     * Create a new trigger
     * @return a RegisteredTriggerValue
     */
    public RegisteredTrigger createTrigger(RegisteredTriggerValue val);

    /**
     * Create new triggers
     * @return a RegisteredTriggerValue
     */
    public void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef) throws TriggerCreateException,
        InvalidOptionException, InvalidOptionValueException;
    
    /**
     * Create new triggers
     *
     * @param subject The user creating the trigger
     * @param alertdef The alert definition value object
     * @param addTxListener Indicates whether a TriggersCreatedListener should be added.
     *                      The default value is true. HHQ-3423: To improve performance when
     *                      creating resource type alert definitions, this should be set to false.
     *                      If false, it is the caller's responsibility to call
     *                      addTriggersCreatedListener() to ensure triggers are registered.
     *  
     * 
     *
     * 
     */
    void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef, boolean addTxListener) 
    throws TriggerCreateException, InvalidOptionException, InvalidOptionValueException;

    /**
     * Completely deletes all triggers when an alert definition is deleted
     */
    public void deleteTriggers(AlertDefinition alertDef);
    
    /**
     * Delete all triggers for an alert definition.
     *
     * @param adId The alert definition id
     * 
     * 
     */
    public void deleteTriggers(Integer adId);
    
    void addTriggersCreatedTxListener(final List triggersOrEvents);
    
}

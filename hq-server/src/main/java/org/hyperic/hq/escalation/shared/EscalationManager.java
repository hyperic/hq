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
package org.hyperic.hq.escalation.shared;

import java.util.Collection;
import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAction;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.ActionConfigInterface;

/**
 * Local interface for EscalationManager.
 */
public interface EscalationManager {
    /**
     * Create a new escalation chain
     * 
     * @see Escalation for information on fields
     */
    public Escalation createEscalation(String name, String description, boolean pauseAllowed, long maxWaitTime,
                                       boolean notifyAll, boolean repeat) throws DuplicateObjectException;

    public EscalationState findEscalationState(PerformsEscalations def);

    /**
     * Update an escalation chain
     * 
     * @see Escalation for information on fields
     */
    public void updateEscalation(AuthzSubject subject, Escalation esc, String name, String description,
                                 boolean pauseAllowed, long maxWaitTime, boolean notifyAll, boolean repeat)
        throws DuplicateObjectException, PermissionException;

    /**
     * Add an action to the end of an escalation chain. Any escalations
     * currently in progress using this chain will be canceled.
     */
    public void addAction(Escalation e, ActionConfigInterface cfg, long waitTime);

    /**
     * Remove an action from an escalation chain. Any escalations currently in
     * progress using this chain will be canceled.
     */
    public void removeAction(Escalation e, Integer actId);

    /**
     * Delete an escalation chain. This method will throw an exception if the
     * escalation chain is in use. TODO: Probably want to allow for the fact
     * that people DO want to delete while states exist.
     */
    public void deleteEscalation(AuthzSubject subject, Escalation e) throws PermissionException, ApplicationException;

    public Escalation findById(Integer id);

    public Escalation findById(AuthzSubject subject, Integer id) throws PermissionException;

    public Collection<Escalation> findAll(AuthzSubject subject) throws PermissionException;

    public Escalation findByName(AuthzSubject subject, String name) throws PermissionException;

    public Escalation findByName(String name);

    /**
     * Start an escalation. If the entity performing escalations does not have
     * an assigned escalation or if the escalation has already been started,
     * then this method call will be a no-op.
     * 
     * @param def The entity performing escalations.
     * @param creator Object which will create an {@link Escalatable} object if
     *        invoking this method actually starts an escalation.
     * @return <code>true</code> if the escalation is started;
     *         <code>false</code> if not because either there is no escalation
     *         assigned to the entity or the escalation is already in progress.
     */
    public boolean startEscalation(PerformsEscalations def, EscalatableCreator creator);

    public Escalatable getEscalatable(EscalationState s);

    /**
     * End an escalation. This will remove all state for the escalation tied to
     * a specific definition.
     */
    public void endEscalation(PerformsEscalations def);

    /**
     * Find an escalation based on the type and ID of the definition.
     * 
     * @return null if the definition defined by the ID does not have any
     *         escalation associated with it
     */
    public Escalation findByDefId(EscalationAlertType type, Integer defId);

    /**
     * Set the escalation for a given alert definition and type
     */
    public void setEscalation(EscalationAlertType type, Integer defId, Escalation escalation);

    /**
     * Acknowledge an alert, potentially sending out notifications.
     * 
     * @param subject Person who acknowledged the alert
     * @param pause TODO
     */
    public boolean acknowledgeAlert(AuthzSubject subject, EscalationAlertType type, Integer alertId, String moreInfo,
                                    long pause) throws PermissionException;

    /**
     * See if an alert is acknowledgeable
     * 
     * @return true if the alert is currently acknowledgeable
     */
    public boolean isAlertAcknowledgeable(Integer alertId, PerformsEscalations def);

    /**
     * Fix an alert for a an escalation if there is one currently running.
     * 
     * @return true if there was an alert to be fixed.
     */
    public boolean fixAlert(AuthzSubject subject, PerformsEscalations def, String moreInfo) throws PermissionException;

    /**
     * Fix an alert, potentially sending out notifications. The state of the
     * escalation will be terminated and the alert will be marked fixed.
     * 
     * @param subject Person who fixed the alert
     */
    public void fixAlert(AuthzSubject subject, EscalationAlertType type, Integer alertId, String moreInfo)
        throws PermissionException;

    /**
     * Fix an alert, potentially sending out notifications. The state of the
     * escalation will be terminated and the alert will be marked fixed.
     * 
     * @param subject Person who fixed the alert
     */
    public void fixAlert(AuthzSubject subject, EscalationAlertType type, Integer alertId, String moreInfo,
                         boolean suppressNotification) throws PermissionException;

    /**
     * Re-order the actions for an escalation. If there are any states
     * associated with the escalation, they will be cleared.
     * 
     * @param actions a list of {@link EscalationAction}s (already contained
     *        within the escalation) specifying the new order.
     */
    public void updateEscalationOrder(Escalation esc, List<EscalationAction> actions);

    /**
     * Get the # of active escalations within HQ inventory
     */
    public Number getActiveEscalationCount();

    /**
     * Get the # of escalations within HQ inventory
     */
    public Number getEscalationCount();

    public List<EscalationState> getActiveEscalations(int maxEscalations);

    public String getLastFix(PerformsEscalations def);

    /**
     * Called when subject is removed and therefore have to null out the
     * acknowledgedBy field
     */
    public void handleSubjectRemoval(AuthzSubject subject);

    public void startup();

    public Collection<EscalationState> getOrphanedEscalationStates();

    public void removeEscalationState(EscalationState e);

}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.escalation.server.session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.escalation.EscalationEvent;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertRegulator;
import org.hyperic.hq.events.server.session.AlertableRoleCalendarType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.shared.ActionManager;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EscalationManagerImpl implements EscalationManager {
    private final Log log = LogFactory.getLog(EscalationManagerImpl.class);

    private ActionManager actionManager;
    private AlertPermissionManager alertPermissionManager;

    private EscalationDAO escalationDAO;
    private EscalationStateDAO escalationStateDAO;
    private AlertRegulator alertRegulator;
    private EscalationRuntime escalationRuntime;
    private MessagePublisher messagePublisher;

    @Autowired
    public EscalationManagerImpl(ActionManager actionManager,
                                 AlertPermissionManager alertPermissionManager,
                                 EscalationDAO escalationDAO,
                                 EscalationStateDAO escalationStateDAO,
                                 MessagePublisher messagePublisher,
                                 EscalationRuntime escalationRuntime, AlertRegulator alertRegulator) {
        this.actionManager = actionManager;
        this.alertPermissionManager = alertPermissionManager;
        this.escalationDAO = escalationDAO;
        this.escalationStateDAO = escalationStateDAO;
        this.escalationRuntime = escalationRuntime;
        this.messagePublisher = messagePublisher;
        this.alertRegulator = alertRegulator;
    }

    @PostConstruct
    public void initialize() {
        // Make sure the escalation enumeration is loaded and registered so
        // that the escalations run
        ClassicEscalationAlertType.CLASSIC.toString();
    	GalertEscalationAlertType.GALERT.toString();
        AlertableRoleCalendarType.class.getClass();
    }

    private void assertEscalationNameIsUnique(String name) throws DuplicateObjectException {
        Escalation escalation;

        if ((escalation = escalationDAO.findByName(name)) != null) {
            throw new DuplicateObjectException("An escalation with that name " + "already exists",
                escalation);
        }
    }

    /**
     * Create a new escalation chain
     * 
     * @see Escalation for information on fields
     */
    public Escalation createEscalation(String name, String description, boolean pauseAllowed,
                                       long maxWaitTime, boolean notifyAll, boolean repeat)
        throws DuplicateObjectException {
        Escalation escalation;

        assertEscalationNameIsUnique(name);

        escalation = new Escalation(name, description, pauseAllowed, maxWaitTime, notifyAll, repeat);

        escalationDAO.save(escalation);

        return escalation;
    }

    @Transactional(readOnly = true)
    public EscalationState findEscalationState(PerformsEscalations def) {
        return escalationStateDAO.find(def);
    }

    /**
     * Update an escalation chain
     * 
     * @see Escalation for information on fields
     */
    public void updateEscalation(AuthzSubject subject, Escalation escalation, String name,
                                 String description, boolean pauseAllowed, long maxWaitTime,
                                 boolean notifyAll, boolean repeat)
        throws DuplicateObjectException, PermissionException {
        alertPermissionManager.canModifyEscalation(subject.getId());

        if (!escalation.getName().equals(name)) {
            assertEscalationNameIsUnique(name);
        }

        escalation.setName(name);
        escalation.setDescription(description);
        escalation.setPauseAllowed(pauseAllowed);
        escalation.setMaxPauseTime(maxWaitTime);
        escalation.setNotifyAll(notifyAll);
        escalation.setRepeat(repeat);
    }

    private void unscheduleEscalation(Escalation escalation) {
        Collection<EscalationState> escalationStates = escalationStateDAO.findStatesFor(escalation);

        // Unschedule any escalations currently in progress
        for (Iterator<EscalationState> i = escalationStates.iterator(); i.hasNext();) {
            escalationRuntime.endEscalation(i.next());
        }
    }

    /**
     * Add an action to the end of an escalation chain. Any escalations
     * currently in progress using this chain will be canceled.
     */
    public void addAction(Escalation escalation, ActionConfigInterface config, long waitTime) {
        Action action = actionManager.createAction(config);

        escalation.addAction(waitTime, action);
        unscheduleEscalation(escalation);
    }

    /**
     * Remove an action from an escalation chain. Any escalations currently in
     * progress using this chain will be canceled.
     */
    public void removeAction(Escalation escalation, Integer actionId) {
        // Iterate through the actions and find the one escalation action
        Action action = null;

        for (Iterator<EscalationAction> i = escalation.getActionsList().iterator(); i.hasNext();) {
            EscalationAction escalationAction = i.next();

            if (escalationAction.getAction().getId().equals(actionId)) {
                action = escalationAction.getAction();
                i.remove();

                break;
            }
        }

        if (action == null) {
            return;
        }

        unscheduleEscalation(escalation);
        actionManager.markActionDeleted(action);
    }

    /**
     * Delete an escalation chain. This method will throw an exception if the
     * escalation chain is in use.
     * 
     * TODO: Probably want to allow for the fact that people DO want to delete
     * while states exist.
     */
    public void deleteEscalation(AuthzSubject subject, Escalation escalation)
        throws PermissionException, ApplicationException {
        alertPermissionManager.canRemoveEscalation(subject.getId());

        List<EscalationAlertType> escalationAlertTypes = EscalationAlertType.getAll();

        for (Iterator<EscalationAlertType> i = escalationAlertTypes.iterator(); i.hasNext();) {
            EscalationAlertType escalationAlertType = i.next();

            if (escalationAlertType.escalationInUse(escalation)) {
                if (log.isDebugEnabled()) {
                    log.debug("Escalation [" + escalation.getId() + ", " + escalation.getName() +
                              "] in use by:");

                    Collection<PerformsEscalations> performers = escalationAlertType
                        .getPerformersOfEscalation(escalation);

                    for (Iterator<PerformsEscalations> j = performers.iterator(); j.hasNext();) {
                        PerformsEscalations alertDefinition = j.next();

                        log.debug("[" + alertDefinition.getName() + " id=" +
                                  alertDefinition.getId() + "]");
                    }
                }

                throw new ApplicationException("The escalation is currently " + "in use");
            }
        }

        escalationDAO.remove(escalation);
    }

    @Transactional(readOnly = true)
    public Escalation findById(Integer id) {
        return escalationDAO.findById(id);
    }

    @Transactional(readOnly = true)
    public Escalation findById(AuthzSubject subject, Integer id) throws PermissionException {
        return escalationDAO.findById(id);
    }

    @Transactional(readOnly = true)
    public Collection<Escalation> findAll(AuthzSubject subject) throws PermissionException {
        return escalationDAO.findAllOrderByName();
    }

    @Transactional(readOnly = true)
    public Escalation findByName(AuthzSubject subject, String name) throws PermissionException {
        return escalationDAO.findByName(name);
    }

    @Transactional(readOnly = true)
    public Escalation findByName(String name) {
        return escalationDAO.findByName(name);
    }

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
    public boolean startEscalation(PerformsEscalations alertDefinition, EscalatableCreator creator) {
        if (!alertRegulator.alertsAllowed()) {
            return false;
        }

        if (alertDefinition.getEscalation() == null) {
            return false;
        }

        boolean started = false;

        try {
            // HHQ-1395: It would be preferable to acquire the exclusive
            // lock until we schedule the escalation, but this may cause a
            // deadlock since creating the escalatable executes actions which
            // may take an arbitrary amount of time to execute.
            //
            // Assume we may throw an unchecked exception prior to scheduling.
            // This is possible, especially when creating the escalatable. If
            // this happens, make sure to clear the uncommitted escalation
            // state cache.
            escalationRuntime.acquireMutex();

            try {
                if (escalationStateExists(alertDefinition)) {
                    return started = false;
                }
            } finally {
                escalationRuntime.releaseMutex();
            }

            try {
                Escalatable alert = creator.createEscalatable();

                // HQ-1348: Recovery alerts are automatically fixed
                // so don't start escalation if the alert is fixed
                if (!alert.getAlertInfo().isFixed()) {
                    EscalationState escalationState = new EscalationState(alert);

                    escalationStateDAO.save(escalationState);
                    log.debug("Escalation started: state=" + escalationState.getId());
                    escalationRuntime.scheduleEscalation(escalationState);

                    started = true;
                }
            } catch (ResourceDeletedException e) {
                log.debug(e);
            } finally {
                if (!started) {
                    escalationRuntime.removeFromUncommittedEscalationStateCache(alertDefinition,
                        false);
                }
            }

        } catch (InterruptedException e) {
            log.error("Failed to start escalation for " + "alert def id=" +
                      alertDefinition.getId() + "; type=" +
                      alertDefinition.getAlertType().getCode(), e);
        }

        return started;
    }

    private boolean escalationStateExists(PerformsEscalations alertDefinition) {
        // Checks if there is an uncommitted escalation state for this def.
        boolean existsInCache = escalationRuntime
            .addToUncommittedEscalationStateCache(alertDefinition);
        boolean existsInDb = false;

        try {
            // Checks if there is a committed escalation state for this def.
            existsInDb = escalationStateDAO.find(alertDefinition) != null;
        } catch (Exception e) {
            log
                .warn("There is already one escalation in progress for " + "alert def id=" +
                      alertDefinition.getId() + "; type=" +
                      alertDefinition.getAlertType().getCode());

            // HHQ-915: A hibernate exception will occur when looking up the
            // escalation state if more than one exists. This shouldn't happen,
            // but if it does, don't create another escalation.
            existsInDb = true;
        }

        // Possible scenarios when storing an escalation state ->
        // how to remove the def from the uncommitted cache:
        // in_cache=false, in_db=false -> schedule to remove on commit
        // in_cache=true, in_db=false -> do nothing,
        // - will be removed from cache post-commit
        // in_cache=false, in_db=true -> remove immediately
        // in_cache=true, in_db=true -> (a timing issue),
        // - will be removed from cache post-commit,
        // but to be safe, remove immediately
        if (existsInDb) {
            escalationRuntime.removeFromUncommittedEscalationStateCache(alertDefinition, false);
        } else if (!existsInCache && !existsInDb) {
            escalationRuntime.removeFromUncommittedEscalationStateCache(alertDefinition, true);
        }

        if (existsInCache || existsInDb) {
            log.debug("startEscalation called on [" + alertDefinition + "] but it was " +
                      "already running");
        }

        return existsInCache || existsInDb;
    }

    @Transactional(readOnly = true)
    public Escalatable getEscalatable(EscalationState escalationState) {
        return escalationRuntime.getEscalatable(escalationState);
    }

    /**
     * End an escalation. This will remove all state for the escalation tied to
     * a specific definition.
     */
    public void endEscalation(PerformsEscalations alertDefinition) {
        escalationRuntime.unscheduleAllEscalationsFor(alertDefinition);
    }

    /**
     * This method is only for internal use by the {@link EscalationRuntime}. It
     * ensures that we have a session setup prior to executing any actions.
     * 
     * This method executes the action pointed at by the state, determines the
     * next stage of the escalation and (optionally) ends it, thus unscheduling
     * any further executions.
     */

    /**
     * Find an escalation based on the type and ID of the definition.
     * 
     * @return null if the definition defined by the ID does not have any
     *         escalation associated with it
     */
    @Transactional(readOnly = true)
    public Escalation findByDefId(EscalationAlertType escalationAlertType, Integer definitionId) {
        return escalationAlertType.findDefinition(definitionId).getEscalation();
    }

    /**
     * Set the escalation for a given alert definition and type
     */
    public void setEscalation(EscalationAlertType escalationAlertType, Integer defId,
                              Escalation escalation) {
        escalationAlertType.setEscalation(defId, escalation);
    }

    /**
     * Acknowledge an alert, potentially sending out notifications.
     * 
     * @param subject Person who acknowledged the alert
     * @param pause TODO
     */
    public boolean acknowledgeAlert(AuthzSubject subject, EscalationAlertType escalationAlertType,
                                    Integer alertId, String moreInfo, long pause)
        throws PermissionException {
        Escalatable alert = escalationAlertType.findEscalatable(alertId);
        PerformsEscalations alertDefinition = alert.getDefinition();

        if (!isAlertAcknowledgeable(alertId, alertDefinition)) {
            return false;
        }

        if (moreInfo == null || moreInfo.trim().length() == 0) {
            moreInfo = "";
        }

        EscalationState escalationState = escalationStateDAO.find(alert);
        Escalation escalation = alertDefinition.getEscalation();

        if (pause > 0 && escalation.isPauseAllowed()) {
            long nextTime;

            if (pause > escalation.getMaxPauseTime()) {
                pause = escalation.getMaxPauseTime();
            }

            if (pause == Long.MAX_VALUE) {
                nextTime = pause;
                moreInfo = " and paused escalation until fixed. " + moreInfo;
            } else {
                nextTime = System.currentTimeMillis() + pause;

                FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(pause,
                    UnitsConstants.UNIT_DURATION, UnitsConstants.SCALE_MILLI));

                moreInfo = " and paused escalation for " + fmtd + ". " + moreInfo;
            }

            if (nextTime > escalationState.getNextActionTime()) {
                escalationState.setNextActionTime(nextTime);
                escalationRuntime.scheduleEscalation(escalationState);
            }
        } else {
            if (moreInfo.length() > 0) {
                moreInfo = ". " + moreInfo;
            }
        }

        fixOrNotify(subject, alert, escalationState, escalationAlertType, false, moreInfo, false);

        return true;
    }

    /**
     * See if an alert is acknowledgeable
     * 
     * @return true if the alert is currently acknowledgeable
     */
    @Transactional(readOnly = true)
    public boolean isAlertAcknowledgeable(Integer alertId, PerformsEscalations alertDefinition) {
        if (alertDefinition.getEscalation() != null) {
            EscalationState escState = escalationStateDAO.find(alertDefinition);

            if (escState != null) {
                if (escState.getAlertId() == alertId.intValue() &&
                    escState.getAcknowledgedBy() == null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Fix an alert for a an escalation if there is one currently running.
     * 
     * @return true if there was an alert to be fixed.
     */
    public boolean fixAlert(AuthzSubject subject, PerformsEscalations alertDefinition,
                            String moreInfo) throws PermissionException {
        EscalationState escalationState = escalationStateDAO.find(alertDefinition);

        if (escalationState == null) {
            return false;
        }

        // Find the alert, to see if it's been fixed.
        Integer alertId = new Integer(escalationState.getAlertId());
        Escalatable escalation = escalationState.getAlertType().findEscalatable(alertId);

        // Strange condition, since we shouldn't have an escalation state if
        // it has been fixed.
        if (escalation.getAlertInfo().isFixed()) {
            log.warn("Found a fixed alert inside an escalation.  alert=" + alertId + " defid=" +
                     alertDefinition.getDefinitionInfo().getId() + " alertType=" +
                     escalationState.getAlertType().getCode());

            return false;
        }

        fixOrNotify(subject, escalation, escalationState, escalationState.getAlertType(), true,
            moreInfo, false);

        return true;
    }

    /**
     * Fix an alert, potentially sending out notifications. The state of the
     * escalation will be terminated and the alert will be marked fixed.
     * 
     * @param subject Person who fixed the alert
     */
    public void fixAlert(AuthzSubject subject, EscalationAlertType escalationAlertType,
                         Integer alertId, String moreInfo) throws PermissionException {
        fixAlert(subject, escalationAlertType, alertId, moreInfo, false);
    }

    /**
     * Fix an alert, potentially sending out notifications. The state of the
     * escalation will be terminated and the alert will be marked fixed.
     * 
     * @param subject Person who fixed the alert
     */
    public void fixAlert(AuthzSubject subject, EscalationAlertType escalationAlertType,
                         Integer alertId, String moreInfo, boolean suppressNotification)
        throws PermissionException {
        Escalatable escalation = escalationAlertType.findEscalatable(alertId);
        EscalationState escalationState = escalationStateDAO.find(escalation);

        fixOrNotify(subject, escalation, escalationState, escalationAlertType, true, moreInfo,
            suppressNotification);
    }

    private void fixOrNotify(AuthzSubject subject, Escalatable alert,
                             EscalationState escalationState,
                             EscalationAlertType escalationAlertType, boolean fixed,
                             String moreInfo, boolean suppressNotification)
        throws PermissionException {
        final boolean debug = log.isDebugEnabled();
        Integer alertId = alert.getAlertInfo().getId();
        boolean acknowledged = !fixed;

        if (alert.getAlertInfo().isFixed()) {
            log.warn(subject.getFullName() + " attempted to fix or " + " acknowledge the " +
                     escalationAlertType + " id=" + alertId + " but it was already fixed");
            return;
        }

        if (escalationState == null && acknowledged) {
            log.debug(subject.getFullName() + " acknowledged alertId[" + alertId + "] for type [" +
                      escalationAlertType + "], but it wasn't " +
                      "running or was previously acknowledged.  " + "Button Masher?");
            return;
        }

        // HQ-1295: Does user have sufficient permissions?
        // ...check if user can fix/acknowledge this alert...
        // HHQ-3784 to avoid deadlocks use the this table order when updating/inserting:
        // 1) EAM_ESCALATION_STATE, 2) EAM_ALERT, 3) EAM_ALERT_ACTION_LOG
        alertPermissionManager.canFixAcknowledgeAlerts(subject, alert.getDefinition().getDefinitionInfo());

        if (fixed) {
            if (moreInfo == null || moreInfo.trim().length() == 0) {
                moreInfo = "(Fixed by " + subject.getFullName() + ")";
            }

            if(debug) log.debug(subject.getFullName() + " has fixed alertId=" + alertId);
            if (escalationState != null) {
               escalationRuntime.endEscalation(escalationState);
             }

            escalationAlertType.changeAlertState(alert, subject, EscalationStateChange.FIXED);
            escalationAlertType.logActionDetails(alert, null, moreInfo, subject);

        } else {
            if (moreInfo == null || moreInfo.trim().length() == 0) {
                moreInfo = "";
            }

            if (escalationState.getAcknowledgedBy() != null) {
                log.warn(subject.getFullName() + " attempted to acknowledge " +
                         escalationAlertType + " alert=" + alertId + " but it was already " +
                         "acknowledged by " + escalationState.getAcknowledgedBy().getFullName());

                return;
            }

            if (debug) log.debug(subject.getFullName() + " has acknowledged alertId=" +  alertId);
            escalationState.setAcknowledgedBy(subject);
            escalationAlertType.changeAlertState(alert, subject, EscalationStateChange.ACKNOWLEDGED);
            String msg = subject.getFullName() + " acknowledged " + "the alert" + moreInfo;
            escalationAlertType.logActionDetails(alert, null, msg, subject);
        }

        if (!suppressNotification && alertRegulator.alertNotificationsAllowed()) {
            if (escalationState != null) {
                sendNotifications(escalationState, alert, subject, escalationState.getEscalation()
                    .isNotifyAll(), fixed, moreInfo);
            } else if (fixed) { // The alert's escalation chain has completed
                sendFixedNotifications(subject, alert, moreInfo);
            }
        }
    }

    private String getNotificationMessage(AuthzSubject subject, boolean fixed, Escalatable alert,
                                          String moreInfo) {
        return subject.getFullName() + " has " + (fixed ? "fixed" : "acknowledged") +
               " the alert raised by [" + alert.getDefinition().getName() + "]. " + moreInfo;
    }

    /**
     * Send a fixed notification for an alert whose escalation has ended
     */
    private void sendFixedNotifications(AuthzSubject subject, Escalatable alert, String moreInfo) {
        Escalation escalation = alert.getDefinition().getEscalation();

        if (escalation == null) {
            // nothing to do

            return;
        }

        String message = getNotificationMessage(subject, true, alert, moreInfo);
        List<EscalationAction> escalationActions = escalation.getActions();

        for (Iterator<EscalationAction> i = escalationActions.iterator(); i.hasNext();) {
            EscalationAction escalationAction = i.next();
            Action action = escalationAction.getAction();

            try {
                Class clazz = Class.forName(action.getClassName());

                if (!Notify.class.isAssignableFrom(clazz)) {
                    continue;
                }

                Notify notify = (Notify) action.getInitializedAction();

                notify.send(alert, EscalationStateChange.FIXED, message, new HashSet());
            } catch (Exception e) {
                log.warn("Unable to send fixed notification alert", e);
            }
        }
    }

    /**
     * Send an acknowledge or fixed notification to the actions.
     * 
     * @param state State specifying the escalation chain to use
     * @param notifyAll If false, only send to previously executed actions.
     */
    private void sendNotifications(EscalationState escalationState, Escalatable alert,
                                   AuthzSubject subject, boolean notifyAll, boolean fixed,
                                   String moreInfo) {
        String notificationMessage = getNotificationMessage(subject, fixed, alert, moreInfo);

        List<EscalationAction> escalationActions = escalationState.getEscalation().getActions();
        int idx = (notifyAll ? escalationActions.size() : escalationState.getNextAction()) - 1;

        while (idx >= 0) {
            EscalationAction escalationAction = escalationActions.get(idx--);
            Action action = escalationAction.getAction();

            try {
                Class clazz = Class.forName(action.getClassName());
                Notify notify;

                if (!Notify.class.isAssignableFrom(clazz)) {
                    continue;
                }

                notify = (Notify) action.getInitializedAction();

                notify.send(alert, fixed ? EscalationStateChange.FIXED
                                        : EscalationStateChange.ACKNOWLEDGED, notificationMessage,
                    new HashSet());
            } catch (Exception e) {
                log.warn("Unable to send notification alert", e);
            }
        }

        // Send event to be logged
        messagePublisher.publishMessage(EventConstants.EVENTS_TOPIC, new EscalationEvent(alert,
            notificationMessage));
    }

    /**
     * Re-order the actions for an escalation. If there are any states
     * associated with the escalation, they will be cleared.
     * 
     * @param actions a list of {@link EscalationAction}s (already contained
     *        within the escalation) specifying the new order.
     */
    public void updateEscalationOrder(Escalation escalation, List<EscalationAction> actions) {
        if (actions.size() != escalation.getActions().size()) {
            throw new IllegalArgumentException("Actions size must be the same");
        }

        for (Iterator<EscalationAction> i = actions.iterator(); i.hasNext();) {
            EscalationAction action = i.next();

            if (escalation.getAction(action.getAction().getId()) == null) {
                throw new IllegalArgumentException("Action id=" + action.getAction().getId() +
                                                   " not found");
            }
        }

        escalation.setActionsList(actions);

        unscheduleEscalation(escalation);
    }

    /**
     * Get the # of active escalations within HQ inventory
     */
    @Transactional(readOnly = true)
    public Number getActiveEscalationCount() {
        return new Integer(escalationStateDAO.size());
    }

    /**
     * Get the # of escalations within HQ inventory
     */
    @Transactional(readOnly = true)
    public Number getEscalationCount() {
        return new Integer(escalationDAO.size());
    }

    @Transactional(readOnly = true)
    public List<EscalationState> getActiveEscalations(int maxEscalations) {
        return escalationStateDAO.getActiveEscalations(maxEscalations);
    }

    @Transactional(readOnly = true)
    public String getLastFix(PerformsEscalations def) {
        if (def != null) {
            EscalationAlertType type = def.getAlertType();
            return type.getLastFixedNote(def);
        }

        return null;
    }

    /**
     * Called when subject is removed and therefore have to null out the
     * acknowledgedBy field
     */
    public void handleSubjectRemoval(AuthzSubject subject) {
        escalationStateDAO.handleSubjectRemoval(subject);
    }

    public void startup() {
        log.info("Starting up Escalation subsystem");

        boolean debugLog = log.isDebugEnabled();

        for (Iterator<EscalationState> i = escalationStateDAO.findAll().iterator(); i.hasNext();) {
            EscalationState state = i.next();

            if (debugLog) {
                log.debug("Loading escalation state [" + state.getId() + "]");
            }

            escalationRuntime.scheduleEscalation(state);
        }
    }

    public Collection<EscalationState> getOrphanedEscalationStates() {
        return escalationStateDAO.getOrphanedEscalationStates();
    }

    public void removeEscalationState(EscalationState e) {
        escalationStateDAO.remove(e);
    }
}

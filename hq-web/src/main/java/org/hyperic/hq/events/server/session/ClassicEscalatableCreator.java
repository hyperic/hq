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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventPayload;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This class has the knowledge to create an {@link Escalatable} object based on
 * a {@link TriggerFiredEvent} if the escalation subsytem deems it necessary.
 */
public class ClassicEscalatableCreator implements EscalatableCreator {
    private static final Log _log = LogFactory.getLog(ClassicEscalatableCreator.class);

    private final AlertDefinition _def;
    private final AlertConditionsSatisfiedZEvent _event;

    private final MessagePublisher messagePublisher;
    private final AlertManager alertMan;

    /**
     * Creates an instance.
     * 
     * @param def The alert definition.
     * @param event The event that triggered the escalation.
     * @param messagePublisher The messenger to use for publishing
     *        AlertFiredEvents
     * @param alertMan The alert manager to use
     */
    public ClassicEscalatableCreator(AlertDefinition def, AlertConditionsSatisfiedZEvent event,
                                     MessagePublisher messagePublisher, AlertManager alertMan) {
        _def = def;
        _event = event;
        this.messagePublisher = messagePublisher;
        this.alertMan = alertMan;
    }

    /**
     * In the classic escalatable architecture, we still need to support the
     * execution of the actions defined for the regular alert defintion (in
     * addition to executing the actions specified by the escalation).
     * 
     * Here, we generate the alert and also execute the old-skool actions. May
     * or may not be the right place to do that.
     */
    public Escalatable createEscalatable() throws ResourceDeletedException {
        Escalatable escalatable = createEscalatableNoNotify();
        registerAlertFiredEvent(escalatable.getAlertInfo().getId(),
            (AlertConditionsSatisfiedZEventPayload) _event.getPayload());
        return escalatable;
    }

    Escalatable createEscalatableNoNotify() throws ResourceDeletedException {
        final Alert alert = createAlert();
        createConditionLogs(alert, (AlertConditionsSatisfiedZEventPayload) _event.getPayload());
        String shortReason = alertMan.getShortReason(alert);
        String longReason = alertMan.getLongReason(alert);
        executeActions(alert, shortReason, longReason);
        return createEscalatable(alert, shortReason, longReason);
    }

    private Alert createAlert() throws ResourceDeletedException {
        Resource r = _def.getResource();
        if (r == null || r.isInAsyncDeleteState()) {
            throw ResourceDeletedException.newInstance(r);
        }
        return alertMan.createAlert(_def, ((AlertConditionsSatisfiedZEventPayload) _event
            .getPayload()).getTimestamp());
    }

    private void createConditionLogs(final Alert alert,
                                     final AlertConditionsSatisfiedZEventPayload payload) {
        // Create a alert condition logs for every condition that triggered the
        // alert

        // Create the trigger event map
        Map trigMap = new HashMap();
        TriggerFiredEvent[] events = payload.getTriggerFiredEvents();
        for (int i = 0; i < events.length; i++) {
            trigMap.put(events[i].getInstanceId(), events[i]);
        }

        Collection conds = _def.getConditions();
        for (Iterator i = conds.iterator(); i.hasNext();) {
            AlertCondition cond = (AlertCondition) i.next();

            if (shouldCreateConditionLogFor(cond, trigMap)) {
                AlertConditionLogValue clog = new AlertConditionLogValue();
                clog.setCondition(cond.getAlertConditionValue());

                Integer trigId = cond.getTrigger().getId();
                clog.setValue(trigMap.get(trigId).toString());

                alert.createConditionLog(clog.getValue(), cond);
            }
        }
    }

    private void executeActions(Alert alert, String shortReason, String longReason) {
        Collection actions = _def.getActions();
        // Iterate through the actions
        for (Iterator i = actions.iterator(); i.hasNext();) {
            Action act = (Action) i.next();

            try {
                ActionExecutionInfo execInfo = new ActionExecutionInfo(shortReason, longReason,
                    Collections.EMPTY_LIST);

                String detail = act.executeAction(alert, execInfo);

                alertMan.logActionDetail(alert, act, detail, null);
            }catch(OptimisticLockingFailureException e) {
                throw e;
            }
            catch (Exception e) {
                // For any exception, just log it. We can't afford not
                // letting the other actions go un-processed.
                _log.warn("Error executing action [" + act + "]", e);
            }
        }
    }

    private void registerAlertFiredEvent(final Integer alertId,
                                         final AlertConditionsSatisfiedZEventPayload payload) {
        try {
            TransactionSynchronizationManager
                .registerSynchronization(new TransactionSynchronization() {
                    public void suspend() {
                    }

                    public void resume() {
                    }

                    public void flush() {
                    }

                    public void beforeCompletion() {
                    }

                    public void beforeCommit(boolean readOnly) {
                    }

                    public void afterCompletion(int status) {
                    }

                    public void afterCommit() {
                        // TODO HE-565 Possibly have MessagePublisher always
                        // wait until successful tx commit before publishing
                        // messages
                        messagePublisher.publishMessage(EventConstants.EVENTS_TOPIC,
                            new AlertFiredEvent(alertId, _def.getId(), AppdefUtil
                                .newAppdefEntityId(_def.getResource()), _def.getName(), payload
                                .getTimestamp(), payload.getMessage()));
                    }
                });
        }catch(OptimisticLockingFailureException e) {
            throw e;
        } catch (Throwable t) {
            _log
                .error(
                    "Error registering to send an AlertFiredEvent on transaction commit.  The alert will be fired, but the event will not be sent.  This could cause a future recovery alert not to fire.",
                    t);
        }
    }

    public AlertDefinitionInterface getAlertDefinition() {
        return _def;
    }

    public static Escalatable createEscalatable(Alert alert, String shortReason, String longReason) {
        return new ClassicEscalatable(alert, shortReason, longReason);
    }

    private boolean shouldCreateConditionLogFor(AlertCondition cond, Map triggerMap) {
        if (cond.getType() == EventConstants.TYPE_ALERT) {
            // Don't create a log for recovery alerts, so that we don't
            // get the multi-condition effect in the logs
            return false;
        }

        Integer trigId = cond.getTrigger().getId();

        return triggerMap.containsKey(trigId);
    }

}

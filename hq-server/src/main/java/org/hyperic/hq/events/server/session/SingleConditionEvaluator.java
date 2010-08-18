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

package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Implementation of {@link AlertConditionEvaluator} that sends a new
 * {@link AlertConditionsSatisfiedZEvent} to its {@link ExecutionStrategy}
 * whenever a trigger is fired, representing the evaluation of a single
 * condition.
 * @author jhickey
 *
 */
public class SingleConditionEvaluator implements AlertConditionEvaluator {
    private final Integer alertDefinitionId;

    private final ExecutionStrategy executionStrategy;

    /**
     *
     * @param alertDefinitionId The ID of the alert definition whose conditions
     *        are being evaluated
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        an {@link AlertConditionsSatisfiedZEvent} when a condition has
     *        been met
     */
    public SingleConditionEvaluator(Integer alertDefinitionId, ExecutionStrategy executionStrategy) {
        this.alertDefinitionId = alertDefinitionId;
        this.executionStrategy = executionStrategy;
    }

    public Integer getAlertDefinitionId() {
        return this.alertDefinitionId;
    }

    public ExecutionStrategy getExecutionStrategy() {
        return this.executionStrategy;
    }

    public Serializable getState() {
        return null;
    }

    public void initialize(Serializable initialState) {
       //No-Op
    }

    public void triggerFired(TriggerFiredEvent event) {
        executionStrategy.conditionsSatisfied(new AlertConditionsSatisfiedZEvent(alertDefinitionId.intValue(),
                                                                                 new TriggerFiredEvent[] { event }));
    }

    public void triggerNotFired(TriggerNotFiredEvent event) {
        // No-Op
    }

}

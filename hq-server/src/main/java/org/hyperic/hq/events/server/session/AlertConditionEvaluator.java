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

/**
 * Evaluates whether or not an alert should fire when a specific event causes a
 * single alert condition to evaluate to true or false
 * @author jhickey
 *
 */
public interface AlertConditionEvaluator {

    /**
     *
     * @return The ID of the alert definition associated with this evaluator
     */
    Integer getAlertDefinitionId();

    /**
     *
     * @return The {@link ExecutionStrategy} used by this evaluator to fire alert condition satisfied events
     */
    ExecutionStrategy getExecutionStrategy();

    /**
     *
     * @return Any state held by this evaluator that should be persisted between server restarts.  May be null if no state saved.
     */
    Serializable getState();

    /**
     * Initializes this evaluator
     * @param initialState Any state that was saved by the evaluator with this alertDefinitionId the last time the server was shutdown.  May be null if no state saved.
     */
    void initialize(Serializable initialState);

    /**
     * A trigger was fired, indicating an alert condition evaluated to true
     * @param event The {@link TriggerFiredEvent} representing the data that
     *        caused the condition to evaluate to true
     */
    void triggerFired(TriggerFiredEvent event);

    /**
     * A trigger was not fired, indicating that an alert condition evaluated to
     * false
     * @param event The {@link TriggerNotFiredEvent} representing the data that
     *        caused the condition to evaluate to true
     */
    void triggerNotFired(TriggerNotFiredEvent event);

}

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

package org.hyperic.hq.events;

import java.io.Serializable;
import java.util.Map;





/**
 * Repository to save and access state of AlertConditionEvaluators and
 * their associated ExecutionStrategys.
 * @author jhickey
 * 
 */
public interface AlertConditionEvaluatorStateRepository {
    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the AlertConditionEvaluator
     *         with that ID.
     */
    Map<Integer, Serializable> getAlertConditionEvaluatorStates();

    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the ExecutionStrategy with
     *         that ID.
     */
    Map<Integer, Serializable> getExecutionStrategyStates();

    /**
     * Persists states of AlertConditionEvaluators
     * @param alertConditionEvaluatorStates A Map where key is alert definition
     *        ID and value is the Serializable state of the
     *        AlertConditionEvaluator with that ID.
     */
    void saveAlertConditionEvaluatorStates(Map<Integer, Serializable> alertConditionEvaluatorStates);

    /**
     * Persists states of  ExecutionStrategys
     * @param executionStrategyStates A Map where key is alert definition ID and
     *        value is the Serializable state of the  ExecutionStrategy
     *        with that ID.
     */
    void saveExecutionStrategyStates(Map<Integer, Serializable> executionStrategyStates);

}

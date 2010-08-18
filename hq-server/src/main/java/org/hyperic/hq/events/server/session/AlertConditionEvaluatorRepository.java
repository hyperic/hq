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

import java.util.Map;

import org.hyperic.hq.events.AlertConditionEvaluatorStateRepository;


/**
 * Repository of {@link AlertConditionEvaluator}s
 * @author jhickey
 * 
 */
public interface AlertConditionEvaluatorRepository {

    /**
     * Add to the repository
     * @param alertConditionEvaluator The {@link AlertConditionEvaluator} to add
     *        the repository
     */
    void addAlertConditionEvaluator(AlertConditionEvaluator alertConditionEvaluator);

    /**
     * 
     * @param alertDefinitionId The ID of the alert definition
     * @return The corresponding {@link AlertConditionEvaluator} or null if none
     *         exists
     */
    AlertConditionEvaluator getAlertConditionEvaluatorById(Integer alertDefinitionId);

    /**
     * Get the {@link AlertConditionEvaluatorStateRepository}
     */
    AlertConditionEvaluatorStateRepository getStateRepository();

    /**
     * Get all the alert condition evaluators
     */
    Map<Integer, AlertConditionEvaluator> getAlertConditionEvaluators();

    /**
     * Remove from the repository
     * @param alertDefinitionId The ID of the alert definition whose
     *        {@link AlertConditionEvaluator} should be removed from the
     *        repository
     */
    void removeAlertConditionEvaluator(Integer alertDefinitionId);

}

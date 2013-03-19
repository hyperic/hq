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

import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Determines if an alert should be fired once the conditions associated with an
 * alert definition have been met
 * @author jhickey
 * 
 */
public interface ExecutionStrategy {

    /**
     * Indicates that all conditions associated with an alert definition have
     * been met
     * @param event An {@link AlertConditionsSatisfiedZEvent} to process
     */
    boolean conditionsSatisfied(AlertConditionsSatisfiedZEvent event);

    /**
     * 
     * @return Any state held by this strategy that should be persisted between
     *         server restarts. May be null if no state saved.
     */
    Serializable getState();

    /**
     * Initializes this strategy
     * @param initialState Any state that was saved by the strategy with this
     *        alertDefinitionId the last time the server was shutdown. May be
     *        null if no state saved.
     */
    void initialize(Serializable initialState);
}

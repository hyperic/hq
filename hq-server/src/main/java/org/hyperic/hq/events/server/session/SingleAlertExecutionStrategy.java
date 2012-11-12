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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Implementation of {@link ExecutionStrategy} that simply enqueues an
 * {@link AlertConditionsSatisfiedZEvent} for processing. This is typically used
 * by alert definitions with a frequency of everytime or once.
 * @author jhickey
 *
 */
public class SingleAlertExecutionStrategy implements ExecutionStrategy {

    private final ZeventEnqueuer zeventEnqueuer;

    private final Log log = LogFactory.getLog(SingleAlertExecutionStrategy.class);

    /**
     *
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to use for sending
     *        {@link AlertConditionsSatisfiedZEvent}s
     */
    public SingleAlertExecutionStrategy(ZeventEnqueuer zeventEnqueuer) {
        this.zeventEnqueuer = zeventEnqueuer;
    }

    
    public boolean conditionsSatisfied(AlertConditionsSatisfiedZEvent event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Firing event " + event);
            }
            zeventEnqueuer.enqueueEvent(event);
            return true;
        } catch (InterruptedException e) {
            log.warn("Interrupted enqueuing an AlertConditionsSatisfiedZEvent.  Event: " + event +
                     " will not be processed.  Cause: " + e.getMessage());
        }
        return false;
    }

    public Serializable getState() {
        return null;
    }

    public void initialize(Serializable initialState) {
        // No-Op
    }

}

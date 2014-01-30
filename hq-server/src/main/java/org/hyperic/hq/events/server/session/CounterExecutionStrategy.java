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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.server.shared.HeartbeatCurrentTime;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventPayload;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * 
 * Implementation of {@link ExecutionStrategy} that fires an
 * {@link AlertConditionsSatisfiedZEvent} when a certain number of events have
 * occurred within a given time window. Logic originally kept in a
 * CounterTrigger class.
 * @author jhickey
 * 
 */
public class CounterExecutionStrategy implements ExecutionStrategy {

    private final Object lock = new Object();
    private final long count;
    private final long timeRange; // timeRange is in milliseconds
    private final Log log = LogFactory.getLog(CounterExecutionStrategy.class);
    private final ZeventEnqueuer zeventEnqueuer;
    private List expirations;
    private HeartbeatCurrentTime heartbeatCurrentTime;

    /**
     * 
     * @param count The number of times the alert conditions must be satisfied
     * @param timeRange The time window (in milliseconds) in which the specified
     *        count of alert conditions must be satisfied.
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to use for sending
     *        {@link AlertConditionsSatisfiedZEvent}s
     */
    public CounterExecutionStrategy(long count, long timeRange, ZeventEnqueuer zeventEnqueuer) {
        this(count, timeRange, zeventEnqueuer, Bootstrap.getBean(HeartbeatCurrentTime.class));
    }

    // only used for tests mainly to inject HeartbeatCurrentTime
    CounterExecutionStrategy(long count, long timeRange, ZeventEnqueuer zeventEnqueuer,
                             HeartbeatCurrentTime heartbeatCurrentTime) {
        this.count = count;
        this.timeRange = timeRange;
        this.zeventEnqueuer = zeventEnqueuer;
        this.expirations = new ArrayList();
        this.heartbeatCurrentTime = heartbeatCurrentTime;
    }
    
    private void clearExpired() {
        for (Iterator<Long> iterator = expirations.iterator(); iterator.hasNext();) {
            Long expiration = iterator.next();
            if (expiration.longValue() < heartbeatCurrentTime.getTimeMillis()) {
                iterator.remove();
            }
        }
    }
    
    public boolean conditionsSatisfied(AlertConditionsSatisfiedZEvent event) {
        synchronized (lock) {
            AlertConditionsSatisfiedZEventPayload payload = (AlertConditionsSatisfiedZEventPayload) event.getPayload();
            expirations.add(Long.valueOf(payload.getTimestamp() + timeRange));
            clearExpired();
            if (expirations.size() >= count) {
                payload.setMessage("Occurred " + expirations.size() + " times in the span of " + (timeRange /
                                   MeasurementConstants.MINUTE) + " minutes");
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Firing event " + event);
                    }
                    zeventEnqueuer.enqueueEvent(event);
                    expirations.clear();
                    return true;
                } catch (InterruptedException e) {
                    log.warn("Interrupted enqueuing an AlertConditionsSatisfiedZEvent.  Event: " +
                             event +
                             " may not be processed unless triggering condition occurs again within the specified time range.  Cause: " +
                             e.getMessage());
                }
            }
        }
        return false;
    }

    long getCount() {
        return count;
    }

    public Serializable getState() {
        return (Serializable) this.expirations;
    }

    long getTimeRange() {
        return timeRange;
    }

    public void initialize(Serializable initialState) {
        if(initialState == null) {
            return;
        }
        if (!(initialState instanceof List)) {
            log.warn("Received persisted state that was not an instance of list.  Count will be reset to 0.");
            return;
        }
        this.expirations = (List) initialState;
    }

}

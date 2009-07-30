package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private final List expirations;

    /**
     *
     * @param count The number of times the alert conditions must be satisfied
     * @param timeRange The time window (in milliseconds) in which the specified
     *        count of alert conditions must be satisfied.
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to use for sending
     *        {@link AlertConditionsSatisfiedZEvent}s
     */
    public CounterExecutionStrategy(long count, long timeRange, ZeventEnqueuer zeventEnqueuer) {
        this.count = count;
        this.timeRange = timeRange;
        this.zeventEnqueuer = zeventEnqueuer;
        this.expirations = new ArrayList();
    }

    /**
     *
     * @param count The number of times the alert conditions must be satisfied
     * @param timeRange The time window (in milliseconds) in which the specified
     *        count of alert conditions must be satisfied.
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to use for sending
     *        {@link AlertConditionsSatisfiedZEvent}s
     * @param expirations The list to use for storing event expiration times
     */
    public CounterExecutionStrategy(long count, long timeRange, ZeventEnqueuer zeventEnqueuer, List expirations) {
        this.count = count;
        this.timeRange = timeRange;
        this.zeventEnqueuer = zeventEnqueuer;
        this.expirations = expirations;
    }

    private void clearExpired() {
        for (Iterator iterator = expirations.iterator(); iterator.hasNext();) {
            Long expiration = (Long) iterator.next();
            if (expiration.longValue() < System.currentTimeMillis()) {
                iterator.remove();
            }

        }
    }

    public void conditionsSatisfied(AlertConditionsSatisfiedZEvent event) {
        synchronized (lock) {
            AlertConditionsSatisfiedZEventPayload payload = (AlertConditionsSatisfiedZEventPayload) event.getPayload();
            expirations.add(Long.valueOf(payload.getTimestamp() + timeRange));
            clearExpired();
            if (expirations.size() >= count) {
                payload.setMessage("Occurred " + expirations.size() + " times in the span of " + timeRange /
                                   MeasurementConstants.MINUTE + " minutes");
                try {
                    zeventEnqueuer.enqueueEvent(event);
                    expirations.clear();
                } catch (InterruptedException e) {
                    log.warn("Interrupted enqueuing an AlertConditionsSatisfiedZEvent.  Event: " +
                             event +
                             " may not be processed unless triggering condition occurs again within the specified time range.  Cause: " +
                             e.getMessage());
                }
            }
        }
    }

    long getCount() {
        return count;
    }

    long getTimeRange() {
        return timeRange;
    }

}

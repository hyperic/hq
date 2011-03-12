package org.hyperic.hq.event.data;

import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.inventory.domain.Resource;

public interface EventLogRepositoryCustom {

    /**
     * Delete event logs in chunks.
     * 
     * @param from The timestamp to delete from.
     * @param to The timestamp to delete to
     * @return The number of event logs deleted.
     */
    int deleteLogsInTimeRange(long from, long to);

    /**
     * Insert the event logs in batch, with batch size specified by the
     * <code>hibernate.jdbc.batch_size</code> configuration property.
     * 
     * @param eventLogs The event logs to insert.
     */
    void insertLogs(EventLog[] eventLogs);

    /**
     * 
     * @param resource The resource for which events should be checked
     * @param begin The beginning of the time range, inclusive
     * @param end The end of the time range, not inclusive
     * @param intervals The number of intervals to divide the time range into
     * @return An array with element indexes matching each interval. The value
     *         will be true if logs exists for the specified Resource in that
     *         interval
     */
    boolean[] logsExistPerInterval(Resource resource, long begin, long end, int intervals);
}

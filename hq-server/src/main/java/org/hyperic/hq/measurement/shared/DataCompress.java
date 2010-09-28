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

package org.hyperic.hq.measurement.shared;

/**
 * Local interface for DataCompress.
 */
public interface DataCompress {

    /**
     * Create views for metric data if not already present in the DB
     */
    void createMetricDataViews();

    /**
     * Truncates tables containing measurement data older than the specified
     * timestamp
     * @param truncateBefore Data older than this timestamp (in milliseconds)
     *        will be truncated
     */
    void truncateMeasurementData(long truncateBefore);
    
    /**
     * Get the start time for a compression operation
     * @param toInterval The metric interval table to move data to (one of
     *        MeasurementConstants.HOUR, MeasurementConstants.SIX_HOUR, or
     *        MeasurementConstants.DAY)
     * @param now The current time
     * @return The start time or 0 if no data found to compress
     */
    long getCompressionStartTime(long toInterval, long now);

    /**
     * Compress data by moving it from one table to another
     * @param toInterval The metric interval table to move data to (one of
     *        MeasurementConstants.HOUR, MeasurementConstants.SIX_HOUR, or
     *        MeasurementConstants.DAY)
     * @param now Current time (used for calculating where to move data from if
     *        toInterval is set to HOUR)
     * @param startWindow Starting timestamp of data to move to toInterval table
     * @param endWindow Ending timestamp of data to move to toInterval table
     */
    void compressData(long toInterval, long now, long startWindow, long endWindow);

    /**
     * Delete data from a dataInterval table with timestamp between startWindow
     * and endWindow
     * @param dataInterval The metric interval table to purge measurements from (one of
     *        MeasurementConstants.HOUR, MeasurementConstants.SIX_HOUR, or
     *        MeasurementConstants.DAY)
     * @param startWindow Starting timestamp of data to delete
     * @param endWindow Ending timestamp of data to delete
     */
    void purgeMeasurements(long dataInterval, long startWindow, long endWindow);

    /**
     * Delete data from the metric problem table with timestamp between startWindow
     * and endWindow
     * @param startWindow Starting timestamp of data to delete
     * @param endWindow  Ending timestamp of data to delete
     */
    void purgeMetricProblems(long startWindow, long endWindow);

    /**
     * Get the oldest timestamp from a data interval table
     * @param dataInterval The metric interval table to retrieve timstamp from (one of
     *        MeasurementConstants.HOUR, MeasurementConstants.SIX_HOUR, or
     *        MeasurementConstants.DAY)
     * @return The oldest timestamp from a table
     */
    long getMinTimestamp(long dataInterval);

    /**
     * Get the oldest timestamp from the metric problem table
     * @return The oldest timestamp from the metric problem table
     */
    long getMetricProblemMinTimestamp();

}

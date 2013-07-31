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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.measurement.server.session.TopNData;
import org.hyperic.hq.plugin.system.TopReport;
import org.hyperic.hq.plugin.system.TopReport.TOPN_SORT_TYPE;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for DataManager.
 */
public interface DataManager {
    /**
     * Save the new MetricValue to the database
     * @param dp the new MetricValue
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     */
    public void addData(Integer mid, MetricValue mv, boolean overwrite);

    /**
     * Write metric data points to the DB with transaction
     * @param data a list of {@link DataPoint}s
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     */
    public boolean addData(List<DataPoint> data);

    public boolean addData(List<DataPoint> data, Connection conn);

    /**
     * Write metric datapoints to the DB without transaction
     * @param data a list of {@link DataPoint}s
     * @param overwrite If true, attempt to over-write values when an insert of
     *        the data fails (i.e. it already exists). You may not want to
     *        over-write values when, for instance, the back filler is inserting
     *        data.
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     */
    public void addData(List<DataPoint> data, boolean overwrite);

    public void addData(List<DataPoint> data, String aggTable, Connection conn) throws Exception;

    public boolean addTopData(List<TopNData> topNData);

    /**
     * Fetch the list of historical data points given a begin and end time
     * range. Returns a PageList of DataPoints without begin rolled into time
     * windows.
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @param prependUnknowns determines whether to prepend AVAIL_UNKNOWN if the
     *        corresponding time window is not accounted for in the database.
     *        Since availability is contiguous this will not occur unless the
     *        time range precedes the first availability point.
     * @return the list of data points
     */
    public PageList<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end, PageControl pc,
                                                          boolean prependAvailUnknowns);

    public List<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end, boolean prependAvailUnknowns, int maxDtps) throws IllegalArgumentException, TimeframeSizeException, TimeframeBoundriesException;

    /**
     * Fetch the list of historical data points given a begin and end time
     * range. Returns a PageList of DataPoints without begin rolled into time
     * windows.
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     */
    public PageList<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end, PageControl pc);

    /**
     * Aggregate data across the given metric IDs, returning max, min, avg, and
     * count of number of unique metric IDs
     * @param measurements {@link List} of {@link Measurement}s
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the An array of aggregate values
     */
    public double[] getAggregateData(List<Measurement> measurements, long begin, long end);

    /**
     * Fetch the list of historical data points, grouped by template, given a
     * begin and end time range. Does not return an entry for templates with no
     * associated data. PLEASE NOTE: The
     * {@link MeasurementConstants.IND_LAST_TIME} index in the {@link double[]}
     * part of the returned map does not contain the real last value. Instead it
     * is an averaged value calculated from the last 1/60 of the specified time
     * range. If this becomes an issue the best way I can think of to solve is
     * to pass in a boolean "getRealLastTime" and issue another query to get the
     * last value if this is set. It is much better than the alternative of
     * always querying the last metric time because not all pages require this
     * value.
     * @param measurements The List of {@link Measurement}s to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerImpl#getHistoricalData()
     * @return the {@link Map} of {@link Integer} to {@link double[]} which
     *         represents templateId to data points
     */
    public Map<Integer, double[]> getAggregateDataByTemplate(List<Measurement> measurements, long begin, long end);

    /**
     * Fetch the list of historical data points given a start and stop time
     * range and interval
     * @param measurements The List of Measurements to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param interval Interval for the time range
     * @param type Collection type for the metric
     * @param returnMetricNulls Specifies whether intervals with no data should
     *        be return as nulls
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerImpl#getHistoricalData()
     * @return the list of data points
     */
    public PageList<HighLowMetricValue> getHistoricalData(List<Measurement> measurements, long begin, long end,
                                                          long interval, int type, boolean returnMetricNulls,
                                                          PageControl pc);

    /**
     * Get the last MetricValue for the given Measurement.
     * @param m The Measurement
     * @return The MetricValue or null if one does not exist.
     */
    public MetricValue getLastHistoricalData(Measurement m);

    /**
     * Fetch the most recent data point for particular Measurements.
     * @param measurements The List of MeasurementIds to query. In the list of
     *        MeasurementIds null values are allowed as placeholders.
     * @param timestamp Only use data points with collection times greater than
     *        the given timestamp.
     * @return A Map of measurement ids to MetricValues. TODO: We should change
     *         this method to now allow NULL values. This is legacy and only
     *         used by the Metric viewer and Availabilty Summary portlets.
     */
    public Map<Integer, MetricValue> getLastDataPoints(List<Integer> measurements, long timestamp);

    /**
     * Get data points from cache only
     */
    public Collection<Integer> getCachedDataPoints(Collection<Integer> mids,
                                                   Map<Integer, MetricValue> data,
                                                   long timestamp);

    /**
     * Get a Baseline data.
     */
    public double[] getBaselineData(Measurement meas, long begin, long end);

    /**
     * Fetch a map of aggregate data values keyed by metrics given a start and
     * stop time range
     * @param tids The template id's of the Measurement
     * @param iids The instance id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the
     *        data if the time range spans more than one data table's max
     *        timerange
     * @return the Map of data points
     */
    public Map<Integer, double[]> getAggregateDataByMetric(java.lang.Integer[] tids, java.lang.Integer[] iids,
                                                           long begin, long end, boolean useAggressiveRollup);

    /**
     * Fetch a map of aggregate data values keyed by metrics given a start and
     * stop time range
     * @param measurements The id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the
     *        data if the time range spans more than one data table's max
     *        timerange
     * @return the map of data points
     */
    public Map<Integer, double[]> getAggregateDataByMetric(List<Measurement> measurements, long begin, long end,
                                                           boolean useAggressiveRollup);
    
    /**
     * @return array of {@link MetricValue} representing the raw metric data
     *         collected. Since Availability just keeps state changes this does
     *         not apply, therefore one {@link MetricValue} will be returned per
     *         interval.
     * 
     * @param publishedInterval {@link AtomicLong} interval, in millis, of the 
     *        dataset which is returned from the api. For example for queries
     *        that are a month long HQ will use a daily rollup table to retrieve
     *        the data from. In this case publishedInterval would be set with an
     *        interval that represents one day.
     * 
     * 
     */
    public Collection<HighLowMetricValue> getRawData(Measurement m, long begin, long end, AtomicLong publishedInterval);

    public Map<Integer, double[]> getAggregateDataAndAvailUpByMetric(final List<Measurement> hqMsmts, long beginMilli,
            long endMilli) throws SQLException;

    /**
     * @param resourceId
     * @param from
     * @param to
     * @return
     */
    List<Long> getAvailableTopDataTimes(int resourceId, long from, long to);

    /**
     * @param resourceId
     * @param time
     * @return
     */
    TopNData getTopNData(int resourceId, long time);

    /**
     * @param resourceId
     * @param time
     * @return
     */
    TopReport getTopReport(int resourceId, long time);

    /**
     * @param resourceId
     * @param count
     * @return
     */
    List<Long> getLatestAvailableTopDataTimes(int resourceId, int count);

    /**
     * @param timeToKeep
     * @return
     */
    int purgeTopNData(Date timeToKeep);

}

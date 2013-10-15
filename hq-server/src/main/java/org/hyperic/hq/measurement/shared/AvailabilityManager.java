/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.hyperic.hq.measurement.server.session.AvailabilityFallbackCheckQue;
import org.hyperic.hq.measurement.server.session.DataInserter;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AvailabilityManager.
 */
public interface AvailabilityManager {

    public Measurement getAvailMeasurement(Resource resource);

    public List<Measurement> getPlatformResources();

    public long getDowntime(Resource resource, long begin, long end) throws MeasurementNotFoundException;

    public List<Integer> getAllAvailIds();

    public List<Measurement> getAvailMeasurementChildren(Resource resource, String resourceRelationType);

    public Map<Integer, List<Measurement>> getAvailMeasurementChildren(List<Integer> resourceIds,
                                                                       String resourceRelationType);

    public List<Measurement> getAvailMeasurementParent(Resource resource, String resourceRelationType);

    public Map<Integer, List<Measurement>> getAvailMeasurementParent(List<Integer> resourceIds,
                                                                     String resourceRelationType);

    public Map<Integer, List<Measurement>> getAvailMeasurementDirectParent(List<Integer> resourceIds,
            String resourceRelationType);

    /**
     * Get Availability measurements (disabled) in scheduled downtime. 
     */
    public Map<Integer, Measurement> getAvailMeasurementsInDowntime(Collection<AppdefEntityID> eids);

    /**
     * TODO: Can this method be combined with the one that takes an array?
     */
    public PageList<HighLowMetricValue> getHistoricalAvailData(Measurement m, long begin, long end, PageControl pc,
                                                               boolean prependUnknowns);

    /**
     * Fetches historical availability encapsulating the specified time range
     * for each measurement id in mids;
     * @param mids measurement ids
     * @param begin time range start
     * @param end time range end
     * @param interval interval of each time range window
     * @param pc page control
     * @param prependUnknowns determines whether to prepend AVAIL_UNKNOWN if the
     *        corresponding time window is not accounted for in the database.
     *        Since availability is contiguous this will not occur unless the
     *        time range precedes the first availability point.
     * @see MeasurementConstants #AVAIL_UNKNOWN
     */
    public PageList<HighLowMetricValue> getHistoricalAvailData(Integer[] mids, long begin, long end, long interval,
                                                               PageControl pc, boolean prependUnknowns);

    /**
     * Get the list of Raw RLE objects for a resource
     * @return List<AvailabilityDataRLE>
     */
    public List<AvailabilityDataRLE> getHistoricalAvailData(Resource res, long begin, long end);

    public Map<Integer, double[]> getAggregateData(Integer[] mids, long begin, long end);

    public Map<Integer, double[]> getAggregateDataByTemplate(Integer[] mids, long begin, long end);

    public Map<Integer, double[]> getAggregateData(Integer[] tids, Integer[] iids, long begin, long end);

    public Map<Integer, MetricValue> getLastAvail(Collection<? extends Object> resources,
                                                  Map<Integer, List<Measurement>> measCache);

    public MetricValue getLastAvail(Measurement m);

    /**
     * Only unique measurement ids should be passed in. Duplicate measurement
     * ids will be filtered out from the returned Map if present.
     * @return {@link Map} of {@link Integer} to {@link MetricValue} Integer is
     *         the measurementId
     */
    public Map<Integer, MetricValue> getLastAvail(Integer[] mids);

    public List<DownMetricValue> getUnavailEntities(List<Integer> includes);

    /**
     * Add a single Availablility Data point.
     * @mid The Measurement id
     * @mval The MetricValue to store.
     * 
     * DO NOT CALL THIS DIRECTLY, IT SHOULD BE CALLED VIA measurementInserterHolder.getAvailDataInserter().insertMetrics()
     * in a synchronized block on measurementInserterHolder.getAvailDataInserter()
     */
    public void addData(Integer mid, MetricValue mval);

    /**
     * Process Availability data. The default behavior is to send the data
     * points to the event handlers.
     * @param availPoints List of DataPoints
     * DO NOT CALL THIS DIRECTLY, IT SHOULD BE CALLED VIA measurementInserterHolder.getAvailDataInserter().insertMetrics()
     * in a synchronized block on measurementInserterHolder.getAvailDataInserter(), for example - 
     * DataInserter inserter = measurementInserterHolder.getAvailDataInserter();
     *  synchronized (inserter.getLock()) {
     *           	try {
     *           		inserter.insertMetrics(list);
     *           	} catch (Exception exp) {
     *           	}
     *           }
     */
    public void addData(List<DataPoint> availPoints);

    /**
     * Process Availability data.
     * @param availPoints List of DataPoints
     * @param sendData Indicates whether to send the data to event handlers. The
     *        default behavior is true. If false, the calling method should call
     *        sendDataToEventHandlers directly afterwards.
     *        
     * DO NOT CALL THIS DIRECTLY, IT SHOULD BE CALLED VIA measurementInserterHolder.getAvailDataInserter().insertMetrics()
     * in a synchronized block on measurementInserterHolder.getAvailDataInserter(), for example - 
     * DataInserter inserter = measurementInserterHolder.getAvailDataInserter();
     *  synchronized (inserter.getLock()) {
     *           	try {
     *           		inserter.insertMetrics(list);
     *           	} catch (Exception exp) {
     *           	}
     *           }
     */
    public void addData(List<DataPoint> availPoints, boolean sendData);
    /**
     * @param availDataPoints
     * @param sendData
     * @param addedByServer
     * 
     * DO NOT CALL THIS DIRECTLY, IT SHOULD BE CALLED VIA measurementInserterHolder.getAvailDataInserter().insertMetrics()
     * in a synchronized block on measurementInserterHolder.getAvailDataInserter(), for example - 
     * DataInserter inserter = measurementInserterHolder.getAvailDataInserter();
     *  synchronized (inserter.getLock()) {
     *           	try {
     *           		inserter.insertMetrics(list);
     *           	} catch (Exception exp) {
     *           	}
     *           }
     */
    public void addData(List<DataPoint> availDataPoints, boolean sendData, boolean addedByServer);

    
    /**
     * This method should only be called by the AvailabilityCheckService and is
     * used to filter availability data points based on hierarchical alerting
     * rules.
     */
    public void sendDataToEventHandlers(Map<Integer, DataPoint> data);

    /**
     * @return true if the OS platform associated with the agentId is available.  Since multiple
     * platforms may be associated with an agent, {@link PlatformDetector}.isSupportedPlatform() is
     * used to determine which association to check availability against.
     * @see PlatformDetector#isSupportedPlatform()
     */
    public boolean platformIsAvailableOrUnknown(int agentId);

    public AvailabilityFallbackCheckQue getFallbackCheckQue();

    public Map<Integer, double[]> getAggregateDataAndAvailUpByMetric(final List<Integer> avIds, long begin, long end) throws SQLException;

    public Map<Integer, DownMetricValue> getUnavailResMap();

}

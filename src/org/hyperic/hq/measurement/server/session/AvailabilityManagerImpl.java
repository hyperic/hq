/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.server.session;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.AvailabilityMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AvailabityManagerEJBImpl class is a stateless session bean that can be
 * used to retrieve Availability Data RLE points
 * 
 */
@Service
@Transactional
public class AvailabilityManagerImpl implements AvailabilityManager {

    private final Log _log = LogFactory.getLog(AvailabilityManagerImpl.class);
    private final Log _traceLog = LogFactory.getLog(AvailabilityManagerImpl.class.getName() + "Trace");
    private static final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_UNKNOWN = MeasurementConstants.AVAIL_UNKNOWN;
    private static final int IND_MIN = MeasurementConstants.IND_MIN;
    private static final int IND_AVG = MeasurementConstants.IND_AVG;
    private static final int IND_MAX = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private static final int IND_UP_TIME = IND_LAST_TIME + 1;
    private static final int IND_TOTAL_TIME = IND_UP_TIME + 1;
    private static final long MAX_AVAIL_TIMESTAMP = AvailabilityDataRLE.getLastTimestamp();
    private static final String ALL_EVENTS_INTERESTING_PROP = "org.hq.triggers.all.events.interesting";
    private static final int DEFAULT_INTERVAL = 60;

    private static final String AVAIL_MANAGER_METRICS_INSERTED = ConcurrentStatsCollector.AVAIL_MANAGER_METRICS_INSERTED;
    /**
     * {@link Map} of {@link DataPoint} to {@link AvailabilityDataRLE}
     */
    private Map<DataPoint, AvailabilityDataRLE> createMap = null;
    /**
     * {@link Map} of {@link DataPoint} to {@link AvailabilityDataRLE}
     */
    private Map<DataPoint, AvailabilityDataRLE> removeMap = null;
    /**
     * {@link Map} of {@link Integer} to ({@link TreeSet} of
     * {@link AvailabilityDataRLE}).
     * <p>
     * The {@link Map} key of {@link Integer} == {@link Measurement}.getId().
     * <p>
     * The {@link TreeSet}'s comparator sorts by {@link AvailabilityDataRLE}
     * .getStartime().
     */
    private Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails = null;

    private static final long MAX_DATA_BACKLOG_TIME = 7 * MeasurementConstants.DAY;

    private MeasurementManager measurementManager;

    private ResourceManager resourceManager;

    private MessagePublisher messenger;

    private AvailabilityDataDAO availabilityDataDAO;

    private MeasurementDAO measurementDAO;

    @Autowired
    public AvailabilityManagerImpl(ResourceManager resourceManager, MessagePublisher messenger,
                                   AvailabilityDataDAO availabilityDataDAO, MeasurementDAO measurementDAO) {
        this.resourceManager = resourceManager;
        this.messenger = messenger;
        this.availabilityDataDAO = availabilityDataDAO;
        this.measurementDAO = measurementDAO;
    }

    // To break AvailabilityManager - MeasurementManager circular dependency
    // TODO: Check why we need this when we have
    // MeasurementManagerImpl.setCircularDependencies()?
    @Autowired
    public void setMeasurementManager(MeasurementManager measurementManager) {
        this.measurementManager = measurementManager;
    }

    /**
     * 
     */
    public Measurement getAvailMeasurement(Resource resource) {
        return measurementDAO.findAvailMeasurement(resource);
    }

    /**
     * 
     */
    public List<Measurement> getPlatformResources() {
        return measurementDAO.findAvailMeasurementsByInstances(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null);
    }

    /**
     * @return Down time in ms for the Resource availability
     * 
     * 
     */
    public long getDowntime(Resource resource, long begin, long end) throws MeasurementNotFoundException {
        Measurement meas = measurementDAO.findAvailMeasurement(resource);
        if (meas == null) {
            throw new MeasurementNotFoundException("Availability measurement " + "not found for resource " +
                                                   resource.getId());
        }
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO.getHistoricalAvails(meas, begin, end, false);
        long rtn = 0l;
        for (AvailabilityDataRLE avail : availInfo) {
            if (avail.getAvailVal() != AVAIL_DOWN) {
                continue;
            }
            long endtime = avail.getEndtime();
            if (endtime == MAX_AVAIL_TIMESTAMP) {
                endtime = System.currentTimeMillis();
            }
            rtn += (endtime - avail.getStartime());
        }
        return rtn;
    }

    /**
     * @return List of all measurement ids for availability, ordered
     * 
     * 
     */
    public List<Integer> getAllAvailIds() {
        return measurementDAO.findAllAvailIds();
    }

    /**
     * 
     */
    public List<Measurement> getAvailMeasurementChildren(Resource resource, String resourceRelationType) {
        final List<Integer> sList = Collections.singletonList(resource.getId());
        List<Measurement> rtn = getAvailMeasurementChildren(sList, resourceRelationType).get(resource.getId());
        if (rtn == null) {
            rtn = new ArrayList<Measurement>(0);
        }
        return rtn;
    }

    /**
     * @param {@link List} of {@link Integer} resource ids
     * @return {@link Map} of {@link Integer} to {@link List} of
     *         {@link Measurement}
     * 
     */
    public Map<Integer, List<Measurement>> getAvailMeasurementChildren(List<Integer> resourceIds,
                                                                       String resourceRelationType) {
        final List<Object[]> objects = measurementDAO.findRelatedAvailMeasurements(resourceIds, resourceRelationType);

        return convertAvailMeasurementListToMap(objects);
    }

    /**
     * 
     */
    public List<Measurement> getAvailMeasurementParent(Resource resource, String resourceRelationType) {
        final List<Integer> sList = Collections.singletonList(resource.getId());
        List<Measurement> rtn = getAvailMeasurementParent(sList, resourceRelationType).get(resource.getId());
        if (rtn == null) {
            rtn = new ArrayList<Measurement>(0);
        }
        return rtn;
    }

    /**
     * 
     */
    public Map<Integer, List<Measurement>> getAvailMeasurementParent(List<Integer> resourceIds,
                                                                     String resourceRelationType) {
        final List<Object[]> objects = measurementDAO.findParentAvailMeasurements(resourceIds, resourceRelationType);

        return convertAvailMeasurementListToMap(objects);
    }

    private Map<Integer, List<Measurement>> convertAvailMeasurementListToMap(List<Object[]> objects) {
        final Map<Integer, List<Measurement>> rtn = new HashMap<Integer, List<Measurement>>(objects.size());
        for (Object[] o : objects) {
            final Integer rId = (Integer) o[0];
            final Measurement m = (Measurement) o[1];
            List<Measurement> tmp;
            if (null == (tmp = rtn.get(rId))) {
                tmp = new ArrayList<Measurement>();
                rtn.put(rId, tmp);
            }
            tmp.add(m);
        }
        return rtn;
    }

    /**
     * TODO: Can this method be combined with the one that takes an array?
     * 
     * 
     */
    public PageList<HighLowMetricValue> getHistoricalAvailData(Measurement m, long begin, long end, PageControl pc,
                                                               boolean prependUnknowns) {
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO.getHistoricalAvails(m, begin, end, pc.isDescending());
        return getPageList(availInfo, begin, end, m.getInterval(), prependUnknowns);
    }

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
     * @see org.hyperic.hq.measurement.MeasurementConstants#AVAIL_UNKNOWN
     * 
     */
    public PageList<HighLowMetricValue> getHistoricalAvailData(Integer[] mids, long begin, long end, long interval,
                                                               PageControl pc, boolean prependUnknowns) {
        if (mids.length == 0) {
            return new PageList<HighLowMetricValue>();
        }
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO.getHistoricalAvails(mids, begin, end, pc
            .isDescending());
        return getPageList(availInfo, begin, end, interval, prependUnknowns);
    }

    /**
     * Get the list of Raw RLE objects for a resource
     * @return List<AvailabilityDataRLE>
     * 
     */
    public List<AvailabilityDataRLE> getHistoricalAvailData(Resource res, long begin, long end) {
        return availabilityDataDAO.getHistoricalAvails(res, begin, end);
    }

    private Collection<HighLowMetricValue> getDefaultHistoricalAvail(long timestamp) {
        HighLowMetricValue[] rtn = new HighLowMetricValue[DEFAULT_INTERVAL];
        Arrays.fill(rtn, new HighLowMetricValue(AVAIL_UNKNOWN, timestamp));
        return Arrays.asList(rtn);
    }

    private PageList<HighLowMetricValue> getPageList(List<AvailabilityDataRLE> availInfo, long begin, long end,
                                                     long interval, boolean prependUnknowns) {
        PageList<HighLowMetricValue> rtn = new PageList<HighLowMetricValue>();
        begin += interval;
        for (Iterator<AvailabilityDataRLE> it = availInfo.iterator(); it.hasNext();) {
            AvailabilityDataRLE rle = it.next();
            long availStartime = rle.getStartime();
            long availEndtime = rle.getEndtime();
            if (availEndtime < begin) {
                continue;
            }
            LinkedList<AvailabilityDataRLE> queue = new LinkedList<AvailabilityDataRLE>();
            queue.add(rle);
            int i = 0;
            for (long curr = begin; curr <= end; curr += interval) {
                long next = curr + interval;
                next = (next > end) ? end : next;
                long endtime = ((AvailabilityDataRLE) queue.getFirst()).getEndtime();
                while (next > endtime) {
                    // it should not be the case that there are no more
                    // avails in the array, but we need to handle it
                    if (it.hasNext()) {
                        AvailabilityDataRLE tmp = (AvailabilityDataRLE) it.next();
                        queue.addFirst(tmp);
                        endtime = tmp.getEndtime();
                    } else {
                        endtime = availEndtime;
                        int measId = rle.getMeasurement().getId().intValue();
                        String msg = "Measurement, " + measId + ", for interval " + begin + " - " + end +
                                     " did not return a value for range " + curr + " - " + (curr + interval);
                        _log.warn(msg);
                    }
                }
                endtime = availEndtime;
                while (curr > endtime) {
                    queue.removeLast();
                    // this should not happen unless the above !it.hasNext()
                    // else condition is true
                    if (queue.size() == 0) {
                        rle = new AvailabilityDataRLE(rle.getMeasurement(), rle.getEndtime(), next, AVAIL_UNKNOWN);
                        queue.addLast(rle);
                    }
                    rle = (AvailabilityDataRLE) queue.getLast();
                    availStartime = rle.getStartime();
                    availEndtime = rle.getEndtime();
                    endtime = availEndtime;
                }
                HighLowMetricValue val;
                if (curr >= availStartime) {
                    val = getMetricValue(queue, curr);
                } else if (prependUnknowns) {
                    val = new HighLowMetricValue(AVAIL_UNKNOWN, curr);
                    val.incrementCount();
                } else {
                    i++;
                    continue;
                }
                if (rtn.size() <= i) {
                    rtn.add(round(val));
                } else {
                    updateMetricValue(val, (HighLowMetricValue) rtn.get(i));
                }
                i++;
            }
        }
        if (rtn.size() == 0) {
            rtn.addAll(getDefaultHistoricalAvail(end));
        }
        return rtn;
    }

    private HighLowMetricValue round(HighLowMetricValue val) {
        final BigDecimal b = new BigDecimal(val.getValue(), new MathContext(10));
        val.setValue(b.doubleValue());
        return val;
    }

    private HighLowMetricValue updateMetricValue(HighLowMetricValue newVal, HighLowMetricValue oldVal) {
        if (newVal.getHighValue() == AVAIL_UNKNOWN || newVal.getHighValue() > oldVal.getHighValue()) {
            oldVal.setHighValue(newVal.getHighValue());
        }
        if (newVal.getLowValue() == AVAIL_UNKNOWN || newVal.getLowValue() < oldVal.getLowValue()) {
            oldVal.setLowValue(newVal.getLowValue());
        }
        int count = oldVal.getCount();
        if (oldVal.getValue() == AVAIL_UNKNOWN) {
            double value = newVal.getValue();
            oldVal.setValue(value);
            oldVal.setCount(1);
        } else if (newVal.getValue() == AVAIL_UNKNOWN) {
            return oldVal;
        } else {
            double value = ((newVal.getValue() + (oldVal.getValue() * count))) / (count + 1);
            oldVal.setValue(value);
            oldVal.incrementCount();
            round(oldVal);
        }
        return oldVal;
    }

    private HighLowMetricValue getMetricValue(List<AvailabilityDataRLE> avails, long timestamp) {
        if (avails.size() == 1) {
            AvailabilityDataRLE rle = avails.get(0);
            return new HighLowMetricValue(rle.getAvailVal(), timestamp);
        }
        double value = 0;
        for (AvailabilityDataRLE rle : avails) {
            double availVal = rle.getAvailVal();
            value += availVal;
        }
        value = value / avails.size();
        HighLowMetricValue val = new HighLowMetricValue(value, timestamp);
        val.incrementCount();
        return val;
    }

    /**
     * @return {@link Map} of {@link Measurement} to {@link double[]}. Array is
     *         comprised of 5 elements: [IND_MIN] [IND_AVG] [IND_MAX]
     *         [IND_CFG_COUNT] [IND_LAST_TIME]
     * 
     */
    public Map<Integer, double[]> getAggregateData(Integer[] mids, long begin, long end) {
        List<Object[]> avails = availabilityDataDAO.findAggregateAvailability(mids, begin, end);
        return getAggData(avails, false);
    }

    /**
     * @return {@link Map} of {@link MeasurementTemplate.getId} to {@link
     *         double[]}. Array is comprised of 5 elements: [IND_MIN] [IND_AVG]
     *         [IND_MAX] [IND_CFG_COUNT] [IND_LAST_TIME]
     * 
     */
    public Map<Integer, double[]> getAggregateDataByTemplate(Integer[] mids, long begin, long end) {
        List<Object[]> avails = availabilityDataDAO.findAggregateAvailability(mids, begin, end);
        return getAggData(avails, true);
    }

    /**
     * @return {@link Map} of {@link MeasurementTemplate.getId} to {@link
     *         double[]}. Array is comprised of 5 elements: [IND_MIN] [IND_AVG]
     *         [IND_MAX] [IND_CFG_COUNT] [IND_LAST_TIME]
     * 
     */
    public Map<Integer, double[]> getAggregateData(Integer[] tids, Integer[] iids, long begin, long end) {
        List<Object[]> avails = availabilityDataDAO.findAggregateAvailability(tids, iids, begin, end);
        return getAggData(avails, true);
    }

    private Map<Integer, double[]> getAggData(List<Object[]> avails, boolean useTidKey) {
        Map<Integer, double[]> rtn = new HashMap<Integer, double[]>();
        if (avails.size() == 0) {
            // Nothing to do, return an empty Map.
            return rtn;
        }
        for (Object[] objs : avails) {
            double[] data;
            Integer key = null;
            if (useTidKey) {
                if (objs[0] instanceof Measurement) {
                    key = ((Measurement) objs[0]).getTemplate().getId();
                } else {
                    key = (Integer) objs[0];
                }
            } else {
                key = ((Measurement) objs[0]).getId();
            }
            if (null == (data = (double[]) rtn.get(key))) {
                data = new double[IND_TOTAL_TIME + 1];
                data[IND_MIN] = MeasurementConstants.AVAIL_UP;
                data[IND_MAX] = MeasurementConstants.AVAIL_PAUSED;
                rtn.put(key, data);
            }

            data[IND_MIN] = Math.min(data[IND_MIN], ((Double) objs[1]).doubleValue());
            data[IND_MAX] = Math.max(data[IND_MAX], ((Double) objs[3]).doubleValue());

            // Expect data to be sorted by end time, so that the last value
            // returned is the final count and the last value
            data[IND_CFG_COUNT] = ((java.lang.Number) objs[4]).doubleValue();
            data[IND_LAST_TIME] = ((Double) objs[2]).doubleValue();

            data[IND_UP_TIME] += ((Double) objs[5]).doubleValue();
            data[IND_TOTAL_TIME] += ((Double) objs[6]).doubleValue();
        }

        // Now calculate the average value
        for (double[] data : rtn.values()) {
            data[IND_AVG] += data[IND_UP_TIME] / data[IND_TOTAL_TIME];
        }
        return rtn;
    }

    /**
     * @param resources Collection may be of type {@link Resource},
     *        {@link AppdefEntityId}, {@link AppdefEntityValue},
     *        {@link AppdefResourceValue} or {@link Integer}
     * @param measCache Map<Integer, List> optional arg (may be null) to supply
     *        measurement id(s) of ResourceIds. Integer => Resource.getId()
     * @return Map<Integer, MetricValue> Integer => Measurement.getId()
     * 
     */
    public Map<Integer, MetricValue> getLastAvail(Collection<? extends Object> resources,
                                                  Map<Integer, List<Measurement>> measCache) {
        final Set<Integer> midsToGet = new HashSet<Integer>(resources.size());
        final List<Resource> resToGet = new ArrayList<Resource>(resources.size());
        for (Object o : resources) {
            Resource resource = null;
            if (o instanceof AppdefEntityValue) {
                AppdefEntityValue rv = (AppdefEntityValue) o;
                AppdefEntityID aeid = rv.getID();
                resource = resourceManager.findResource(aeid);
            } else if (o instanceof AppdefEntityID) {
                AppdefEntityID aeid = (AppdefEntityID) o;
                resource = resourceManager.findResource(aeid);
            } else if (o instanceof Resource) {
                resource = (Resource) o;
            } else if (o instanceof AppdefResourceValue) {
                AppdefResourceValue res = (AppdefResourceValue) o;
                AppdefEntityID aeid = res.getEntityId();
                resource = resourceManager.findResource(aeid);
            } else {
                resource = resourceManager.findResourceById((Integer) o);
            }
            List<Measurement> measurements = null;
            if (measCache != null) {
                measurements = measCache.get(resource.getId());
            }
            if (measurements == null || measurements.size() == 0) {
                resToGet.add(resource);
                continue;
            }
            for (Measurement m : measurements) {
                midsToGet.add(m.getId());
            }
        }
        if (!resToGet.isEmpty()) {
            final Collection<List<Measurement>> measIds = measurementManager.getAvailMeasurements(resToGet).values();
            for (List<Measurement> measurementList : measIds) {
                for (Measurement m : measurementList) {
                    midsToGet.add(m.getId());
                }
            }
        }
        return getLastAvail((Integer[]) midsToGet.toArray(new Integer[0]));
    }

    /**
     * 
     */
    public MetricValue getLastAvail(Measurement m) {
        Map<Integer, MetricValue> map = getLastAvail(new Integer[] { m.getId() });
        MetricValue mv = (MetricValue) map.get(m.getId());
        if (mv == null) {
            return new MetricValue(AVAIL_UNKNOWN, System.currentTimeMillis());
        } else {
            return mv;
        }
    }

    /**
     * Only unique measurement ids should be passed in. Duplicate measurement
     * ids will be filtered out from the returned Map if present.
     * 
     * @return {@link Map} of {@link Integer} to {@link MetricValue} Integer is
     *         the measurementId
     * 
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, MetricValue> getLastAvail(Integer[] mids) {
        if (mids.length == 0) {
            return Collections.EMPTY_MAP;
        }
        // Don't modify callers array
        final List<Integer> midList = Collections.unmodifiableList(Arrays.asList(mids));
        final Map<Integer, MetricValue> rtn = new HashMap<Integer, MetricValue>(midList.size());
        final List<AvailabilityDataRLE> list = availabilityDataDAO.findLastAvail(midList);
        for (AvailabilityDataRLE avail : list) {
            final Integer mid = avail.getMeasurement().getId();
            final AvailabilityMetricValue mVal = new AvailabilityMetricValue(avail.getAvailVal(), avail.getStartime(),
                avail.getApproxEndtime());
            rtn.put(mid, mVal);
        }
        // fill in missing measurements
        final long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), MeasurementConstants.MINUTE);

        for (Integer mid : midList) {

            if (!rtn.containsKey(mid)) {
                final MetricValue mVal = new MetricValue(AVAIL_UNKNOWN, now);
                rtn.put(mid, mVal);
            }
        }

        return rtn;
    }

    /**
     * @param includes List<Integer> of mids. If includes is null then all
     *        unavail entities will be returned.
     * 
     */
    public List<DownMetricValue> getUnavailEntities(List<Integer> includes) {
        List<DownMetricValue> rtn;
        if (includes != null) {
            rtn = new ArrayList<DownMetricValue>(includes.size());
        } else {
            rtn = new ArrayList<DownMetricValue>();
        }
        List<AvailabilityDataRLE> unavails = availabilityDataDAO.getDownMeasurements(includes);
        for (AvailabilityDataRLE rle : unavails) {
            Measurement meas = rle.getMeasurement();
            long timestamp = rle.getStartime();
            Integer mid = meas.getId();
            MetricValue val = new MetricValue(AVAIL_DOWN, timestamp);
            rtn.add(new DownMetricValue(meas.getEntityId(), mid, val));
        }
        return rtn;
    }

    /**
     * Add a single Availablility Data point.
     * @mid The Measurement id
     * @mval The MetricValue to store.
     * 
     */
    public void addData(Integer mid, MetricValue mval) {
        List<DataPoint> l = new ArrayList<DataPoint>(1);
        l.add(new DataPoint(mid, mval));
        addData(l);
    }

    /**
     * Process Availability data. The default behavior is to send the data
     * points to the event handlers.
     * 
     * @param availPoints List of DataPoints
     * 
     * 
     */
    public void addData(List<DataPoint> availPoints) {
        addData(availPoints, true);
    }

    /**
     * Process Availability data.
     * 
     * @param availPoints List of DataPoints
     * @param sendData Indicates whether to send the data to event handlers. The
     *        default behavior is true. If false, the calling method should call
     *        sendDataToEventHandlers directly afterwards.
     * 
     * 
     */
    public void addData(List<DataPoint> availPoints, boolean sendData) {
        if (availPoints == null || availPoints.size() == 0) {
            return;
        }
        List<DataPoint> updateList = new ArrayList<DataPoint>(availPoints.size());
        List<DataPoint> outOfOrderAvail = new ArrayList<DataPoint>(availPoints.size());
        AvailabilityCache cache = AvailabilityCache.getInstance();
        createMap = new HashMap<DataPoint, AvailabilityDataRLE>();
        removeMap = new HashMap<DataPoint, AvailabilityDataRLE>();
        final boolean debug = _log.isDebugEnabled();
        long begin = -1;
        Map<Integer, StringBuilder> state = null;
        synchronized (cache) {
            try {
                cache.beginTran();
                begin = getDebugTime(debug);
                updateCache(availPoints, updateList, outOfOrderAvail);
                debugTimes(begin, "updateCache", availPoints.size());
                begin = getDebugTime(debug);
                setCurrAvails(outOfOrderAvail, updateList);
                debugTimes(begin, "setCurrAvails", outOfOrderAvail.size() + updateList.size());
                state = captureCurrAvailState();
                begin = getDebugTime(debug);
                updateStates(updateList);
                debugTimes(begin, "updateStates", updateList.size());
                begin = getDebugTime(debug);
                updateOutOfOrderState(outOfOrderAvail);
                flushCreateAndRemoves();
                logErrorInfo(state, availPoints);
                debugTimes(begin, "updateOutOfOrderState", outOfOrderAvail.size());
                cache.commitTran();
            } catch (Throwable e) {
                logErrorInfo(state, availPoints);
                _log.error(e.getMessage(), e);
                cache.rollbackTran();
                throw new SystemException(e);
            } finally {
                createMap = null;
                removeMap = null;
                currAvails = null;
            }
        }
        ConcurrentStatsCollector.getInstance().addStat(availPoints.size(), AVAIL_MANAGER_METRICS_INSERTED);
        if (sendData) {
            begin = getDebugTime(debug);
            sendDataToEventHandlers(availPoints);
            debugTimes(begin, "sendDataToEventHandlers", availPoints.size());
        }
    }

    private void flushCreateAndRemoves() {
        for (AvailabilityDataRLE rle : removeMap.values()) {
            availabilityDataDAO.remove(rle);
        }
        // for some reason if flush is not run, then create() will throw
        // Hibernate NonUniqueObjectExceptions
        availabilityDataDAO.getSession().flush();
        for (AvailabilityDataRLE rle : createMap.values()) {
            AvailabilityDataId id = new AvailabilityDataId();
            id.setMeasurement(rle.getMeasurement());
            id.setStartime(rle.getStartime());
            availabilityDataDAO.create(rle.getMeasurement(), rle.getStartime(), rle.getEndtime(), rle.getAvailVal());
        }
    }

    private long getDebugTime(final boolean debug) {
        if (debug) {
            return System.currentTimeMillis();
        }
        return -1;
    }

    private void debugTimes(final long begin, final String name, final int points) {
        if (_log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - begin;
            _log.debug("AvailabilityInserter time to " + name + " -> " + time + " ms, points: " + points);
        }
    }

    private void logErrorInfo(final Map<Integer, StringBuilder> oldState, final List<DataPoint> availPoints) {
        if (!_traceLog.isDebugEnabled()) {
            return;
        }
        Integer mid;
        Map<Integer, StringBuilder> currState = captureCurrAvailState();
        if (null != (mid = isAvailDataRLEValid())) {
            logAvailState(oldState, mid);
            logStates(availPoints, mid);
            logAvailState(currState, mid);
        } else {
            _traceLog.debug("RLE Data is valid");
        }
    }

    @SuppressWarnings("unchecked")
    private void setCurrAvails(final List<DataPoint> outOfOrderAvail, final List<DataPoint> updateList) {
        if (outOfOrderAvail.size() == 0 && updateList.size() == 0) {
            currAvails = Collections.EMPTY_MAP;
            return;
        }
        long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), 60000);
        HashSet<Integer> mids = getMidsWithinAllowedDataWindow(updateList, now);
        mids.addAll(getMidsWithinAllowedDataWindow(outOfOrderAvail, now));
        if (mids.size() <= 0) {
            currAvails = Collections.EMPTY_MAP;
            return;
        }
        Integer[] mIds = (Integer[]) mids.toArray(new Integer[0]);
        currAvails = availabilityDataDAO.getHistoricalAvailMap(mIds, now - MAX_DATA_BACKLOG_TIME, false);
    }

    private HashSet<Integer> getMidsWithinAllowedDataWindow(final List<DataPoint> states, final long now) {
        HashSet<Integer> mids = new HashSet<Integer>();
        int i = 0;
        for (Iterator<DataPoint> it = states.iterator(); it.hasNext(); i++) {
            DataPoint pt = it.next();
            long timestamp = pt.getTimestamp();
            // only allow data for the last MAX_DATA_BACKLOG_TIME ms
            // this way we don't have to bring too much into memory which could
            // severely impact performance
            if ((now - timestamp) > MAX_DATA_BACKLOG_TIME) {
                it.remove();
                long days = (now - timestamp) / MeasurementConstants.DAY;
                _log.warn(" Avail measurement came in " + days + " days " + " late, dropping: timestamp=" + timestamp +
                          " measId=" + pt.getMetricId() + " value=" + pt.getMetricValue());
                continue;
            }
            Integer mId = pt.getMetricId();
            if (!mids.contains(mId)) {
                mids.add(mId);
            }
        }
        return mids;
    }

    private void updateDup(DataPoint state, AvailabilityDataRLE dup) throws BadAvailStateException {
        if (dup.getAvailVal() == state.getValue()) {
            // nothing to do
        } else if (dup.getAvailVal() != AVAIL_DOWN) {
            String msg = "New DataPoint and current DB value for " + "MeasurementId " + state.getMetricId() +
                         " / timestamp " + state.getTimestamp() + " have conflicting states.  " +
                         "Since a non-zero rle value cannot be overridden, no update." + "\ncurrent rle value -> " +
                         dup +
                         // ask Juilet Sierra why (js) is here
                         ":(js):\npoint trying to override current rle -> " + state;
            throw new BadAvailStateException(msg);
        } else {
            Measurement meas = dup.getMeasurement();
            long newStartime = dup.getStartime() + meas.getInterval();
            insertPointOnBoundry(dup, newStartime, state);
        }
    }

    /**
     * sets avail's startime to newStartime and prepends a new avail obj from
     * avail.getStartime() to newStartime with a value of state.getValue() Used
     * specifically for a point which collides with a RLE on its startime
     */
    private void insertPointOnBoundry(AvailabilityDataRLE avail, long newStartime, DataPoint pt)
        throws BadAvailStateException {
        if (newStartime <= avail.getStartime()) {
            return;
        }
        Measurement meas = avail.getMeasurement();
        if (avail.getEndtime() == MAX_AVAIL_TIMESTAMP) {
            AvailabilityCache cache = AvailabilityCache.getInstance();
            DataPoint tmp = cache.get(pt.getMetricId());
            if (tmp == null || pt.getTimestamp() >= tmp.getTimestamp()) {
                updateAvailVal(avail, pt.getValue());
            } else {
                prependState(pt, avail);
            }
        } else if (newStartime < avail.getEndtime()) {
            prependState(pt, avail);
        } else if (newStartime > avail.getEndtime()) {
            removeAvail(avail);
        } else if (newStartime == avail.getEndtime()) {
            AvailabilityDataRLE after = findAvailAfter(pt);
            if (after == null) {
                throw new BadAvailStateException("Availability measurement_id=" + pt.getMetricId() +
                                                 " does not have a availability point after timestamp " +
                                                 pt.getTimestamp());
            }
            if (after.getAvailVal() == pt.getValue()) {
                // resolve by removing the before obj, if it exists,
                // and sliding back the start time of after obj
                AvailabilityDataRLE before = findAvailBefore(pt);
                if (before == null) {
                    after = updateStartime(after, avail.getStartime());
                } else if (before.getAvailVal() == after.getAvailVal()) {
                    removeAvail(avail);
                    removeAvail(before);
                    after = updateStartime(after, before.getStartime());
                }
            } else {
                // newStartime == avail.getEndtime() &&
                // newStartime == after.getStartime() &&
                // newStartime < after.getEndtime() &&
                // pt.getValue() != after.getAvailVal()
                // therefore, need to push back startTime and set the value
                long interval = meas.getInterval();
                if ((after.getStartime() + interval) < after.getEndtime()) {
                    prependState(pt, after);
                } else {
                    DataPoint afterPt = new DataPoint(meas.getId().intValue(), after.getAvailVal(), after.getStartime());
                    AvailabilityDataRLE afterAfter = findAvailAfter(afterPt);
                    if (afterAfter.getAvailVal() == pt.getValue()) {
                        removeAvail(after);
                        afterAfter = updateStartime(afterAfter, pt.getTimestamp());
                    } else {
                        updateAvailVal(after, pt.getValue());
                    }
                }
            }
        }
    }

    private AvailabilityDataRLE findAvail(DataPoint state) {
        Integer mId = state.getMetricId();
        Collection<AvailabilityDataRLE> rles = currAvails.get(mId);
        long start = state.getTimestamp();
        for (AvailabilityDataRLE rle : rles) {
            if (rle.getStartime() == start) {
                return rle;
            }
        }
        return null;
    }

    private AvailabilityDataRLE findAvailAfter(DataPoint state) {
        final Integer mId = state.getMetricId();
        final TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        final long start = state.getTimestamp();
        final AvailabilityDataRLE tmp = new AvailabilityDataRLE();
        // tailSet is inclusive so we need to add 1 to start
        tmp.setStartime(start + 1);
        final SortedSet<AvailabilityDataRLE> set = rles.tailSet(tmp);
        if (set.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE) set.first();
    }

    private AvailabilityDataRLE findAvailBefore(DataPoint state) {
        Integer mId = state.getMetricId();
        TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        long start = state.getTimestamp();
        AvailabilityDataRLE tmp = new AvailabilityDataRLE();
        // headSet is inclusive so we need to subtract 1 from start
        tmp.setStartime(start - 1);
        SortedSet<AvailabilityDataRLE> set = rles.headSet(tmp);
        if (set.size() == 0) {
            return null;
        }
        return set.last();
    }

    private void merge(DataPoint state) throws BadAvailStateException {
        AvailabilityDataRLE dup = findAvail(state);
        if (dup != null) {
            updateDup(state, dup);
            return;
        }
        AvailabilityDataRLE before = findAvailBefore(state);
        AvailabilityDataRLE after = findAvailAfter(state);
        if (before == null && after == null) {
            // this shouldn't happen here
            Measurement meas = getMeasurement(state.getMetricId());
            create(meas, state.getTimestamp(), state.getValue());
        } else if (before == null) {
            if (after.getAvailVal() != state.getValue()) {
                prependState(state, after);
            } else {
                after = updateStartime(after, state.getTimestamp());
            }
        } else if (after == null) {
            // this shouldn't happen here
            updateState(state);
        } else {
            insertAvail(before, after, state);
        }
    }

    private void insertAvail(AvailabilityDataRLE before, AvailabilityDataRLE after, DataPoint state) {

        if (state.getValue() != after.getAvailVal() && state.getValue() != before.getAvailVal()) {
            Measurement meas = getMeasurement(state.getMetricId());
            long pivotTime = state.getTimestamp() + meas.getInterval();
            create(meas, state.getTimestamp(), pivotTime, state.getValue());
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, pivotTime);
        } else if (state.getValue() == after.getAvailVal() && state.getValue() != before.getAvailVal()) {
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, state.getTimestamp());
        } else if (state.getValue() != after.getAvailVal() && state.getValue() == before.getAvailVal()) {
            // this is fine
        } else if (state.getValue() == after.getAvailVal() && state.getValue() == before.getAvailVal()) {
            // this should never happen or else there is something wrong
            // in the code
            String msg = "AvailabilityData [" + before + "] and [" + after +
                         "] have the same values.  This should not be the case.  " + "Cleaning up";
            _log.warn(msg);
            updateEndtime(before, after.getEndtime());
            removeAvail(after);
        }
    }

    private boolean prependState(DataPoint state, AvailabilityDataRLE avail) {
        AvailabilityDataRLE before = findAvailBefore(state);
        Measurement meas = avail.getMeasurement();
        if (before != null && before.getAvailVal() == state.getValue()) {
            long newStart = state.getTimestamp() + meas.getInterval();
            updateEndtime(before, newStart);
            avail = updateStartime(avail, newStart);
        } else {
            long newStart = state.getTimestamp() + meas.getInterval();
            long endtime = newStart;
            avail = updateStartime(avail, newStart);
            create(avail.getMeasurement(), state.getTimestamp(), endtime, state.getValue());
        }
        return true;
    }

    private void updateAvailVal(AvailabilityDataRLE avail, double val) {
        Measurement meas = avail.getMeasurement();
        DataPoint state = new DataPoint(meas.getId().intValue(), val, avail.getStartime());
        AvailabilityDataRLE before = findAvailBefore(state);
        if (before == null || before.getAvailVal() != val) {
            avail.setAvailVal(val);
        } else {
            removeAvail(before);
            avail = updateStartime(avail, before.getStartime());
            avail.setAvailVal(val);
        }
    }

    private void updateEndtime(AvailabilityDataRLE avail, long endtime) {
        avail.setEndtime(endtime);
    }

    private AvailabilityDataRLE updateStartime(AvailabilityDataRLE avail, long start) {
        // this should not be the case here, but want to make sure and
        // avoid HibernateUniqueKeyExceptions :(
        AvailabilityDataRLE tmp;
        Measurement meas = avail.getMeasurement();
        Integer mId = meas.getId();
        DataPoint tmpState = new DataPoint(mId.intValue(), avail.getAvailVal(), start);
        if (null != (tmp = findAvail(tmpState))) {
            removeAvail(tmp);
        }
        removeAvail(avail);
        return create(meas, start, avail.getEndtime(), avail.getAvailVal());
    }

    private void removeAvail(AvailabilityDataRLE avail) {
        long start = avail.getStartime();
        Integer mId = avail.getMeasurement().getId();
        TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        if (rles.remove(avail)) {
            DataPoint key = new DataPoint(mId.intValue(), avail.getAvailVal(), start);
            createMap.remove(key);
            removeMap.put(key, avail);
        }
    }

    private AvailabilityDataRLE getLastAvail(DataPoint state) {
        Integer mId = state.getMetricId();
        TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        if (rles.size() == 0) {
            return null;
        }
        return rles.last();
    }

    private AvailabilityDataRLE create(Measurement meas, long start, long end, double val) {
        AvailabilityDataRLE rtn = _createAvail(meas, start, end, val);
        createMap.put(new DataPoint(meas.getId().intValue(), val, start), rtn);
        Integer mId = meas.getId();
        Collection<AvailabilityDataRLE> rles = currAvails.get(mId);
        rles.add(rtn);
        return rtn;
    }

    private AvailabilityDataRLE _createAvail(Measurement meas, long start, long end, double val) {
        AvailabilityDataRLE rtn = new AvailabilityDataRLE();
        rtn.setMeasurement(meas);
        rtn.setStartime(start);
        rtn.setEndtime(end);
        rtn.setAvailVal(val);
        return rtn;
    }

    private AvailabilityDataRLE create(Measurement meas, long start, double val) {
        AvailabilityDataRLE rtn = _createAvail(meas, start, MAX_AVAIL_TIMESTAMP, val);
        createMap.put(new DataPoint(meas.getId().intValue(), val, start), rtn);
        Integer mId = meas.getId();
        Collection<AvailabilityDataRLE> rles = currAvails.get(mId);
        // I am assuming that this will be cleaned up by the caller where it
        // will update the rle before rtn if one exists
        rles.add(rtn);
        return rtn;
    }

    private boolean updateState(DataPoint state) throws BadAvailStateException {
        AvailabilityDataRLE avail = getLastAvail(state);
        final boolean debug = _log.isDebugEnabled();
        long begin = -1;
        if (avail == null) {
            Measurement meas = getMeasurement(state.getMetricId());
            create(meas, state.getTimestamp(), state.getValue());
            return true;
        } else if (state.getTimestamp() < avail.getStartime()) {
            if (debug) {
                begin = System.currentTimeMillis();
            }
            merge(state);
            if (debug) {
                long now = System.currentTimeMillis();
                _log.debug("updateState.merge() -> " + (now - begin) + " ms");
            }
            return false;
        } else if (state.getTimestamp() == avail.getStartime() && state.getValue() != avail.getAvailVal()) {
            if (debug) {
                begin = System.currentTimeMillis();
            }
            updateDup(state, avail);
            if (debug) {
                long now = System.currentTimeMillis();
                _log.debug("updateState.updateDup() -> " + (now - begin) + " ms");
            }
            return true;
        } else if (state.getValue() == avail.getAvailVal()) {
            if (debug) {
                _log.debug("no update state == avail " + state + " == " + avail);
            }
            return true;
        }
        if (debug) {
            _log.debug("updating endtime on avail -> " + avail + ", updating to state -> " + state);
        }
        updateEndtime(avail, state.getTimestamp());
        create(avail.getMeasurement(), state.getTimestamp(), state.getValue());
        return true;
    }

    private void updateStates(List<DataPoint> states) {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        if (states.size() == 0) {
            return;
        }
        // as a performance optimization, fetch all the last avails
        // at once, rather than one at a time in updateState()
        final boolean debug = _log.isDebugEnabled();
        for (DataPoint state : states) {
            try {
                // need to check again since there could be multiple
                // states with the same id in the list
                DataPoint currState = cache.get(state.getMetricId());
                if (currState != null && currState.getValue() == state.getValue()) {
                    continue;
                }
                boolean updateCache = updateState(state);
                if (debug) {
                    _log.debug("state " + state + " was updated, cache updated: " + updateCache);
                }
                if (updateCache) {
                    cache.put(state.getMetricId(), state);
                }
            } catch (BadAvailStateException e) {
                _log.warn(e.getMessage());
            }
        }
    }

    private void updateOutOfOrderState(List<DataPoint> outOfOrderAvail) {
        if (outOfOrderAvail.size() == 0) {
            return;
        }
        for (DataPoint state : outOfOrderAvail) {
            try {
                // do not update the cache here, the timestamp is out of order
                merge(state);
            } catch (BadAvailStateException e) {
                _log.warn(e.getMessage());
            }
        }
    }

    private void updateCache(List<DataPoint> availPoints, List<DataPoint> updateList, List<DataPoint> outOfOrderAvail) {
        if (availPoints.size() == 0) {
            return;
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        final boolean debug = _log.isDebugEnabled();
        for (DataPoint pt : availPoints) {
            int id = pt.getMetricId().intValue();
            MetricValue mval = pt.getMetricValue();
            double val = mval.getValue();
            long timestamp = mval.getTimestamp();
            DataPoint newState = new DataPoint(id, val, timestamp);
            DataPoint oldState = cache.get(new Integer(id));
            // we do not want to update the state if it changes
            // instead change it when the db is changed in order
            // to ensure the state of memory to db
            // ONLY update memory state here if there is no change
            if (oldState != null && timestamp < oldState.getTimestamp()) {
                outOfOrderAvail.add(newState);
            } else if (oldState == null || oldState.getValue() == AVAIL_NULL || oldState.getValue() != val) {
                updateList.add(newState);
                if (debug) {
                    String msg = "value of state " + newState + " differs from" + " current value" +
                                 ((oldState != null) ? oldState.toString() : " old state does not exist");
                    _log.debug(msg);
                }
            } else {
                cache.put(new Integer(id), newState);
            }
        }
    }

    private void sendDataToEventHandlers(List<DataPoint> data) {
        int maxCapacity = data.size();
        ArrayList<MeasurementEvent> events = new ArrayList<MeasurementEvent>(maxCapacity);
        Map<Integer, MeasurementEvent> downEvents = new HashMap<Integer, MeasurementEvent>(maxCapacity);
        List<MeasurementZevent> zevents = new ArrayList<MeasurementZevent>(maxCapacity);

        boolean allEventsInteresting = Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        for (DataPoint dp : data) {
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();

            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (RegisteredTriggers.isTriggerInterested(event) || allEventsInteresting) {
                measurementManager.buildMeasurementEvent(event);
                if (event.getValue().getValue() == AVAIL_DOWN) {
                    Resource r = resourceManager.findResource(event.getResource());
                    if (r != null && !r.isInAsyncDeleteState()) {
                        downEvents.put(r.getId(), event);
                    }
                } else {
                    events.add(event);
                }
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }

        if (!downEvents.isEmpty()) {
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance().getHierarchicalAlertingManager().suppressMeasurementEvents(
                downEvents, true);

            if (!downEvents.isEmpty()) {
                events.addAll(downEvents.values());
            }
        }

        if (!events.isEmpty()) {
            messenger.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }

        if (!zevents.isEmpty()) {
            ZeventManager.getInstance().enqueueEventsAfterCommit(zevents);
        }
    }

    /**
     * This method should only be called by the AvailabilityCheckService and is
     * used to filter availability data points based on hierarchical alerting
     * rules.
     * 
     * 
     */
    public void sendDataToEventHandlers(Map<Integer, DataPoint> data) {
        int maxCapacity = data.size();
        Map<Integer, MeasurementEvent> events = new HashMap<Integer, MeasurementEvent>(maxCapacity);
        List<MeasurementZevent> zevents = new ArrayList<MeasurementZevent>(maxCapacity);

        boolean allEventsInteresting = Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        for (Integer resourceIdKey : data.keySet()) {

            DataPoint dp = data.get(resourceIdKey);
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (RegisteredTriggers.isTriggerInterested(event) || allEventsInteresting) {
                measurementManager.buildMeasurementEvent(event);
                events.put(resourceIdKey, event);
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }

        if (!events.isEmpty()) {
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance().getHierarchicalAlertingManager().suppressMeasurementEvents(events,
                false);

            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC, new ArrayList<MeasurementEvent>(events.values()));
        }

        if (!zevents.isEmpty()) {
            ZeventManager.getInstance().enqueueEventsAfterCommit(zevents);
        }
    }

    private Measurement getMeasurement(Integer mId) {
        return measurementManager.getMeasurement(mId);
    }

    public static AvailabilityManager getOne() {
        return Bootstrap.getBean(AvailabilityManager.class);
    }

    private Map<Integer, StringBuilder> captureCurrAvailState() {
        if (!_traceLog.isDebugEnabled()) {
            return null;
        }
        Map<Integer, StringBuilder> rtn = new HashMap<Integer, StringBuilder>();
        for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : currAvails.entrySet()) {
            Integer mid = entry.getKey();
            Collection<AvailabilityDataRLE> rles = entry.getValue();
            StringBuilder buf = new StringBuilder("\n");
            for (AvailabilityDataRLE rle : rles) {
                buf.append(mid).append(" | ").append(rle.getStartime()).append(" | ").append(rle.getEndtime()).append(
                    " | ").append(rle.getAvailVal()).append("\n");
            }
            rtn.put(mid, buf);
        }
        return rtn;
    }

    private void logAvailState(Map<Integer, StringBuilder> availState, Integer mid) {
        StringBuilder buf = (StringBuilder) availState.get(mid);
        _traceLog.debug(buf.toString());
    }

    private void logStates(List<DataPoint> states, Integer mid) {
        StringBuilder log = new StringBuilder("\n");
        for (DataPoint pt : states) {
            if (!pt.getMetricId().equals(mid)) {
                continue;
            }
            log.append(pt.getMetricId()).append(" | ").append(pt.getTimestamp()).append(" | ").append(
                pt.getMetricValue()).append("\n");
        }
        _traceLog.debug(log.toString());
    }

    private Integer isAvailDataRLEValid() {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        synchronized (cache) {
            for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : currAvails.entrySet()) {
                Integer mId = entry.getKey();
                Collection<AvailabilityDataRLE> rles = entry.getValue();
                if (!isAvailDataRLEValid(mId, cache.get(mId), rles)) {
                    return mId;
                }
            }
        }
        return null;
    }

    private boolean isAvailDataRLEValid(Integer measId, DataPoint lastPt, Collection<AvailabilityDataRLE> avails) {
        AvailabilityDataRLE last = null;
        Set<Long> endtimes = new HashSet<Long>();
        for (AvailabilityDataRLE avail : avails) {
            Long endtime = new Long(avail.getEndtime());
            if (endtimes.contains(endtime)) {
                _log.error("list for MID=" + measId + " contains two or more of the same endtime=" + endtime);
                return false;
            }
            endtimes.add(endtime);
            if (last == null) {
                last = avail;
                continue;
            }
            if (last.getAvailVal() == avail.getAvailVal()) {
                _log.error("consecutive availpoints have the same value");
                return false;
            } else if (last.getEndtime() != avail.getStartime()) {
                _log.error("there are gaps in the availability table");
                return false;
            } else if (last.getStartime() > avail.getStartime()) {
                _log.error("startime availability is out of order");
                return false;
            } else if (last.getEndtime() > avail.getEndtime()) {
                _log.error("endtime availability is out of order");
                return false;
            }
            last = avail;
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        if (((DataPoint) cache.get(measId)).getValue() != lastPt.getValue()) {
            _log.error("last avail data point does not match cache");
            return false;
        }
        return true;
    }
}

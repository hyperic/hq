/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.sql.SQLException;
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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentDAO;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.MaintenanceEvent;
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
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AvailabityManagerImpl class is a stateless session bean that can be used
 * to retrieve Availability Data RLE points
 * 
 */
@Service
@Transactional
public class AvailabilityManagerImpl implements AvailabilityManager {

    private final Log log = LogFactory.getLog(AvailabilityManagerImpl.class);
    private final Log traceLog = LogFactory.getLog(AvailabilityManagerImpl.class.getName() + "Trace");
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

    private static final long MAX_DATA_BACKLOG_TIME = 7 * MeasurementConstants.DAY;

    private AuthzSubjectManager authzSubjectManager;

    private MeasurementManager measurementManager;

    private ResourceGroupManager groupManager;

    private ResourceManager resourceManager;

    private MessagePublisher messenger;

    private AvailabilityDataDAO availabilityDataDAO;

    private AvailabilityFallbackCheckQue fallbackCheckQue;
    private MeasurementDAO measurementDAO;
    private MessagePublisher messagePublisher;
    private RegisteredTriggers registeredTriggers;
    private AvailabilityCache availabilityCache;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private AgentDAO agentDAO;
    
    @Autowired
    public AvailabilityManagerImpl(AuthzSubjectManager authzSubjectManager, ResourceManager resourceManager,
            ResourceGroupManager groupManager, MessagePublisher messenger, AvailabilityDataDAO availabilityDataDAO,
            MeasurementDAO measurementDAO, MessagePublisher messagePublisher, RegisteredTriggers registeredTriggers,
            AvailabilityCache availabilityCache, AgentDAO agentDAO, ConcurrentStatsCollector concurrentStatsCollector) {
        this.authzSubjectManager = authzSubjectManager;
        this.resourceManager = resourceManager;
        this.groupManager = groupManager;
        this.messenger = messenger;
        this.availabilityDataDAO = availabilityDataDAO;
        this.measurementDAO = measurementDAO;
        this.messagePublisher = messagePublisher;
        this.registeredTriggers = registeredTriggers;
        this.availabilityCache = availabilityCache;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.agentDAO = agentDAO;
        //this.measurementIDsMonitoredByServer = new HashSet<Integer>();
        this.fallbackCheckQue = new AvailabilityFallbackCheckQue();
    }

    @PostConstruct
    public void initStatsCollector() {
        concurrentStatsCollector.register(ConcurrentStatsCollector.AVAIL_MANAGER_METRICS_INSERTED);
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
    @Transactional(readOnly = true)
    public Measurement getAvailMeasurement(Resource resource) {
        return measurementDAO.findAvailMeasurement(resource);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<Measurement> getPlatformResources() {
        return measurementDAO.findAvailMeasurementsByInstances(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null);
    }

    
    public AvailabilityFallbackCheckQue getFallbackCheckQue() {
    	return this.fallbackCheckQue;
    }
    
    
    /**
     * @return Down time in ms for the Resource availability
     * 
     * 
     */
    @Transactional(readOnly = true)
    public long getDowntime(Resource resource, long rangeBegin, long rangeEnd) throws MeasurementNotFoundException {
        Measurement meas = measurementDAO.findAvailMeasurement(resource);
        if (meas == null) {
            throw new MeasurementNotFoundException("Availability measurement " + "not found for resource "
                    + resource.getId());
        }
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO
                .getHistoricalAvails(meas, rangeBegin, rangeEnd, false);
        long rtn = 0l;
        for (AvailabilityDataRLE avail : availInfo) {
            if (avail.getAvailVal() != AVAIL_DOWN) {
                continue;
            }
            long dataDownEndTime = avail.getEndtime();

            if (dataDownEndTime == MAX_AVAIL_TIMESTAMP) {
                dataDownEndTime = System.currentTimeMillis();
            }
            if (dataDownEndTime > rangeEnd) {
                // use range end if the data down end time is greater than the
                // end range.
                dataDownEndTime = rangeEnd;
            }
            long dataDownStartTime = avail.getStartime();
            // Make sure the start of the down time is not earlier then the
            // begin time
            if (dataDownStartTime < rangeBegin) {
                dataDownStartTime = rangeBegin;
            }
            rtn += (dataDownEndTime - dataDownStartTime);
        }
        return rtn;
    }

    /**
     * @return List of all measurement ids for availability, ordered
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Integer> getAllAvailIds() {
        return measurementDAO.findAllAvailIds();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Map<Integer, List<Measurement>> getAvailMeasurementChildren(List<Integer> resourceIds,
            String resourceRelationType) {
        final List<Object[]> objects = measurementDAO.findRelatedAvailMeasurements(resourceIds, resourceRelationType);
        return convertAvailMeasurementListToMap(objects);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Map<Integer, List<Measurement>> getAvailMeasurementParent(List<Integer> resourceIds,
            String resourceRelationType) {
        final List<Object[]> objects = measurementDAO.findParentAvailMeasurements(resourceIds, resourceRelationType);
        return convertAvailMeasurementListToMap(objects);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, List<Measurement>> getAvailMeasurementDirectParent(List<Integer> resourceIds,
            String resourceRelationType) {
        final List<Object[]> objects = measurementDAO.findDirectParentAvailMeasurements(resourceIds, resourceRelationType);
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
     * Get Availability measurements (disabled) in scheduled downtime.
     */
    @Transactional(readOnly = true)
    public Map<Integer, Measurement> getAvailMeasurementsInDowntime(Collection<AppdefEntityID> eids) {
        Map<Integer, Measurement> measMap = new HashMap<Integer, Measurement>();

        try {
            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

            // TODO: Resolve circular dependency and autowire
            // MaintenanceEventManager
            List<MaintenanceEvent> events = PermissionManagerFactory.getInstance().getMaintenanceEventManager()
                    .getMaintenanceEvents(overlord, MaintenanceEvent.STATE_RUNNING);

            for (MaintenanceEvent event : events) {
                AppdefEntityID entityId = event.getAppdefEntityID();
                Collection<Resource> resources = null;

                if (entityId.isGroup()) {
                    ResourceGroup group = groupManager.findResourceGroupById(entityId.getId());
                    resources = groupManager.getMembers(group);
                } else {
                    Resource resource = resourceManager.findResource(entityId);
                    resources = Collections.singletonList(resource);
                }

                for (Resource resource : resources) {
                    List<Measurement> measurements = getAvailMeasurementChildren(resource,
                            AuthzConstants.ResourceEdgeContainmentRelation);

                    measurements.add(getAvailMeasurement(resource));

                    if (!measurements.isEmpty()) {
                        for (Measurement m : measurements) {
                            if (m == null) {
                                // measurement could be null if resource has not
                                // been configured
                                continue;
                            }
                            Resource r = m.getResource();
                            if (r == null || r.isInAsyncDeleteState()) {
                                continue;
                            }
                            // availability measurement in scheduled downtime
                            // are disabled
                            if (!m.isEnabled() && eids.contains(AppdefUtil.newAppdefEntityId(r))) {
                                measMap.put(r.getId(), m);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not find availability measurements in downtime: " + e.getMessage(), e);
        }

        return measMap;
    }

    /**
     * TODO: Can this method be combined with the one that takes an array?
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> getHistoricalAvailData(Measurement m, long begin, long end, PageControl pc,
            boolean prependUnknowns) {
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO.getHistoricalAvails(m, begin, end, pc.isDescending());
        return getPageList(availInfo, begin, end, m.getInterval(), prependUnknowns);
    }

    /**
     * Fetches historical availability encapsulating the specified time range
     * for each measurement id in mids;
     * 
     * @param mids
     *            measurement ids
     * @param begin
     *            time range start
     * @param end
     *            time range end
     * @param interval
     *            interval of each time range window
     * @param pc
     *            page control
     * @param prependUnknowns
     *            determines whether to prepend AVAIL_UNKNOWN if the
     *            corresponding time window is not accounted for in the
     *            database. Since availability is contiguous this will not occur
     *            unless the time range precedes the first availability point.
     * @see org.hyperic.hq.measurement.MeasurementConstants#AVAIL_UNKNOWN
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> getHistoricalAvailData(Integer[] mids, long begin, long end, long interval,
            PageControl pc, boolean prependUnknowns) {
        if (mids.length == 0) {
            return new PageList<HighLowMetricValue>();
        }
        List<AvailabilityDataRLE> availInfo = availabilityDataDAO.getHistoricalAvails(mids, begin, end,
                pc.isDescending());
        return getPageList(availInfo, begin, end, interval, prependUnknowns);
    }

    /**
     * Get the list of Raw RLE objects for a resource
     * 
     * @return List<AvailabilityDataRLE>
     * 
     */
    @Transactional(readOnly = true)
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
        for (Iterator<AvailabilityDataRLE> it = availInfo.iterator(); it.hasNext();) {
            AvailabilityDataRLE rle = it.next();
            long availStartime = rle.getStartime();
            long availEndtime = rle.getEndtime();
            //skip measurements that are before first time slot
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
                //while next time slot is after measurement time range
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
                        String msg = "Measurement, " + measId + ", for interval " + begin + " - " + end
                                + " did not return a value for range " + curr + " - " + (curr + interval);
                        log.warn(msg);
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
        HighLowMetricValue val;
        if (avails.size() == 1) {
            AvailabilityDataRLE rle = avails.get(0);
            val = new HighLowMetricValue(rle.getAvailVal(), timestamp);
        } else if (avails.size() == 2) {
            AvailabilityDataRLE rle = null;
            // HHQ-5244
            // If there are only two RLEs then averaging them doesn't make sense
            // as it can make
            // a value of 0 show up as .5 for an interval
            // The following logic makes sure that the actual value is provided
            if (avails.get(0).getEndtime() < timestamp && avails.get(1).getStartime() >= timestamp) {
                rle = avails.get(0);
            } else {
                rle = avails.get(1);
            }
            val = new HighLowMetricValue(rle.getAvailVal(), timestamp);
        }
        else {
            double value = 0;
            for (AvailabilityDataRLE rle : avails) {
                double availVal = rle.getAvailVal();
                value += availVal;
            }
            value = value / avails.size();
            val = new HighLowMetricValue(value, timestamp);
        }
        val.incrementCount();
        return val;
    }

    /**
     * @return {@link Map} of {@link Measurement} to {@link double[]}. Array is
     *         comprised of 5 elements: [IND_MIN] [IND_AVG] [IND_MAX]
     *         [IND_CFG_COUNT] [IND_LAST_TIME]
     * 
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataByTemplate(Integer[] mids, long begin, long end) {
        List<Object[]> avails = availabilityDataDAO.findAggregateAvailability(mids, begin, end);
        return getAggData(avails, true);
    }

    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataAndAvailUpByMetric(final List<Integer> mids, long begin, long end)
    throws SQLException {
        Map<Integer,Double> avails = availabilityDataDAO.findAggregateAvailabilityUp(mids, begin, end);
        Map<Integer, double[]> msmtToAvg = new HashMap<Integer, double[]>();
        Set<Integer> unavailMsmtsIds = new HashSet<Integer>(mids);
        if (null != avails) {
            for (Map.Entry<Integer, Double> availIdToAvg : avails.entrySet()) {
                Integer availId = availIdToAvg.getKey();
                if (!unavailMsmtsIds.remove(availId)) {
                    throw new RuntimeException("unknown availability measurement returned while querying for availability data");
                }
                Double availAvg = availIdToAvg.getValue();
                double[] data = new double[IND_TOTAL_TIME + 1];
                data[IND_AVG] = availAvg.doubleValue();
                msmtToAvg.put(availId, data);
            }
        }
        
        // resources which were not up in the time frame, wouldn't be returned, and have to be initialized
        final double[] availDownData = new double[IND_TOTAL_TIME + 1];
        for (Integer unavailMsmtId : unavailMsmtsIds) {
            msmtToAvg.put(unavailMsmtId, availDownData);
        }
        return msmtToAvg;

    }

    /**
     * @return {@link Map} of {@link MeasurementTemplate.getId} to {@link
     *         double[]}. Array is comprised of 5 elements: [IND_MIN] [IND_AVG]
     *         [IND_MAX] [IND_CFG_COUNT] [IND_LAST_TIME]
     * 
     */
    @Transactional(readOnly = true)
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
            data[IND_CFG_COUNT] += (objs[4] == null) ? 0 : ((java.lang.Number) objs[4]).doubleValue();
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
     * @param resources
     *            Collection may be of type {@link Resource},
     *            {@link AppdefEntityId}, {@link AppdefEntityValue},
     *            {@link AppdefResourceValue} or {@link Integer}
     * @param measCache
     *            Map<Integer, List> optional arg (may be null) to supply
     *            measurement id(s) of ResourceIds. Integer => Resource.getId().
     *            If a measurement is not specified in the measCache parameter
     *            it will be added to the map
     * @return Map<Integer, MetricValue> Integer => Measurement.getId()
     * 
     */
    @Transactional(readOnly = true)
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
            try {
                if (resource == null || resource.isInAsyncDeleteState()) {
                    continue;
                }
            } catch (ObjectNotFoundException e) {
                // resource is in async delete state, ignore
                log.debug("resource not found from object=" + o, e);
                continue;
            }
            if (measCache != null) {
                measurements = measCache.get(resource.getId());
            }
            if (measurements == null || measurements.size() == 0) {
                resToGet.add(resource);
                continue;
            }
            for (Measurement m : measurements) {
                // populate the Map if value doesn't exist
                if (measCache != null) {
                    List<Measurement> measids = measCache.get(m.getResource().getId());
                    if (measids == null) {
                        measids = new ArrayList<Measurement>();
                        measids.add(m);
                        measCache.put(m.getResource().getId(), measids);
                    }

                }
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Map<Integer, MetricValue> getLastAvail(Integer[] mids) {
        if (mids.length == 0) {
            return Collections.emptyMap();
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
     * @return {@link Map} of {@link Integer} representing {@link Resource}Id to {@link DownMetricValue}
     */
    @Transactional(readOnly = true)
    public Map<Integer, DownMetricValue> getUnavailResMap() {
        final Map<Integer, DownMetricValue> rtn = new HashMap<Integer, DownMetricValue>();
        final List<AvailabilityDataRLE> unavails = availabilityDataDAO.getDownMeasurements(null);
        for (final AvailabilityDataRLE rle : unavails) {
            final Measurement meas = rle.getMeasurement();
            final Resource resource = meas.getResource();
            if (resource == null || resource.isInAsyncDeleteState()) {
                continue;
            }
            final long timestamp = rle.getStartime();
            final Integer mid = meas.getId();
            final MetricValue val = new MetricValue(AVAIL_DOWN, timestamp);
            rtn.put(resource.getId(), new DownMetricValue(meas.getEntityId(), mid, val));
        }
        return rtn;
    }

    /**
     * @param includes List<Integer> of mids. If includes is null then all
     *        unavail entities will be returned.
     * 
     */
    @Transactional(readOnly = true)
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
     * 
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
     * @param availPoints
     *            List of DataPoints
     * 
     * 
     */
    public void addData(List<DataPoint> availPoints) {
        addData(availPoints, true, false);
    }

    
    public void addData(List<DataPoint> availDataPoints, boolean sendData) {
        addData(availDataPoints, sendData, false);
    }
    
    
    /**
     * Process Availability data.
     * For each measurement Id (for each resource's availability):
     * If Availability Value is the same as in the DB (AvailabilityDataRLE), only its timestamp is updated in the availabilityCache.
     * Otherwise (availability status changed) - update the availabilityCache and the DB record.
     * @param availPoints List of DataPoints
     * @param sendData Indicates whether to send the data to event handlers. The
     *        default behavior is true. If false, the calling method should call
     *        sendDataToEventHandlers directly afterwards.
     * 
     * @param availPoints
     *            List of DataPoints
     * @param sendData
     *            Indicates whether to send the data to event handlers. The
     *            default behavior is true. If false, the calling method should
     *            call sendDataToEventHandlers directly afterwards.
     * 
     * 
     */
    public void addData(List<DataPoint> availDataPoints, boolean sendData, boolean addedByServer) {
        if (availDataPoints == null || availDataPoints.size() == 0) {
            return;
        }
        Collection<DataPoint> pointsToUpdate = this.fallbackCheckQue.beforeDataUpdate(availDataPoints, addedByServer);
        List<DataPoint> availPoints =  new ArrayList<DataPoint>(pointsToUpdate);
        
        List<DataPoint> updateList = new ArrayList<DataPoint>(availPoints.size());
        List<DataPoint> outOfOrderAvail = new ArrayList<DataPoint>(availPoints.size());
        Map<DataPoint, AvailabilityDataRLE> createMap = new HashMap<DataPoint, AvailabilityDataRLE>();
        Map<DataPoint, AvailabilityDataRLE> removeMap = new HashMap<DataPoint, AvailabilityDataRLE>();
        Map<Integer, StringBuilder> state = null;
        Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails = Collections.emptyMap();
        if (log.isDebugEnabled()) {
            log.debug(availDataPoints);
        }
        synchronized (availabilityCache) {
            try {
                availabilityCache.beginTran();
                updateCache(availPoints, updateList, outOfOrderAvail);
                currAvails = createCurrAvails(outOfOrderAvail, updateList); // get current DB Availability state for the measurements.
                state = captureCurrAvailState(currAvails); // this method is called for logging.
                updateStates(updateList, currAvails, createMap, removeMap);
                updateOutOfOrderState(outOfOrderAvail, currAvails, createMap, removeMap);
                flushCreateAndRemoves(createMap, removeMap);
                checkAvailabilityState(availPoints);
                logErrorInfo(state, availPoints, currAvails);
                availabilityCache.commitTran();
            } catch (Throwable e) {
                logErrorInfo(state, availPoints, currAvails);
                log.error(e.getMessage(), e);
                availabilityCache.rollbackTran();
                throw new SystemException(e);
            }

        }

        concurrentStatsCollector.addStat(availPoints.size(), AVAIL_MANAGER_METRICS_INSERTED);

        if (sendData) {
            sendDataToEventHandlers(availPoints);
        }
    }

	private void flushCreateAndRemoves(Map<DataPoint, AvailabilityDataRLE> createMap,
                                       Map<DataPoint, AvailabilityDataRLE> removeMap) {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();

        if (debug)
            watch.markTimeBegin("remove");
        for (Map.Entry<DataPoint, AvailabilityDataRLE> entry : removeMap.entrySet()) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE) entry.getValue();
            // if we call remove() on an object which is already in the session
            // hibernate will throw NonUniqueObjectExceptions
            AvailabilityDataRLE tmp = availabilityDataDAO.get(rle.getAvailabilityDataId());
            if (tmp != null) {
                availabilityDataDAO.remove(tmp);
            } else {
                availabilityDataDAO.remove(rle);
            }
        }
        if (debug) {
            watch.markTimeEnd("remove");
            watch.markTimeBegin("flush");
        }
        // addData() could be overwriting RLE data points (i.e. from 0.0 to 1.0)
        // with the same ID. If this is the scenario, then we must run
        // flush() in order to ensure that these old objects are not in the
        // session when the equivalent create() on the same ID is run,
        // thus avoiding NonUniqueObjectExceptions
        availabilityDataDAO.getSession().flush();
        if (debug) {
            watch.markTimeEnd("flush");
            watch.markTimeBegin("create");
        }
        List<MeasurementZevent> events = new ArrayList<MeasurementZevent>(createMap.entrySet().size());
        for (Map.Entry<DataPoint, AvailabilityDataRLE> entry : createMap.entrySet()) {
            DataPoint dp = entry.getKey();
            AvailabilityDataRLE rle = (AvailabilityDataRLE) entry.getValue();
            AvailabilityDataId id = new AvailabilityDataId();
            Measurement m = rle.getMeasurement();
            id.setMeasurement(m);
            id.setStartime(rle.getStartime());
            
            availabilityDataDAO.create(rle.getMeasurement(), rle.getStartime(), rle.getEndtime(), rle.getAvailVal());
            log.debug("added: Availability "+rle.getAvailVal() + " starttime " + rle.getStartime() + " endtime " + rle.getEndtime());
            events.add(new MeasurementZevent(m.getId().intValue(), dp.getMetricValue()));
        }
        ZeventManager.getInstance().enqueueEventsAfterCommit(events);
        if (debug) {
            watch.markTimeEnd("create");
            log.debug("AvailabilityInserter flushCreateAndRemoves: " + watch + ", points {remove=" + removeMap.size()
                    + ", create=" + createMap.size() + "}");
        }
    }

    private void logErrorInfo(final Map<Integer, StringBuilder> oldState, final List<DataPoint> availPoints,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        if (!traceLog.isDebugEnabled()) {
            return;
        }
        Integer mid;
        Map<Integer, StringBuilder> currState = captureCurrAvailState(currAvails);
        if (null != (mid = isAvailDataRLEValid(currAvails))) {
            logAvailState(oldState, mid);
            logStates(availPoints, mid);
            logAvailState(currState, mid);
        } else {
            traceLog.debug("RLE Data is valid");
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * get AvailabilityDataRLEs for the given DataPoints' Measurement IDs, with endData within the last 7 days.
     * If several AvailabilityDataRLEs exist for the same Measurement, they are listed in ascending order.
     * @param outOfOrderAvail
     * @param updateList
     * @return
     */
    private Map<Integer, TreeSet<AvailabilityDataRLE>> createCurrAvails(final List<DataPoint> outOfOrderAvail,
            final List<DataPoint> updateList) {
        Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails = null;
        final StopWatch watch = new StopWatch();
        try {
            if (outOfOrderAvail.size() == 0 && updateList.size() == 0) {
                currAvails = Collections.EMPTY_MAP;
            }
            long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), 60000);
            HashSet<Integer> mids = getMidsWithinAllowedDataWindow(updateList, now);
            mids.addAll(getMidsWithinAllowedDataWindow(outOfOrderAvail, now));
            if (mids.size() <= 0) {
                currAvails = Collections.EMPTY_MAP;

            }
            Integer[] mIds = (Integer[]) mids.toArray(new Integer[0]);
            currAvails = availabilityDataDAO.getHistoricalAvailMap(mIds, now - MAX_DATA_BACKLOG_TIME, false);
            return currAvails;
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("AvailabilityInserter setCurrAvails: " + watch + ", size=" + currAvails.size());
            }
        }
    }

    /**
     * allow only data given in the last MAX_DATA_BACKLOG_TIME ms
     * @param states - metric data list.
     * @param now - "current" timestamp
     * @return set of measurement IDs for measurements with "current" DataPoint (MetricData).
     */
    private HashSet<Integer> getMidsWithinAllowedDataWindow(final List<DataPoint> states, final long now) {
        HashSet<Integer> mids = new HashSet<Integer>();
        //int i = 0;
        for (Iterator<DataPoint> it = states.iterator(); it.hasNext(); ) {
            DataPoint pt = it.next();
            long timestamp = pt.getTimestamp();
            // only allow data for the last MAX_DATA_BACKLOG_TIME ms
            // this way we don't have to bring too much into memory which could
            // severely impact performance
            if ((now - timestamp) > MAX_DATA_BACKLOG_TIME) {
                it.remove();
                long days = (now - timestamp) / MeasurementConstants.DAY;
                log.warn(" Avail measurement came in " + days + " days " + " late, dropping: timestamp=" + timestamp +
                          " measId=" + pt.getMeasurementId() + " value=" + pt.getMetricValue());
                continue;
            }
            Integer mId = pt.getMeasurementId();
            if (!mids.contains(mId)) {
                mids.add(mId);
            }
        }
        return mids;
    }

    private void updateDup(DataPoint state, AvailabilityDataRLE dup,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) throws BadAvailStateException {
        if (dup.getAvailVal() == state.getValue()) {
            // nothing to do
        } else if (dup.getAvailVal() != AVAIL_DOWN) {
            String msg = "New DataPoint and current DB value for " + "MeasurementId " + state.getMeasurementId() +
                         " / timestamp " + state.getTimestamp() + " have conflicting states.  " +
                         "Since a non-zero rle value cannot be overridden, no update." + "\ncurrent rle value -> " +
                         dup +
                         // ask Juilet Sierra why (js) is here
                         ":(js):\npoint trying to override current rle -> " + state;
            throw new BadAvailStateException(msg);
        } else {
            Measurement meas = dup.getMeasurement();
            long newStartime = dup.getStartime() + meas.getInterval();
            insertPointOnBoundry(dup, newStartime, state, currAvails, createMap, removeMap);
        }
    }

    /**
     * sets avail's startime to newStartime and prepends a new avail obj from
     * avail.getStartime() to newStartime with a value of state.getValue() Used
     * specifically for a point which collides with a RLE on its startime
     */
    private void insertPointOnBoundry(AvailabilityDataRLE avail, long newStartime, DataPoint pt,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) throws BadAvailStateException {
        if (newStartime <= avail.getStartime()) {
            return;
        }
        Measurement meas = avail.getMeasurement();
        if (avail.getEndtime() == MAX_AVAIL_TIMESTAMP) {
            DataPoint tmp = availabilityCache.get(pt.getMeasurementId());
            if (tmp == null || pt.getTimestamp() >= tmp.getTimestamp()) {
                updateAvailVal(avail, pt.getValue(), currAvails, createMap, removeMap);
            } else {
                prependState(pt, avail, currAvails, createMap, removeMap);
            }
        } else if (newStartime < avail.getEndtime()) {
            prependState(pt, avail, currAvails, createMap, removeMap);
        } else if (newStartime > avail.getEndtime()) {
            removeAvail(avail, currAvails, createMap, removeMap);
        } else if (newStartime == avail.getEndtime()) {
            AvailabilityDataRLE after = findAvailAfter(pt, currAvails);
            if (after == null) {
                throw new BadAvailStateException("Availability measurement_id=" + pt.getMeasurementId() +
                                                 " does not have a availability point after timestamp " +
                                                 pt.getTimestamp());
            }
            if (after.getAvailVal() == pt.getValue()) {
                // resolve by removing the before obj, if it exists,
                // and sliding back the start time of after obj
                AvailabilityDataRLE before = findAvailBefore(pt, currAvails);
                if (before == null) {
                    after = updateStartime(after, avail.getStartime(), currAvails, createMap, removeMap);
                } else if (before.getAvailVal() == after.getAvailVal()) {
                    removeAvail(avail, currAvails, createMap, removeMap);
                    removeAvail(before, currAvails, createMap, removeMap);
                    after = updateStartime(after, before.getStartime(), currAvails, createMap, removeMap);
                }
            } else {
                // newStartime == avail.getEndtime() &&
                // newStartime == after.getStartime() &&
                // newStartime < after.getEndtime() &&
                // pt.getValue() != after.getAvailVal()
                // therefore, need to push back startTime and set the value
                long interval = meas.getInterval();
                if ((after.getStartime() + interval) < after.getEndtime()) {
                    prependState(pt, after, currAvails, createMap, removeMap);
                } else {
                    DataPoint afterPt = new DataPoint(meas.getId().intValue(), after.getAvailVal(), after.getStartime());
                    AvailabilityDataRLE afterAfter = findAvailAfter(afterPt, currAvails);
                    if (afterAfter.getAvailVal() == pt.getValue()) {
                        removeAvail(after, currAvails, createMap, removeMap);
                        afterAfter = updateStartime(afterAfter, pt.getTimestamp(), currAvails, createMap, removeMap);
                    } else {
                        updateAvailVal(after, pt.getValue(), currAvails, createMap, removeMap);
                    }
                }
            }
        }
    }

    private AvailabilityDataRLE findAvail(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        Integer mId = state.getMeasurementId();
        Collection<AvailabilityDataRLE> rles = currAvails.get(mId);
        long start = state.getTimestamp();
        for (AvailabilityDataRLE rle : rles) {
            if (rle.getStartime() == start) {
                return rle;
            }
        }
        return null;
    }

    private AvailabilityDataRLE findAvailAfter(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        final Integer mId = state.getMeasurementId();
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

    private AvailabilityDataRLE findAvailBefore(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        Integer mId = state.getMeasurementId();
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

    private void merge(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails,
                       Map<DataPoint, AvailabilityDataRLE> createMap,
                       Map<DataPoint, AvailabilityDataRLE> removeMap)
    throws BadAvailStateException {
        AvailabilityDataRLE dup = findAvail(state, currAvails);
        if (dup != null) {
            updateDup(state, dup, currAvails, createMap, removeMap);
            return;
        }
        AvailabilityDataRLE before = findAvailBefore(state, currAvails);
        AvailabilityDataRLE after = findAvailAfter(state, currAvails);
        if (before == null && after == null) {
            // this shouldn't happen here unless the resource was deleted
            Measurement meas = getMeasurement(state.getMeasurementId());
            // HHQ-5648 measurement could have been deleted
            if (meas == null) {
                return;
            }
            create(meas, state.getTimestamp(), state.getValue(), currAvails, createMap);
        } else if (before == null) {
            if (after.getAvailVal() != state.getValue()) {
                prependState(state, after, currAvails, createMap, removeMap);
            } else {
                after = updateStartime(after, state.getTimestamp(), currAvails, createMap, removeMap);
            }
        } else if (after == null) {
            // this shouldn't happen here
            updateState(state, currAvails, createMap, removeMap);
        } else {
            insertAvail(before, after, state, currAvails, createMap, removeMap);
        }
    }

    private void insertAvail(AvailabilityDataRLE before, AvailabilityDataRLE after, DataPoint state,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) {
        if (state.getValue() != after.getAvailVal() && state.getValue() != before.getAvailVal()) {
            Measurement meas = getMeasurement(state.getMeasurementId());
            long pivotTime = state.getTimestamp() + meas.getInterval();
            create(meas, state.getTimestamp(), pivotTime, state.getValue(), currAvails, createMap);
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, pivotTime, currAvails, createMap, removeMap);
        } else if (state.getValue() == after.getAvailVal() && state.getValue() != before.getAvailVal()) {
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, state.getTimestamp(), currAvails, createMap, removeMap);
        } else if (state.getValue() != after.getAvailVal() && state.getValue() == before.getAvailVal()) {
            // this is fine
        } else if (state.getValue() == after.getAvailVal() && state.getValue() == before.getAvailVal()) {
            // this should never happen or else there is something wrong
            // in the code
            String msg = "AvailabilityData [" + before + "] and [" + after
                    + "] have the same values.  This should not be the case.  " + "Cleaning up";
            log.warn(msg);
            updateEndtime(before, after.getEndtime());
            removeAvail(after, currAvails, createMap, removeMap);
        }
    }

    private boolean prependState(DataPoint state, AvailabilityDataRLE avail,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) {
        AvailabilityDataRLE before = findAvailBefore(state, currAvails);
        Measurement meas = avail.getMeasurement();
        if (before != null && before.getAvailVal() == state.getValue()) {
            long newStart = state.getTimestamp() + meas.getInterval();
            updateEndtime(before, newStart);
            avail = updateStartime(avail, newStart, currAvails, createMap, removeMap);
        } else {
            long newStart = state.getTimestamp() + meas.getInterval();
            long endtime = newStart;
            avail = updateStartime(avail, newStart, currAvails, createMap, removeMap);
            create(avail.getMeasurement(), state.getTimestamp(), endtime, state.getValue(), currAvails, createMap);
        }
        return true;
    }

    private void updateAvailVal(AvailabilityDataRLE avail, double val,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) {
        Measurement meas = avail.getMeasurement();
        DataPoint state = new DataPoint(meas.getId().intValue(), val, avail.getStartime());
        AvailabilityDataRLE before = findAvailBefore(state, currAvails);
        if (before == null || before.getAvailVal() != val) {
            avail.setAvailVal(val);
        } else {
            removeAvail(before, currAvails, createMap, removeMap);
            avail = updateStartime(avail, before.getStartime(), currAvails, createMap, removeMap);
            avail.setAvailVal(val);
        }
    }

    private void updateEndtime(AvailabilityDataRLE avail, long endtime) {
        avail.setEndtime(endtime);
    }

    private AvailabilityDataRLE updateStartime(AvailabilityDataRLE avail, long start,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) {
        // this should not be the case here, but want to make sure and
        // avoid HibernateUniqueKeyExceptions :(
        AvailabilityDataRLE tmp;
        Measurement meas = avail.getMeasurement();
        Integer mId = meas.getId();
        DataPoint tmpState = new DataPoint(mId.intValue(), avail.getAvailVal(), start);
        if (null != (tmp = findAvail(tmpState, currAvails))) {
            removeAvail(tmp, currAvails, createMap, removeMap);
        }
        removeAvail(avail, currAvails, createMap, removeMap);
        return create(meas, start, avail.getEndtime(), avail.getAvailVal(), currAvails, createMap);
    }

    private void removeAvail(AvailabilityDataRLE avail, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails,
            Map<DataPoint, AvailabilityDataRLE> createMap, Map<DataPoint, AvailabilityDataRLE> removeMap) {
        long start = avail.getStartime();
        Integer mId = avail.getMeasurement().getId();
        TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        if (rles.remove(avail)) {
            DataPoint key = new DataPoint(mId.intValue(), avail.getAvailVal(), start);
            createMap.remove(key);
            removeMap.put(key, avail);
        }
    }

    private AvailabilityDataRLE getLastAvail(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        Integer mId = state.getMeasurementId();
        TreeSet<AvailabilityDataRLE> rles = currAvails.get(mId);
        if (rles.size() == 0) {
            return null;
        }
        return rles.last();
    }

    private AvailabilityDataRLE create(Measurement meas, long start, long end, double val,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap) {
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

    private AvailabilityDataRLE create(Measurement meas, long start, double val,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap) {
        AvailabilityDataRLE rtn = _createAvail(meas, start, MAX_AVAIL_TIMESTAMP, val);
        createMap.put(new DataPoint(meas.getId().intValue(), val, start), rtn);
        Integer mId = meas.getId();
        Collection<AvailabilityDataRLE> rles = currAvails.get(mId);
        // I am assuming that this will be cleaned up by the caller where it
        // will update the rle before rtn if one exists
        rles.add(rtn);
        return rtn;
    }

    private boolean updateState(DataPoint state, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails,
            Map<DataPoint, AvailabilityDataRLE> createMap, Map<DataPoint, AvailabilityDataRLE> removeMap)
            throws BadAvailStateException {
        AvailabilityDataRLE avail = getLastAvail(state, currAvails);
        final boolean debug = log.isDebugEnabled();
        long begin = -1;
        if (avail == null) {
            Measurement meas = getMeasurement(state.getMeasurementId());
            create(meas, state.getTimestamp(), state.getValue(), currAvails, createMap);
            return true;
        } else if (state.getTimestamp() < avail.getStartime()) {
            if (debug) {
                begin = System.currentTimeMillis();
            }
            merge(state, currAvails, createMap, removeMap);
            if (debug) {
                long now = System.currentTimeMillis();
                log.debug("updateState.merge() -> " + (now - begin) + " ms");
            }
            return false;
        } else if (state.getTimestamp() == avail.getStartime() && state.getValue() != avail.getAvailVal()) {
            if (debug) {
                begin = System.currentTimeMillis();
            }
            updateDup(state, avail, currAvails, createMap, removeMap);
            if (debug) {
                long now = System.currentTimeMillis();
                log.debug("updateState.updateDup() -> " + (now - begin) + " ms");
            }
            return true;
        } else if (state.getValue() == avail.getAvailVal()) {
            if (debug) {
                log.debug("no update state == avail " + state + " == " + avail);
            }
            return true;
        }
        if (debug) {
            log.debug("updating endtime on avail -> " + avail + ", updating to state -> " + state);
        }
        updateEndtime(avail, state.getTimestamp());
        create(avail.getMeasurement(), state.getTimestamp(), state.getValue(), currAvails, createMap);
        return true;
    }

    
    /**
     * update DB and availabilityCache with the changes marked in list states.
     * Create/Remove is not actually done here - should call flushCreateAndRemove for the createMap and removeMap.
     * @param states - States to update.
     * @param currAvails - current DB state for measurement IDs.
     * @param createMap - in/out param. filled with new AvailabilityDataRLEs
     * @param removeMap - in/out param. filled with AvailabilityDataRLEs to remove.
     */
    private void updateStates(List<DataPoint> states, Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails,
            Map<DataPoint, AvailabilityDataRLE> createMap, Map<DataPoint, AvailabilityDataRLE> removeMap) {

        if (states.size() == 0) {
            return;
        }
        // as a performance optimization, fetch all the last avails
        // at once, rather than one at a time in updateState()
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        int numUpdates = 0;
        for (DataPoint state : states) {
            try {
                // need to check again since there could be multiple
                // states with the same id in the list
                DataPoint currState = availabilityCache.get(state.getMeasurementId());
                if (currState != null && currState.getValue() == state.getValue()) {
                    continue;
                }
                boolean updateCache = updateState(state, currAvails, createMap, removeMap);
                if (debug) {
                    log.debug("state " + state + " was updated, cache updated: " + updateCache);
                }
                if (updateCache) {
                    availabilityCache.put(state.getMeasurementId(), state);
                    numUpdates++;
                }
            } catch (BadAvailStateException e) {
                log.warn(e.getMessage());
            }
        }
        if (debug) {
            log.debug("AvailabilityInserter updateStates: " + watch + ", points {total=" + states.size()
                    + ", updateCache=" + numUpdates + "}");
        }
    }

    /**
     * update DB with the changes marked in list states.
     * do not update the cache here, the timestamp is out of order.
     * Create/Remove is not actually done here - should call flushCreateAndRemove for the createMap and removeMap.
     * @param outOfOrderAvail - States that are not synched with the cache.
     * @param currAvails - current DB state for measurement IDs.
     * @param createMap - in/out param. filled with new AvailabilityDataRLEs
     * @param removeMap - in/out param. filled with AvailabilityDataRLEs to remove.
     */
    private void updateOutOfOrderState(List<DataPoint> outOfOrderAvail,
            Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails, Map<DataPoint, AvailabilityDataRLE> createMap,
            Map<DataPoint, AvailabilityDataRLE> removeMap) {
        if (outOfOrderAvail.size() == 0) {
            return;
        }
        final StopWatch watch = new StopWatch();
        int numBadAvailState = 0;
        for (DataPoint state : outOfOrderAvail) {
            try {
                // do not update the cache here, the timestamp is out of order
                merge(state, currAvails, createMap, removeMap);
            } catch (BadAvailStateException e) {
                numBadAvailState++;
                log.warn(e.getMessage());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("AvailabilityInserter updateOutOfOrderState: " + watch + ", points {total="
                    + outOfOrderAvail.size() + ", badAvailState=" + numBadAvailState + "}");
        }
    }

    /**
     * update the cache with states that are equal to current state and have a newer timestamp.
     * return 2 lists for updating - updateList and outOfOrderAvail.
     * @param availPoints - DataPoint list that represents availability status of resources.
     * @return updateList - Old and new states have different values and cache needs to be updated.
     * @return outOfOrderAvail - Old metric has a newer timestamp
     */
    private void updateCache(List<DataPoint> availPoints, List<DataPoint> updateList, List<DataPoint> outOfOrderAvail) {
        if (availPoints.size() == 0) {
            return;
        }
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        for (DataPoint pt : availPoints) {
            Integer meas_id = pt.getMeasurementId();
            MetricValue mval = pt.getMetricValue();
            double val = mval.getValue();
            long timestamp = mval.getTimestamp();
            DataPoint newState = new DataPoint(pt);
            DataPoint oldState = availabilityCache.get(meas_id);
            // we do not want to update the state if it changes
            // instead change it when the db is changed in order
            // to ensure the state of memory to db
            // ONLY update memory state here if there is no change
            
            // check if the "new" state is actually older than the state saved in the cache
            // if so - OUT_OF_ORDER
            if (oldState != null && timestamp < oldState.getTimestamp()) {
                outOfOrderAvail.add(newState);
            }
            // else - check if the new old state is null or has a different value than the cache.
            // if so - UPDATE
            else if (oldState == null || oldState.getValue() == AVAIL_NULL || oldState.getValue() != val) {
                updateList.add(newState);
                if (debug) {
                    String msg = "value of state[" + newState + "] differs from" + " current value["
                            + ((oldState != null) ? oldState.toString() : "old state does not exist") + "]";
                    log.debug(msg);
                }
            }
            // else - old state exists and with the same value, only a different timestamp. updating the cache.
            else {
                availabilityCache.put(meas_id, newState);
            }
        }
        if (debug) {
            log.debug("AvailabilityInserter updateCache: " + watch + ", points {total=" + availPoints.size()
                    + ", outOfOrder=" + outOfOrderAvail.size() + ", updateToDb=" + updateList.size()
                    + ", updateCacheTimestamp=" + (availPoints.size() - outOfOrderAvail.size() - updateList.size())
                    + "}");
        }
    }

	private void checkAvailabilityState(List<DataPoint> availPoints) {
		// The following code (until the creation of the StopWatch) is a fix for Jira issue [HHQ-5524].
        // There is a strange scenario where the cache holds an availability metric with 'available' value while
        // in the database the same availability metric value is 'not available' (in the HQ_AVAIL_DATA_RLE table).
        // In that case we need to clear the availability cache because it is out of sync with the database.
        // It is very hard to understand what causes this issue but it happens a lot in scale environments.
        // If we don't clear the cache the resource which this availability
        // metric belongs to will appear to be 'down' while it is not,
        // clearing the cache causes the metric to be updated in the database to the correct value.
        final List<Integer> includes = new ArrayList<Integer>(availPoints.size());
        for (final DataPoint point : availPoints) {
            includes.add(point.getMeasurementId());
        }
        // Find all of the availPoints in the database and check if the same datapoint 
        // is not in sync with the cache
        final List<AvailabilityDataRLE> avails = availabilityDataDAO.findLastAvail(includes);
        final List<Integer> mids = new ArrayList<Integer>();
        for (final AvailabilityDataRLE data : avails) {
            final Integer mid = data.getMeasurement().getId();
            final DataPoint stateInCache = availabilityCache.get(mid);
            if (null != stateInCache && (stateInCache.getValue() != data.getAvailVal())) {
                availabilityCache.remove(mid);
                mids.add(mid);
            }
        }
        if (!mids.isEmpty()) {
            log.info("The state of the Availability cache is out of sync with the database for measurementIds='" + mids +
                     "', clearing these metrics from the Availability cache");
        }
    }

    private void sendDataToEventHandlers(List<DataPoint> data) {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        int maxCapacity = data.size();
        ArrayList<MeasurementEvent> events = new ArrayList<MeasurementEvent>(maxCapacity);
        Map<Integer, MeasurementEvent> downEvents = new HashMap<Integer, MeasurementEvent>(maxCapacity);
        List<MeasurementZevent> zevents = new ArrayList<MeasurementZevent>(maxCapacity);
        boolean allEventsInteresting = Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);
        if (debug)
            watch.markTimeBegin("isTriggerInterested");
        for (DataPoint dp : data) {
            Integer metricId = dp.getMeasurementId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);
            if (registeredTriggers.isTriggerInterested(event) || allEventsInteresting) {
                measurementManager.buildMeasurementEvent(event);
                if (event.getValue().getValue() == AVAIL_DOWN) {
                    AppdefEntityID eventResourceAEID = event.getResource();
                    Resource r = (null == eventResourceAEID ? null : resourceManager.findResource(eventResourceAEID));
                    if (r != null && !r.isInAsyncDeleteState()) {
                        downEvents.put(r.getId(), event);
                    }
                } else {
                    events.add(event);
                }
            }
            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }
        if (debug)
            watch.markTimeEnd("isTriggerInterested");
        if (!downEvents.isEmpty()) {
            if (debug)
                watch.markTimeBegin("suppressMeasurementEvents");
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance().getHierarchicalAlertingManager()
                    .suppressMeasurementEvents(downEvents, true);
            if (debug)
                watch.markTimeEnd("suppressMeasurementEvents");
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
        if (debug) {
            log.debug("AvailabilityInserter sendDataToEventHandlers: " + watch + ", points {total=" + maxCapacity
                    + ", downEvents=" + downEvents.size() + ", eventsToPublish=" + events.size()
                    + ", zeventsToEnqueue=" + zevents.size() + "}");
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
            Integer metricId = dp.getMeasurementId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);
            if (registeredTriggers.isTriggerInterested(event) || allEventsInteresting) {
                measurementManager.buildMeasurementEvent(event);
                events.put(resourceIdKey, event);
            }
            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }
        if (!events.isEmpty()) {
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance().getHierarchicalAlertingManager()
                    .suppressMeasurementEvents(events, false);
            messagePublisher.publishMessage(EventConstants.EVENTS_TOPIC,
                    new ArrayList<MeasurementEvent>(events.values()));
        }
        if (!zevents.isEmpty()) {
            ZeventManager.getInstance().enqueueEventsAfterCommit(zevents);
        }
    }

    private Measurement getMeasurement(Integer mId) {
        return measurementManager.getMeasurement(mId);
    }

    private Map<Integer, StringBuilder> captureCurrAvailState(Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {
        if (!traceLog.isDebugEnabled()) {
            return null;
        }
        Map<Integer, StringBuilder> rtn = new HashMap<Integer, StringBuilder>();
        for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : currAvails.entrySet()) {
            Integer mid = entry.getKey();
            Collection<AvailabilityDataRLE> rles = entry.getValue();
            StringBuilder buf = new StringBuilder("\n");
            for (AvailabilityDataRLE rle : rles) {
                buf.append(mid).append(" | ").append(rle.getStartime()).append(" | ").append(rle.getEndtime())
                        .append(" | ").append(rle.getAvailVal()).append("\n");
            }
            rtn.put(mid, buf);
        }
        return rtn;
    }

    private void logAvailState(Map<Integer, StringBuilder> availState, Integer mid) {
        StringBuilder buf = (StringBuilder) availState.get(mid);
        traceLog.debug(buf.toString());
    }

    private void logStates(List<DataPoint> states, Integer mid) {
        StringBuilder log = new StringBuilder("\n");
        for (DataPoint pt : states) {
            if (!pt.getMeasurementId().equals(mid)) {
                continue;
            }
            log.append(pt.getMeasurementId()).append(" | ").append(pt.getTimestamp()).append(" | ").append(
                pt.getMetricValue()).append("\n");
        }
        traceLog.debug(log.toString());
    }

    private Integer isAvailDataRLEValid(Map<Integer, TreeSet<AvailabilityDataRLE>> currAvails) {

        synchronized (availabilityCache) {
            for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : currAvails.entrySet()) {
                Integer mId = entry.getKey();
                Collection<AvailabilityDataRLE> rles = entry.getValue();
                if (!isAvailDataRLEValid(mId, availabilityCache.get(mId), rles)) {
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
                log.error("list for MID=" + measId + " contains two or more of the same endtime=" + endtime);
                return false;
            }
            endtimes.add(endtime);
            if (last == null) {
                last = avail;
                continue;
            }
            if (last.getAvailVal() == avail.getAvailVal()) {
                log.error("consecutive availpoints have the same value, " + "first={" + last + "}, last={" + avail
                        + "}");
                return false;
            } else if (last.getEndtime() != avail.getStartime()) {
                log.error("there are gaps in the availability table" + "first={" + last + "}, last={" + avail + "}");
                return false;
            } else if (last.getStartime() > avail.getStartime()) {
                log.error("startime availability is out of order" + "first={" + last + "}, last={" + avail + "}");
                return false;
            } else if (last.getEndtime() > avail.getEndtime()) {
                log.error("endtime availability is out of order" + "first={" + last + "}, last={" + avail + "}");
                return false;
            }
            last = avail;
        }

        if (((DataPoint) availabilityCache.get(measId)).getValue() != lastPt.getValue()) {
            log.error("last avail data point does not match cache");
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean platformIsAvailableOrUnknown(int agentId) {
        final Agent agent = agentDAO.get(agentId);
        if (agent == null) {
            return false;
        }
        Resource resource = null;
        Collection<Platform> platforms = agent.getPlatforms();
        for (final Platform p : platforms) {
            if (PlatformDetector.isSupportedPlatform(p.getResource().getPrototype().getName())) {
                resource = p.getResource();
                if (resource == null || resource.isInAsyncDeleteState()) {
                    return false;
                }
                break;
            }
        }
        final Measurement m = measurementManager.getAvailabilityMeasurement(resource);
        if (m == null) {
            return true;
        }
        final MetricValue last = getLastAvail(m);
        if (last == null) {
            return true;
        }
        if (last.getValue() == MeasurementConstants.AVAIL_UP ||
                last.getValue() == MeasurementConstants.AVAIL_UNKNOWN) {
            return true;
        }
        return false;
    }

}

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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManagerUtil;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.AvailabilityMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.stats.ConcurrentStatsCollector;
import org.hyperic.util.timer.StopWatch;

/** The AvailabityManagerEJBImpl class is a stateless session bean that can be
 *  used to retrieve Availability Data RLE points
 *
 * @ejb:bean name="AvailabilityManager"
 *      jndi-name="ejb/measurement/AvailabilityManager"
 *      local-jndi-name="LocalAvailabilityManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="Required"
 */
public class AvailabilityManagerEJBImpl
    extends SessionEJB implements SessionBean {

    private final Log _log = LogFactory.getLog(AvailabilityManagerEJBImpl.class);
    private final Log _traceLog = LogFactory.getLog(
        AvailabilityManagerEJBImpl.class.getName() + "Trace");
    private final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;
    private final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private final double AVAIL_UNKNOWN =
        MeasurementConstants.AVAIL_UNKNOWN;
    private final int IND_MIN       = MeasurementConstants.IND_MIN;
    private final int IND_AVG       = MeasurementConstants.IND_AVG;
    private final int IND_MAX       = MeasurementConstants.IND_MAX;
    private final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    private final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private final int IND_UP_TIME   = IND_LAST_TIME + 1;
    private final int IND_TOTAL_TIME = IND_UP_TIME + 1;
    private final long MAX_AVAIL_TIMESTAMP =
        AvailabilityDataRLE.getLastTimestamp();
    private final String ALL_EVENTS_INTERESTING_PROP =
        "org.hq.triggers.all.events.interesting";
    private final int DEFAULT_INTERVAL = 60;
    private final AvailabilityDataDAO _dao = getAvailabilityDataDAO();
    private static final String AVAIL_MANAGER_METRICS_INSERTED =
        ConcurrentStatsCollector.AVAIL_MANAGER_METRICS_INSERTED;
    /**
     * {@link Map} of {@link DataPoint} to {@link AvailabilityDataRLE}
     */
    private Map _createMap = null;
    /**
     * {@link Map} of {@link DataPoint} to {@link AvailabilityDataRLE}
     */
    private Map _removeMap = null;
    /**
     * {@link Map} of {@link Integer} to ({@link TreeSet} of
     *  {@link AvailabilityDataRLE}).
     * <p>The {@link Map} key of {@link Integer} == {@link Measurement}.getId().
     * <p>The {@link TreeSet}'s comparator sorts by
     *  {@link AvailabilityDataRLE}.getStartime().
     */
    private Map _currAvails = null;

    private final long MAX_DATA_BACKLOG_TIME = 7 * MeasurementConstants.DAY;

    /**
     * @ejb:interface-method
     */
    public Measurement getAvailMeasurement(Resource resource) {
        return getMeasurementDAO().findAvailMeasurement(resource);
    }

    /**
     * @ejb:interface-method
     */
    public List getPlatformResources() {
        return getMeasurementDAO().findAvailMeasurementsByInstances(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null);
    }

    /**
     * @return Down time in ms for the Resource availability
     *
     * @ejb:interface-method
     */
    public long getDowntime(Resource resource, long begin, long end)
        throws MeasurementNotFoundException
    {
        Measurement meas = getMeasurementDAO().findAvailMeasurement(resource);
        if (meas == null) {
            throw new MeasurementNotFoundException("Availability measurement " +
                                                   "not found for resource " +
                                                   resource.getId());
        }
        List availInfo = _dao.getHistoricalAvails(meas, begin, end, false);
        long rtn = 0l;
        for (Iterator i=availInfo.iterator(); i.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)i.next();
            if (avail.getAvailVal() != AVAIL_DOWN) {
                continue;
            }
            long endtime = avail.getEndtime();
            if (endtime == MAX_AVAIL_TIMESTAMP) {
                endtime = System.currentTimeMillis();
            }
            rtn += (endtime-avail.getStartime());
        }
        return rtn;
    }

    /**
     * @return List of all measurement ids for availability, ordered
     *
     * @ejb:interface-method
     */
    public List getAllAvailIds() {
        return getMeasurementDAO().findAllAvailIds();
    }

    /**
     * @ejb:interface-method
     */
    public List getAvailMeasurementChildren(Resource resource,
                                            String resourceRelationType) {
        final List sList = Collections.singletonList(resource.getId());
        List rtn = (List) getAvailMeasurementChildren(sList, resourceRelationType)
                                    .get(resource.getId());
        if (rtn == null) {
            rtn = new ArrayList(0);
        }
        return rtn;
    }

    /**
     * @param {@link List} of {@link Integer} resource ids
     * @return {@link Map} of {@link Integer} to {@link List} of
     * {@link Measurement}
     * @ejb:interface-method
     */
    public Map getAvailMeasurementChildren(List resourceIds,
                                           String resourceRelationType) {
        final List objects = getMeasurementDAO().findRelatedAvailMeasurements(
            resourceIds, resourceRelationType);

        return convertAvailMeasurementListToMap(objects);
    }

    /**
     * @ejb:interface-method
     */
    public List getAvailMeasurementParent(Resource resource,
                                          String resourceRelationType) {
        final List sList = Collections.singletonList(resource.getId());
        List rtn = (List) getAvailMeasurementParent(sList, resourceRelationType)
                                    .get(resource.getId());
        if (rtn == null) {
            rtn = new ArrayList(0);
        }
        return rtn;
    }

    /**
     * @ejb:interface-method
     */
    public Map getAvailMeasurementParent(List resourceIds,
                                         String resourceRelationType) {
        final List objects = getMeasurementDAO().findParentAvailMeasurements(
                                      resourceIds,
                                      resourceRelationType);

        return convertAvailMeasurementListToMap(objects);
    }

    private Map convertAvailMeasurementListToMap(List objects) {
        final Map rtn = new HashMap(objects.size());
        for (final Iterator it=objects.iterator(); it.hasNext(); ) {
            final Object[] o = (Object[])it.next();
            final Integer rId = (Integer)o[0];
            final Measurement m = (Measurement)o[1];
            List tmp;
            if (null == (tmp = (List)rtn.get(rId))) {
                tmp = new ArrayList();
                rtn.put(rId, tmp);
            }
            tmp.add(m);
        }
        return rtn;
    }

    /**
     * TODO: Can this method be combined with the one that takes an array?
     *
     * @ejb:interface-method
     */
    public PageList getHistoricalAvailData(Measurement m, long begin, long end,
                                           PageControl pc,
                                           boolean prependUnknowns) {
        List availInfo = _dao.getHistoricalAvails(
            m, begin, end, pc.isDescending());
        return getPageList(
            availInfo, begin, end, m.getInterval(), prependUnknowns);
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
     * corresponding time window is not accounted for in the database.  Since
     * availability is contiguous this will not occur unless the time range
     * precedes the first availability point.
     * @see org.hyperic.hq.measurement.MeasurementConstants#AVAIL_UNKNOWN
     * @ejb:interface-method
     */
    public PageList getHistoricalAvailData(Integer[] mids, long begin, long end,
                                           long interval, PageControl pc,
                                           boolean prependUnknowns) {
        if (mids.length == 0) {
            return new PageList();
        }
        List availInfo = _dao.getHistoricalAvails(mids, begin,
            end, pc.isDescending());
        return getPageList(availInfo, begin, end, interval, prependUnknowns);
    }

    // XXX scottmf, not used right now.  Will be used in some fashion to calc
    // the correct availability uptime percent
    private double getUpTime(List availInfo, long begin, long end) {
        long totalUptime = 0;
        long totalTime = 0;
        for (Iterator it=availInfo.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE)it.next();
            long endtime = rle.getEndtime();
            long startime = rle.getStartime();
            long total = Math.min(endtime, end) - Math.max(startime, begin);
            totalUptime += total*rle.getAvailVal();
            totalTime += total;
        }
        return (double)totalUptime/(double)totalTime*100;
    }

    /**
     * Get the list of Raw RLE objects for a resource
     * @return List<AvailabilityDataRLE>
     * @ejb:interface-method
     */
    public List getHistoricalAvailData(Resource res, long begin, long end) {
        return _dao.getHistoricalAvails(res, begin, end);
    }

    private Collection getDefaultHistoricalAvail(long timestamp) {
        HighLowMetricValue[] rtn = new HighLowMetricValue[DEFAULT_INTERVAL];
        Arrays.fill(rtn, new HighLowMetricValue(AVAIL_UNKNOWN, timestamp));
        return Arrays.asList(rtn);
    }

    private PageList getPageList(List availInfo, long begin, long end,
                                 long interval, boolean prependUnknowns) {
        PageList rtn = new PageList();
        begin += interval;
        for (Iterator it=availInfo.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE)it.next();
            long availStartime = rle.getStartime();
            long availEndtime = rle.getEndtime();
            if (availEndtime < begin) {
                continue;
            }
            LinkedList queue = new LinkedList();
            queue.add(rle);
            int i=0;
            for (long curr=begin; curr<=end; curr+=interval) {
                long next = curr + interval;
                next = (next > end) ? end : next;
                long endtime =
                    ((AvailabilityDataRLE)queue.getFirst()).getEndtime();
                while (next > endtime) {
                    // it should not be the case that there are no more
                    // avails in the array, but we need to handle it
                    if (it.hasNext()) {
                        AvailabilityDataRLE tmp = (AvailabilityDataRLE)it.next();
                        queue.addFirst(tmp);
                        endtime = tmp.getEndtime();
                    } else {
                        endtime = availEndtime;
                        int measId = rle.getMeasurement().getId().intValue();
                        String msg = "Measurement, " + measId +
                            ", for interval " + begin + " - " + end +
                            " did not return a value for range " +
                            curr + " - " + (curr + interval);
                        _log.warn(msg);
                    }
                }
                endtime = availEndtime;
                while (curr > endtime) {
                    queue.removeLast();
                    // this should not happen unless the above !it.hasNext()
                    // else condition is true
                    if (queue.size() == 0) {
                        rle = new AvailabilityDataRLE(rle.getMeasurement(),
                            rle.getEndtime(), next, AVAIL_UNKNOWN);
                        queue.addLast(rle);
                    }
                    rle = (AvailabilityDataRLE)queue.getLast();
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
                    updateMetricValue(val, (HighLowMetricValue)rtn.get(i));
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

    private HighLowMetricValue updateMetricValue(HighLowMetricValue newVal,
                                                 HighLowMetricValue oldVal) {
        if (newVal.getHighValue() == AVAIL_UNKNOWN ||
                newVal.getHighValue() > oldVal.getHighValue()) {
            oldVal.setHighValue(newVal.getHighValue());
        }
        if (newVal.getLowValue() == AVAIL_UNKNOWN ||
                newVal.getLowValue() < oldVal.getLowValue()) {
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
            double value =
                ((newVal.getValue()+(oldVal.getValue()*count)))/(count+1);
            oldVal.setValue(value);
            oldVal.incrementCount();
            round(oldVal);
        }
        return oldVal;
    }

    private HighLowMetricValue getMetricValue(List avails, long timestamp) {
        if (avails.size() == 1) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE)avails.get(0);
            return new HighLowMetricValue(rle.getAvailVal(), timestamp);
        }
        double value = 0;
        for (Iterator i=avails.iterator(); i.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE)i.next();
            double availVal = rle.getAvailVal();
            value += availVal;
        }
        value = value/avails.size();
	    HighLowMetricValue val = new HighLowMetricValue(value, timestamp);
            val.incrementCount();
	    return val;
	}

    /**
     * @return {@link Map} of {@link Measurement} to {@link double[]}.
     * Array is comprised of 5 elements:
     * [IND_MIN]
     * [IND_AVG]
     * [IND_MAX]
     * [IND_CFG_COUNT]
     * [IND_LAST_TIME]
     * @ejb:interface-method
     */
    public Map getAggregateData(Integer[] mids, long begin, long end) {
        List avails = _dao.findAggregateAvailability(mids, begin, end);
        return getAggData(avails, false);
    }

    /**
     * @return {@link Map} of {@link MeasurementTemplate.getId} to
     * {@link double[]}.
     * Array is comprised of 5 elements:
     * [IND_MIN]
     * [IND_AVG]
     * [IND_MAX]
     * [IND_CFG_COUNT]
     * [IND_LAST_TIME]
     * @ejb:interface-method
     */
    public Map getAggregateDataByTemplate(Integer[] mids, long begin, long end) {
        List avails = _dao.findAggregateAvailability(mids, begin, end);
        return getAggData(avails, true);
    }

    /**
     * @return {@link Map} of {@link MeasurementTemplate.getId} to
     * {@link double[]}.
     * Array is comprised of 5 elements:
     * [IND_MIN]
     * [IND_AVG]
     * [IND_MAX]
     * [IND_CFG_COUNT]
     * [IND_LAST_TIME]
     * @ejb:interface-method
     */
    public Map getAggregateData(Integer[] tids, Integer[] iids,
                                long begin, long end) {
        List avails = _dao.findAggregateAvailability(tids, iids, begin, end);
        return getAggData(avails, true);
    }

    private Map getAggData(List avails, boolean useTidKey)
    {
        Map rtn = new HashMap();
        if (avails.size() == 0) {
            // Nothing to do, return an empty Map.
            return rtn;
        }
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            Object[] objs = (Object[]) it.next();

            double[] data;
            Integer key = null;
            if (useTidKey) {
                if (objs[0] instanceof Measurement) {
                    key = ((Measurement)objs[0]).getTemplate().getId();
                } else {
                    key = (Integer)objs[0];
                }
            } else {
                key = ((Measurement)objs[0]).getId();
            }
            if (null == (data = (double[])rtn.get(key))) {
                data = new double[IND_TOTAL_TIME + 1];
                data[IND_MIN] = MeasurementConstants.AVAIL_UP;
                data[IND_MAX] = MeasurementConstants.AVAIL_PAUSED;
                rtn.put(key, data);
            }

            data[IND_MIN] =
                Math.min(data[IND_MIN], ((Double)objs[1]).doubleValue());
            data[IND_MAX] =
                Math.max(data[IND_MAX], ((Double)objs[3]).doubleValue());

            // Expect data to be sorted by end time, so that the last value
            // returned is the final count and the last value
            data[IND_CFG_COUNT] = (objs[4] == null) 
                                    ? 0 : ((java.lang.Number)objs[4]).doubleValue();
            data[IND_LAST_TIME] = ((Double)objs[2]).doubleValue();

            data[IND_UP_TIME]    += ((Double)objs[5]).doubleValue();
            data[IND_TOTAL_TIME] += ((Double)objs[6]).doubleValue();
        }

        // Now calculate the average value
        for (Iterator it = rtn.values().iterator(); it.hasNext(); ) {
            double[] data = (double[]) it.next();
            data[IND_AVG] += data[IND_UP_TIME] / data[IND_TOTAL_TIME];
        }
        return rtn;
    }

    /**
     * @param resources Collection may be of type {@link Resource},
     *  {@link AppdefEntityId}, {@link AppdefEntityValue},
     *  {@link AppdefResourceValue} or {@link Integer}
     * @param measCache Map<Integer, List> optional arg (may be null) to supply
     * measurement id(s) of ResourceIds. Integer => Resource.getId().  If a 
     * measurement is not specified in the measCache parameter it will be added
     * to the map
     * @return Map<Integer, MetricValue> Integer => Measurement.getId()
     * @ejb:interface-method
     */
    public Map getLastAvail(Collection resources, Map measCache) {
        final MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        final Set midsToGet = new HashSet(resources.size());
        final List resToGet = new ArrayList(resources.size());
        final ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();
        for (final Iterator it=resources.iterator(); it.hasNext(); ) {
            final Object o = it.next();
            Resource resource = null;
            if (o instanceof AppdefEntityValue) {
                AppdefEntityValue rv = (AppdefEntityValue) o;
                AppdefEntityID aeid = rv.getID();
                resource = resMan.findResource(aeid);
            } else if (o instanceof AppdefEntityID) {
                AppdefEntityID aeid = (AppdefEntityID) o;
                resource = resMan.findResource(aeid);
            } else if (o instanceof Resource) {
                resource = (Resource) o;
            } else if (o instanceof AppdefResourceValue){
                AppdefResourceValue res = (AppdefResourceValue) o;
                AppdefEntityID aeid = res.getEntityId();
                resource = resMan.findResource(aeid);
            } else {
                resource = resMan.findResourceById((Integer) o);
            }
            List measIds = null;
            try {
                if (resource == null || resource.isInAsyncDeleteState()) {
                    continue;
                }
            } catch (ObjectNotFoundException e) {
                // resource is in async delete state, ignore
                _log.debug("resource not found from object=" + o ,e);
                continue;
            }
            if (measCache != null) {
                measIds = (List)measCache.get(resource.getId());
            }
            if (measIds == null || measIds.size() == 0) {
                resToGet.add(resource);
                continue;
            }
            for (final Iterator iter=measIds.iterator(); iter.hasNext(); ) {
                final Measurement m = (Measurement)iter.next();
                midsToGet.add(m.getId());
            }
        }
        if (!resToGet.isEmpty()) {
            final Collection measIds = mMan.getAvailMeasurements(resToGet).values();
            for (final Iterator it=measIds.iterator(); it.hasNext(); ) {
                final List mids = (List)it.next();
                for (final Iterator iter=mids.iterator(); iter.hasNext(); ) {
                    final Measurement m = (Measurement)iter.next();
                    // populate the Map if value doesn't exist
                    if (measCache != null && measCache != Collections.EMPTY_MAP) {
                        List measids = (List) measCache.get(m.getResource().getId());
                        if (measids == null) {
                            measids = new ArrayList();
                            measids.add(m);
                            measCache.put(m.getResource(), measids);
                        }
                    }
                    midsToGet.add(m.getId());
                }
            }
        }
        return getLastAvail((Integer[])midsToGet.toArray(new Integer[0]));
    }

    /**
     * @ejb:interface-method
     */
    public MetricValue getLastAvail(Measurement m) {
        Map map = getLastAvail(new Integer[] { m.getId() });
        MetricValue mv = (MetricValue)map.get(m.getId());
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
     * @return {@link Map} of {@link Integer} to {@link MetricValue}
     * Integer is the measurementId
     * @ejb:interface-method
     */
    public Map getLastAvail(Integer[] mids) {
        if (mids.length == 0) {
            return Collections.EMPTY_MAP;
        }
        // Don't modify callers array
        final List midList = Collections.unmodifiableList(Arrays.asList(mids));
        final Map rtn = new HashMap(midList.size());
        final List list = _dao.findLastAvail(midList);
        for (final Iterator i=list.iterator(); i.hasNext(); ) {
            final AvailabilityDataRLE avail = (AvailabilityDataRLE)i.next();
            final Integer mid = avail.getMeasurement().getId();
            final AvailabilityMetricValue mVal =
                new AvailabilityMetricValue(avail.getAvailVal(),
                                            avail.getStartime(),
                                            avail.getApproxEndtime());
            rtn.put(mid, mVal);
        }
        // fill in missing measurements
        final long now = TimingVoodoo.roundDownTime(
            System.currentTimeMillis(), MeasurementConstants.MINUTE);
        if (midList.size() > 0) {
            for (final Iterator i=midList.iterator(); i.hasNext(); ) {
                final Integer mid = (Integer)i.next();
                if (!rtn.containsKey(mid)) {
                    final MetricValue mVal = new MetricValue(AVAIL_UNKNOWN, now);
                    rtn.put(mid, mVal);
                }
            }
        }
        return rtn;
    }

    /**
     * @param includes List<Integer> of mids.  If includes is null then all
     * unavail entities will be returned.
     * @ejb:interface-method
     */
    public List getUnavailEntities(List includes) {
        List rtn;
        if (includes != null) {
            rtn = new ArrayList(includes.size());
        } else {
            rtn = new ArrayList();
        }
        List unavails = _dao.getDownMeasurements(includes);
        for (Iterator it=unavails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE) it.next();
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
     * @ejb:interface-method
     */
    public void addData(Integer mid, MetricValue mval) {
        List l = new ArrayList(1);
        l.add(new DataPoint(mid, mval));
        addData(l);
    }

    /**
     * Process Availability data.
     * The default behavior is to send the data points
     * to the event handlers.
     *
     * @param availPoints List of DataPoints
     *
     * @ejb:interface-method
     */
    public void addData(List availPoints) {
        addData(availPoints, true);
    }

    /**
     * Process Availability data.
     *
     * @param availPoints List of DataPoints
     * @param sendData
     *          Indicates whether to send the data to event handlers.
     *          The default behavior is true. If false, the calling method
     *          should call sendDataToEventHandlers directly afterwards.
     *
     * @ejb:transaction type="RequiresNew"
     * @ejb:interface-method
     */
    public void addData(List availPoints, boolean sendData)
    {
        if (availPoints == null || availPoints.size() == 0) {
            return;
        }
        List updateList = new ArrayList(availPoints.size());
        List outOfOrderAvail = new ArrayList(availPoints.size());
        AvailabilityCache cache = AvailabilityCache.getInstance();
        _createMap = new HashMap();
        _removeMap = new HashMap();
        final boolean debug = _log.isDebugEnabled();
        Map state = null;
        synchronized (cache) {
            try {
                cache.beginTran();
                updateCache(availPoints, updateList, outOfOrderAvail);
                setCurrAvails(outOfOrderAvail, updateList);
                state = captureCurrAvailState();
                updateStates(updateList);
                updateOutOfOrderState(outOfOrderAvail);
                flushCreateAndRemoves();
                logErrorInfo(state, availPoints);
                cache.commitTran();
            } catch (Throwable e) {
                logErrorInfo(state, availPoints);
                _log.error(e.getMessage(), e);
                cache.rollbackTran();
                throw new SystemException(e);
            } finally {
                _createMap = null;
                _removeMap = null;
                _currAvails = null;
            }
        }
        ConcurrentStatsCollector.getInstance().addStat(
            availPoints.size(), AVAIL_MANAGER_METRICS_INSERTED);
        if (sendData) {
            sendDataToEventHandlers(availPoints);
        }
    }

    private void flushCreateAndRemoves() {
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        
        if (debug) watch.markTimeBegin("remove");
        for (Iterator it=_removeMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            AvailabilityDataRLE rle = (AvailabilityDataRLE)entry.getValue();
            // if we call remove() on an object which is already in the session
            // hibernate will throw NonUniqueObjectExceptions
            AvailabilityDataRLE tmp = _dao.getById(rle.getAvailabilityDataId());
            if (tmp != null) {
                _dao.remove(tmp);
            } else {
                _dao.remove(rle);
            }
        }
        if (debug) {
            watch.markTimeEnd("remove");
            watch.markTimeBegin("flush");
        }
        
        // addData() could be overwriting RLE data points (i.e. from 0.0 to 1.0)
        // with the same ID.  If this is the scenario, then we must run
        // flush() in order to ensure that these old objects are not in the
        // session when the equivalent create() on the same ID is run,
        // thus avoiding NonUniqueObjectExceptions
        _dao.getSession().flush();
        
        if (debug) {
            watch.markTimeEnd("flush");
            watch.markTimeBegin("create");
        }
        
        for (Iterator it=_createMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            AvailabilityDataRLE rle = (AvailabilityDataRLE)entry.getValue();
            AvailabilityDataId id = new AvailabilityDataId();
            id.setMeasurement(rle.getMeasurement());
            id.setStartime(rle.getStartime());
            _dao.create(rle.getMeasurement(), rle.getStartime(),
                        rle.getEndtime(), rle.getAvailVal());
        }
        
        if (debug) {
            watch.markTimeEnd("create");
            _log.debug("AvailabilityInserter flushCreateAndRemoves: " + watch
                            + ", points {remove=" + _removeMap.size()
                            + ", create=" + _createMap.size()
                            + "}");
        }
    }

    private void logErrorInfo(final Map oldState, final List availPoints) {
        if (!_traceLog.isDebugEnabled()) {
            return;
        }
        Integer mid;
        Map currState = captureCurrAvailState();
        if (null != (mid = isAvailDataRLEValid())) {
            logAvailState(oldState, mid);
            logStates(availPoints, mid);
            logAvailState(currState, mid);
        } else {
            _traceLog.debug("RLE Data is valid");
        }
    }

    private void setCurrAvails(final List outOfOrderAvail,
                               final List updateList) {
        
        final StopWatch watch = new StopWatch();
        try {
            if (outOfOrderAvail.size() == 0 && updateList.size() == 0) {
                _currAvails = Collections.EMPTY_MAP;
                return;
            }
            long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), 60000);
            HashSet mids = getMidsWithinAllowedDataWindow(updateList, now);
            mids.addAll(getMidsWithinAllowedDataWindow(outOfOrderAvail, now));
            if (mids.size() <= 0) {
                _currAvails = Collections.EMPTY_MAP;
                return;
            }
            Integer[] mIds = (Integer[])mids.toArray(new Integer[0]);
            _currAvails = _dao.getHistoricalAvailMap(
                mIds, now-MAX_DATA_BACKLOG_TIME, false);            
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("AvailabilityInserter setCurrAvails: " + watch
                                + ", size=" + _currAvails.size());
            }
        }
    }

    private HashSet getMidsWithinAllowedDataWindow(final List states,
                                                   final long now) {
        HashSet mids = new HashSet();
        int i=0;
        for (Iterator it=states.iterator(); it.hasNext(); i++) {
            DataPoint pt = (DataPoint)it.next();
            long timestamp = pt.getTimestamp();
            // only allow data for the last MAX_DATA_BACKLOG_TIME ms
            // this way we don't have to bring too much into memory which could
            // severely impact performance
            if ((now-timestamp) > MAX_DATA_BACKLOG_TIME) {
                it.remove();
                long days = (now-timestamp)/MeasurementConstants.DAY;
                _log.warn(" Avail measurement came in " + days + " days " +
                          " late, dropping: timestamp=" + timestamp +
                          " measId=" + pt.getMetricId() +
                          " value=" + pt.getMetricValue());
                continue;
            }
            Integer mId = pt.getMetricId();
            if (!mids.contains(mId)) {
                mids.add(mId);
            }
        }
        return mids;
    }

    private void updateDup(DataPoint state, AvailabilityDataRLE dup)
        throws BadAvailStateException
    {
        if (dup.getAvailVal() == state.getValue()) {
            // nothing to do
        } else  if (dup.getAvailVal() != AVAIL_DOWN) {
            String msg = "New DataPoint and current DB value for " +
            "MeasurementId " + state.getMetricId() + " / timestamp " +
            state.getTimestamp() + " have conflicting states.  " +
            "Since a non-zero rle value cannot be overridden, no update." +
            "\ncurrent rle value -> " + dup +
            // ask Juilet Sierra why (js) is here
            ":(js):\npoint trying to override current rle -> " + state;
            throw new BadAvailStateException(msg);
        } else {
            Measurement meas = dup.getMeasurement();
            long newStartime = dup.getStartime()+meas.getInterval();
            insertPointOnBoundry(dup, newStartime, state);
        }
    }

    /**
     * sets avail's startime to newStartime and prepends a new avail obj
     * from avail.getStartime() to newStartime with a value of state.getValue()
     * Used specifically for a point which collides with a RLE on its startime
     */
    private void insertPointOnBoundry(AvailabilityDataRLE avail,
                                      long newStartime,
                                      DataPoint pt)
        throws BadAvailStateException
    {
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
                throw new BadAvailStateException(
                    "Availability measurement_id=" + pt.getMetricId() +
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
                // newStartime <  after.getEndtime()  &&
                // pt.getValue() != after.getAvailVal()
                // therefore, need to push back startTime and set the value
                long interval = meas.getInterval();
                if ( (after.getStartime()+interval) < after.getEndtime() ) {
                    prependState(pt, after);
                } else {
                    DataPoint afterPt = new DataPoint(meas.getId().intValue(),
                        after.getAvailVal(), after.getStartime());
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
        Collection rles = (Collection)_currAvails.get(mId);
        long start = state.getTimestamp();
        for (Iterator it=rles.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE)it.next();
            if (rle.getStartime() == start) {
                return rle;
            }
        }
        return null;
    }

    private AvailabilityDataRLE findAvailAfter(DataPoint state) {
        final Integer mId = state.getMetricId();
        final TreeSet rles = (TreeSet)_currAvails.get(mId);
        final long start = state.getTimestamp();
        final AvailabilityDataRLE tmp = new AvailabilityDataRLE();
        // tailSet is inclusive so we need to add 1 to start
        tmp.setStartime(start+1);
        final SortedSet set = rles.tailSet(tmp);
        if (set.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)set.first();
    }

    private AvailabilityDataRLE findAvailBefore(DataPoint state) {
        Integer mId = state.getMetricId();
        TreeSet rles = (TreeSet)_currAvails.get(mId);
        long start = state.getTimestamp();
        AvailabilityDataRLE tmp = new AvailabilityDataRLE();
        // headSet is inclusive so we need to subtract 1 from start
        tmp.setStartime(start-1);
        SortedSet set = rles.headSet(tmp);
        if (set.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)set.last();
    }

    private void merge(DataPoint state)
            throws BadAvailStateException {
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

    private void insertAvail(AvailabilityDataRLE before,
        AvailabilityDataRLE after, DataPoint state) {

        if (state.getValue() != after.getAvailVal() &&
                state.getValue() != before.getAvailVal()) {
            Measurement meas = getMeasurement(state.getMetricId());
            long pivotTime = state.getTimestamp() + meas.getInterval();
            create(meas, state.getTimestamp(), pivotTime, state.getValue());
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, pivotTime);
        } else if (state.getValue() == after.getAvailVal() &&
                   state.getValue() != before.getAvailVal()) {
            updateEndtime(before, state.getTimestamp());
            after = updateStartime(after, state.getTimestamp());
        } else if (state.getValue() != after.getAvailVal() &&
                   state.getValue() == before.getAvailVal()) {
            // this is fine
        } else if (state.getValue() == after.getAvailVal() &&
                   state.getValue() == before.getAvailVal()) {
            // this should never happen or else there is something wrong
            // in the code
            String msg = "AvailabilityData [" + before + "] and [" + after +
                "] have the same values.  This should not be the case.  " +
                "Cleaning up";
            _log.warn(msg);
            updateEndtime(before, after.getEndtime());
            removeAvail(after);
        }
    }

    private boolean prependState(DataPoint state, AvailabilityDataRLE avail) {
        AvailabilityDataRLE before = findAvailBefore(state);
        Measurement meas = avail.getMeasurement();
        if (before != null && before.getAvailVal() == state.getValue()) {
            long newStart =  state.getTimestamp() + meas.getInterval();
            updateEndtime(before, newStart);
            avail = updateStartime(avail, newStart);
        } else {
            long newStart =  state.getTimestamp() + meas.getInterval();
            long endtime = newStart;
            avail = updateStartime(avail, newStart);
            create(avail.getMeasurement(), state.getTimestamp(),
                   endtime, state.getValue());
        }
        return true;
    }

    private void updateAvailVal(AvailabilityDataRLE avail, double val) {
        Measurement meas = avail.getMeasurement();
        DataPoint state = new DataPoint(meas.getId().intValue(), val,
                                        avail.getStartime());
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

    private AvailabilityDataRLE updateStartime(AvailabilityDataRLE avail,
                                               long start) {
        // this should not be the case here, but want to make sure and
        // avoid HibernateUniqueKeyExceptions :(
        AvailabilityDataRLE tmp;
        Measurement meas = avail.getMeasurement();
        Integer mId = meas.getId();
        DataPoint tmpState = new DataPoint(
            mId.intValue(), avail.getAvailVal(), start);
        if (null != (tmp = findAvail(tmpState))) {
            removeAvail(tmp);
        }
        removeAvail(avail);
        return create(meas, start, avail.getEndtime(), avail.getAvailVal());
    }

    private void removeAvail(AvailabilityDataRLE avail) {
        long start = avail.getStartime();
        Integer mId = avail.getMeasurement().getId();
        TreeSet rles = (TreeSet)_currAvails.get(mId);
        if (rles.remove(avail)) {
            DataPoint key =
                new DataPoint(mId.intValue(), avail.getAvailVal(), start);
            _createMap.remove(key);
            _removeMap.put(key, avail);
        }
    }

    private AvailabilityDataRLE getLastAvail(DataPoint state) {
        Integer mId = state.getMetricId();
        TreeSet rles = (TreeSet)_currAvails.get(mId);
        if (rles.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)rles.last();
    }

    private AvailabilityDataRLE create(Measurement meas, long start,
                                       long end, double val) {
        AvailabilityDataRLE rtn = _createAvail(meas, start, end, val);
        _createMap.put(new DataPoint(meas.getId().intValue(), val, start), rtn);
        Integer mId = meas.getId();
        Collection rles = (Collection)_currAvails.get(mId);
        rles.add(rtn);
        return rtn;
    }

    private AvailabilityDataRLE _createAvail(Measurement meas, long start,
                                             long end, double val) {
        AvailabilityDataRLE rtn = new AvailabilityDataRLE();
        rtn.setMeasurement(meas);
        rtn.setStartime(start);
        rtn.setEndtime(end);
        rtn.setAvailVal(val);
        return rtn;
    }

    private AvailabilityDataRLE create(Measurement meas, long start, double val) {
        AvailabilityDataRLE rtn = _createAvail(meas, start, MAX_AVAIL_TIMESTAMP, val);
        _createMap.put(new DataPoint(meas.getId().intValue(), val, start), rtn);
        Integer mId = meas.getId();
        Collection rles = (Collection)_currAvails.get(mId);
        // I am assuming that this will be cleaned up by the caller where it
        // will update the rle before rtn if one exists
        rles.add(rtn);
        return rtn;
    }

    private boolean updateState(DataPoint state)
        throws BadAvailStateException {
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
	    } else if (state.getTimestamp() == avail.getStartime() &&
                   state.getValue() != avail.getAvailVal()) {
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
	        _log.debug("updating endtime on avail -> " + avail +
	                   ", updating to state -> " + state);
	    }
	    updateEndtime(avail, state.getTimestamp());
	    create(avail.getMeasurement(), state.getTimestamp(),
	        state.getValue());
	    return true;
    }

    private void updateStates(List states) {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        if (states.size() == 0) {
            return;
        }
        // as a performance optimization, fetch all the last avails
        // at once, rather than one at a time in updateState()
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        int numUpdates = 0;
        for (Iterator i=states.iterator(); i.hasNext(); ) {
            DataPoint state = (DataPoint)i.next();
            try {
                // need to check again since there could be multiple
                // states with the same id in the list
                DataPoint currState = cache.get(state.getMetricId());
                if (currState != null &&
                    currState.getValue() == state.getValue()) {
                    continue;
                }
                boolean updateCache = updateState(state);
                if (debug) {
                    _log.debug("state " + state +
                               " was updated, cache updated: " + updateCache);
                }
                if (updateCache) {
                    cache.put(state.getMetricId(), state);
                    numUpdates++;
                }
            } catch (BadAvailStateException e) {
                _log.warn(e.getMessage());
            }
        }
        if (debug) {
            _log.debug("AvailabilityInserter updateStates: " + watch
                            + ", points {total=" + states.size()
                            + ", updateCache=" + numUpdates
                            + "}");
        }
    }

    private void updateOutOfOrderState(List outOfOrderAvail) {
        if (outOfOrderAvail.size() == 0) {
            return;
        }
        
        final StopWatch watch = new StopWatch();
        int numBadAvailState = 0;
        
        for (Iterator i=outOfOrderAvail.iterator(); i.hasNext(); ) {
            try {
            	DataPoint state = (DataPoint)i.next();
            	// do not update the cache here, the timestamp is out of order
                merge(state);
            } catch (BadAvailStateException e) {
                numBadAvailState++;
                _log.warn(e.getMessage());
            }
        }
        
        if (_log.isDebugEnabled()) {
            _log.debug("AvailabilityInserter updateOutOfOrderState: " + watch
                            + ", points {total=" + outOfOrderAvail.size()
                            + ", badAvailState=" + numBadAvailState
                            + "}");
        }
    }

    private void updateCache(List availPoints, List updateList,
                             List outOfOrderAvail)
    {
        if (availPoints.size() == 0) {
            return;
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        for (Iterator i=availPoints.iterator(); i.hasNext(); ) {
            DataPoint pt = (DataPoint)i.next();
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
            } else if (oldState == null || oldState.getValue() == AVAIL_NULL ||
                    oldState.getValue() != val) {
                updateList.add(newState);
                if (debug) {
                    String msg = "value of state[" + newState +
                                 "] differs from current value[" +
                                 ((oldState != null) ? oldState.toString() :
                                     "old state does not exist") + "]";
                    _log.debug(msg);
                }
            } else {
                cache.put(new Integer(id), newState);
	        }
        }
        if (debug) {
            _log.debug("AvailabilityInserter updateCache: " + watch
                            + ", points {total=" + availPoints.size()
                            + ", outOfOrder=" + outOfOrderAvail.size()
                            + ", updateToDb=" + updateList.size()
                            + ", updateCacheTimestamp="
                            + (availPoints.size() - outOfOrderAvail.size() - updateList.size())
                            + "}");
        }
    }

    private void sendDataToEventHandlers(List data) {
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        int maxCapacity = data.size();
        ArrayList events  = new ArrayList(maxCapacity);
        Map downEvents  = new HashMap(maxCapacity);
        List zevents = new ArrayList(maxCapacity);
        MeasurementManagerLocal measMan = MeasurementManagerEJBImpl.getOne();

        final boolean allEventsInteresting =
            Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        if (debug) watch.markTimeBegin("isTriggerInterested");
        for (Iterator i = data.iterator(); i.hasNext();) {
            DataPoint dp = (DataPoint) i.next();
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();

            MeasurementEvent event = new MeasurementEvent(metricId, val);

            boolean isEventInteresting =
                allEventsInteresting || RegisteredTriggers.isTriggerInterested(event);
            if (isEventInteresting) {
                measMan.buildMeasurementEvent(event);
                if (event.getValue().getValue() == AVAIL_DOWN) {
                    Resource r = getResource(event.getResource());
                    if (r != null && !r.isInAsyncDeleteState()) {
                        downEvents.put(r.getId(), event);
                    }
                } else {
                    events.add(event);
                }
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }
        if (debug) watch.markTimeEnd("isTriggerInterested");

        if (!downEvents.isEmpty()) {
            if (debug) watch.markTimeBegin("suppressMeasurementEvents");
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance()
                .getHierarchicalAlertingManager()
                    .suppressMeasurementEvents(downEvents, true);
            if (debug) watch.markTimeEnd("suppressMeasurementEvents");

            if (!downEvents.isEmpty()) {
                events.addAll(downEvents.values());
            }
        }

        if (!events.isEmpty()) {
            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }

        if (!zevents.isEmpty()) {
            ZeventManager.getInstance().enqueueEventsAfterCommit(zevents);
        }
        
        if (debug) {
            _log.debug("AvailabilityInserter sendDataToEventHandlers: " + watch
                            + ", points {total=" + maxCapacity
                            + ", downEvents=" + downEvents.size()
                            + ", eventsToPublish=" + events.size()
                            + ", zeventsToEnqueue=" + zevents.size()
                            + "}");
        }
    }

    /**
     * This method should only be called by the AvailabilityCheckService
     * and is used to filter availability data points based on
     * hierarchical alerting rules.
     *
     * @ejb:interface-method
     */
    public void sendDataToEventHandlers(Map data) {
        int maxCapacity = data.size();
        Map events  = new HashMap(maxCapacity);
        List zevents = new ArrayList(maxCapacity);
        MeasurementManagerLocal measMan = MeasurementManagerEJBImpl.getOne();

        boolean allEventsInteresting =
            Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        for (Iterator i = data.keySet().iterator(); i.hasNext();) {
            Integer resourceIdKey = (Integer) i.next();
            DataPoint dp = (DataPoint) data.get(resourceIdKey);
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (RegisteredTriggers.isTriggerInterested(event)
                    || allEventsInteresting) {
                measMan.buildMeasurementEvent(event);
                events.put(resourceIdKey, event);
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }

        if (!events.isEmpty()) {
            // Determine whether the measurement events can
            // be suppressed as part of hierarchical alerting
            PermissionManagerFactory.getInstance()
                .getHierarchicalAlertingManager()
                    .suppressMeasurementEvents(events, false);

            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC,
                                  new ArrayList(events.values()));
        }

        if (!zevents.isEmpty()) {
            ZeventManager.getInstance().enqueueEventsAfterCommit(zevents);
        }
    }

    private Measurement getMeasurement(Integer mId) {
        return MeasurementManagerEJBImpl.getOne().getMeasurement(mId);
    }

    public static AvailabilityManagerLocal getOne() {
        try {
            return AvailabilityManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private Map captureCurrAvailState() {
        if (!_traceLog.isDebugEnabled()) {
            return null;
        }
        Map rtn = new HashMap();
        for (Iterator it=_currAvails.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Integer mid = (Integer)entry.getKey();
            Collection rles = (Collection)entry.getValue();
            StringBuilder buf = new StringBuilder("\n");
            for (Iterator ii=rles.iterator(); ii.hasNext(); ) {
                AvailabilityDataRLE rle = (AvailabilityDataRLE)ii.next();
                buf.append(mid).append(" | ")
                   .append(rle.getStartime()).append(" | ")
                   .append(rle.getEndtime()).append(" | ")
                   .append(rle.getAvailVal()).append("\n");
            }
            rtn.put(mid, buf);
        }
        return rtn;
    }

    private void logAvailState(Map availState, Integer mid) {
        StringBuilder buf = (StringBuilder)availState.get(mid);
        _traceLog.debug(buf.toString());
    }

    private void logStates(List states, Integer mid) {
        StringBuilder log = new StringBuilder("\n");
        for (Iterator it=states.iterator(); it.hasNext(); ) {
            DataPoint pt = (DataPoint)it.next();
            if (!pt.getMetricId().equals(mid)) {
                continue;
            }
            log.append(pt.getMetricId()).append(" | ")
               .append(pt.getTimestamp()).append(" | ")
               .append(pt.getMetricValue()).append("\n");
        }
        _traceLog.debug(log.toString());
    }

    private Integer isAvailDataRLEValid() {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        synchronized(cache) {
            for (Iterator it=_currAvails.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Integer mId = (Integer)entry.getKey();
                Collection rles = (Collection)entry.getValue();
                if (!isAvailDataRLEValid(mId, cache.get(mId), rles)) {
                    return mId;
                }
            }
        }
        return null;
    }

    private boolean isAvailDataRLEValid(Integer measId, DataPoint lastPt,
                                        Collection avails) {
        AvailabilityDataRLE last = null;
        Set endtimes = new HashSet();
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)it.next();
            Long endtime = new Long(avail.getEndtime());
            if (endtimes.contains(endtime)) {
                _log.error("list for MID=" + measId +
                           " contains two or more of the same endtime=" + endtime);
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
        if (((DataPoint)cache.get(measId)).getValue() != lastPt.getValue()) {
            _log.error("last avail data point does not match cache");
            return false;
        }
        return true;
    }

    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}

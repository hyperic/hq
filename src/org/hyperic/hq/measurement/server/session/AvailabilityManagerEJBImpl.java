/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManagerUtil;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

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
    private static final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_UNKNOWN =
        MeasurementConstants.AVAIL_UNKNOWN;
    private static final int IND_MIN       = MeasurementConstants.IND_MIN;
    private static final int IND_AVG       = MeasurementConstants.IND_AVG;
    private static final int IND_MAX       = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private static final long MAX_AVAIL_TIMESTAMP =
        AvailabilityDataRLE.getLastTimestamp();
    private static final String ALL_EVENTS_INTERESTING_PROP = 
        "org.hq.triggers.all.events.interesting";
    private static final int DEFAULT_INTERVAL = 60;
    
    /**
     * @ejb:interface-method
     */
    public Measurement getAvailMeasurement(Resource resource) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        return dao.getAvailMeasurement(resource);
    }
    
    /**
     * @ejb:interface-method
     */
    public List getPlatformResources() {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        return dao.findAvailabilityByInstances(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null);
    }

    /**
     * @return List of all measurement ids for availability, ordered
     * 
     * @ejb:interface-method
     */
    public List getAllAvailIds() {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        return dao.getAllAvailIds();
    }

    /**
     * @ejb:interface-method
     */
    public List getAvailMeasurementChildren(Resource resource) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List list = new ArrayList();
        list.add(resource.getId());
        return dao.getAvailMeasurementChildren(list);
    }
    
    /**
     * TODO: Can this method be combined with the one that takes an array?
     * 
     * @ejb:interface-method
     */
    public PageList getHistoricalAvailData(Integer mid, long begin, long end,
                                           PageControl pc) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List availInfo = dao.getHistoricalAvails(mid.intValue(), begin,
                                                 end, pc.isDescending());
        return getPageList(availInfo, begin, end);
    }

    /**
     * @ejb:interface-method
     */
    public PageList getHistoricalAvailData(Integer[] mids, long begin, long end,
                                           PageControl pc) {
        if (mids.length == 0) {
            return new PageList();
        }
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List availInfo = dao.getHistoricalAvails(mids, begin,
            end, pc.isDescending());
        return getPageList(availInfo, begin, end);
    }
    
    private Collection getDefaultHistoricalAvail(long timestamp)
    {
        HighLowMetricValue[] rtn = new HighLowMetricValue[DEFAULT_INTERVAL];
        Arrays.fill(rtn, new HighLowMetricValue(AVAIL_UNKNOWN, timestamp));
        return Arrays.asList(rtn);
    }

    private PageList getPageList(List availInfo, long begin, long end) {
        PageList rtn = new PageList();
        long interval = (end-begin)/DEFAULT_INTERVAL;
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
            for (long curr=begin; curr<end; curr+=interval) {
                long next = curr+interval;
                long endtime =
                    ((AvailabilityDataRLE)queue.getFirst()).getEndtime();
                while (next > endtime) {
                    AvailabilityDataRLE tmp = (AvailabilityDataRLE)it.next();
                    queue.addFirst(tmp);
                    endtime = tmp.getEndtime();
                }
                endtime = availEndtime;
                while (curr > endtime) {
                    queue.removeLast();
                    rle = (AvailabilityDataRLE)queue.getLast();
                    availStartime = rle.getStartime();
                    availEndtime = rle.getEndtime();
                    endtime = availEndtime;
                }
                HighLowMetricValue val;
                if (curr >= availStartime) {
                    val = getMetricValue(queue, curr);
                } else {
                    val = new HighLowMetricValue(AVAIL_UNKNOWN, curr);
                    val.incrementCount();
                }
                if (rtn.size() <= i) {
                    rtn.add(val);
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
     * @ejb:interface-method
     */
    public Map getAggregateData(Integer[] tids, Integer[] iids,
                                long begin, long end)
    {
        List tidList = new ArrayList(Arrays.asList(tids));
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List avails = dao.findAggregateAvailability(tids, iids, begin, end);
        long interval = (end - begin)/DEFAULT_INTERVAL;
        Map rtn = new HashMap();
        double now = TimingVoodoo.roundDownTime(System.currentTimeMillis(),
                                                60000);
        if (avails.size() == 0) {
            for (Iterator i=tidList.iterator(); i.hasNext(); ) {
                Integer tid = (Integer)i.next();
                rtn.put(tid, getDefaultData(interval, now));
            }
            return rtn;
        }
        int i = 0;
        Map lastMap = new HashMap();
        Object[] objs = (Object[])avails.get(i++);
        for (long curr = begin; curr < end; curr += interval) {
            Integer tid = (Integer)objs[0];
            while (begin > ((Long)objs[5]).longValue()) {
                objs = (Object[])avails.get(i++);
            }
            double[] data = new double[5];
            data[IND_MIN] = ((Double)objs[1]).doubleValue();
            data[IND_AVG] = ((Double)objs[2]).doubleValue();
            data[IND_MAX] = ((Double)objs[3]).doubleValue();
            data[IND_CFG_COUNT] = (double)interval;
            Long endtime = (Long)objs[5];
            Double availVal = (Double)objs[6];
            MetricValue mval;
            long lendtime = endtime.longValue();
            if (null == (mval = (MetricValue)lastMap.get(tid)) ||
                mval.getTimestamp() > lendtime)
            {
                mval = new MetricValue(availVal, lendtime);
                lastMap.put(tid, mval);
            }
            rtn.put(tid, data);
        }

        for (Iterator it=lastMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Integer tid = (Integer)entry.getKey();
            MetricValue lastVal = (MetricValue)entry.getValue();
            double[] data = (double[])rtn.get(tid);
            data[IND_LAST_TIME] = lastVal.getValue();
        }
        return rtn;
    }

    private double[] getDefaultData(double interval, double timestamp) {
        double[] data = new double[5];
        data[IND_MIN] = AVAIL_UNKNOWN;
        data[IND_AVG] = AVAIL_UNKNOWN;
        data[IND_MAX] = AVAIL_UNKNOWN;
        data[IND_CFG_COUNT] = interval;
        data[IND_LAST_TIME] = AVAIL_UNKNOWN;
        return data;
    }

    /**
     * @ejb:interface-method
     */
    public List getLastAvail(Integer mid) {
        Integer[] mids = new Integer[1];
        List rtn = new ArrayList();
        mids[0] = mid;
        Map map = getLastAvail(mids, MAX_AVAIL_TIMESTAMP - 1);
        MetricValue mVal = (MetricValue)map.get(mid);
        if (mVal == null) {
            rtn.add(new MetricValue(AVAIL_UNKNOWN, System.currentTimeMillis()));
        } else {
            rtn.add(mVal);
        }
        return rtn;
    }

    /**
     * @ejb:interface-method
     */
    public Map getLastAvail(Integer[] mids) {
        return getLastAvail(mids, -1);
    }

    /**
     * @ejb:interface-method
     */
    public Map getLastAvail(Integer[] mids, long after) {
        Map rtn = new HashMap();
        if (mids.length == 0) {
            return rtn;
        }
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List list;
        List midList = new ArrayList(Arrays.asList(mids));
        if (after != -1) {
            list = dao.findLastAvail(midList, after);
        } else {
            list = dao.findLastAvail(midList);
        }
        long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), 60000);
        for (Iterator i=list.iterator(); i.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)i.next();
            Integer mid = avail.getMeasurement().getId();
            long endtime;
            if (avail.getEndtime() == MAX_AVAIL_TIMESTAMP) {
                endtime = now;
            } else {
                endtime = avail.getEndtime();
            }
            MetricValue tmp;
            if (null == (tmp = (MetricValue)rtn.get(mid)) ||
                    endtime > tmp.getTimestamp()) {
                MetricValue mVal = new MetricValue(avail.getAvailVal(), endtime);
                rtn.put(avail.getMeasurement().getId(), mVal);
                midList.remove(avail.getMeasurement().getId());
            }
        }
        // fill in missing measurements
        if (midList.size() > 0) {
            for (Iterator i=midList.iterator(); i.hasNext(); ) {
                Integer mid = (Integer)i.next();
                MetricValue mVal = new MetricValue(AVAIL_UNKNOWN, now);
                rtn.put(mid, mVal);
            }
        }
        return rtn;
    }

    /**
     * @ejb:interface-method
     */
    public List getUnavailEntities(List includes) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List rtn = new ArrayList();
        List down = dao.getDownMeasurements();
        for (Iterator i=down.iterator(); i.hasNext(); ) {
            AvailabilityDataRLE rle = (AvailabilityDataRLE) i.next();
            Measurement meas = rle.getMeasurement();
            long timestamp = rle.getStartime();
            Integer mid = meas.getId();
            if (includes != null && !includes.contains(mid)) {
                continue;
            }
            MetricValue val = new MetricValue(AVAIL_DOWN, timestamp);
            rtn.add(new DownMetricValue(meas.getEntityId(), mid, val));
        }
        return rtn;
    }

    /**
     * @param availPoints list of availability points
     * 
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void addData(List availPoints)
    {
        List updateList = new ArrayList(availPoints.size());
        List outOfOrderAvail = new ArrayList(availPoints.size());
        LastAvailUpObj avail = LastAvailUpObj.getInst();
        synchronized (avail) {
            updateCache(availPoints, updateList, outOfOrderAvail);
            updateStates(updateList);
        }
        updateOutOfOrderState(outOfOrderAvail);
        sendDataToEventHandlers(availPoints);
    }

    private void updateDup(DataPoint state, AvailabilityDataRLE dup)
            throws BadAvailStateException {
        updateDup(state, dup, false);
    }

    private void updateDup(DataPoint state, AvailabilityDataRLE dup,
            boolean ignoreInvalidState) throws BadAvailStateException {
        if (dup.getAvailVal() == state.getValue()) {
            // nothing to do
        } else  if (!ignoreInvalidState && dup.getAvailVal() != AVAIL_DOWN) {
            String msg = "New DataPoint and current DB value for " +
             "MeasurementId " + state.getMetricId() + " / timestamp " +
             state.getTimestamp() + " have conflicting states, no update";
            throw new BadAvailStateException(msg);
        } else {
            Measurement meas = dup.getMeasurement();
            long interval = meas.getInterval();
            long newStartime = dup.getStartime()+interval;
            resolveStarttime(dup, newStartime, state.getValue());
        }
    }

    private void resolveStarttime(AvailabilityDataRLE avail, long newStartime,
                                 double newAvailVal)
        throws BadAvailStateException {
        
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        if (avail.getEndtime() == MAX_AVAIL_TIMESTAMP) {
            dao.updateStartime(avail, newStartime);
            return;
        } else if (newStartime > avail.getEndtime()) {
            dao.remove(avail);
            return;
        } else if (newStartime == avail.getEndtime()) {
            DataPoint state =
                new DataPoint(avail.getMeasurement().getId().intValue(),
                               avail.getAvailVal(), avail.getStartime());
            AvailabilityDataRLE after = dao.findAvailAfter(state);
            dao.remove(avail);
            if (after.getAvailVal() == newAvailVal) {
                AvailabilityDataRLE before = dao.findAvailBefore(state);
                if (before != null &&
                        before.getAvailVal() == after.getAvailVal()) {
                    dao.remove(before);
                    dao.updateStartime(after, before.getStartime());
                }
                else {
                    dao.updateStartime(after, avail.getStartime());
                }
                return;
            } else  if (newStartime >= after.getStartime()) {
                dao.remove(after);
	            dao.create(avail.getMeasurement(), avail.getStartime(),
	                       after.getEndtime(), state.getValue());
            }
            return;
        }
        dao.updateStartime(avail, newStartime);
    }

    private void merge(DataPoint state)
            throws BadAvailStateException {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        AvailabilityDataRLE dup = dao.findAvail(state);
        if (dup != null) {
            updateDup(state, dup);
            return;
        }
        AvailabilityDataRLE before = dao.findAvailBefore(state);
        AvailabilityDataRLE after = dao.findAvailAfter(state);
        if (before == null && after == null) {
            // this shouldn't happen here
            Measurement meas = getMeasurement(state.getMetricId().intValue());
            dao.create(meas, state.getTimestamp(),
                       MAX_AVAIL_TIMESTAMP,
                       state.getValue());
        } else if (before == null) {
            if (after.getAvailVal() != state.getValue()) {
                prependState(state, after);
            } else {
                dao.updateStartime(after, state.getTimestamp());
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

        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        if (state.getValue() != after.getAvailVal() &&
                state.getValue() != before.getAvailVal()) {
            Measurement meas = getMeasurement(state.getMetricId().intValue());
            long pivotime = meas.getInterval();
            dao.create(meas, state.getTimestamp(), pivotime,
                       state.getValue());
            dao.updateEndtime(before, state.getTimestamp());
            dao.updateStartime(after, pivotime);
        } else if (state.getValue() == after.getAvailVal() &&
                   state.getValue() != before.getAvailVal()) {
            dao.updateEndtime(before, state.getTimestamp());
            dao.updateStartime(after, state.getTimestamp());
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
            dao.updateEndtime(before, after.getEndtime());
            dao.remove(after);
        }
    }

    private boolean prependState(DataPoint state) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        AvailabilityDataRLE avail = dao.findAvailAfter(state);
        if (avail == null) {
            Measurement meas = getMeasurement(state.getMetricId().intValue());
            dao.create(meas, state.getTimestamp(), MAX_AVAIL_TIMESTAMP,
                state.getValue());
            return true;
        }
        return prependState(state, avail);
    }

    private boolean prependState(DataPoint state, AvailabilityDataRLE avail) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        Measurement meas = avail.getMeasurement();
        long newStart =  state.getTimestamp() + meas.getInterval();
        long endtime = newStart;
        updateStartime(avail, newStart);
        dao.create(avail.getMeasurement(), state.getTimestamp(),
            endtime, state.getValue());
        return true;
    }
    
    /*
     * !!!need to move all calls to this
     */
    private void updateStartime(AvailabilityDataRLE avail, long start) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        // this should not be the case here, but want to make sure and
        // avoid HibernateUniqueKeyExceptions :(
        AvailabilityDataRLE tmp;
        DataPoint tmpState = new DataPoint(
            avail.getMeasurement().getId().intValue(),
            avail.getAvailVal(), start);
        if (null != (tmp = dao.findAvail(tmpState))) {
            dao.remove(tmp);
        }
        dao.updateStartime(avail, start);
    }

    private boolean updateState(DataPoint state)
        throws BadAvailStateException {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        List mids = new ArrayList();
        mids.add(state.getMetricId());
        List avails = dao.findLastAvail(mids);
        AvailabilityDataRLE avail = null;
        if (avails.size() > 0) {
            avail = (AvailabilityDataRLE)avails.get(0);
        }
	    if (avail == null) {
	        Measurement meas = getMeasurement(state.getMetricId().intValue());
	        dao.create(meas,
	            state.getTimestamp(), state.getValue());
	        return true;
	    } else if (state.getTimestamp() < avail.getStartime()) {
	        merge(state);
	        return true;
	    } else if (state.getTimestamp() == avail.getStartime() &&
                   state.getValue() != avail.getAvailVal()) {
	        updateDup(state, avail);
	        return true;
	    } else if (state.getValue() == avail.getAvailVal()) {
	        _log.debug("no update state == avail " + state + " == " + avail);
	        return false;
	    }
	    _log.debug("updating endtime on avail -> " + avail +
	        ", updating to state -> " + state);
	    dao.updateEndtime(avail, state.getTimestamp());
	    dao.create(avail.getMeasurement(), state.getTimestamp(),
	        state.getValue());
	    return true;
    }

    private void updateStates(List states) {
        LastAvailUpObj avail = LastAvailUpObj.getInst();
        for (Iterator i=states.iterator(); i.hasNext(); ) {
            DataPoint state = (DataPoint)i.next();
            try {
                // need to check again since there could be multiple
                // states with the same id in the list
                DataPoint currState = avail.get(state.getMetricId());
                if (currState != null &&
                    currState.getValue() == state.getValue()) {
                    continue;
                }
                boolean update = updateState(state);
                _log.debug("state " + state + " was updated, status " + update);
                avail.put(state.getMetricId(), state);
            } catch (BadAvailStateException e) {
                _log.warn(e.getMessage());
            }
        }
    }

    private void updateOutOfOrderState(List outOfOrderAvail) {
        for (Iterator i=outOfOrderAvail.iterator(); i.hasNext(); ) {
            try {
            	DataPoint state = (DataPoint)i.next();
            	// do not update the cache here, the timestamp is out of order
                merge(state);
            } catch (BadAvailStateException e) {
                _log.warn(e.getMessage());
            }
        }
    }

    private void updateCache(List availPoints, List updateList,
                             List outOfOrderAvail)
    {
        LastAvailUpObj avail = LastAvailUpObj.getInst();
        for (Iterator i=availPoints.iterator(); i.hasNext(); ) {
            DataPoint pt = (DataPoint)i.next();
			int id = pt.getMetricId().intValue();
            MetricValue mval = pt.getMetricValue();
            double val = mval.getValue();
            long timestamp = mval.getTimestamp();
            DataPoint newState = new DataPoint(id, val, timestamp);
            DataPoint oldState = avail.get(new Integer(id));
            // we do not want to update the state if it changes
            // instead change it when the db is changed in order
            // to ensure the state of memory to db
            // ONLY update memory state here if there is no change
            if (oldState != null && timestamp < oldState.getTimestamp()) {
                outOfOrderAvail.add(newState);
            } else if (oldState == null || oldState.getValue() == AVAIL_NULL ||
                    oldState.getValue() != val) {
                updateList.add(newState);
                _log.debug("value of state " + newState + " differs from" +
                           " current value" + ((oldState == null) ?
                           " old state does not exist" : ""));
            } else {
                avail.put(new Integer(id), newState);
	        }
        }
    }
    
    private void sendDataToEventHandlers(List data) {
        ArrayList events  = new ArrayList();
        List zevents = new ArrayList();

        boolean allEventsInteresting = 
            Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        for (Iterator i = data.iterator(); i.hasNext();) {
            DataPoint dp = (DataPoint) i.next();
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (RegisteredTriggers.isTriggerInterested(event) ||
                    allEventsInteresting) {
                events.add(event);
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }

        if (!events.isEmpty()) {
            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }

        if (!zevents.isEmpty()) {
            try {
                // XXX:  Shouldn't this be a transactional queueing?
                ZeventManager.getInstance().enqueueEvents(zevents);
            } catch(InterruptedException e) {
                _log.warn("Interrupted while sending availability events.  " +
                          "Some data may be lost");
            }
        }
    }

    private static Measurement getMeasurement(int id) {
        MeasurementManagerLocal derMan =
            MeasurementManagerEJBImpl.getOne();
        return derMan.getMeasurement(new Integer(id));
    }

    public static AvailabilityManagerLocal getOne() {
        try {
            return AvailabilityManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}

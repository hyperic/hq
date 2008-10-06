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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManager_testLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManager_testUtil;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.server.MBeanUtil;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="AvailabilityManager_test"
 *      jndi-name="ejb/authz/AvailabilityManager_test"
 *      local-jndi-name="LocalAvailabilityManager_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class AvailabilityManager_testEJBImpl implements SessionBean {

    private static final String _logCtx =
        AvailabilityManager_testEJBImpl.class.getName();
    private final Log _log = LogFactory.getLog(_logCtx);
    private static final String AVAIL_TAB = "HQ_AVAIL_DATA_RLE";
    private static final Integer PLAT_MEAS_ID = new Integer(10100);
    private static final Integer SERVICE_MEAS_ID = new Integer(13678);
    private final List _list = new ArrayList();
    private static final String BACKFILLER_SERVICE =
        "hyperic.jmx:type=Service,name=AvailabilityCheck";
    
    /**
     * @ejb:interface-method
     */
    public void testInsertScenarios() throws Exception {
        // testing out of order scenarios
        testOverlap();
        testInsertIntoMiddle();
        testPrepend();
        testPrependWithDupValue();
        testNonOneorZeroDupPtInsertAtBegin();
        stressTest1();
        stressTest2();
    }
    
    /**
     * @ejb:interface-method
     */
    public void testCatchup() throws Exception {
        // need to invoke backfiller once so that its initial time is set
        // so that it can start when invoked the next time
        invokeBackfiller(1l);
        testCatchup(PLAT_MEAS_ID);
        testCatchup(SERVICE_MEAS_ID);
    }
    
    /**
     * @ejb:interface-method
     */
    public void testAvailabilityStatusWhenNtwkDwn() throws Exception {
        // need to invoke backfiller once so that its initial time is set
        // so that it can start when invoked the next time
        invokeBackfiller(1l);       
        testAvailabilityForPlatform(PLAT_MEAS_ID);        
    }
    
    /**
     * @ejb:interface-method
     */
    public void testBackfillingForService() throws Exception {        
        invokeBackfiller(1l);
        //Following method will verify that when the platform is down it's 
        //associated resources will be marked down by the backfiller
        //after waiting for one interval from the last cache update time        
        testAvailabilityForService(SERVICE_MEAS_ID);        
    }
    
    private void invokeBackfiller(long timestamp)
        throws InstanceNotFoundException,
               MBeanException,
               ReflectionException,
               MalformedObjectNameException,
               NullPointerException {
        Date date = new Date(timestamp);
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        _log.info("Invoking Backfiller with date " + date);
        ObjectName objName = new ObjectName(BACKFILLER_SERVICE);
        Object[] obj = {date};
        String[] str = {"java.util.Date"};
        server.invoke(objName, "hitWithDate", obj, str);
    }
    
    private void testOverlap() throws Exception {
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        List list = new ArrayList();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        DataPoint pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(1.0, baseTime));
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(0.5, baseTime));
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(1.0, baseTime));
        list.add(pt);
        avail.addData(list);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    private void stressTest2() throws Exception {
        setupAvailabilityTable();
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        ArrayList list = new ArrayList();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        long tmpTime = baseTime;
        for (int i=0; i<1000; i++) {
            double val = 1.0;
            if ((i%5) == 0) {
                val = .5;
            }
            list.add(
                new DataPoint(PLAT_MEAS_ID.intValue(), val, tmpTime+=incrTime));
        }
        avail.addData(list);
        list.clear();
        for (int i=0; i<1000; i++) {
            list.add(
                new DataPoint(PLAT_MEAS_ID.intValue(), 0.0, tmpTime+=incrTime));
        }
        DataPoint pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    private void stressTest1() throws Exception {
        setupAvailabilityTable();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        DataPoint pt = testCatchup(PLAT_MEAS_ID, baseTime, incrTime);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = testCatchup(PLAT_MEAS_ID, (baseTime+120000), incrTime);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }
    
    private void testCatchup(Integer measId) throws Exception {
        setupAvailabilityTable();
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        Measurement meas = mMan.getMeasurement(measId);

        long interval = meas.getInterval();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        invokeBackfiller(baseTime);

        List list = new ArrayList();
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime+interval));
        list.add(pt);
        avail.addData(list);
        // want to invoke backfiller slightly offset from the regular interval
        invokeBackfiller(baseTime+(interval*5)-5000);
        List avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                                                   baseTime+(interval*100));
        if (avails.size() != 2) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 2);
        // all points should be green in db after this
        for (int i=0; i<10; i++) {
            pt = new DataPoint(
                measId, new MetricValue(1.0, baseTime+(interval*i)));
            list.add(pt);
        }
        avail.addData(list);
        Assert.assertTrue(isAvailDataRLEValid(measId, pt));
        avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                                              baseTime+(interval*10));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
    }
    
    private void dumpAvailsToLogger(List avails) {
        Integer id = null;
        for (Iterator i=avails.iterator(); i.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)i.next();
            id = avail.getMeasurement().getId();
            String msg = id + ", " + avail.getStartime() +
                ", " + avail.getEndtime() + ", " + avail.getAvailVal();
            _log.error(msg);
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        if (id == null) {
            return;
        }
        synchronized(cache) {
            _log.error("Cache info -> " + cache.get(id).getTimestamp() + ", " +
                cache.get(id).getValue());
        }
    }

    private DataPoint testCatchup(Integer measId, long baseTime, long incrTime)
        throws Exception {
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);

        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        List list = new ArrayList();
        long tmpTime = baseTime;
        DataPoint pt;

        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime+incrTime*2));
        list.add(pt);
        for (int i=0; i<5; i++) {
            double val = 1.0;
            if ((i%2) == 0) {
                val = 0.0;
            }
            pt = new DataPoint(measId, new MetricValue(val, tmpTime+=incrTime));
            list.add(pt);
        }
        avail.addData(list);

        list.clear();
        pt = new DataPoint(measId, new MetricValue(1.0, tmpTime));
        list.add(pt);
        avail.addData(list);
        return pt;
    }

    private void testAvailabilityForPlatform(Integer measId) throws Exception{
	setupAvailabilityTable();
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        Measurement meas = mMan.getMeasurement(measId);

        long interval = meas.getInterval();        
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        List list = new ArrayList();
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(0.0, baseTime+interval));        
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime+interval*2));        
        list.add(pt);
        // Add DataPoints for three consecutive intervals with varying availability data
        avail.addData(list);
        List avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                baseTime+(interval*10));
        if (avails.size() != 3) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 3);
        // Assume that the network is down from the interval "baseTime+interval*2"
        // Invoke the backfiller for every two interval
        invokeBackfiller(baseTime+interval*4);
        invokeBackfiller(baseTime+interval*6);
        invokeBackfiller(baseTime+interval*8);         
        invokeBackfiller(baseTime+interval*10);
        // Expect the backfiller to fill in the unavailable data
        avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                baseTime+(interval*10));
        if (avails.size() != 4) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 4);
        list.clear();
        //After the network is up we start getting the availability data for the period when the network was down
        for (int i=3; i<=10; i++){
            pt = new DataPoint(measId, new MetricValue(1.0, baseTime+interval*(i)));        
            list.add(pt);
        }
        avail.addData(list);
        // Expect to have 3 availability data after processing the agent data that is sent after network is up
        avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                                                   baseTime+(interval*100));
        if (avails.size() != 3) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 3);      
    }    
    
    private void testAvailabilityForService(Integer measId) throws Exception{
	setupAvailabilityTable();
	setupAvailabilityTable(measId);
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        Measurement meas = mMan.getMeasurement(measId); 

        long interval = meas.getInterval();        
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        List list = new ArrayList();        
        //First, let's make the platform as down
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(0.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime+interval*10));
        list.add(pt);        
        avail.addData(list);
        List avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                baseTime+(interval*20));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
        Measurement meas1 = mMan.getMeasurement(PLAT_MEAS_ID);
        avails = avail.getHistoricalAvailData(meas1.getResource(), baseTime,
                baseTime+(interval*10));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
        //Invoking the backfiller with exactly the same time of the last update time
        invokeBackfiller(baseTime+interval*10);
        avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                baseTime+(interval*20));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1); 
        //Invoking the backfiller one interval after the last update time
        invokeBackfiller(baseTime+interval*11);
        avails = avail.getHistoricalAvailData(meas.getResource(), baseTime,
                baseTime+(interval*20));
        if (avails.size() != 2) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 2);
        list.clear();            
    }
    
    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *      --> updateDup(DataPoint, AvailabilityDataRLE)
     *          --> insertPointOnBoundry(AvailabilityDataRLE, long, DataPoint)
     *      MATCHES THIS CONDITION ->
     *              } else if (newStartime == avail.getEndtime()) {
     *                  } else {
     *                      dao.updateVal(after, pt.getValue());
     *                  }
     */
    private void testNonOneorZeroDupPtInsertAtBegin() throws Exception {
        setupAvailabilityTable();
        long INCRTIME = 60000;
        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=INCRTIME));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=INCRTIME));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=INCRTIME));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=INCRTIME));

        // overwrite first val
        addData(PLAT_MEAS_ID, new MetricValue(0.5, baseTime+INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }
    
    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *  MATCHES THIS CONDITION ->
     *          } else if (before == null) {
     */
    private void testPrependWithDupValue() throws Exception {
        setupAvailabilityTable();

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=120000));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=60000));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=60000));
        pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=60000));

        // prepend state on to the beginning
        addData(PLAT_MEAS_ID, new MetricValue(1.0, baseTime+60000));

        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *  MATCHES THIS CONDITION ->
     *          } else if (before == null) {
     */
    private void testPrepend() throws Exception {
        setupAvailabilityTable();

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=120000));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=60000));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=60000));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=60000));

        // prepend state on to the beginning
        addData(PLAT_MEAS_ID, new MetricValue(1.0, baseTime+60000));

        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *      --> insertAvail(AvailabilityDataRLE, AvailabilityDataRLE, DataPoint)
     *      MATCHES THIS CONDITION ->
     *              } else if (state.getValue() == after.getAvailVal() &&
     *              state.getValue() != before.getAvailVal()) {
     */
    private void testInsertIntoMiddle() throws Exception {
        setupAvailabilityTable();
        int INCRTIME = 240000;

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=INCRTIME));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime+=INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime+=INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));

        // insert into the middle
        long middleTime = baseTime+(INCRTIME*3)+(INCRTIME/2);
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, middleTime));

        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }
    
    private DataPoint addData(Integer measId, MetricValue mVal) {
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        _list.clear();
        DataPoint pt = new DataPoint(measId, mVal);
        _list.add(pt);
        avail.addData(_list);
        return pt;
    }

    private boolean isAvailDataRLEValid(Integer mId, DataPoint lastPt) {
        List mids = Collections.singletonList(mId);
        return isAvailDataRLEValid(mids, lastPt);
    }

    private boolean isAvailDataRLEValid(List mids, DataPoint lastPt) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        boolean descending = false;
        Map avails = dao.getHistoricalAvailMap(
            (Integer[])mids.toArray(new Integer[0]), 0, descending);
        for (Iterator it=avails.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Integer mId = (Integer)entry.getKey();
            List rleList = (List)entry.getValue();
            if (!isAvailDataRLEValid(mId, lastPt, rleList)) {
                return false;
            }
        }
        return true;
    }
        
    private boolean isAvailDataRLEValid(Integer measId, DataPoint lastPt, List avails) {
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
    
    private void setupAvailabilityTable() throws Exception {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        cache.clear();
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        boolean descending = false;
        long start = 0l;
        long end = AvailabilityDataRLE.getLastTimestamp();
        Integer[] mids = new Integer[1];
        mids[0] = PLAT_MEAS_ID;
        List avails = dao.getHistoricalAvails(mids, start, end, descending);
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)it.next();
            dao.remove(avail);
        }
        _log.info("deleted " + avails.size() + " rows from " + AVAIL_TAB +
                  " with measurement Id = " + PLAT_MEAS_ID);
    }

    private void setupAvailabilityTable(Integer measId) throws Exception {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        cache.clear();
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        boolean descending = false;
        long start = 0l;
        long end = AvailabilityDataRLE.getLastTimestamp();
        Integer[] mids = new Integer[1];
        mids[0] = measId;
        List avails = dao.getHistoricalAvails(mids, start, end, descending);
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)it.next();
            dao.remove(avail);
        }
        _log.info("deleted " + avails.size() + " rows from " + AVAIL_TAB +
                  " with measurement Id = " + PLAT_MEAS_ID);
    }
    
    private AvailabilityDataDAO getAvailabilityDataDAO() {
        return new AvailabilityDataDAO(DAOFactory.getDAOFactory());
    }
    
    public static AvailabilityManager_testLocal getOne() {
        try {
            return AvailabilityManager_testUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}

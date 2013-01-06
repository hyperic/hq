/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.db.DatabasePopulator;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
//import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

//Guys 16/12/2012 - temporarily disabled as the db unit population fails on constraint violation 
//(the defferable statement is not working without creating the table with the defferable clause)
@Ignore  
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(loader    = IntegrationTestContextLoader.class,
                      locations = { "classpath*:META-INF/spring/*-context.xml",
                                    "AvailabilityManagerTest-context.xml" })
@DirtiesContext
public class AvailabilityManagerTest {

    private final Log log = LogFactory.getLog(AvailabilityManagerTest.class);
    private static final String AVAIL_TAB = "HQ_AVAIL_DATA_RLE";
    private static final Integer PLAT_MEAS_ID = 10100;
    private static final Integer SERVICE_MEAS_ID = 10224;
    private final List<DataPoint> list = new ArrayList<DataPoint>();

    @Autowired
    private AvailabilityManager aMan;
    @Autowired
    private MeasurementManager mMan;
    @Autowired
    private AvailabilityDataDAO dao;
    @Autowired
    private AvailabilityCheckService availabilityCheckService;
    @Autowired
    private DatabasePopulator dbPopulator;
    
    @Autowired
    private AvailabilityCache availabilityCache;

    public AvailabilityManagerTest() {
    }

    
    @After
    public void after() {
        //Clear the 2nd level cache including regions with queries
        CacheManager.getInstance().clearAll();
    }

    
    private void beforeTest() throws Exception {
        //populate DB here so it will be rolled back with rest of test transaction
        //can use DBPopulator or just create test data programatically
        //TODO may not need to load all this data for every test - move this
        //into only test methods that need it?
        dbPopulator.restoreDatabase();
        setupAvailabilityTable();
    }
    
    @Test
    public void testFindLastAvail() throws Exception {
        dbPopulator.restoreDatabase();
        List<AvailabilityDataRLE> rle = dao.findLastAvail(Collections.singletonList(10100));
        Assert.assertTrue("rle value is incorrect",
            rle.get(0).getAvailVal() == MeasurementConstants.AVAIL_UP);
        Assert.assertTrue(dao.getClass().getName() == AvailabilityDataDAO.class.getName());
    }

    @Test
    public void testCatchup() throws Exception {
    	beforeTest();

    	// need to invoke backfiller once so that its initial time is set
        // so that it can start when invoked the next time
        testCatchup(PLAT_MEAS_ID);
        testCatchup(SERVICE_MEAS_ID);
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) -->
     * insertAvail(AvailabilityDataRLE, AvailabilityDataRLE, DataPoint) MATCHES
     * THIS CONDITION -> } else if (state.getValue() == after.getAvailVal() &&
     * state.getValue() != before.getAvailVal()) {
     */
    @Test
    public void testInsertIntoMiddle() throws Exception {
    	beforeTest();
        int INCRTIME = 240000;
        long baseTime = TimingVoodoo.roundDownTime(now(), 60000);
        long tmpTime = baseTime;
        DataPoint pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += INCRTIME));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        // insert into the middle
        long middleTime = baseTime + (INCRTIME * 3) + (INCRTIME / 2);
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, middleTime));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    @Test
    public void testOverlap() throws Exception {
    	beforeTest();
        List<DataPoint> list = new ArrayList<DataPoint>();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        DataPoint pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(1.0, baseTime));
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(0.5, baseTime));
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(1.0, baseTime));
        list.add(pt);
        addData(list);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    @Test
    public void stressTest2() throws Exception {
    	beforeTest();
        ArrayList<DataPoint> list = new ArrayList<DataPoint>();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        long tmpTime = baseTime;
        for (int i = 0; i < 1000; i++) {
            double val = 1.0;
            if ((i % 5) == 0) {
                val = .5;
            }
            list.add(new DataPoint(PLAT_MEAS_ID.intValue(), val, tmpTime += incrTime));
        }
        addData(list);
        list.clear();
        for (int i = 0; i < 1000; i++) {
            list.add(new DataPoint(PLAT_MEAS_ID.intValue(), 0.0, tmpTime += incrTime));
        }
        DataPoint pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    @Test
    public void stressTest1() throws Exception {
    	beforeTest();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        DataPoint pt = testCatchup(PLAT_MEAS_ID, baseTime, incrTime);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
        pt = testCatchup(PLAT_MEAS_ID, (baseTime + 120000), incrTime);
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    private void backfill(long baseTime) {
        availabilityCheckService.testBackfill(baseTime);
        dao.getSession().flush();
        dao.getSession().clear();
    }

    private void testCatchup(Integer measId) throws Exception {
    	setupAvailabilityTable();
        Measurement meas = mMan.getMeasurement(measId);
        long interval = meas.getInterval();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        DataPoint pt;
        backfill(baseTime);
        List<DataPoint> list = new ArrayList<DataPoint>();
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime + interval));
        list.add(pt);
        addData(list);
        // want to invoke backfiller slightly offset from the regular interval
        backfill(baseTime + (interval * 6) - 5000);
        List<AvailabilityDataRLE> avails = aMan.getHistoricalAvailData(meas.getResource(),
            baseTime, baseTime + (interval * 100));
        if (avails.size() != 2) {
            dumpAvailsToLogger(avails);
        }
        
        Assert.assertEquals(2,avails.size());
        // all points should be green in db after this
        for (int i = 0; i < 10; i++) {
            pt = new DataPoint(measId, new MetricValue(1.0, baseTime + (interval * i)));
            list.add(pt);
        }
        addData(list);
        Assert.assertTrue(isAvailDataRLEValid(measId, pt));
        avails = aMan.getHistoricalAvailData(meas.getResource(), baseTime, baseTime +
                                                                           (interval * 10));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
    }
    
    
    
    private void dumpAvailsToLogger(List<AvailabilityDataRLE> avails, long startTime, long endTime) {
        log.error("Between times: " + startTime + "->" + endTime);
        dumpAvailsToLogger(avails);    
    }

    private void dumpAvailsToLogger(List<AvailabilityDataRLE> avails) {
        Integer id = null;
        for (AvailabilityDataRLE avail : avails) {
            id = avail.getMeasurement().getId();
            String msg = id + ", " + avail.getStartime() + ", " + avail.getEndtime() + ", " +
                         avail.getAvailVal();
            log.error(msg);
        }
       
        if (id == null) {
            return;
        }
        synchronized (availabilityCache) {
        	if (availabilityCache.get(id) != null)
	            log.error("Cache info -> " + availabilityCache.get(id).getTimestamp() + ", " +
	                       availabilityCache.get(id).getValue());
        }
    }

    private DataPoint testCatchup(Integer measId, long baseTime, long incrTime) throws Exception {
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        List<DataPoint> list = new ArrayList<DataPoint>();
        long tmpTime = baseTime;
        DataPoint pt;
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime + incrTime * 2));
        list.add(pt);
        for (int i = 0; i < 5; i++) {
            double val = 1.0;
            if ((i % 2) == 0) {
                val = 0.0;
            }
            pt = new DataPoint(measId, new MetricValue(val, tmpTime += incrTime));
            list.add(pt);
        }
        addData(list);
        list.clear();
        pt = new DataPoint(measId, new MetricValue(1.0, tmpTime));
        list.add(pt);
        addData(list);
        return pt;
    }

    @Test
    public void testAvailabilityStatusWhenNtwkDwn() throws Exception {
    	beforeTest();
        testAvailabilityForPlatform(PLAT_MEAS_ID);
    }

    private void testAvailabilityForPlatform(Integer measId) throws Exception {
        setupAvailabilityTable(measId);
        Measurement meas = mMan.getMeasurement(measId);
        long interval = meas.getInterval();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        List<DataPoint> list = new ArrayList<DataPoint>();
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(0.0, baseTime + interval));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime + interval * 2));
        list.add(pt);
        // Add DataPoints for three consecutive intervals with varying
        // availability data
        addData(list);
        List<AvailabilityDataRLE> avails = aMan.getHistoricalAvailData(meas.getResource(),
            baseTime, baseTime + (interval * 10));
        if (avails.size() != 3) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertEquals("After initializing data:", 3, avails.size());
        // Assume that the network is down from the interval
        // "baseTime+interval*2"
        // Invoke the backfiller for every two interval
        backfill(baseTime + interval * 4);
        backfill(baseTime + interval * 6);
        backfill(baseTime + interval * 8);
        backfill(baseTime + interval * 10);
        // Expect the backfiller to fill in the unavailable data
        avails = aMan.getHistoricalAvailData(meas.getResource(), baseTime, baseTime +
                                                                           (interval * 10));
        if (avails.size() != 4) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertEquals(4,avails.size());
        list.clear();
        // After the network is up we start getting the availability data for
        // the period when the network was down
        for (int i = 3; i <= 10; i++) {
            pt = new DataPoint(measId, new MetricValue(1.0, baseTime + interval * (i)));
            list.add(pt);
        }
        addData(list);
        // Expect to have 3 availability data after processing the agent data
        // that is sent after network is up
        avails = aMan.getHistoricalAvailData(meas.getResource(), baseTime, baseTime +
                                                                           (interval * 100));
        if (avails.size() != 3) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 3);
    }

    @Test
    public void testBackfillingForService() throws Exception {
        // Following method will verify that when the platform is down it's
        // associated resources will be marked down by the backfiller
        // after waiting for one interval from the last cache update time
    	beforeTest();
    	testAvailabilityForService(SERVICE_MEAS_ID);
    }

    private void testAvailabilityForService(Integer measId) throws Exception {
        setupAvailabilityTable(measId);
        Measurement meas = mMan.getMeasurement(measId);
        long interval = meas.getInterval();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        List<DataPoint> list = new ArrayList<DataPoint>();
        // First, let's make the platform as down
        pt = new DataPoint(PLAT_MEAS_ID, new MetricValue(0.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime + interval * 10));
        list.add(pt);
        addData(list);
        //Thread.sleep(10000);
        List<AvailabilityDataRLE> avails = aMan.getHistoricalAvailData(meas.getResource(),
            baseTime, baseTime + (interval * 20));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
        Measurement meas1 = mMan.getMeasurement(PLAT_MEAS_ID);
        avails = aMan.getHistoricalAvailData(meas1.getResource(), baseTime, baseTime +
                                                                            (interval * 10));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertEquals("Verifying initial data", 1, avails.size());
        // Invoking the backfiller with exactly the same time of the last update
        // time
        backfill(baseTime + interval * 10);
        avails = aMan.getHistoricalAvailData(meas.getResource(), baseTime, baseTime +
                                                                           (interval * 20));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
        // Invoking the backfiller one interval after the last update time
        backfill(baseTime + interval * 11);
        avails = aMan.getHistoricalAvailData(meas.getResource(), baseTime, baseTime +
                                                                           (interval * 20));
        if (avails.size() != 2) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertEquals(2,avails.size());
        list.clear();
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) -->
     * updateDup(DataPoint, AvailabilityDataRLE) -->
     * insertPointOnBoundry(AvailabilityDataRLE, long, DataPoint) MATCHES THIS
     * CONDITION -> } else if (newStartime == avail.getEndtime()) { } else {
     * dao.updateVal(after, pt.getValue()); }
     */
    @Test
    public void testNonOneorZeroDupPtInsertAtBegin() throws Exception {
    	beforeTest();
        long INCRTIME = 60000;
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += INCRTIME));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += INCRTIME));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += INCRTIME));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += INCRTIME));
        // overwrite first val
        addData(PLAT_MEAS_ID, new MetricValue(0.5, baseTime + INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) MATCHES
     * THIS CONDITION -> } else if (before == null) {
     */
    @Test
    public void testPrependWithDupValue() throws Exception {
    	beforeTest();
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += 120000));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += 60000));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += 60000));
        pt = addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += 60000));
        // prepend state on to the beginning
        addData(PLAT_MEAS_ID, new MetricValue(1.0, baseTime + 60000));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) MATCHES
     * THIS CONDITION -> } else if (before == null) {
     */
    @Test
    public void testPrepend() throws Exception {
    	beforeTest();
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += 120000));
        addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += 60000));
        addData(PLAT_MEAS_ID, new MetricValue(0.0, tmpTime += 60000));
        pt = addData(PLAT_MEAS_ID, new MetricValue(1.0, tmpTime += 60000));
        // prepend state on to the beginning
        addData(PLAT_MEAS_ID, new MetricValue(1.0, baseTime + 60000));
        Assert.assertTrue(isAvailDataRLEValid(PLAT_MEAS_ID, pt));
    }

    private boolean isAvailDataRLEValid(Integer mId, DataPoint lastPt) {
        List<Integer> mids = Collections.singletonList(mId);
        return isAvailDataRLEValid(mids, lastPt);
    }

    private boolean isAvailDataRLEValid(List<Integer> mids, DataPoint lastPt) {
        boolean descending = false;
        Map<Integer, TreeSet<AvailabilityDataRLE>> avails = dao.getHistoricalAvailMap(
            mids.toArray(new Integer[0]), 0, descending);
        for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : avails.entrySet()) {
            Integer mId = entry.getKey();
            Collection<AvailabilityDataRLE> rleList = entry.getValue();
            if (!isAvailDataRLEValid(mId, lastPt, rleList)) {
                return false;
            }
        }
        return true;
    }

    private void setupAvailabilityTable() throws Exception {
    	setupAvailabilityTable(PLAT_MEAS_ID);
    }

    private void setupAvailabilityTable(Integer measId) throws Exception {
        availabilityCache.clear();
        dao.getSession().clear();
        boolean descending = false;
        long start = 0l;
        long end = AvailabilityDataRLE.getLastTimestamp();
        Integer[] mids = new Integer[1];
        mids[0] = measId;
        List<AvailabilityDataRLE> avails = dao.getHistoricalAvails(mids, start, end, descending);
        for (AvailabilityDataRLE avail : avails) {
            dao.remove(avail);
        }
        dao.getSession().flush();
        log.info("deleted " + avails.size() + " rows from " + AVAIL_TAB +
                  " with measurement Id = " + measId);
    }

    private boolean isAvailDataRLEValid(Integer measId, DataPoint lastPt,
                                        Collection<AvailabilityDataRLE> avails) {
        AvailabilityDataRLE last = null;
        Set<Long> endtimes = new HashSet<Long>();
        for (AvailabilityDataRLE avail : avails) {
            Long endtime = new Long(avail.getEndtime());
            if (endtimes.contains(endtime)) {
                log.error("list for MID=" + measId + " contains two or more of the same endtime=" +
                           endtime);
                return false;
            }
            endtimes.add(endtime);
            if (last == null) {
                last = avail;
                continue;
            }
            if (last.getAvailVal() == avail.getAvailVal()) {
                log.error("consecutive availpoints have the same value");
                return false;
            } else if (last.getEndtime() != avail.getStartime()) {
                log.error("there are gaps in the availability table");
                return false;
            }
            last = avail;
        }
       
        if (availabilityCache.get(measId).getValue() != lastPt.getValue()) {
            log.error("last avail data point does not match cache");
            return false;
        }
        return true;
    }

    private void addData(List<DataPoint> vals) {
        for (DataPoint val : vals) {
            log.info("adding timestamp=" + val.getTimestamp() + ", value=" + val.getValue());
        }
        list.clear();
        list.addAll(vals);
        aMan.addData(list);
        dao.getSession().flush();
        dao.getSession().clear();
    }

    private DataPoint addData(Integer measId, MetricValue mVal) {
        log.info("adding measId="+ measId + ", timestamp=" + mVal.getTimestamp() + ", value=" + mVal.getValue());
        list.clear();
        DataPoint pt = new DataPoint(measId, mVal);
        list.add(pt);
        aMan.addData(list);
        dao.getSession().flush();
        dao.getSession().clear();
        return pt;
    }

    private static final long now() {
        return System.currentTimeMillis();
    }

    
    @Test
    public void testVCFallback() throws Exception {
    	dbPopulator.setSchemaFile("/data/availabilityVCTests.xml.gz");
    	dbPopulator.restoreDatabase();
    

    	testVCFallback(MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_UNKNOWN);
    	testVCFallback(MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_DOWN);
    }

    	
   /*
    * Actions:
    * - add VC_PLATFORM_RESOURCE_MEAS_ID status - the given statusByVc
    * - add old UP status for the VM monitored platform
    * - run Backfill, that should:
    * 	a. update VM-Platform with  statusByVc
    * 	b. update a server/service on the VM with  expectedServerStatus (Unknown/Down)
    * 	c. update the VM's HQ Agent server with status DOWN.
    * - verify the above statuses have been updated.
    */
   public void testVCFallback(double statusByVc, double expectedServerStatus) throws Exception {
    	
        // Last data in DB: 11297, 1344754260000, 9223372036854775807, 1.0
    	final Integer VM_PLATFORM_RESOURCE_MEAS_ID = 10370; // platform resource id: 11083
    	final Integer VC_PLATFORM_RESOURCE_MEAS_ID = 11297; // platform resource id: 11131
    	final Integer VM_SOME_SERVER_MEAS_ID = 10496; // Resource id: 11095
    	final Integer VM_HQ_AGENT_MEAS_ID = 10595; // Resource id: 11087
    	long now = now();
    	
    	// clear previous data, if exists.
    	setupAvailabilityTable(VM_PLATFORM_RESOURCE_MEAS_ID);
    	setupAvailabilityTable(VM_HQ_AGENT_MEAS_ID);
    	setupAvailabilityTable(VM_SOME_SERVER_MEAS_ID);
    	
    	
    	// set initial availability data - current for VC platform, old for VM platform and servers/services
    	addData(VC_PLATFORM_RESOURCE_MEAS_ID, new MetricValue(statusByVc, now));
    	
    	Measurement vmMeas = mMan.getMeasurement(VM_PLATFORM_RESOURCE_MEAS_ID);
        long interval = vmMeas.getInterval();
        long oldTimeStamp = now-(30*interval);
    	addData(VM_PLATFORM_RESOURCE_MEAS_ID, new MetricValue(MeasurementConstants.AVAIL_UP, oldTimeStamp));
    	addData(VM_HQ_AGENT_MEAS_ID, new MetricValue(MeasurementConstants.AVAIL_UP, oldTimeStamp));
    	addData(VM_SOME_SERVER_MEAS_ID, new MetricValue(MeasurementConstants.AVAIL_UP, oldTimeStamp));
    	
    	// run backfill
    	backfill(now);
    	
    	// validate statuses
        long baseTime = oldTimeStamp-interval; //TimingVoodoo.roundDownTime(now, 600000);
        long endTime = now + (interval * 20); 
        
        // check VM Platform status
        int numOfExpectedStatuses = 1;
        if (statusByVc != MeasurementConstants.AVAIL_UP)
        	numOfExpectedStatuses = 2;
        checkStatus(vmMeas, numOfExpectedStatuses, statusByVc, "Checking VM Platform status:", baseTime, endTime);

    
        // check VM Platform Server status
    	Measurement vmServerMeas = mMan.getMeasurement(VM_SOME_SERVER_MEAS_ID);
        checkStatus(vmServerMeas, 2, expectedServerStatus, "Checking server's status:", baseTime, endTime);


        // check VM HQAgent status
    	Measurement vmHqAgentMeas = mMan.getMeasurement(VM_HQ_AGENT_MEAS_ID);
        checkStatus(vmHqAgentMeas, 2, MeasurementConstants.AVAIL_DOWN, "Checking HQ agent status:", baseTime, endTime);
        
    }
    
    
    private void checkStatus(Measurement meas, int expectedNumStatuses, double expectedStatus, String assertionMessage, long startTime, long endTime) {
        List<AvailabilityDataRLE> avails = aMan.getHistoricalAvailData(meas.getResource(), startTime, endTime);
        int availsSize = avails.size();
        if (availsSize != expectedNumStatuses)
        	dumpAvailsToLogger(avails, startTime, endTime);
        Assert.assertEquals(expectedNumStatuses, availsSize);
        AvailabilityDataRLE lastAvail = avails.get(availsSize-1);
        Assert.assertEquals(assertionMessage, new Double(expectedStatus), new Double(lastAvail.getAvailVal()));
    }
}

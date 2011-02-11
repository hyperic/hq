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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;




@DirtiesContext
public class AvailabilityManagerTest extends BaseInfrastructureTest {

    private final Log log = LogFactory.getLog(AvailabilityManagerTest.class);
    private Integer platformMeasurementId;
    private Integer serverMeasurementId;
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
    private AvailabilityCache availabilityCache;
    
    @Autowired
    private AvailabilityDataDAO availabilityDataDao;
    
    @Autowired
    private TemplateManager templateManager;

    private String agentToken="1234289833876-6223436270032869210-5096092157018801322";
    
    private Measurement platformAvail;

    @Before
    public void initializeData() throws Exception {
        createAgent("10.2.0.206", 2144, "1234289833526-6988536330070102285-4109856494195896854",agentToken , "5.0");
        Platform platform = createPlatform(agentToken, "PluginTestPlatform", "Platform1", "Platform1", 2);
        List<MeasurementTemplate> templates = templateManager.findTemplates("PluginTestPlatform", MeasurementConstants.CAT_AVAILABILITY, new Integer[0],PageControl.PAGE_ALL);
        List<Measurement> measurements = mMan.createMeasurements(platform
            .getEntityId(), new Integer[] { templates.get(0).getId() },
            new long[] { templates.get(0).getDefaultInterval() }, new ConfigResponse());
        platformAvail = measurements.get(0);
        platformMeasurementId = platformAvail.getId();
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        Server server = createServer(platform, serverType, "Server1");
        ServiceType serviceType = serviceManager.findServiceTypeByName("PluginTestServer 1.0 Web Module Stats");
        Service service = createService(server.getId(), serviceType, "Service1", "A service", "Here");
        List<MeasurementTemplate> serverTemplates = templateManager.findTemplates("PluginTestServer 1.0 Web Module Stats", MeasurementConstants.CAT_AVAILABILITY, new Integer[0],PageControl.PAGE_ALL);
        List<Measurement> serverMeasurements = mMan.createMeasurements(service
            .getEntityId(), new Integer[] { serverTemplates.get(0).getId() },
            new long[] { serverTemplates.get(0).getDefaultInterval() }, new ConfigResponse());
        serverMeasurementId = serverMeasurements.get(0).getId();
    }
   

    @Test
    public void testFindLastAvail() {
        availabilityDataDao.create(platformAvail, 1234289880000l, 9223372036854775807l,1.0d);
        flushSession();
        List<AvailabilityDataRLE> rle = dao.findLastAvail(Collections.singletonList(platformMeasurementId));
        Assert.assertTrue("rle value is incorrect",
            rle.get(0).getAvailVal() == MeasurementConstants.AVAIL_UP);
    }

    @Test
    public void testCatchup() throws Exception {
        // need to invoke backfiller once so that its initial time is set
        // so that it can start when invoked the next time
        testCatchup(platformMeasurementId);
        testCatchup(serverMeasurementId);
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
        availabilityDataDao.create(platformAvail, 1234289880000l, 9223372036854775807l,1.0d);
        flushSession();
        int INCRTIME = 240000;
        long baseTime = TimingVoodoo.roundDownTime(now(), 60000);
        long tmpTime = baseTime;
        DataPoint pt = addData(platformMeasurementId, new MetricValue(0.0, tmpTime += INCRTIME));
        pt = addData(platformMeasurementId, new MetricValue(1.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
        pt = addData(platformMeasurementId, new MetricValue(0.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
        pt = addData(platformMeasurementId, new MetricValue(1.0, tmpTime += INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
        // insert into the middle
        long middleTime = baseTime + (INCRTIME * 3) + (INCRTIME / 2);
        pt = addData(platformMeasurementId, new MetricValue(1.0, middleTime));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    @Test
    public void testOverlap() throws Exception {
        availabilityDataDao.create(platformAvail, 1234289880000l, 9223372036854775807l,1.0d);
        flushSession();
        List<DataPoint> list = new ArrayList<DataPoint>();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        DataPoint pt = new DataPoint(platformMeasurementId, new MetricValue(1.0, baseTime));
        pt = new DataPoint(platformMeasurementId, new MetricValue(0.5, baseTime));
        pt = new DataPoint(platformMeasurementId, new MetricValue(1.0, baseTime));
        list.add(pt);
        addData(list);
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    @Test
    public void stressTest2() throws Exception {
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
            list.add(new DataPoint(platformMeasurementId.intValue(), val, tmpTime += incrTime));
        }
        addData(list);
        list.clear();
        for (int i = 0; i < 1000; i++) {
            list.add(new DataPoint(platformMeasurementId.intValue(), 0.0, tmpTime += incrTime));
        }
        DataPoint pt = addData(platformMeasurementId, new MetricValue(1.0, tmpTime));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    @Test
    public void stressTest1() throws Exception {
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        DataPoint pt = testCatchup(platformMeasurementId, baseTime, incrTime);
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
        pt = testCatchup(platformMeasurementId, (baseTime + 120000), incrTime);
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    private void backfill(long baseTime) {
        availabilityCheckService.backfill(baseTime);
        dao.getSession().flush();
        dao.getSession().clear();
    }

    private void testCatchup(Integer measId) throws Exception {
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
        testAvailabilityForPlatform(platformMeasurementId);
    }

    private void testAvailabilityForPlatform(Integer measId) throws Exception {
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
        Assert.assertTrue(avails.size() == 3);
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
        testAvailabilityForService(serverMeasurementId);
    }

    private void testAvailabilityForService(Integer measId) throws Exception {
        Measurement meas = mMan.getMeasurement(measId);
        long interval = meas.getInterval();
        long now = now();
        long baseTime = TimingVoodoo.roundDownTime(now, 600000);
        DataPoint pt;
        List<DataPoint> list = new ArrayList<DataPoint>();
        // First, let's make the platform as down
        pt = new DataPoint(platformMeasurementId, new MetricValue(0.0, baseTime));
        list.add(pt);
        pt = new DataPoint(measId, new MetricValue(1.0, baseTime + interval * 10));
        list.add(pt);
        addData(list);
        List<AvailabilityDataRLE> avails = aMan.getHistoricalAvailData(meas.getResource(),
            baseTime, baseTime + (interval * 20));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
        Measurement meas1 = mMan.getMeasurement(platformMeasurementId);
        avails = aMan.getHistoricalAvailData(meas1.getResource(), baseTime, baseTime +
                                                                            (interval * 10));
        if (avails.size() != 1) {
            dumpAvailsToLogger(avails);
        }
        Assert.assertTrue(avails.size() == 1);
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
        long INCRTIME = 60000;
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(platformMeasurementId, new MetricValue(0.0, tmpTime += INCRTIME));
        addData(platformMeasurementId, new MetricValue(1.0, tmpTime += INCRTIME));
        addData(platformMeasurementId, new MetricValue(0.0, tmpTime += INCRTIME));
        pt = addData(platformMeasurementId, new MetricValue(1.0, tmpTime += INCRTIME));
        // overwrite first val
        addData(platformMeasurementId, new MetricValue(0.5, baseTime + INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) MATCHES
     * THIS CONDITION -> } else if (before == null) {
     */
    @Test
    public void testPrependWithDupValue() throws Exception {
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(platformMeasurementId, new MetricValue(1.0, tmpTime += 120000));
        addData(platformMeasurementId, new MetricValue(0.0, tmpTime += 60000));
        addData(platformMeasurementId, new MetricValue(1.0, tmpTime += 60000));
        pt = addData(platformMeasurementId, new MetricValue(0.0, tmpTime += 60000));
        // prepend state on to the beginning
        addData(platformMeasurementId, new MetricValue(1.0, baseTime + 60000));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path updateOutOfOrderState(List) --> merge(DataPoint) MATCHES
     * THIS CONDITION -> } else if (before == null) {
     */
    @Test
    public void testPrepend() throws Exception {
        long baseTime = now();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;
        DataPoint pt;
        addData(platformMeasurementId, new MetricValue(0.0, tmpTime += 120000));
        addData(platformMeasurementId, new MetricValue(1.0, tmpTime += 60000));
        addData(platformMeasurementId, new MetricValue(0.0, tmpTime += 60000));
        pt = addData(platformMeasurementId, new MetricValue(1.0, tmpTime += 60000));
        // prepend state on to the beginning
        addData(platformMeasurementId, new MetricValue(1.0, baseTime + 60000));
        Assert.assertTrue(isAvailDataRLEValid(platformMeasurementId, pt));
    }

    private boolean isAvailDataRLEValid(Integer mId, DataPoint lastPt) {
        List<Integer> mids = Collections.singletonList(mId);
        return isAvailDataRLEValid(mids, lastPt);
    }

    private boolean isAvailDataRLEValid(List<Integer> mids, DataPoint lastPt) {
        boolean descending = false;
        Map<Integer, TreeSet<AvailabilityDataRLE>> avails = dao.getHistoricalAvailMap(
            (Integer[]) mids.toArray(new Integer[0]), 0, descending);
        for (Map.Entry<Integer, TreeSet<AvailabilityDataRLE>> entry : avails.entrySet()) {
            Integer mId = (Integer) entry.getKey();
            Collection<AvailabilityDataRLE> rleList = entry.getValue();
            if (!isAvailDataRLEValid(mId, lastPt, rleList)) {
                return false;
            }
        }
        return true;
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
        log.info("adding timestamp=" + mVal.getTimestamp() + ", value=" + mVal.getValue());
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

}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createStrictControl;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.hibernate.Session;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.db.DatabasePopulator;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.junit.Ignore;

//Guys 16/12/2012 - temporarily disabled as the db unit population fails on constraint violation 
//(the defferable statement is not working without creating the table with the defferable clause)
@Ignore  
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(loader    = IntegrationTestContextLoader.class,
                      locations = {"classpath*:META-INF/spring/*-context.xml", "SrnManagerTest-context.xml"})
@DirtiesContext
public class SrnManagerTest {
    private static final int PLATFORM_ID = 11060;
    
    @Autowired
    private SRNManager srnManager;
    @Autowired
    private ScheduleRevNumDAO scheduleRevNumDAO;
    @Autowired
    private MeasurementManager measurementManager;
    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;
    @Autowired
    private DatabasePopulator dbPopulator;
    
    @Before
    public void initialize() throws Exception {
        dbPopulator.restoreDatabase();
    }

    @Test
    @DirtiesContext
    @Transactional
    public void testScheduleInBackground() {
        Session session = scheduleRevNumDAO.getSession();
        Resource platformRes = resourceManager.findResourceById(PLATFORM_ID);
        AppdefEntityID platformAeid = AppdefUtil.newAppdefEntityId(platformRes);
        SrnId srnId = new SrnId(platformAeid.getType(), platformAeid.getID());
        Collection<AppdefEntityID> list = Collections.singletonList(platformAeid);
        IMocksControl controller = createStrictControl();
        ZeventManager zeventManagerMock = controller.createMock(ZeventManager.class);
        srnManager.setZeventManager(zeventManagerMock);
        Capture<AgentScheduleSyncZevent> capture = new Capture<AgentScheduleSyncZevent>();
        zeventManagerMock.enqueueEventAfterCommit(and(capture(capture), isA(AgentScheduleSyncZevent.class)));
        expectLastCall().times(1);
        controller.replay();

        ScheduleRevNum srn = srnManager.get(platformAeid);
        if (srn == null) {
            srn = scheduleRevNumDAO.create(platformAeid);
        }
        int srnVal = srn.getSrn();
        srnManager.scheduleInBackground(list, true, true);
        ScheduleRevNum newSrn = srnManager.getSrnById(srnId);
        assertEquals(newSrn.getSrn(), srnVal+1);
        AgentScheduleSyncZevent zevent = capture.getValue();
        assertNotNull(zevent);
        scheduleRevNumDAO.flushSession();

        // test if scheduleInBackground obeys the restrictions not to schedule something that was just scheduled
        controller.reset();
        capture.reset();
        srn = srnManager.get(platformAeid);
        srnVal = srn.getSrn();
        controller.replay();
        srnManager.scheduleInBackground(list, true, false);
        controller.verify();
        scheduleRevNumDAO.flushSession();
        newSrn = srnManager.getSrnById(srnId);
        // nothing should have changed since the srn was just scheduled
        assertEquals(srnVal, newSrn.getSrn());
        // there shouldn't be any schedule since the resource was just scheduled right before this call
        assertFalse(capture.hasCaptured());

        // test if scheduleInBackground properly creates an srn when it does *not* exist with overrideRestrictions = false
        long start = now();
        controller.reset();
        capture.reset();
        scheduleRevNumDAO.remove(srn);
        scheduleRevNumDAO.flushSession();
        srn = srnManager.get(platformAeid);
        assertNull(srn);
        zeventManagerMock.enqueueEventAfterCommit(and(capture(capture), isA(AgentScheduleSyncZevent.class)));
        expectLastCall().times(1);
        controller.replay();
        srnManager.scheduleInBackground(list, true, false);
        controller.verify();
        scheduleRevNumDAO.flushSession();
        newSrn = srnManager.get(platformAeid);
        assertNotNull(newSrn);
        assertTrue(newSrn.getSrn() > 0);
        assertTrue(newSrn.getLastSchedule() >= start);
        assertTrue(capture.hasCaptured());
        
        controller.reset();
        // test if unschedule works properly
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        List<Measurement> measurements = measurementManager.findMeasurements(overlord, platformAeid, null, PageControl.PAGE_ALL);
        // remove all measurements and check to see if the platform is unscheduled
        for (Measurement m : measurements) {
            session.delete(m);
        }
        scheduleRevNumDAO.flushSession();
        zeventManagerMock = controller.createMock(ZeventManager.class);
        srnManager.setZeventManager(zeventManagerMock);
        Capture<AgentUnscheduleZevent> unschedCapture = new Capture<AgentUnscheduleZevent>();
        zeventManagerMock.enqueueEventAfterCommit(and(capture(unschedCapture), isA(AgentUnscheduleZevent.class)));
        expectLastCall().times(1);
        controller.replay();
        srnManager.scheduleInBackground(list, true, true);
        controller.verify();
        assertTrue(unschedCapture.hasCaptured());
        AgentUnscheduleZevent unschedZevent = unschedCapture.getValue();
        assertNotNull(unschedZevent);
    }
    
    private long now() {
        return System.currentTimeMillis();
    }

    @Test
    @DirtiesContext
    @SuppressWarnings("unchecked")
    public void testSchedule() {
        Resource platformRes = resourceManager.findResourceById(PLATFORM_ID);
        AppdefEntityID platformAeid = AppdefUtil.newAppdefEntityId(platformRes);
        Collection<AppdefEntityID> list = Collections.singletonList(platformAeid);
        IMocksControl controller = createStrictControl();
        Capture<Collection<AppdefEntityID>> capture = new Capture<Collection<AppdefEntityID>>();
        MeasurementProcessor measurementProcessorMock = controller.createMock(MeasurementProcessor.class);
        srnManager.setMeasurementProcessor(measurementProcessorMock);
        measurementProcessorMock.scheduleSynchronous(and(capture(capture), isA(Collection.class)));
        expectLastCall().times(1);
        controller.replay();
        srnManager.schedule(list, false, true);
        Collection<AppdefEntityID> aeids = capture.getValue();
        assertNotNull(aeids);
        assertTrue(!aeids.isEmpty());
    }

    @Test
    @Transactional
    public void testIncrementSrn() {
        Resource platformRes = resourceManager.findResourceById(PLATFORM_ID);
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(platformRes);
        
        // test that the increments creates an srn if it was deleted or the resource is new
        ScheduleRevNum srn = srnManager.get(aeid);
        if (srn != null) {
            srnManager.removeSrn(aeid);
            scheduleRevNumDAO.flushSession();
        }
        int srnValue = srnManager.incrementSrn(aeid);
        assertEquals(1, srnValue);
        scheduleRevNumDAO.flushSession();
        ScheduleRevNum newsrn = srnManager.get(aeid);

        srn = srnManager.get(aeid);
        int oldValue = srn.getSrn();
        int newSrnValue = srnManager.incrementSrn(aeid);
        assertEquals(oldValue+1, newSrnValue);
        scheduleRevNumDAO.flushSession();
        newsrn = srnManager.get(aeid);
        assertNotSame(newsrn.getLastSchedule(), srn.getLastSchedule());
    }
    
    @Test
    @DirtiesContext
    @Transactional
    public void testRescheduleOutOfSyncSrns() {
        Resource platformRes = resourceManager.findResourceById(PLATFORM_ID);
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(platformRes);
        ScheduleRevNum dbsrn = srnManager.get(aeid);
        if (dbsrn == null) {
            dbsrn = scheduleRevNumDAO.create(aeid);
        }
        SRN srn = new SRN(aeid, dbsrn.getSrn()-1);
        List<SRN> srns = Collections.singletonList(srn);
        
        IMocksControl controller = createStrictControl();
        ZeventManager zeventManagerMock = controller.createMock(ZeventManager.class);
        srnManager.setZeventManager(zeventManagerMock);
        Capture<AgentScheduleSyncZevent> capture = new Capture<AgentScheduleSyncZevent>();
        zeventManagerMock.enqueueEventAfterCommit(and(capture(capture), isA(AgentScheduleSyncZevent.class)));
        expectLastCall().times(1);

        controller.replay();
        srnManager.rescheduleOutOfSyncSrns(srns, true);
        controller.verify();
        assertTrue(capture.hasCaptured());
        AgentScheduleSyncZevent zevent = capture.getValue();
        assertNotNull(zevent);
        scheduleRevNumDAO.flushSession();

        dbsrn = srnManager.get(aeid);
        srn = new SRN(aeid, dbsrn.getSrn());
        srns = Collections.singletonList(srn);
        controller.reset();
        capture.reset();
        controller.replay();
        srnManager.rescheduleOutOfSyncSrns(srns, true);
        controller.verify();
        assertFalse(capture.hasCaptured());
        scheduleRevNumDAO.flushSession();

        controller.reset();
        capture.reset();
        controller.replay();
        srnManager.rescheduleOutOfSyncSrns(srns, false);
        controller.verify();
        assertFalse(capture.hasCaptured());
    }
    
}

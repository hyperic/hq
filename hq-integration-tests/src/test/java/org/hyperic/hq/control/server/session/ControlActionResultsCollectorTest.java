/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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
package org.hyperic.hq.control.server.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.shared.ControlActionTimeoutException;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author jhickey
 * 
 */
@DirtiesContext
public class ControlActionResultsCollectorTest
    extends BaseInfrastructureTest {

    @Autowired
    private ControlActionResultsCollector controlActionResultsCollector;

    @Autowired
    private ControlScheduleManager controlScheduleManager;

    private Server server;

    private Server server2;

    private Server server3;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        createPlatformType("TestPlatform", "test");
        Platform platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1");
        ServerType serverType = createServerType("TestServer", "6.0",
            new String[] { "TestPlatform" }, "test", false);
        server = createServer(platform, serverType, "Server1");
        server2 = createServer(platform, serverType, "Server2");
        server3 = createServer(platform, serverType, "Server3");
        flushSession();
    }

    private Integer addControlHistory(AppdefEntityID resourceId, String status, String message) {
        Integer controlHistoryId = controlScheduleManager.createHistory(resourceId, null, null,
            authzSubjectManager.getOverlordPojo().getName(), "someAction", null, false, startTime,
            startTime, startTime, status, "", message);
        flushSession();
        return controlHistoryId;
    }

    @Test(expected = ControlActionTimeoutException.class)
    public void testWaitForJobResultsTimeout() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history = addControlHistory(server.getEntityId(),
            ControlConstants.STATUS_INPROGRESS, "Some message");
        controlActionResultsCollector.waitForResult(history, 100);
    }

    @Test
    public void testWaitForJobResultsCompleted() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history = addControlHistory(server.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Some message");
        ControlActionResult results = controlActionResultsCollector.waitForResult(history, 5000);
        assertEquals(server.getEntityId(), results.getResource());
        assertEquals("Some message", results.getMessage());
        assertEquals(ControlConstants.STATUS_COMPLETED, results.getStatus());
    }

    @Test
    public void testWaitForJobResultsFailed() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history = addControlHistory(server.getEntityId(), ControlConstants.STATUS_FAILED,
            "Some message");
        ControlActionResult results = controlActionResultsCollector.waitForResult(history, 5000);
        assertEquals(server.getEntityId(), results.getResource());
        assertEquals("Some message", results.getMessage());
        assertEquals(ControlConstants.STATUS_FAILED, results.getStatus());
    }

    @Test
    public void testWaitForAllJobsSomeFail() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history1 = addControlHistory(server.getEntityId(), ControlConstants.STATUS_FAILED,
            "Some message");
        Integer history2 = addControlHistory(server2.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Some other message");
        Integer history3 = addControlHistory(server3.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Another message");

        List<Integer> jobIds = new ArrayList<Integer>(3);
        jobIds.add(history1);
        jobIds.add(history2);
        jobIds.add(history3);

        GroupControlActionResult groupResult = controlActionResultsCollector.waitForGroupResults(
            AppdefEntityID.newGroupID(789), jobIds, 5000);
        assertEquals(ControlConstants.STATUS_FAILED, groupResult.getStatus());
        assertEquals("Some message", groupResult.getMessage());
        Set<ControlActionResult> results = groupResult.getIndividualResults();
        assertEquals(3, results.size());
        for (ControlActionResult result : results) {
            if (server.getEntityId().equals(result.getResource())) {
                assertEquals("Some message", result.getMessage());
                assertEquals(ControlConstants.STATUS_FAILED, result.getStatus());
            } else if (server2.getEntityId().equals(result.getResource())) {
                assertEquals("Some other message", result.getMessage());
                assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
            } else if (server3.getEntityId().equals(result.getResource())) {
                assertEquals("Another message", result.getMessage());
                assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
            } else {
                fail("Unexpected resource ID in ControlActionResult: " + result.getResource());
            }
        }
    }

    @Test
    public void testWaitForAllJobsCompleted() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history1 = addControlHistory(server.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Some message");
        Integer history2 = addControlHistory(server2.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Some other message");
        Integer history3 = addControlHistory(server3.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Another message");

        List<Integer> jobIds = new ArrayList<Integer>(3);
        jobIds.add(history1);
        jobIds.add(history2);
        jobIds.add(history3);

        GroupControlActionResult groupResult = controlActionResultsCollector.waitForGroupResults(
            AppdefEntityID.newGroupID(789), jobIds, 5000);
        assertEquals(ControlConstants.STATUS_COMPLETED, groupResult.getStatus());
        assertNull(groupResult.getMessage());
        Set<ControlActionResult> results = groupResult.getIndividualResults();
        assertEquals(3, results.size());
        for (ControlActionResult result : results) {
            if (server.getEntityId().equals(result.getResource())) {
                assertEquals("Some message", result.getMessage());
                assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
            } else if (server2.getEntityId().equals(result.getResource())) {
                assertEquals("Some other message", result.getMessage());
                assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
            } else if (server3.getEntityId().equals(result.getResource())) {
                assertEquals("Another message", result.getMessage());
                assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
            } else {
                fail("Unexpected resource ID in ControlActionResult: " + result.getResource());
            }
        }
    }

    @Test(expected = ControlActionTimeoutException.class)
    public void testWaitForAllJobsSomeTimeout() throws ControlActionTimeoutException,
        ApplicationException {
        Integer history1 = addControlHistory(server.getEntityId(), ControlConstants.STATUS_FAILED,
            "Some message");
        Integer history2 = addControlHistory(server2.getEntityId(),
            ControlConstants.STATUS_COMPLETED, "Some other message");
        Integer history3 = addControlHistory(server3.getEntityId(),
            ControlConstants.STATUS_INPROGRESS, "Another message");

        List<Integer> jobIds = new ArrayList<Integer>(3);
        jobIds.add(history1);
        jobIds.add(history2);
        jobIds.add(history3);
        controlActionResultsCollector.waitForGroupResults(AppdefEntityID.newGroupID(789), jobIds,
            100);
    }

}

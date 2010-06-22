package org.hyperic.hq.control.server.session;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertEquals;
/**
 * Integration test of {@link ControlScheduleManagerImpl}
 * 
 * @author jhickey
 *
 */
@DirtiesContext
public class ControlScheduleManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private ControlScheduleManager controlScheduleManager;

    private Server server;

    private Server server2;

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
        flushSession();
    }

    private void addControlHistory() {
        controlScheduleManager.createHistory(server.getEntityId(), null, null, authzSubjectManager
            .getOverlordPojo().getName(), "someAction", null, false, startTime, startTime,
            startTime, ControlConstants.STATUS_INPROGRESS, "", null);
        controlScheduleManager.createHistory(server.getEntityId(), null, null, authzSubjectManager
            .getOverlordPojo().getName(), "someAction", null, false, startTime + 1000,
            startTime + 1000, startTime + 1000, ControlConstants.STATUS_INPROGRESS, "", null);
        controlScheduleManager.createHistory(server2.getEntityId(), null, null, authzSubjectManager
            .getOverlordPojo().getName(), "someOtherAction", null, false, startTime + 1000,
            startTime + 1000, startTime + 1000, ControlConstants.STATUS_INPROGRESS, "", null);
        flushSession();
    }

    @Test
    public void testGetOnDemandControlFrequencyLessThanMax() throws Exception {
        addControlHistory();
        PageList<ControlFrequencyValue> frequencies = controlScheduleManager
            .getOnDemandControlFrequency(authzSubjectManager.getOverlordPojo(), 4);

        assertEquals(2, frequencies.size());
        ControlFrequencyValue frequency = frequencies.get(0);
        assertEquals("someAction", frequency.getAction());
        assertEquals(server.getEntityId().getId().intValue(), frequency.getId());
        assertEquals(2, frequency.getNum());
        assertEquals(server.getResource().getName(), frequency.getName());
        assertEquals(server.getEntityId().getType(), frequency.getType());

        ControlFrequencyValue frequency2 = frequencies.get(1);
        assertEquals("someOtherAction", frequency2.getAction());
        assertEquals(server2.getEntityId().getId().intValue(), frequency2.getId());
        assertEquals(1, frequency2.getNum());
        assertEquals(server2.getResource().getName(), frequency2.getName());
        assertEquals(server2.getEntityId().getType(), frequency2.getType());
    }

    @Test
    public void testGetOnDemandControlFrequencyMoreThanMax() throws Exception {
        addControlHistory();
        PageList<ControlFrequencyValue> frequencies = controlScheduleManager
            .getOnDemandControlFrequency(authzSubjectManager.getOverlordPojo(), 1);

        assertEquals(1, frequencies.size());
        ControlFrequencyValue frequency = frequencies.get(0);
        assertEquals("someAction", frequency.getAction());
        assertEquals(server.getEntityId().getId().intValue(), frequency.getId());
        assertEquals(2, frequency.getNum());
        assertEquals(server.getResource().getName(), frequency.getName());
        assertEquals(server.getEntityId().getType(), frequency.getType());
    }

    @Test
    public void testGetOnDemandControlFrequencyNone() throws Exception {
        PageList<ControlFrequencyValue> frequencies = controlScheduleManager
            .getOnDemandControlFrequency(authzSubjectManager.getOverlordPojo(), 1);

        assertEquals(0, frequencies.size());
    }

}

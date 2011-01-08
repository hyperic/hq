package org.hyperic.hq.events.server.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class EventLogManagerTest
    extends BaseInfrastructureTest {

    private Platform platform;

    @Autowired
    private EventLogManager eventLogManager;

    private class TestEvent
        extends AbstractEvent implements ResourceEventInterface {

        private Platform resource;

        public TestEvent(Platform resource) {
            this.resource = resource;
        }

        public AppdefEntityID getResource() {
            return resource.getEntityId();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        createPlatformType("Linux");
        platform = createPlatform(agentToken, "Linux", "Test Platform Linux",
            "Test Platform Linux", 2);
        flushSession();
        eventLogManager.createLog(new TestEvent(platform), "Something", "Some Status", true);
        flushSession();
        clearSession();
    }

    @Test
    public void testLogsExistPerInterval() throws InterruptedException {
        // 1 second per interval, So pause 2 seconds
        Thread.sleep(2000);
        boolean[] exists = eventLogManager.logsExistPerInterval(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo(), System.currentTimeMillis() - 60000,
            System.currentTimeMillis(), 60);

        for (int i = 0; i < 57; i++) {
            assertFalse(exists[i]);
        }
        // Event happened at least 2 seconds ago - test around that range to be
        // sure
        assertTrue(exists[57] || exists[56] || exists[55]);
    }
}

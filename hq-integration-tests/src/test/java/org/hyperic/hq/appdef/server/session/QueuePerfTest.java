package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
public class QueuePerfTest {
    @Autowired
    private AgentManager agentManager;
    private PlatformManager platformManager;
    @Autowired
    private AIQueueManager aiQueueManager;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    //@Before
    public void setUp() throws Exception {
        agentManager.createLegacyAgent("127.0.0.1", 2144, "hqadmin", "agentToken", "4.5");
        // PlatformType platformType =
        // platformManager.createPlatformType("JenOS", "testit");

        for (int i = 1; i <= 1342; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setFqdn("Platform" + i);
            aiPlatform.setAgentToken("agentToken");
            AIServerValue aiServer = new AIServerValue();
            aiServer.setName("Server1");
            aiServer.setAutoinventoryIdentifier("Platform" + i + ".Server1");
            aiServer.setServerTypeName("QueueTest");
            aiPlatform.addAIServerValue(aiServer);
            AIServerValue aiServer2 = new AIServerValue();
            aiServer2.setName("Server2");
            aiServer2.setAutoinventoryIdentifier("Platform" + i + ".Server2");
            aiServer2.setServerTypeName("QueueTest");
            aiPlatform.addAIServerValue(aiServer2);

            aiQueueManager.queue(authzSubjectManager.getOverlordPojo(), aiPlatform, false, false,
                false);
        }

    }

    @Test
    public void testGetQueue() {
        PageControl page = new PageControl();
        page.setPagesize(5);
        StopWatch watch = new StopWatch();
        watch.start("Get Queue");
        aiQueueManager.getQueue(authzSubjectManager.getOverlordPojo(), true, false, true, page);
        watch.stop();
        System.out.println("GetQueue took " + watch.getLastTaskTimeMillis() + " ms");
    }

}

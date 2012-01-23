package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/**
 * Integration test of {@link AIQueueManagerImpl} getQueue method.
 * testGetQueueWithChangesMatchingFQDN can be made into a performance test by
 * removing the @Transactional and increasing the static "NUM" constants
 * @author administrator
 * 
 */
@DirtiesContext
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
@Transactional
public class AIQueueRetrievalTest {
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ServerManager serverManager;

    @Autowired
    private PlatformManager platformManager;
    @Autowired
    private AIQueueManager aiQueueManager;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    private PlatformDAO platformDAO;

    // =902 in perf test env
    private static final int NUM_AGENTS = 4;

    // =450 in perf test env
    private static final int NUM_AGENT_CHANGES = 2;

    // =5 in perf test env
    private static final int NUM_SERVERS_PER_AGENT = 2;

    private static final String IP_ADDRESS = "10.0.0.186";

    private PlatformType platformType;

    @Before
    public void setUp() throws Exception {
        for (int i = 1; i <= NUM_AGENTS; i++) {
            agentManager.createLegacyAgent("127.0.0.1", 2144, "hqadmin", "agentToken" + i, "4.5");
        }
        platformType = platformManager.createPlatformType("JenOS", "testit");
        ServerType serverType = createServerType("JenServer", "6.0", new String[] { "JenOS" },
            "testit", false);

        // create 2 IPs for each platform (localhost and same IP to emulate
        // multi-agent) and NUM_SERVERS servers each
        for (int i = 1; i <= NUM_AGENTS; i++) {
            Platform platform = createPlatform(platformType, "Platform" + i, "Platform" + i,
                IP_ADDRESS, agentManager.getAgent("agentToken" + i));
            for (int j = 1; j <= NUM_SERVERS_PER_AGENT; j++) {
                createServer(platform, serverType, "Server" + j, "Platform" + i + ".Server" + j,
                    "/foo/some" + j);
            }
        }
        platformDAO.getSession().flush();
    }

    private void setUpChangeTestEnv() throws Exception {
        // queue up an AI change - one server changes
        for (int i = 1; i <= NUM_AGENT_CHANGES; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setFqdn("Platform" + i);
            aiPlatform.setAgentToken("agentToken" + i);
            aiPlatform.setPlatformTypeName("JenOS");
            AIIpValue ip = new AIIpValue();
            ip.setAddress("127.0.0.1");
            ip.setNetmask("255.0.0.0");
            ip.setMACAddress("00:00:00:00:00:00");
            aiPlatform.addAIIpValue(ip);

            AIIpValue ip2 = new AIIpValue();
            ip2.setAddress(IP_ADDRESS);
            ip2.setNetmask("255.0.0.0");
            ip2.setMACAddress("00:00:00:00:00:00");
            aiPlatform.addAIIpValue(ip2);

            aiPlatform.setQueueStatus(2);

            // add existing servers unchanged
            for (int j = 1; j <= (NUM_SERVERS_PER_AGENT - 1); j++) {
                AIServerValue aiServer = new AIServerValue();
                aiServer.setName("Server" + j);
                aiServer.setAutoinventoryIdentifier("Platform" + i + ".Server" + j);
                aiServer.setServerTypeName("JenServer");
                aiServer.setInstallPath("/foo/some" + j);
                aiPlatform.addAIServerValue(aiServer);
            }

            // change installpath of one server
            AIServerValue aiServer = new AIServerValue();
            aiServer.setName("Server" + NUM_SERVERS_PER_AGENT);
            aiServer.setAutoinventoryIdentifier("Platform" + i + ".Server" + NUM_SERVERS_PER_AGENT);
            aiServer.setServerTypeName("JenServer");
            aiServer.setInstallPath("/foo/somethingelse");
            aiPlatform.addAIServerValue(aiServer);

            aiQueueManager.queue(authzSubjectManager.getOverlordPojo(), aiPlatform, true, false,
                false);
        }

    }

    @Test
    public void testGetQueueWithChangesMatchingFQDN() throws Exception {
        setUpChangeTestEnv();
        platformDAO.getSession().flush();
        PageControl page = new PageControl();
        page.setPagesize(5);
        StopWatch watch = new StopWatch();
        watch.start("Get Queue");
        PageList<AIPlatformValue> queue = aiQueueManager.getQueue(
            authzSubjectManager.getOverlordPojo(), true, false, true, page);
        watch.stop();
        System.out.println("GetQueue took " + watch.getLastTaskTimeMillis() + " ms");
        assertEquals(2, queue.size());
        for (int i = 1; i <= 2; i++) {
            assertEquals("Platform" + i, queue.get(i - 1).getName());
        }
    }

    @Test
    public void testGetQueueMultipleAgentsSameIpNoFqdn() {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn("PlatformFoo");
        aiPlatform.setAgentToken("agentToken1");
        aiPlatform.setPlatformTypeName("JenOS");
        AIIpValue ip = new AIIpValue();
        ip.setAddress("127.0.0.1");
        ip.setNetmask("255.0.0.0");
        ip.setMACAddress("00:00:00:00:00:00");
        aiPlatform.addAIIpValue(ip);

        AIIpValue ip2 = new AIIpValue();
        ip2.setAddress(IP_ADDRESS);
        ip2.setNetmask("255.0.0.0");
        ip2.setMACAddress("00:00:00:00:00:00");
        aiPlatform.addAIIpValue(ip2);

        aiPlatform.setQueueStatus(2);

        aiQueueManager.queue(authzSubjectManager.getOverlordPojo(), aiPlatform, true, false, false);
        platformDAO.getSession().flush();
        PageControl page = new PageControl();
        page.setPagesize(5);
        PageList<AIPlatformValue> queue = aiQueueManager.getQueue(
            authzSubjectManager.getOverlordPojo(), true, false, true, page);
        // find by agent is not going to work in this test, so we end up
        // w/nothing in queue
        assertEquals(0, queue.size());
    }

    @Test
    public void testGetQueueMatchingIpNoFqdn() throws Exception {
        agentManager.createLegacyAgent("127.0.0.1", 2144, "hqadmin", "theToken", "4.5");
        createPlatform(platformType, "PlatformFoo", "PlatformFoo", "10.0.0.175",
            agentManager.getAgent("theToken"));
        platformDAO.getSession().flush();

        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn("PlatformFooBar");
        aiPlatform.setAgentToken("theToken");
        aiPlatform.setPlatformTypeName("JenOS");
        AIIpValue ip = new AIIpValue();
        ip.setAddress("127.0.0.1");
        ip.setNetmask("255.0.0.0");
        ip.setMACAddress("00:00:00:00:00:00");
        aiPlatform.addAIIpValue(ip);

        AIIpValue ip2 = new AIIpValue();
        ip2.setAddress("10.0.0.175");
        ip2.setNetmask("255.0.0.0");
        ip2.setMACAddress("00:00:00:00:00:00");
        aiPlatform.addAIIpValue(ip2);

        aiPlatform.setQueueStatus(2);

        aiQueueManager.queue(authzSubjectManager.getOverlordPojo(), aiPlatform, true, false, false);
        platformDAO.getSession().flush();
        PageControl page = new PageControl();
        page.setPagesize(5);
        PageList<AIPlatformValue> queue = aiQueueManager.getQueue(
            authzSubjectManager.getOverlordPojo(), true, false, true, page);
        assertEquals(1, queue.size());
        assertEquals("PlatformFoo", queue.get(0).getName());
    }

    private Platform createPlatform(PlatformType platformType, String fqdn, String name,
                                    String remoteIp, Agent agent) throws ApplicationException {

        PlatformValue platform = new PlatformValue();
        platform.setCpuCount(2);
        platform.setPlatformType(platformType.getPlatformTypeValue());
        platform.setAgent(agent);
        platform.setFqdn(fqdn);
        platform.setName(name);

        Platform newPlatform = platformManager.createPlatform(
            authzSubjectManager.getOverlordPojo(), platformType.getId(), platform, agent.getId());

        // always add loopback
        Ip ip1 = platformManager.addIp(newPlatform, "127.0.0.1", "255.0.0.0", "00:00:00:00:00:00");
        Ip ip2 = platformManager.addIp(newPlatform, remoteIp, "255.0.0.0", "00:00:00:00:00:00");

        // Not sure why addIp doesn't flush the IP to DB...
        platformDAO.getSession().save(ip1);
        platformDAO.getSession().save(ip2);
        return newPlatform;
    }

    private ServerType createServerType(String serverTypeName, String serverVersion,
                                        String[] validPlatformTypes, String plugin, boolean virtual)
        throws NotFoundException {
        ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
        serverTypeInfo.setDescription(serverTypeName);
        serverTypeInfo.setName(serverTypeName);
        serverTypeInfo.setVersion(serverVersion);
        serverTypeInfo.setVirtual(virtual);
        serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
        return serverManager.createServerType(serverTypeInfo, plugin);
    }

    private Server createServer(Platform platform, ServerType serverType, String name,
                                String autoInvId, String installpath)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        ServerValue server = new ServerValue();
        server.setName(name);
        server.setAutoinventoryIdentifier(autoInvId);
        server.setInstallPath(installpath);
        return serverManager.createServer(authzSubjectManager.getOverlordPojo(), platform.getId(),
            serverType.getId(), server);
    }

}

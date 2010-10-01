package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.IpValue;
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
import org.hyperic.hq.product.ServerTypeInfo;
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
    @Autowired
    private ServerManager serverManager;

    @Autowired
    private PlatformManager platformManager;
    @Autowired
    private AIQueueManager aiQueueManager;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    //@Before
    public void setUp() throws Exception {

        for (int i = 1; i <= 902; i++) {
            agentManager.createLegacyAgent("127.0.0.1", 2144, "hqadmin", "agentToken" + i, "4.5");
        }
        PlatformType platformType = platformManager.createPlatformType("JenOS", "testit");
        ServerType serverType = createServerType("JenServer", "6.0", new String[] { "JenOS" },
            "testit", false);

        // create 902 platforms with 2 IPs and 5 servers each
        //int ipNum = 100;
        String ipNum="10.0.0.186";
        for (int i = 1; i <= 902; i++) {
            Platform platform = createPlatform(platformType, "Platform" + i, "Platform" + i,
                ipNum, agentManager.getAgent("agentToken" + i));
            for (int j = 1; j <= 5; j++) {
                createServer(platform, serverType, "Server" + j, "Platform" + i + ".Server" + j,
                    "/foo/some" + j);
            }
        }

        //ipNum = 100;
        // queue up an AI change - one server changes and 4 new added
        for (int i = 1; i <= 450; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setFqdn("Platform" + i);
            aiPlatform.setAgentToken("agentToken" + i);
            AIIpValue ip = new AIIpValue();
            ip.setAddress("127.0.0.1");
            ip.setNetmask("255.0.0.0");
            ip.setMACAddress("00:00:00:00:00:00");
            aiPlatform.addAIIpValue(ip);

            AIIpValue ip2 = new AIIpValue();
            ip2.setAddress(ipNum);
            ip2.setNetmask("255.0.0.0");
            ip2.setMACAddress("00:00:00:00:00:00");
            aiPlatform.addAIIpValue(ip2);

            aiPlatform.setQueueStatus(2);

            // add 4 existing servers unchanged
            for (int j = 1; j <= 4; j++) {
                AIServerValue aiServer = new AIServerValue();
                aiServer.setName("Server" + j);
                aiServer.setAutoinventoryIdentifier("Platform" + i + ".Server" + j);
                aiServer.setServerTypeName("JenServer");
                aiServer.setInstallPath("/foo/some" + j);
                aiPlatform.addAIServerValue(aiServer);
            }

            // change installpath of 5th server
            AIServerValue aiServer = new AIServerValue();
            aiServer.setName("Server5");
            aiServer.setAutoinventoryIdentifier("Platform" + i + ".Server5");
            aiServer.setServerTypeName("JenServer");
            aiServer.setInstallPath("/foo/somethingelse");
            aiPlatform.addAIServerValue(aiServer);

            aiQueueManager.queue(authzSubjectManager.getOverlordPojo(), aiPlatform, true, false,
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
        platformManager.addIp(newPlatform, "127.0.0.1", "255.0.0.0", "00:00:00:00:00:00");
        platformManager.addIp(newPlatform, remoteIp, "255.0.0.0", "00:00:00:00:00:00");

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

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
package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.classextension.EasyMock;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of {@link AIQueueManagerImpl}
 * @author jhickey
 * 
 */
@DirtiesContext
public class AIQueueManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AIQueueManagerImpl aiQueueManager;
    private AIPlatformValue queuedAIPlatform;
    private Platform platform;
    private AgentCommandsClientFactory agentCommandsClientFactory;
    private AgentCommandsClient agentCommandsClient;

    @Before
    public void setUp() throws Exception {
        this.agentCommandsClientFactory = EasyMock.createMock(AgentCommandsClientFactory.class);
        this.agentCommandsClient = EasyMock.createMock(AgentCommandsClient.class);
        aiQueueManager.setAgentCommandsClientFactory(agentCommandsClientFactory);
        createAgent("127.0.0.1", 2144, "hqadmin", "agentToken", "4.5");
        PlatformType platformType = createPlatformType("JenOS");
        ResourceType platformResType = resourceManager.findResourceTypeById(platformType.getId());
        platformResType.addPropertyType(new PropertyType("numCpus", "Number of CPUs"));

        // Add the platform to AI Queue with custom prop value of 4 CPUs
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn("Platform1");
        aiPlatform.setAgentToken("agentToken");
        ConfigResponse platformConfig = new ConfigResponse();
        platformConfig.setValue("numCpus", 4);
        aiPlatform.setCustomProperties(platformConfig.encode());
        this.queuedAIPlatform = aiQueueManager.queue(authzSubjectManager.getOverlordPojo(),
            aiPlatform, false, false, false);
        flush();
        // Add the platform to inventory with custom prop value of 2 CPUs
        this.platform = createPlatform("agentToken", "JenOS", "Platform1", "Platform1", 5);
        platform.getResource().setProperty("numCpus", "2");
    }

    /**
     * Verifies HHQ-4185 behavior - testing "side effect" setting of custom
     * properties when the findAIPlatformById method is called. TODO get rid of
     * this behavior and make find read-only
     * @throws EncodingException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     */
    @Test
    public void testFindAIPlatformByIdUpdatesCProps() throws EncodingException,
        AppdefEntityNotFoundException, PermissionException {
        // Do a find, which for some reason triggers updating the actual
        // platform's cprop value with the one from the AI Queue
        aiQueueManager.findAIPlatformById(authzSubjectManager.getOverlordPojo(),
            queuedAIPlatform.getId());
        assertEquals("4", platform.getResource().getProperty("numCpus"));
    }

    /**
     * Verifies HHQ-4185 behavior - testing "side effect" setting of custom
     * properties when the findAIPlatformByFqdn method is called. TODO get rid
     * of this behavior and make find read-only
     * @throws EncodingException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     */
    @Test
    public void testFindAIPlatformByFqdnUpdatesCProps() throws AppdefEntityNotFoundException,
        PermissionException {
        // Do a find, which for some reason triggers updating the actual
        // platform's cprop value with the one from the AI Queue
        aiQueueManager.findAIPlatformByFqdn(authzSubjectManager.getOverlordPojo(),
            queuedAIPlatform.getFqdn());
        assertEquals("4", platform.getResource().getProperty("numCpus"));
    }

    @Test
    public void testProcessQueueAddPlatformsAndServers() throws EncodingException,
        PermissionException, ValidationException, AIQApprovalException, NotFoundException,
        AgentConnectionException, AgentRemoteException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn("Platform2");
        aiPlatform.setAgentToken("agentToken");
        aiPlatform.setCpuCount(4);
        aiPlatform.setPlatformTypeName("JenOS");
        aiPlatform.setCertdn("Certdn");
        ConfigResponse platformConfig = new ConfigResponse();
        platformConfig.setValue("numCpus", 4);
        aiPlatform.setCustomProperties(platformConfig.encode());

        ServerType serverType = createServerType("My Type", "1.0", new String[] { "JenOS" });
        ResourceType srvrResType = resourceManager.findResourceTypeById(serverType.getId());
        srvrResType.addPropertyType(new PropertyType("Max Heap","Max heap size"));
        AIServerValue aiServer = new AIServerValue();
        aiServer.setAutoinventoryIdentifier("Server1");
        aiServer.setName("Server1");
        aiServer.setServerTypeName("My Type");
        aiServer.setInstallPath("/some/place");
        ConfigResponse serverConfig = new ConfigResponse();
        serverConfig.setValue("Max Heap", 256);
        aiServer.setCustomProperties(serverConfig.encode());

        aiPlatform.addAIServerValue(aiServer);
       
        AIPlatformValue queuedAIPlatform2 = aiQueueManager.queue(
            authzSubjectManager.getOverlordPojo(), aiPlatform, false, false, false);
        flush();
        List<Integer> aiPlatforms = Arrays.asList(new Integer[] { queuedAIPlatform2.getId() });
        AIServerValue queuedAIServerValue = (AIServerValue) queuedAIPlatform2
            .getAddedAIServerValues().iterator().next();
        List<Integer> aiServers = Arrays.asList(new Integer[] { queuedAIServerValue.getId() });
        EasyMock.expect(agentCommandsClientFactory.getClient(EasyMock.isA(Agent.class))).andReturn(
            agentCommandsClient);
        EasyMock.expect(agentCommandsClient.ping()).andReturn(0l);
        replay();
        aiQueueManager.processQueue(authzSubjectManager.getOverlordPojo(), aiPlatforms, aiServers,
            null, AIQueueConstants.Q_DECISION_APPROVE);
        verify();
        //Verify that the custom props were applied to the newly created Platform and Server after AI Queue Approval
        Resource createdPlatform = resourceManager.findResourceByName("Platform2");
        Map<String,Object> expectedPlatformProps = new HashMap<String,Object>();
        expectedPlatformProps.put("numCpus","4");
        assertEquals(expectedPlatformProps,createdPlatform.getProperties(false));
        
        Resource createdServer = resourceManager.findResourceByName("Server1");
        Map<String,Object> expectedServerProps = new HashMap<String,Object>();
        expectedServerProps.put("Max Heap","256");
        assertEquals(expectedServerProps,createdServer.getProperties(false));
    }

    private void replay() {
        EasyMock.replay(agentCommandsClient, agentCommandsClientFactory);
    }

    private void verify() {
        EasyMock.verify(agentCommandsClient, agentCommandsClientFactory);
    }
}

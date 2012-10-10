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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.easymock.classextension.EasyMock;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.agent.client.ControlCommandsClientFactory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of the {@link ControlActionExecutor}
 * @author jhickey
 * 
 */
@DirtiesContext
public class GroupControlActionExecutorTest
    extends BaseInfrastructureTest {

    @Autowired
    private GroupControlActionExecutor groupControlActionExecutor;

    @Autowired
    private ControlActionExecutorImpl controlActionExecutor;

    private ControlCommandsClientFactory clientFactory;

    private ControlPluginManager controlPluginManager;

    @Autowired
    private ProductPluginDeployer productPluginDeployer;

    @Autowired
    private ConfigManager configManager;

    private ResourceGroup resourceGroup;

    private AppdefEntityID groupId;

    @Autowired
    private ControlScheduleManager controlScheduleManager;

    @Autowired
    private ControlManager controlManager;

    private Server server;

    private Server server2;

    private ServerType serverType;

    public class MockControlCommandsClient implements ControlCommandsClient {
        private String message;
        private int status;

        public MockControlCommandsClient(int status, String message) {
            this.message = message;
            this.status = status;
        }

        public void controlPluginAdd(String pluginName, String pluginType, ConfigResponse response)
            throws AgentRemoteException, AgentConnectionException {
        }

        public void controlPluginCommand(String pluginName, String pluginType, Integer id,
                                         String action, String args) throws AgentRemoteException,
            AgentConnectionException {
            try {
                ControlPlugin controlPlugin = (ControlPlugin) controlPluginManager
                    .getPlugin("PluginTestServer 1.0");
                controlPlugin.doAction(action);
                // Update the results
                controlManager.sendCommandResult(id, status, startTime, endTime, message);
            } catch (PluginNotFoundException e) {
                throw new AgentRemoteException("Plugin not found");
            } catch (PluginException e) {
                throw new AgentRemoteException("PluginException", e);
            }
        }

        public void controlPluginRemove(String pluginName) throws AgentRemoteException,
            AgentConnectionException {
        }

    }

    @Before
    public void setUp() throws Exception {
        String agentToken = "agetToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        // The test-plugin.jar defines a "TestPlatform" and a "TestServer" type.
        // This plugin should be deployed automatically by the
        // ProductPluginDeployer during
        // any integration test

        Platform platform = createPlatform(agentToken, "PluginTestPlatform", "Platform1",
            "Platform1");
        serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        server = createServer(platform, serverType, "Server1");
        server2 = createServer(platform, serverType, "Server2");
        configManager.configureResponse(authzSubjectManager.getOverlordPojo(), server
            .getConfigResponse(), server.getEntityId(), new ConfigResponse().encode(),
            new ConfigResponse().encode(), new ConfigResponse().encode(), null, null, false);

        Set<Server> servers = new HashSet<Server>(2);
        servers.add(server);
        servers.add(server2);
        resourceGroup = createServerResourceGroup(servers, "My Group");
        this.groupId = AppdefEntityID.newGroupID(resourceGroup.getId());
        clientFactory = EasyMock.createMock(ControlCommandsClientFactory.class);
        controlPluginManager = productPluginDeployer.getProductPluginManager()
            .getControlPluginManager();
        controlActionExecutor.setControlCommandsClientFactory(clientFactory);
        flushSession();
    }

    @Test
    public void testExecuteGroupControlActionNoResources() throws GroupDuplicateNameException,
        GroupCreationException, AppdefEntityNotFoundException, GroupNotCompatibleException,
        PermissionException {
//        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
//            AppdefEntityConstants.APPDEF_TYPE_SERVER, serverType.getId());
//        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo("Empty Group", "",
//            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS, resourceManager
//                .findResourcePrototype(appDefEntTypeId), "", 0, false, false);
//        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(authzSubjectManager
//            .getOverlordPojo(), gCInfo, new ArrayList<Role>(0), new ArrayList<Resource>(0));
//        AppdefEntityID groupId = AppdefEntityID.newGroupID(resGrp.getId());
//
//        GroupControlActionResult result = groupControlActionExecutor.executeGroupControlAction(
//            groupId, authzSubjectManager.getOverlordPojo().getName(), new Date(), false, "",
//            "stop", null, new int[0], ControlManagerImpl.DEFAULT_RESOURCE_TIMEOUT);
//        flushSession();
//        // validate returned DTO
//        assertEquals(groupId, result.getResource());
//        assertEquals(ControlConstants.STATUS_FAILED, result.getStatus());
//        assertEquals("Group contains no resources", result.getMessage());
//        assertTrue(result.getIndividualResults().isEmpty());
//        validateControlHistoryInDB(groupId, ControlConstants.STATUS_FAILED,
//            "Group contains no resources");
    }

    @Test
    public void testExecuteGroupControlActionUnorderedAllSuccess()
        throws AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException,
        AgentNotFoundException {
//        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(
//            new MockControlCommandsClient(0, "Everything is OK!"));
//        EasyMock.expect(clientFactory.getClient(server2.getEntityId())).andReturn(
//            new MockControlCommandsClient(0, "Everything is OK!"));
//        EasyMock.replay(clientFactory);
//
//        GroupControlActionResult result = groupControlActionExecutor.executeGroupControlAction(
//            groupId, authzSubjectManager.getOverlordPojo().getName(), new Date(), false, "",
//            "stop", null, null, ControlManagerImpl.DEFAULT_RESOURCE_TIMEOUT);
//        flushSession();
//        EasyMock.verify(clientFactory);
//        // validate returned DTO
//        assertEquals(groupId, result.getResource());
//        assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
//        assertNull(result.getMessage());
//        assertEquals(2, result.getIndividualResults().size());
//        validateControlHistoryInDB(groupId, ControlConstants.STATUS_COMPLETED, null);
    }

    @Test
    public void testExecuteGroupControlActionUnorderedSomeFailures()
        throws AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException,
        AgentNotFoundException {
//        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(
//            new MockControlCommandsClient(0, "Everything is OK!"));
//        EasyMock.expect(clientFactory.getClient(server2.getEntityId())).andReturn(
//            new MockControlCommandsClient(1, "Everything is NOT OK!"));
//        EasyMock.replay(clientFactory);
//
//        GroupControlActionResult result = groupControlActionExecutor.executeGroupControlAction(
//            groupId, authzSubjectManager.getOverlordPojo().getName(), new Date(), false, "",
//            "stop", null, null, ControlManagerImpl.DEFAULT_RESOURCE_TIMEOUT);
//        flushSession();
//        EasyMock.verify(clientFactory);
//        // validate returned DTO
//        assertEquals(groupId, result.getResource());
//        assertEquals(ControlConstants.STATUS_FAILED, result.getStatus());
//        assertEquals("Everything is NOT OK!", result.getMessage());
//        assertEquals(2, result.getIndividualResults().size());
//        validateControlHistoryInDB(groupId, ControlConstants.STATUS_FAILED, "Everything is NOT OK!");
    }

    @Test
    public void testExecuteGroupControlActionOrderedSuccess() throws AgentNotFoundException,
        AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException {
//        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(
//            new MockControlCommandsClient(0, "Everything is OK!"));
//        EasyMock.expect(clientFactory.getClient(server2.getEntityId())).andReturn(
//            new MockControlCommandsClient(0, "Everything is OK!"));
//        EasyMock.replay(clientFactory);
//
//        GroupControlActionResult result = groupControlActionExecutor.executeGroupControlAction(
//            groupId, authzSubjectManager.getOverlordPojo().getName(), new Date(), false, "",
//            "stop", null, new int[] { server2.getId(), server.getId() },
//            ControlManagerImpl.DEFAULT_RESOURCE_TIMEOUT);
//        flushSession();
//        EasyMock.verify(clientFactory);
//        // validate returned DTO
//        assertEquals(groupId, result.getResource());
//        assertEquals(ControlConstants.STATUS_COMPLETED, result.getStatus());
//        assertNull(result.getMessage());
//        assertEquals(2, result.getIndividualResults().size());
//        validateControlHistoryInDB(groupId, ControlConstants.STATUS_COMPLETED, null);
    }

    @Test
    public void testExecuteGroupControlActionOrderedSomeFailure()
        throws AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException,
        AgentNotFoundException {
//        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(
//            new MockControlCommandsClient(1, "Everything is NOT OK!"));
//        EasyMock.replay(clientFactory);
//
//        GroupControlActionResult result = groupControlActionExecutor.executeGroupControlAction(
//            groupId, authzSubjectManager.getOverlordPojo().getName(), new Date(), false, "",
//            "stop", null, new int[] { server.getId(), server2.getId() },
//            ControlManagerImpl.DEFAULT_RESOURCE_TIMEOUT);
//        flushSession();
//        EasyMock.verify(clientFactory);
//        // validate returned DTO
//        assertEquals(groupId, result.getResource());
//        assertEquals(ControlConstants.STATUS_FAILED, result.getStatus());
//        assertTrue(result.getMessage().startsWith("Job id "));
//        assertTrue(result.getMessage().endsWith("Everything is NOT OK!"));
//        // Should bail out after first failed job when running ordered
//        assertEquals(1, result.getIndividualResults().size());
//        validateControlHistoryInDB(groupId, ControlConstants.STATUS_FAILED, result.getMessage());
    }

    private void validateControlHistoryInDB(AppdefEntityID groupId, String expectedStatus,
                                            String expectedMessage)
        throws AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException {
//        PageList<ControlHistory> historyList = controlScheduleManager.findJobHistory(
//            authzSubjectManager.getOverlordPojo(), groupId, new PageControl());
//        assertEquals(1, historyList.size());
//        ControlHistory history = historyList.iterator().next();
//        assertEquals(expectedStatus, history.getStatus());
//        assertEquals(expectedMessage, history.getMessage());
    }

}

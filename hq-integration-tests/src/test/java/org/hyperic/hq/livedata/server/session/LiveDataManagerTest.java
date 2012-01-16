/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.livedata.server.session;

import org.easymock.EasyMock;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformTypeDAO;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServerTypeDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClientFactory;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.server.session.ProductPluginDeployer;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.thoughtworks.xstream.XStream;
@DirtiesContext
public class LiveDataManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private ServerTypeDAO serverTypeDAO;

    @Autowired
    private PlatformTypeDAO platformTypeDAO;

    @Autowired
    private LiveDataManagerImpl liveDataManager;

    private Server server;

    private LiveDataCommandsClientFactory clientFactory;

    private LiveDataCommandsClient commandsClient;

    private LiveDataPluginManager liveDataPluginManager;

    @Autowired
    private ProductPluginDeployer productPluginDeployer;

    public class MockLiveDataCommandsClient implements LiveDataCommandsClient {
     
        public LiveDataResult getData(AppdefEntityID id, String type, String command,
                                      ConfigResponse config) throws AgentRemoteException {
            Object result;
            try {
                LiveDataPlugin liveDataPlugin = (LiveDataPlugin) liveDataPluginManager
                    .getPlugin("PluginTestServer 1.0");
                result = liveDataPlugin.getData(command, config);
            } catch (PluginNotFoundException e) {
                throw new AgentRemoteException("Plugin not found");
            } catch (PluginException e) {
                throw new AgentRemoteException("PluginException", e);
            }
            // We are verifying that the Plugin ClassLoader was properly set by
            // serializing and deserializing a custom object returned from our
            // TestLiveDataPlugin
            XStream xstream = new XStream();
            String xml = xstream.toXML(result);
            xstream.fromXML(xml);
            return new LiveDataResult(id, xml);
        }
    }

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        // The test-plugin.jar defines a "TestPlatform" and a "TestServer" type.
        // This plugin should be deployed automatically by the
        // ProductPluginDeployer during
        // any integration test
        platformTypeDAO.findByName("PluginTestPlatform");
        Platform platform = createPlatform(agentToken, "PluginTestPlatform", "Platform1", "Platform1");
        ServerType serverType = serverTypeDAO.findByName("PluginTestServer 1.0");
        server = createServer(platform, serverType, "Server1");
        clientFactory = EasyMock.createMock(LiveDataCommandsClientFactory.class);
        commandsClient = new MockLiveDataCommandsClient();
        liveDataPluginManager = productPluginDeployer.getProductPluginManager()
            .getLiveDataPluginManager();
        liveDataManager.setLiveDataCommandsClientFactory(clientFactory);
    }

    @Test
    public void testGetData() throws Exception {
        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(commandsClient);
        EasyMock.replay(clientFactory);
        liveDataManager.getData(authzSubjectManager.getOverlordPojo(), new LiveDataCommand(server
            .getEntityId(), "someCommand", new ConfigResponse()));
        EasyMock.verify(clientFactory);
    }

    @Test
    public void testGetDataBatch() throws Exception {
        EasyMock.expect(clientFactory.getClient(server.getEntityId())).andReturn(commandsClient);
        EasyMock.replay(clientFactory);
        liveDataManager.getData(authzSubjectManager.getOverlordPojo(),
            new LiveDataCommand[] { new LiveDataCommand(server.getEntityId(), "someCommand",
                new ConfigResponse()) });
        EasyMock.verify(clientFactory);
    }
}

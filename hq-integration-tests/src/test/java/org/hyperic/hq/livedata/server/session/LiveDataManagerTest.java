package org.hyperic.hq.livedata.server.session;

import org.easymock.classextension.EasyMock;
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

import com.thoughtworks.xstream.XStream;

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
        @Override
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

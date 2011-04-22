package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;

import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class ServerManagerTest
    extends BaseInfrastructureTest {

    private String agentToken = "agentToken123";

    private Server testServer;

    private Platform testPlatform;

    private Platform testPlatform2;

    @Autowired
    private ConfigManager configManager;

    private ConfigResponse productConfig;

    private ConfigResponse measConfig;

    private ConfigResponse controlConfig;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException,
        EncodingException {
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        createPlatformType("Linux");
        testPlatform = createPlatform(agentToken, "Linux", "Platform1", "Platform1", 4);
        testPlatform2 = createPlatform(agentToken, "Linux", "Platform2", "Platform2", 4);
        // Create ServerType
        ServerType testServerType = createServerType("Test Server", "6.0", new String[] { "Linux" });
        testServer = createServer(testPlatform, testServerType, "Server1");
        productConfig = new ConfigResponse();
        productConfig.setValue("url", "what");
        measConfig = new ConfigResponse();
        measConfig.setValue("unit", "hz");
        measConfig.setValue("collectionType", "jmx");
        controlConfig = new ConfigResponse();
        controlConfig.setValue("extension", "bin");
        configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            testServer.getEntityId(), productConfig.encode(), measConfig.encode(),
            controlConfig.encode());
    }

    @Test
    public void testCloneServer() throws ValidationException, PermissionException,
        NotFoundException, VetoException, EncodingException {
        Server clone = serverManager.cloneServer(authzSubjectManager.getOverlordPojo(),
            testPlatform2, testServer);
        // Check the server name and Platform association. Everything else
        // should be covered by test
        // of createServer method
        assertEquals("Platform2 Server1", clone.getName());
        assertEquals(testPlatform2.getName(), clone.getPlatform().getName());
        assertEquals(productConfig, ConfigResponse.decode(configManager.toConfigResponse(clone.getResource()
            .getConfig(ProductPlugin.TYPE_PRODUCT))));
        assertEquals(
            measConfig,
            ConfigResponse.decode(configManager.toConfigResponse(clone.getResource().getConfig(
                ProductPlugin.TYPE_MEASUREMENT))));
        assertEquals(controlConfig, ConfigResponse.decode(configManager.toConfigResponse(clone.getResource()
            .getConfig(ProductPlugin.TYPE_CONTROL))));
    }
}

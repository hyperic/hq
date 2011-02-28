package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of {@link ConfigManagerImpl}
 * @author jhickey
 * 
 */
@DirtiesContext
public class ConfigManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private ConfigManager configManager;

    private Platform platform;

    private Server server;

    private Service service;

    @Before
    public void setUp() throws ApplicationException, NotFoundException {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        platform = createPlatform(agentToken, "PluginTestPlatform", "TestPlatform1",
            "TestPlatform1", 2);
        platformManager.addIp(platform, "10.1.4.5", "255:255:255:0", "181");
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        server = createServer(platform, serverType, "Server1");
        ServiceType serviceType = serviceManager
            .findServiceTypeByName("PluginTestServer 1.0 Web Module Stats");
        service = createService(server.getId(), serviceType, "Service1", "A svc", "Somewhere");
    }

    @Test
    public void testConfigureResponseNewConfigSomeUndefined() throws EncodingException {
        // Platform only has meas and product config
        // Add some undefined attributes to product config
        ConfigResponse platConfig = new ConfigResponse();
        platConfig.setValue("DisplayName", "fancy");
        platConfig.setValue("NewProp", "hello!");

        ConfigResponse measConfig = new ConfigResponse();
        measConfig.setValue("url", "http://something.com");
        measConfig.setValue("password", "boo");

        // Add undefined control config
        // This emulates the surprises a plugin might throw at us
        ConfigResponse controlConfig = new ConfigResponse();
        controlConfig.setValue("timeout", "30");
        assertTrue(configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), platConfig.encode(), measConfig.encode(),
            controlConfig.encode()));
        Config actualProduct = platform.getResource().getConfig(ProductPlugin.TYPE_PRODUCT);
        assertEquals(2, actualProduct.getValues().size());
        assertEquals("fancy", actualProduct.getValue("DisplayName"));
        assertEquals("hello!", actualProduct.getValue("NewProp"));

        Config actualMeas = platform.getResource().getConfig(ProductPlugin.TYPE_MEASUREMENT);
        assertEquals(2, actualMeas.getValues().size());
        assertEquals("http://something.com", actualMeas.getValue("url"));
        assertEquals("boo", actualMeas.getValue("password"));

        Config actualControl = platform.getResource().getConfig(ProductPlugin.TYPE_CONTROL);
        assertEquals(1, actualControl.getValues().size());
        assertEquals("30", actualControl.getValue("timeout"));
    }

    @Test
    public void testConfigureResponseUpdate() throws EncodingException {
        ConfigResponse platConfig = new ConfigResponse();
        platConfig.setValue("DisplayName", "fancy");
        assertTrue(configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), platConfig.encode(), ConfigResponse.EMPTY_CONFIG,
            ConfigResponse.EMPTY_CONFIG));
        ConfigResponse updatedConfig = new ConfigResponse();
        updatedConfig.setValue("DisplayName", "smooth");
        updatedConfig.setValue("NewProp", "hello!");
        assertTrue(configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), updatedConfig.encode(), ConfigResponse.EMPTY_CONFIG,
            ConfigResponse.EMPTY_CONFIG));
        Config actualProduct = platform.getResource().getConfig(ProductPlugin.TYPE_PRODUCT);
        assertEquals(2, actualProduct.getValues().size());
        assertEquals("smooth", actualProduct.getValue("DisplayName"));
        assertEquals("hello!", actualProduct.getValue("NewProp"));
    }

    @Test
    public void testConfigureResponseNoChange() throws EncodingException {
        ConfigResponse platConfig = new ConfigResponse();
        platConfig.setValue("DisplayName", "fancy");
        assertTrue(configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), platConfig.encode(), ConfigResponse.EMPTY_CONFIG,
            ConfigResponse.EMPTY_CONFIG));
        assertFalse(configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), platConfig.encode(), ConfigResponse.EMPTY_CONFIG,
            ConfigResponse.EMPTY_CONFIG));
    }

    @Test
    public void testGetMergedConfigResponse() throws AppdefEntityNotFoundException,
        ConfigFetchException, PermissionException, EncodingException {
        ConfigResponse srvrControlConfig = new ConfigResponse();
        srvrControlConfig.setValue("service_name", "svc");
        configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            server.getEntityId(), ConfigResponse.EMPTY_CONFIG, ConfigResponse.EMPTY_CONFIG,
            srvrControlConfig.encode());

        ConfigResponse platConfig = new ConfigResponse();
        platConfig.setValue("DisplayName", "fancy");
        configManager.configureResponse(authzSubjectManager.getOverlordPojo(),
            platform.getEntityId(), platConfig.encode(), ConfigResponse.EMPTY_CONFIG,
            ConfigResponse.EMPTY_CONFIG);

        ConfigResponse controlConfig = configManager.getMergedConfigResponse(
            authzSubjectManager.getOverlordPojo(), ProductPlugin.TYPE_CONTROL,
            service.getEntityId(), true);
        // see if Server's control config is returned as part of service.
        // product config is also included
        Properties expected = new Properties();
        expected.setProperty("service_name", "svc");
        expected.setProperty(ProductPlugin.PROP_PLATFORM_NAME, platform.getName());
        expected.setProperty(ProductPlugin.PROP_PLATFORM_FQDN, platform.getFqdn());
        expected
            .setProperty(ProductPlugin.PROP_PLATFORM_TYPE, platform.getPlatformType().getName());
        expected.setProperty(ProductPlugin.PROP_PLATFORM_IP, "10.1.4.5");
        expected.setProperty(ProductPlugin.PROP_PLATFORM_ID, String.valueOf(platform.getId()));
        expected.setProperty(ProductPlugin.PROP_INSTALLPATH, server.getInstallPath());
        expected.setProperty("DisplayName", "fancy");
        assertEquals(expected, controlConfig.toProperties());
    }
}

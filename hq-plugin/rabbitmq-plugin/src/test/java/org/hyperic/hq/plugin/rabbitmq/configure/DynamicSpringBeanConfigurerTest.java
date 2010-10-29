package org.hyperic.hq.plugin.rabbitmq.configure;

import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;

import static org.junit.Assert.*;

/**
 * DynamicSpringBeanConfigurerTest
 * @author Helena Edelson
 */
@Ignore("Until connections are mocked")
public class DynamicSpringBeanConfigurerTest {

    private static final String HOSTNAME = "vm-host";

    private ConfigResponse serverConfig;

    @Before
    public void before() throws PluginException {
        this.serverConfig = new ConfigResponse();
        this.serverConfig.setValue(DetectorConstants.HOST, HOSTNAME);
        serverConfig.setValue(DetectorConstants.USERNAME, "guest");
        this.serverConfig.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
    }


    @Test
    @ExpectedException(IllegalArgumentException.class)
    public void noHost() throws PluginException {
        serverConfig.setValue(DetectorConstants.HOST, null);
        PluginContextCreator.createContext(Configuration.toConfiguration(serverConfig));
        ConfigurationManager configurationManager = PluginContextCreator.getBean(ConfigurationManager.class);
        assertNotNull(configurationManager);
    }

    @Test
    public void createDynamicBeansAssertFailSuccess() throws PluginException {
        this.serverConfig.setValue(DetectorConstants.PASSWORD, "wrongPassword");

        try {
            Configuration configuration = Configuration.toConfiguration(serverConfig);
            assertFalse(configuration.isConfigured());
            PluginContextCreator.createContext(configuration);
            assertFalse(PluginContextCreator.isInitialized());
        }
        catch (PluginException e) {
            this.serverConfig.setValue(DetectorConstants.AUTHENTICATION, ErlangCookieHandler.configureCookie(serverConfig));
            Configuration configuration = Configuration.toConfiguration(serverConfig);
            assertTrue(configuration.isConfigured());
            PluginContextCreator.createContext(configuration);
            assertTrue(PluginContextCreator.isInitialized());
        }
    }

    @Test
    public void createDynamicBeansAssertSuccess() throws PluginException {
        this.serverConfig.setValue(DetectorConstants.PASSWORD, "guest");
        this.serverConfig.setValue(DetectorConstants.AUTHENTICATION, ErlangCookieHandler.configureCookie(this.serverConfig));

        Configuration configuration = Configuration.toConfiguration(serverConfig);
        assertTrue(configuration.isConfigured());

        PluginContextCreator.createContext(Configuration.toConfiguration(serverConfig));
        assertTrue(PluginContextCreator.isInitialized());
    }

}

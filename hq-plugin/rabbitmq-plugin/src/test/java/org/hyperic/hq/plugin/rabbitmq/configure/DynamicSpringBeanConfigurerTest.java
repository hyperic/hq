package org.hyperic.hq.plugin.rabbitmq.configure;

import org.hyperic.hq.plugin.rabbitmq.core.*; 
import org.hyperic.hq.plugin.rabbitmq.validate.PluginValidator;
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
public class DynamicSpringBeanConfigurerTest {

    private static final String HOSTNAME = "vmhost";

    private ConfigResponse serviceConfig;

    @Before
    public void before() throws PluginException {
        this.serviceConfig = new ConfigResponse();
        this.serviceConfig.setValue(DetectorConstants.HOST, HOSTNAME);
        this.serviceConfig.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
        this.serviceConfig.setValue(DetectorConstants.AUTHENTICATION, ErlangCookieHandler.configureCookie(serviceConfig));
    }

    @Test @ExpectedException(IllegalArgumentException.class) 
    public void noUsername() {
        PluginContextCreator.createContext(serviceConfig, new Class[]{RabbitConfiguration.class});
    }

    @Test @Ignore("Until connections are mocked")
    public void createDynamicBeansAssertSuccess() throws PluginException {
        serviceConfig.setValue(DetectorConstants.USERNAME, "guest");
        serviceConfig.setValue(DetectorConstants.PASSWORD, "guest");

        if (PluginValidator.isConfigured(serviceConfig.toProperties())) {
            PluginContextCreator.createContext(serviceConfig, new Class[]{RabbitConfiguration.class});
            RabbitGateway rabbitGateway = PluginContextCreator.getBean(RabbitGateway.class);
            assertNotNull(rabbitGateway);
            assertNotNull(rabbitGateway.getRabbitStatus());
        }
    }

}

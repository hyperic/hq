package org.hyperic.hq.plugin.rabbitmq;

import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitUtils;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*; 

/**
 * RabbitMQPluginTest
 * @author Helena Edelson
 */
@Ignore("Manual cookie value to connect to each node is required")
public class RabbitPluginTest extends AbstractPluginTest {

    @Test @Ignore("not finished")
    public void inferCookieFile() {
        /** can return null */
        String preUserUIConfig = RabbitUtils.handleCookie(this.createConfigResponse());
        logger.debug(preUserUIConfig);

        ConfigResponse pluginConfig = new ConfigResponse();
        pluginConfig.setValue("host", "localhost");
        pluginConfig.setValue("username", "guest");
        pluginConfig.setValue("password", "guest");

        String postUserUIConfig = RabbitUtils.handleCookie(pluginConfig);
        assertNotNull(postUserUIConfig);
        assertTrue(postUserUIConfig.length() > 0);
    }

    @Test 
    public void initialize() throws PluginException {
        RabbitProductPlugin.initializeGateway(productPlugin.getConfig());

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        assertNotNull(rabbitGateway);
    }
}

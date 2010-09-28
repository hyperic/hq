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
@Ignore("Need to mock the connection for automation")
public class RabbitPluginTest extends AbstractPluginTest {


    @Test 
    public void initialize() throws PluginException {
        RabbitProductPlugin.initializeGateway(productPlugin.getConfig());

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        assertNotNull(rabbitGateway);
    }
}

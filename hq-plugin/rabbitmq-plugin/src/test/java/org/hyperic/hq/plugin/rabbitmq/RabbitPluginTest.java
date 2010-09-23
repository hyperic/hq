package org.hyperic.hq.plugin.rabbitmq;

import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*; 

/**
 * RabbitMQPluginTest
 * @author Helena Edelson
 */
public class RabbitPluginTest extends AbstractPluginTest {

    @Test @Ignore("Manual cookie value to connect to each node is required")
    public void initialize() throws PluginException {
        RabbitProductPlugin.initializeGateway(productPlugin.getConfig());

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        assertNotNull(rabbitGateway);
    }
}

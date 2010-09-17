package org.hyperic.hq.plugin.rabbitmq;
 
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Properties;

/**
 * RabbitMQPluginTest
 *
 * @author Helena Edelson
 */
@Ignore
public class RabbitPluginTest extends AbstractPluginTest {

    @Test
    public void initialize() throws PluginException {
        RabbitProductPlugin.initializeGateway(productPlugin.getConfig());
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        assertNotNull(rabbitGateway);
    }
}

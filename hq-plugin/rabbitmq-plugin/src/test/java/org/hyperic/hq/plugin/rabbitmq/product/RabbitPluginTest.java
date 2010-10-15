package org.hyperic.hq.plugin.rabbitmq.product;

import org.hyperic.hq.plugin.rabbitmq.AbstractPluginTest;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.rabbit.admin.QueueInfo;

import java.util.List;

import static org.junit.Assert.*;

/**
 * RabbitMQPluginTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class RabbitPluginTest extends AbstractPluginTest {


    @Test 
    public void initialize() throws PluginException {
        RabbitProductPlugin.createRabbitContext(productPlugin.getConfig());

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        assertNotNull(rabbitGateway);
        List<QueueInfo> queues = rabbitGateway.getQueues("/");
        assertTrue(queues.size() > 0);
    }
}

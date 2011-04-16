package org.hyperic.hq.operation.rabbit.shared;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.api.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
@Ignore("have to mock the connection")
public class ChannelTemplateTests {

    private ChannelTemplate channelTemplate = new ChannelTemplate(new ConnectionFactory());

    @Test
    public void execute() {
        this.channelTemplate.execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    String exchange = "test.foo";
                    channel.exchangeDeclare(exchange, Constants.SHARED_EXCHANGE_TYPE, true, false, null);
                    String queue = channel.queueDeclare("foo", true, true, false, null).getQueue();
                    channel.queueBind(queue, exchange, "ops.foo.*");
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });
    }
}

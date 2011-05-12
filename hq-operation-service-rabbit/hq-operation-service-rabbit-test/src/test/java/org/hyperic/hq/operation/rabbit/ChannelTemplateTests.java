package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore("not working with a mock Connection")
public class ChannelTemplateTests {

    private ChannelTemplate channelTemplate = new ChannelTemplate(new ConnectionFactory());

    @Test
    public void execute() {
        this.channelTemplate.execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    String exchange = "test.foo";
                    channel.exchangeDeclare(exchange, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
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

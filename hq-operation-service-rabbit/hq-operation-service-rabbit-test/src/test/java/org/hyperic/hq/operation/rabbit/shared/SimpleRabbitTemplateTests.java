package org.hyperic.hq.operation.rabbit.shared;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.bizapp.client.RegisterAgentResult;
import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.rabbit.api.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.core.SimpleRabbitTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

@Ignore("not working with a mock Connection")
public class SimpleRabbitTemplateTests {

    private SimpleRabbitTemplate rabbitTemplate;

    private String requestQueue;

    private String responseQueue;

    private final RegisterAgentRequest data =
            new RegisterAgentRequest(null, "authTokenValue", "5.0", 1, "localhost", 7071, "hqadmin", "hqadmin", false);

    @Before
    public void prepare() {
        ConnectionFactory cf = new ConnectionFactory();
        this.rabbitTemplate = new SimpleRabbitTemplate(cf, new JsonMappingConverter());
        ChannelTemplate channelTemplate = new ChannelTemplate(cf);

        channelTemplate.execute(new ChannelCallback<Object>() {
            public RegisterAgentResult doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare("to.server", "topic", true, false, null);
                    requestQueue = channel.queueDeclare("requestQueue", true, false, false, null).getQueue();
                    channel.queueBind(requestQueue, "to.server", "request.*");

                    channel.exchangeDeclare("to.agent", "topic", true, false, null);
                    responseQueue = channel.queueDeclare("responseQueue", true, false, false, null).getQueue();
                    channel.queueBind(requestQueue, "to.agent", "request.*");

                } catch (Exception e) {

                }
                return null;
            }
        });

    }

    @Test
    public void send() {
        AMQP.BasicProperties bp = MessageConstants.getBasicProperties(this.data); 
        this.rabbitTemplate.send("to.server", "request.register", data, bp);
    }

    @Test
    public void sendAndReceive() throws ExecutionException, TimeoutException, InterruptedException {
        final String message = "message";
        final AMQP.BasicProperties bp = MessageConstants.getBasicProperties(message);

        ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<Object> received = executor.submit(new Callable<Object>() {
			public Object call() throws Exception {
                return rabbitTemplate.sendAndReceive(requestQueue, "to.server", "request.register", message, bp);
			}
		});
        assertEquals(message, received.get(1000, TimeUnit.MILLISECONDS));
    }
}

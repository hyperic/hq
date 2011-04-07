package org.hyperic.hq.operation.rabbit.mapping;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.RoutingConstants;
import org.hyperic.hq.operation.rabbit.util.Routings;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
public class RabbitBindingsRegistry implements BindingRegistry {

    private final Map<String, OperationRouting> operationToBindingPatternMappings = new ConcurrentHashMap<String, OperationRouting>();

    private final ChannelTemplate channelTemplate;

    private final Routings routings;

    public RabbitBindingsRegistry(ConnectionFactory connectionFactory) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.routings = new Routings();
        initialize();
    }

    /**
     * Initializes all components - manual because the agent is not using Spring
     */
    private void initialize() {
        // for loop registering each 
    }

    /**
     * Declare, bind then register components.
     * Queues declared are durable, exclusive, non-auto-delete
     * @param operation the operaton meta-data
     */
    public void registerBinding(final Operation operation) {
        boolean success = this.channelTemplate.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare(operation.exchangeName(), RoutingConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                    String queue = channel.queueDeclare("", true, true, false, null).getQueue();
                    channel.queueBind(queue, operation.exchangeName(), operation.value()); 
                    return true;
                } catch (IOException e) {
                    throw new ChannelException(e);
                }
            }
        });
        if (success) {
            this.operationToBindingPatternMappings.put(operation.operationName(), new OperationRouting(operation.exchangeName(), operation.value()));
        }
    }

}

package org.hyperic.hq.amqp.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple config for prototype.
 * @author Helena Edelson
 */
@Configuration
public class CommonAmqpConfiguration {

    /** to consume from the agent */
    protected final String agentQueueName = "queues.agent";

    /** to consume from the server */
    protected final String serverQueueName = "queues.server";

    /** to send to a server */
    protected final String serverDirectExchangeName = "exchanges.direct.server";

    /** to send to an agent */
    protected final String agentExchangeName = "exchanges.direct.agent";

    protected final String fanoutExchangeName = "exchanges.fanout";    
    protected final String agentSubscriptionName = "exchanges.topic.agent";

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        return new SingleConnectionFactory();
    }

    @Bean
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(rabbitConnectionFactory());
    }

    /**
     * To route to the Server.
     */
    @Bean
    public DirectExchange serverExchange() {
        DirectExchange e = new DirectExchange(serverDirectExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(serverQueue()).to(e).with(serverQueue().getName()));
        return e;
    }

    @Bean
    public Queue anonymousQueue() { 
        return amqpAdmin().declareQueue();
    }

    /**
     * To consume from the server
     */
    @Bean
    public Queue serverQueue() { 
        Queue queue = new Queue(serverQueueName);
        amqpAdmin().declareQueue(queue);
        return queue;
    }

    @Bean
    public DirectExchange agentExchange() {
        DirectExchange e = new DirectExchange(agentExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(agentQueue()).to(e).with(agentQueue().getName()));
        //amqpAdmin().declareBinding(BindingBuilder.from(agentQueue()).to(serverExchange()).with(agentQueue().getName()));
        return e;
    }

    /**
     * Server listens on this Queue
     * @return
     */
    @Bean
    public Queue agentQueue() {
        Queue queue = new Queue(agentQueueName);
        amqpAdmin().declareQueue(queue);
        return queue;
    }

    /* Specific Queues/Exchanges - these and their creation/binding
       will be pulled out of Spring and done dynamically. */

    @Bean
    public Queue directQueue() {
        return amqpAdmin().declareQueue();
    }


    @Bean
    public Queue fanoutQueue() {
        return amqpAdmin().declareQueue();
    }

    @Bean
    public Queue topicQueue() {
        return amqpAdmin().declareQueue();
    }
 
    @Bean
    public FanoutExchange fanoutExchange() {
        FanoutExchange e = new FanoutExchange(fanoutExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }

    @Bean
    public TopicExchange topicExchange() {
        TopicExchange e = new TopicExchange(agentSubscriptionName, true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }
}

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

    protected final String routingKey = "ping";

    protected final String agentExchange = "agent";

    protected final String serverExchange = "server";

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
        DirectExchange e = new DirectExchange(serverExchange);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(serverQueue()).to(e).with(routingKey));
        return e;
    }

    @Bean
    public Queue serverQueue() { 
        return amqpAdmin().declareQueue();
    }

    @Bean
    public DirectExchange agentExchange() {
        DirectExchange e = new DirectExchange(agentExchange);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(agentQueue()).to(e).with(routingKey));
        return e;
    }

    @Bean
    public Queue agentQueue() {
        return amqpAdmin().declareQueue();
    }

    /* temporary, for tests */

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
        FanoutExchange e = new FanoutExchange("fanout", true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }

    @Bean
    public TopicExchange topicExchange() {
        TopicExchange e = new TopicExchange("topic", true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }
 
}

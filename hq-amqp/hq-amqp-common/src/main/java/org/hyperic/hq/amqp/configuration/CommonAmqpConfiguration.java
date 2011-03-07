package org.hyperic.hq.amqp.configuration;

import org.hyperic.hq.amqp.admin.RabbitAdminTemplate;
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
/* @ImportResource("classpath:META-INF/spring/rabbit.xml") */
public class CommonAmqpConfiguration implements CommonConfiguration {
    /* 
    @Value("${exchanges.agent.server}") private String agentToServerExchangeName;
    @Value("${exchanges.direct}") private String directExchangeName;
    @Value("${exchanges.fanout}") private String fanoutExchangeName;
    @Value("${exchanges.topic}") private String topicExchangeName;
    */

    protected final String agentToServerQueueName = "queues.agentToServer";
    protected final String serverToAgentQueueName = "queues.serverToAgent";

    protected final String agentToServerDirectExchangeName = "exchanges.direct.agentToServer";
    protected final String serverToAgentDirectExchangeName = "exchanges.direct.serverToAgent";

    protected final String fanoutExchangeName = "exchanges.fanout";
    
    protected final String agentSubscriptionName = "exchanges.topic.agentToServer";

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        return new SingleConnectionFactory();
    }

    @Bean
    public RabbitAdminTemplate adminTemplate() {
        return new RabbitAdminTemplate();
    }

    @Bean
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(rabbitConnectionFactory());
    }

    /**
     * Agent sends to agentToServerExchange to route to the Server.
     */
    @Bean
    public DirectExchange agentToServerExchange() {
        DirectExchange e = new DirectExchange(agentToServerDirectExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }
 
    /**
     * Agent consumes from the server
     */
    @Bean
    public Queue serverToAgentQueue() {
        Queue queue = new Queue(serverToAgentQueueName);
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

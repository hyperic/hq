package org.hyperic.hq.amqp.configuration;

import org.hyperic.hq.amqp.admin.RabbitAdminTemplate;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prototype only.
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
    
    private final String agentToServerExchangeName = "agentServerExchange";
    private final String directExchangeName = "direct";
    private final String fanoutExchangeName = "fanout";
    private final String topicExchangeName = "topic";

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

    @Bean
    public Queue serverToAgentQueue() {
        Queue queue = new Queue("serverToAgentQueue");
        amqpAdmin().declareQueue(queue);
        return queue;
    }

    @Bean
    public Queue agentToServerQueue() {
        Queue queue = new Queue("agentServerQueue");
        amqpAdmin().declareQueue(queue);
        return queue;
    }
    @Bean
    public DirectExchange agentToServerExchange() {
        DirectExchange e = new DirectExchange(agentToServerExchangeName, true, false);
        amqpAdmin().declareExchange(e); 
        amqpAdmin().declareBinding(BindingBuilder.from(agentToServerQueue()).to(e).with(agentToServerQueue().getName()));
        return e;
    }

    /* Specific Queues/Exchanges - these and their creation/binding
       will be pulled out of Spring and done dynamically. */

    @Bean
    public Queue directQueue() {
        return amqpAdmin().declareQueue();
    }
    @Bean
    public DirectExchange directExchange() {
        DirectExchange e = new DirectExchange(directExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        return e;
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
        TopicExchange e = new TopicExchange(topicExchangeName, true, false);
        amqpAdmin().declareExchange(e);
        return e;
    }
}

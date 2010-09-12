package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.RabbitScheduler;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.erlang.core.ErlangTemplate;

/**
 * RabbitTestConfig
 *
 * @author Helena Edelson
 */
@Configuration
public class RabbitTestConfiguration {

    @Value("${hyperic.rabbit.routingkey}")
    private String routingKey;

    @Value("${hyperic.rabbit.request.queue}")
    private String requestQueueName;

    @Value("${hyperic.rabbit.response.queue}")
    private String responseQueueName;

    @Value("${hyperic.rabbit.exchange}")
    private String exchangeName;

    @Value("${conn.rabbit.host}")
    private String host;

    @Value("${conn.rabbit.username}")
    private String username;

    @Value("${conn.rabbit.password}")
    private String password;

    @Value("${consumer.concurrentConsumers}")
    private int concurrentConsumers;

    @Bean
    public SingleConnectionFactory singleConnectionFactory() {
        SingleConnectionFactory connectionFactory = new SingleConnectionFactory(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public RabbitBrokerAdmin rabbitBrokerAdmin() {
        return new RabbitBrokerAdmin(singleConnectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(singleConnectionFactory());
        rabbitTemplate.setExchange(directExchange().getName());
        rabbitTemplate.setQueue(requestQueue().getName());
        return rabbitTemplate;
    }

    @Bean
    public RabbitGateway rabbitGateway() {
        RabbitGateway rabbitGateway = new RabbitBrokerGateway(rabbitBrokerAdmin());
        rabbitGateway.createQueue(requestQueue().getName());
        rabbitGateway.createQueue(responseQueue().getName());
        return rabbitGateway;
    }

    @Bean
    public ErlangGateway erlangGatway() {
        return new ErlangBrokerGateway(rabbitBrokerAdmin().getErlangTemplate());
    }

    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueueName);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(responseQueueName);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName + ExchangeType.topic);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(exchangeName + ExchangeType.fanout);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(exchangeName + ExchangeType.direct);
    }

    @Bean
    public Binding requestBinding() {
        return BindingBuilder.from(requestQueue()).to(fanoutExchange());
    }

    @Bean
    public Binding replyBinding() {
        return BindingBuilder.from(responseQueue()).to(directExchange()).withQueueName();
    }

    @Bean
    public SimpleMessageListenerContainer listenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(singleConnectionFactory());
        container.setQueues(requestQueue());
        container.setMessageListener(messageListenerAdapter());
        container.setConcurrentConsumers(concurrentConsumers);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter();
    }

    @Bean
    public RabbitScheduler rabbitTaskScheduler() {
        return new RabbitScheduler(rabbitTemplate());
    }


}


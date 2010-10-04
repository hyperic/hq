package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitBrokerManager;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.RabbitScheduler;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.ImportResource;
import org.springframework.erlang.core.ErlangTemplate;

/**
 * RabbitTestConfig
 * @author Helena Edelson
 */ 
@ImportResource("classpath:/org/hyperic/hq/plugin/rabbitmq/*-context.xml")
public class RabbitTestConfiguration {

    /** Mock: Sigar OperatingSystem.getInstance().getName();*/
    private @Value("${platform.type}") String platformType;

    private @Value("${hostname}") String hostname;

    private @Value("${username}") String username;

    private @Value("${password}") String password;

    private String MARKET_DATA_EXCHANGE_NAME = "stocks.*";

    private String STOCK_REQUEST_QUEUE_NAME = "stocks.*";

    private String STOCK_RESPONSE_QUEUE_NAME = "stocks.response";

    private String MARKET_DATA_ROUTING_KEY = "stocks.nasdaq.*"; 

    @Bean
    public ConfigResponse serverConfig() {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, hostname);
        conf.setValue(DetectorConstants.USERNAME, username);
        conf.setValue(DetectorConstants.PASSWORD, password);
        conf.setValue(DetectorConstants.PLATFORM_TYPE, platformType);
        
        return conf;
    }

    @Bean
    public SingleConnectionFactory singleConnectionFactory() {
        SingleConnectionFactory connectionFactory = new SingleConnectionFactory(hostname);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public RabbitBrokerAdmin rabbitBrokerAdmin() {
        String value = ErlangCookieHandler.configureCookie(serverConfig());
        return new HypericBrokerAdmin(singleConnectionFactory(),value);
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(singleConnectionFactory());
        rabbitTemplate.setImmediatePublish(true);
        rabbitTemplate.setMandatoryPublish(true);

        return rabbitTemplate;
    }

    @Bean
    public RabbitGateway rabbitGateway() { 
        return new RabbitBrokerGateway();
    }

    @Bean
    public RabbitManager rabbitManager() {
        return new RabbitBrokerManager(rabbitGateway());
    }

    @Bean
    public ErlangConverter erlangConverter() {
        return new HypericErlangConverter(rabbitBrokerAdmin().getErlangTemplate());
    }

    @Bean
    public Queue marketDataQueue() {
        Queue marketDataQueue = new Queue(STOCK_REQUEST_QUEUE_NAME);
        marketDataQueue.setDurable(true); 
        return marketDataQueue;
    }

    @Bean
    public TopicExchange marketDataExchange() {
        return new TopicExchange(MARKET_DATA_EXCHANGE_NAME);
    }

    /**
     * Binds to the market data exchange. Interested in any stock quotes.
     */
    @Bean
    public Binding marketDataBinding() {
        return BindingBuilder.from(marketDataQueue()).to(marketDataExchange()).with(MARKET_DATA_ROUTING_KEY);
    }

    @Bean
    public Queue responseQueue() {
        Queue queue = new Queue(STOCK_RESPONSE_QUEUE_NAME);
        queue.setDurable(true);
        return queue;
    }

   /* @Bean
    public SimpleMessageListenerContainer asyncListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(singleConnectionFactory());
        container.setQueues(marketDataQueue()); //, responseQueue()
        container.setConcurrentConsumers(5);
        MessageListenerAdapter adapter = new MessageListenerAdapter();
        adapter.setDelegate(new PojoHandler());
        container.setMessageListener(adapter);
 
        return container;
    }*/

    @Bean
    public RabbitScheduler rabbitTaskScheduler() {
        return new RabbitScheduler(rabbitTemplate());
    }


}


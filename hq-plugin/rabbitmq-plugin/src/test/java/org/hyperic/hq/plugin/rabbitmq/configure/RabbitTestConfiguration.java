package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.populate.PojoHandler;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.RabbitScheduler;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
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

    private String MARKET_DATA_EXCHANGE_NAME = "app.stock.quotes";

    private String STOCK_REQUEST_QUEUE_NAME = "app.stock.quotes";

    private String STOCK_RESPONSE_QUEUE_NAME = "app.stock.response";

    private String MARKET_DATA_ROUTING_KEY = "app.stock.quotes.nasdaq.*"; 

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
        String value = RabbitUtils.configureCookie(serverConfig());
        return new HypericBrokerAdmin(singleConnectionFactory(),value);
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(singleConnectionFactory());
        rabbitTemplate.setExchange(marketDataExchange().getName());
        rabbitTemplate.setQueue(marketDataQueue().getName());
        rabbitTemplate.setRoutingKey(MARKET_DATA_ROUTING_KEY);
        rabbitTemplate.setImmediatePublish(true);
        rabbitTemplate.setMandatoryPublish(true);

        return rabbitTemplate;
    }

    @Bean
    public RabbitGateway rabbitGateway() { 
        return new RabbitBrokerGateway(rabbitBrokerAdmin());
    }

    @Bean
    public ErlangTemplate erlangTemplate() {
        return rabbitBrokerAdmin().getErlangTemplate();
    }

    @Bean
    public ErlangConverter erlangConverter() {
        return new JErlangConverter();
    }

    @Bean
    public ErlangGateway erlangGatway() {
        return new ErlangBrokerGateway();
    }

    @Bean
    public Queue marketDataQueue() {
        Queue marketDataQueue = new Queue(STOCK_REQUEST_QUEUE_NAME);
        rabbitBrokerAdmin().declareQueue(marketDataQueue);
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
    public Queue requestQueue() {
        return new Queue(STOCK_REQUEST_QUEUE_NAME);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(STOCK_RESPONSE_QUEUE_NAME);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(MARKET_DATA_EXCHANGE_NAME + ExchangeTypes.FANOUT);
    }

    @Bean
    public SimpleMessageListenerContainer asyncListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(singleConnectionFactory());
        container.setQueues(marketDataQueue()); //, responseQueue()
        container.setConcurrentConsumers(5);
        MessageListenerAdapter adapter = new MessageListenerAdapter();
        adapter.setDelegate(new PojoHandler());
        container.setMessageListener(adapter);
 
        return container;
    }

    @Bean
    public RabbitScheduler rabbitTaskScheduler() {
        return new RabbitScheduler(rabbitTemplate());
    }


}


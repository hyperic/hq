package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitBrokerManager;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

/**
 * RabbitTestConfig
 * @author Helena Edelson
 */
@ImportResource("classpath:/org/hyperic/hq/plugin/rabbitmq/*-context.xml")
public class RabbitTestConfiguration {

    private @Value("${platform.type}") String platformType;

    private @Value("${hostname}") String hostname;

    private @Value("${username}") String username;

    private @Value("${password}") String password;

    private @Value("${nodename}") String nodename;


    @Bean
    public Configuration configuration() throws PluginException {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, "hedelson");
        conf.setValue(DetectorConstants.SERVER_NAME, "rabbit@localhost");
        conf.setValue(DetectorConstants.USERNAME, username);
        conf.setValue(DetectorConstants.PASSWORD, password);
        conf.setValue(DetectorConstants.PLATFORM_TYPE, platformType);
        String auth = ErlangCookieHandler.configureCookie(conf);
        conf.setValue(DetectorConstants.AUTHENTICATION, auth);

        return Configuration.toConfiguration(conf);
    }

    @Bean
    public RabbitGateway rabbitGateway() throws PluginException {
        return new RabbitBrokerGateway(configuration());
    }

    @Bean
    public RabbitManager rabbitManager() throws PluginException {
        return new RabbitBrokerManager(rabbitGateway());
    }

    @Bean
    public Queue stocksQueue() {
        return new Queue("stocks.quotes");
    }

    @Bean
    public Queue trendsQueue() {
        return new Queue("market.trends");
    }

    @Bean
    public Queue alertsQueue() {
        return new Queue("market.alerts");
    }

    /** Requires the Queue exist in the broker */
    /*
    @Bean
   public SimpleMessageListenerContainer listenerContainer() {
       SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
       container.setConnectionFactory(singleConnectionFactory());
       container.setQueues(stocksQueue());
       container.setMessageListener(new MessageListenerAdapter(new StockQuoteHandler()));
       container.setAutoAck(true);
       return container;
   } */

    /*
    @Bean
    public RabbitScheduler rabbitScheduler() {
        return new RabbitScheduler();
    }

       @Bean
       public TopicExchange marketDataExchange() {
           return new TopicExchange(MARKET_DATA_EXCHANGE_NAME);
       }

       @Bean
       public Binding marketDataBinding() {
           return BindingBuilder.from(stocksQueue()).to(marketDataExchange()).with(MARKET_DATA_ROUTING_KEY);
       }

       @Bean
       public Queue responseQueue() {
           Queue queue = new Queue(STOCK_RESPONSE_QUEUE_NAME);
           queue.setDurable(true);
           return queue;
       }
    */

}


package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.populate.StockQuoteHandler;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

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
    public ConfigResponse configResponse() {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, hostname);
        conf.setValue(DetectorConstants.SERVER_NAME, nodename);
        conf.setValue(DetectorConstants.USERNAME, username);
        conf.setValue(DetectorConstants.PASSWORD, password);
        conf.setValue(DetectorConstants.PLATFORM_TYPE, platformType);
        String auth = null;
        try {
            auth = ErlangCookieHandler.configureCookie(conf);
        } catch (PluginException e) {
            //handle
        }
        conf.setValue(DetectorConstants.AUTHENTICATION, auth);
        return conf;
    }

//    @Bean
//    public Configuration configuration() {
//        return Configuration.toConfiguration(configResponse());
//    }

//    @Bean
//    public ConfigurationManager configurationManager() {
//        return new RabbitConfigurationManager(configuration());
//    }

    @Bean
    public Queue stocksQueue() {
        Queue queue = new Queue("stocks.quotes");
        queue.setDurable(true);
        return queue;
    }

    @Bean
    public Queue trendsQueue() {
        Queue queue = new Queue("market.trends");
        queue.setDurable(true);
        return queue;
    }

    @Bean
    public Queue alertsQueue() {
        Queue queue = new Queue("market.alerts");
        queue.setDurable(true);
        return queue;
    }

//    @Bean
//    public CachingConnectionFactory ccf() {
//        return configurationManager().getConnectionFactory();
//    }

    @Bean
    public List<Queue> queues() {
        List<Queue> queues = new ArrayList<Queue>();
        queues.add(stocksQueue());
        queues.add(alertsQueue());
        queues.add(trendsQueue());
        return queues;
    }

    /*@Bean @Lazy 
    public SimpleMessageListenerContainer mlc() { 
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(configurationManager().getConnectionFactory());
        container.setQueues(stocksQueue(), alertsQueue(), trendsQueue());
        container.setMessageListener(new MessageListenerAdapter(new StockQuoteHandler()));
        return container;
    }*/
    
    /*
    @Bean
    public RabbitScheduler rabbitScheduler() {
        return new RabbitScheduler();
    }

    @Bean
    public TopicExchange marketDataExchange() {
        return new TopicExchange(stocksQueue().getName());
    }

    @Bean
    public Binding marketDataBinding() {
        return BindingBuilder.from(stocksQueue()).to(marketDataExchange()).with(routingKey);
    }

    */

}


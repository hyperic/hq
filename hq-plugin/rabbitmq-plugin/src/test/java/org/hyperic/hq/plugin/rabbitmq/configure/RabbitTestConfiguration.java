package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitBrokerManager;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.hyperic.hq.plugin.rabbitmq.populate.StockQuoteHandler;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.RabbitScheduler;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

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

        private final String marketDataQueueName = "stocks.quotes";


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
        public CachingConnectionFactory connectionFactory() {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(hostname);
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
            return connectionFactory;
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
            try {
               String auth = ErlangCookieHandler.configureCookie(serverConfig());
               if (auth != null) {
                   return new HypericBrokerAdmin(singleConnectionFactory(), auth);
               }
            } catch (PluginException e) {
                // handle
            }
            return null;
        }

        @Bean
        public RabbitTemplate rabbitTemplate() {
            RabbitTemplate template = new RabbitTemplate(singleConnectionFactory());
            return template;
        }

        @Bean
        public BeanPostProcessor postProcessor() {
            return new ScheduledAnnotationBeanPostProcessor();
        }

        @Bean
        public ErlangConverter erlangConverter() {
            return new HypericErlangConverter(rabbitBrokerAdmin().getErlangTemplate());
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
        public Queue marketDataQueue() {
            Queue marketDataQueue = new Queue(this.marketDataQueueName);
            //marketDataQueue.setDurable(true);
            return marketDataQueue;
        }

        /** Requires the Queue exist in the broker */
       /* @Bean
        public SimpleMessageListenerContainer listenerContainer() {
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setConnectionFactory(singleConnectionFactory());
            container.setQueues(marketDataQueue());
            container.setMessageListener(new MessageListenerAdapter(new StockQuoteHandler()));
            container.setAutoAck(true);
            return container;
        }*/

        /*
        @Bean
        public RabbitScheduler rabbitScheduler() {
            return new RabbitScheduler();
        }
        */

        /*
        @Bean
        public TopicExchange marketDataExchange() {
            return new TopicExchange(MARKET_DATA_EXCHANGE_NAME);
        }

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
     */

}


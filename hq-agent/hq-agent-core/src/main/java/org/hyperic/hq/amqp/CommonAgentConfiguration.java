package org.hyperic.hq.amqp;
 
import org.hyperic.hq.amqp.configuration.CommonAmqpConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public class CommonAgentConfiguration extends CommonAmqpConfiguration {

    @Bean
    public RabbitTemplate agentRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(serverDirectExchangeName);
        template.setRoutingKey(agentQueueName);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer serverListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitConnectionFactory()); 
        container.setMessageListener(new MessageListenerAdapter(new SimpleAsyncResponseHandler()));
        container.setQueues(agentQueue());
        return container;
    }

}

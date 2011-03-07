package org.hyperic.hq.amqp;

import org.hyperic.hq.amqp.configuration.CommonAmqpConfiguration;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;

/**
 * @author Helena Edelson
 */
//@Configuration
public class CommonAgentConfiguration extends CommonAmqpConfiguration {

    @Bean
    public AmqpTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(agentToServerDirectExchangeName);
        template.setRoutingKey(serverToAgentQueueName);
        return template;
    }

      @Bean
    public SimpleMessageListenerContainer agentListener() throws InterruptedException {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitConnectionFactory()); 
        container.setMessageListener(new MessageListenerAdapter(new SimpleAsyncResponseHandler()));
        container.setQueues(serverToAgentQueue());
        return container;
    }

}

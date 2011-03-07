package org.hyperic.hq.amqp.configuration;

/**
 * @author Helena Edelson
 */
//@Configuration
public class CommonAgentConfiguration {

   /* private final String agentToServerExchangeName = "agentServerExchange";

    private final String agentToServerRoutingKey = "agentServerQueue";

    private final String serverToAgentQueueName = "serverToAgentQueue";

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
    public AmqpTemplate agentAmqpTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(agentToServerExchangeName);
        template.setRoutingKey(agentToServerRoutingKey);
        return template;
    }

     @Bean
    public Queue serverToAgentQueue() {
        return new Queue(serverToAgentQueueName);
    }

      @Bean
    public SimpleMessageListenerContainer agentListener() throws InterruptedException {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitConnectionFactory()); 
        container.setMessageListener(new MessageListenerAdapter(new AgentMessageHandler(agentAmqpTemplate())));
        container.setQueues(serverToAgentQueue());
        return container;
    }*/

}

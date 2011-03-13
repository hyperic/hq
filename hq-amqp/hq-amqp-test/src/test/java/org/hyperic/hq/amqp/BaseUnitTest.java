package org.hyperic.hq.amqp;

import org.junit.After;
import org.junit.Before;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

/**
 * @author Helena Edelson
 */
public class BaseUnitTest {

    protected SimpleMessageListenerContainer agentListener;

    protected SimpleMessageListenerContainer serverListener;

    protected RabbitTemplate agentTemplate;

    protected RabbitTemplate serverTemplate;
 
    protected Queue agentQueue;

    protected Queue serverQueue;

    protected DirectExchange agentExchange;

    protected DirectExchange serverExchange;

    protected RabbitAdmin admin;

    @Before
    public void initialize() {
        ConnectionFactory cf = new SingleConnectionFactory();
        this.admin = new RabbitAdmin(cf);

        prepareExchanges();
        prepareQueues();
        prepareTemplates(cf);
        //prepareListeners(cf);
    }

    private void prepareExchanges() {
        this.agentExchange = new DirectExchange("agent", false, false);
        admin.declareExchange(agentExchange);
        this.serverExchange = new DirectExchange("server", false, false);
        admin.declareExchange(serverExchange);
        //amqpAdmin().declareBinding(BindingBuilder.from(agentQueue).to(agentExchange).with(agentQueue.getName()));
         //amqpAdmin().declareBinding(BindingBuilder.from(serverQueue).to(serverExchange).with(serverQueue.getName()));
    }

    private void prepareQueues() {
        this.agentQueue = admin.declareQueue();
        this.serverQueue = admin.declareQueue();
    }

    private void prepareTemplates(ConnectionFactory cf) {
        this.agentTemplate = new RabbitTemplate(cf);
        agentTemplate.setExchange(serverExchange.getName());
        agentTemplate.setRoutingKey(serverQueue.getName()); 

        this.serverTemplate = new RabbitTemplate(cf);
        serverTemplate.setExchange(agentExchange.getName());
        serverTemplate.setRoutingKey(agentQueue.getName());
    }

    private void prepareListeners(ConnectionFactory cf) {
        this.agentListener = new SimpleMessageListenerContainer(cf);
        agentListener.setMessageListener(new MessageListenerAdapter(new Handler(agentTemplate)));
        agentListener.setQueues(agentQueue);
        agentListener.afterPropertiesSet();

        this.serverListener = new SimpleMessageListenerContainer(cf);
        serverListener.setMessageListener(new MessageListenerAdapter(new Handler(serverTemplate)));
        serverListener.setQueues(serverQueue);
        serverListener.afterPropertiesSet();        
    }

    @After
    public void destroy() {
        this.agentListener.stop();
        this.serverListener.stop();
    }

    /**
     * Message handler
     */
    public class Handler {

        private AmqpTemplate template;

        public Handler(AmqpTemplate template) {
            this.template = template;
        }

        public void handleMessage(String message) {
            System.out.println("received message=" + message);
            template.convertAndSend("Response message["+message+"]");
        }
    }
}

package org.hyperic.hq.operation.rabbit.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

/**
 * @author Helena Edelson
 */
@Ignore
public class AbstractSamqpTest {
    protected SimpleMessageListenerContainer requestListener;

    protected SimpleMessageListenerContainer responseListener;

    protected RabbitTemplate requestTemplate;

    protected RabbitTemplate responseTemplate;

    protected Queue requestQueue;

    protected Queue responseQueue;

    protected Exchange requestExchange;

    protected Exchange responseExchange;

    protected RabbitAdmin admin;

    protected final String routingKey;

    protected final boolean listen;

    /**
     * Pass in the routing
     */
    public AbstractSamqpTest(String routingKey, Exchange requestExchange, Exchange responseExchange, boolean listen) {
        this.routingKey = routingKey;
        this.requestExchange = requestExchange;
        this.responseExchange = responseExchange;
        this.listen = listen;
    }

    @Before
    public void prepare() {
        ConnectionFactory cf = new SingleConnectionFactory();
        this.admin = new RabbitAdmin(cf);
        prepareRouting(cf);
        prepareListeners(cf);
    }

    private void prepareRouting(ConnectionFactory cf) {
        admin.declareExchange(requestExchange);
        admin.declareExchange(responseExchange);

        this.requestQueue = admin.declareQueue();
        this.responseQueue = admin.declareQueue();

        /* very ugly but flexible */
        if (requestExchange instanceof TopicExchange) {
            admin.declareBinding(BindingBuilder.from(requestQueue).to((TopicExchange) requestExchange).with(routingKey));
            admin.declareBinding(BindingBuilder.from(responseQueue).to((TopicExchange) responseExchange).with(routingKey));
        } else if (requestExchange instanceof FanoutExchange) {
            admin.declareBinding(BindingBuilder.from(requestQueue).to((FanoutExchange) requestExchange));
            admin.declareBinding(BindingBuilder.from(responseQueue).to((FanoutExchange) responseExchange));
        } else if (requestExchange instanceof DirectExchange) {
            admin.declareBinding(BindingBuilder.from(requestQueue).to((DirectExchange) requestExchange).with(routingKey));
            admin.declareBinding(BindingBuilder.from(responseQueue).to((DirectExchange) responseExchange).with(routingKey));
        }


        this.requestTemplate = new RabbitTemplate(cf);
        requestTemplate.setExchange(requestExchange.getName());
        requestTemplate.setRoutingKey(routingKey);

        this.responseTemplate = new RabbitTemplate(cf);
        responseTemplate.setExchange(responseExchange.getName());
        responseTemplate.setRoutingKey(routingKey);
    }

    private void prepareListeners(ConnectionFactory cf) {
        if (listen) {
            this.requestListener = new SimpleMessageListenerContainer(cf);
            requestListener.setMessageListener(new MessageListenerAdapter(new Handler(responseTemplate)));
            requestListener.setQueues(requestQueue);
            requestListener.afterPropertiesSet();

           /* this.responseListener = new SimpleMessageListenerContainer(cf);
            responseListener.setMessageListener(new MessageListenerAdapter(new Handler(responseTemplate)));
            responseListener.setQueues(responseQueue);
            responseListener.afterPropertiesSet();*/
        }
    }

    @After
    public void destroy() {
        if (requestListener != null && responseListener != null) {
            this.requestListener.stop();
            this.responseListener.stop();
        }
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
            template.convertAndSend("Response message[" + message + "]");
        }
    }
}

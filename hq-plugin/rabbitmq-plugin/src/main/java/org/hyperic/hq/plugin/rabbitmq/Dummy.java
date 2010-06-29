/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author administrator
 */
public class Dummy implements Runnable {

    private static Log log = LogFactory.getLog(Dummy.class);
    RabbitMQProducer p;
    RabbitMQConsumer c;
    String host = "/";

    public Dummy() {
        p = new RabbitMQProducer();
        c = new RabbitMQConsumer();
    }

    public void send() {
        try {
            p.produce();
            c.consume();
        } catch (Exception ex) {
            log.debug(ex.getMessage(), ex);
        }
    }

    public void run() {
        try {
            while (true) {
                send();
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            log.error(ex, ex);
        }
    }

    public class RabbitMQProducer {

        public void produce() throws Exception {
            ConnectionParameters params = new ConnectionParameters();
            params.setUsername("guest");
            params.setPassword("guest");
            params.setVirtualHost(host);
            params.setRequestedHeartbeat(0);
            ConnectionFactory factory = new ConnectionFactory(params);
            Connection conn = factory.newConnection("localhost", 5672);
            Channel channel = conn.createChannel();
            String exchangeName = "myExchange";
            String routingKey = "testRoute";
            int run = 0;
            while (run++ < 6) {
                byte[] messageBodyBytes = ("Hello, world (" + run + ") !").getBytes();
                channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
            }
            channel.close();
            conn.close();
        }
    }

    public class RabbitMQConsumer {

        public void consume() throws Exception {
            ConnectionParameters params = new ConnectionParameters();
            params.setUsername("guest");
            params.setPassword("guest");
            params.setVirtualHost(host);
            params.setRequestedHeartbeat(0);
            ConnectionFactory factory = new ConnectionFactory(params);
            Connection conn = factory.newConnection("127.0.0.1", 5672);
            Channel channel = conn.createChannel();
            String exchangeName = "myExchange";
            String queueName = "myQueue";
            String routingKey = "testRoute";
            boolean durable = true;
            channel.exchangeDeclare(exchangeName, "direct", durable);
            channel.queueDeclare(queueName, durable);
            channel.queueBind(queueName, exchangeName, routingKey);
            boolean noAck = false;
            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, noAck, consumer);
            int run = 0;
            while (run++ < 5) {
                QueueingConsumer.Delivery delivery;
                try {
                    delivery = consumer.nextDelivery();
                } catch (InterruptedException ie) {
                    continue;
                }
                System.out.println("Message received" + new String(delivery.getBody()));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            channel.close();
            conn.close();
        }
    }
}

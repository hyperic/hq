/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
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
    int portNumber = AMQP.PROTOCOL.PORT + 4;

    public Dummy() {
        p = new RabbitMQProducer();
        c = new RabbitMQConsumer();
    }

    public void send() {
        try {
            p.produce();
            c.consume();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
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
            try {
                String hostName = "localhost";
                String exchange = "";
                String routingKey = "SimpleQueue";

                ConnectionFactory cfconn = new ConnectionFactory();
                cfconn.setHost(hostName);
                cfconn.setPort(portNumber);
                Connection conn = cfconn.newConnection();

                Channel ch = conn.createChannel();

                if (exchange.equals("")) {
                    ch.queueDeclare(routingKey, false, false, false, null);
                }
                for (int n = 0; n < 6; n++) {
                    String message = "(" + n + ") the time is " + new java.util.Date().toString();
                    ch.basicPublish(exchange, routingKey, null, message.getBytes());
                    log.debug(" ==> Message: " + message);
                }
                ch.close();
                conn.close();
            } catch (Exception e) {
                System.err.println("Main thread caught exception: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public class RabbitMQConsumer {

        public void consume() throws Exception {
            try {
                String hostName = "localhost";
                String queueName = "SimpleQueue";

                ConnectionFactory connFactory = new ConnectionFactory();
                connFactory.setHost(hostName);
                connFactory.setPort(portNumber);
                Connection conn = connFactory.newConnection();

                final Channel ch = conn.createChannel();

                ch.queueDeclare(queueName, false, false, false, null);

                QueueingConsumer consumer = new QueueingConsumer(ch);
                ch.basicConsume(queueName, consumer);
                for (int n = 0; n < 5; n++) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    log.debug("<==  Message: " + new String(delivery.getBody()));
                    ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            } catch (Exception ex) {
                System.err.println("Main thread caught exception: " + ex);
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }
}

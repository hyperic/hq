package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.connection.ConnectionException;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO secure credentials with connection factory
 * @author Helena Edelson
 */
public class RabbitMessageListener {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Semaphore cancellationLock = new Semaphore(0);

    private final ChannelTemplate channelTemplate;

    private volatile String queueName;

    private volatile String[] workerQueues;

    private ErrorHandler errorHandler;

    private long receiveTimeout = 1000;

    private volatile int concurrentConsumers = 1;

    private volatile Set<Channel> channels = null;

    private volatile Set<QueueingConsumer> consumers;

    private final Object consumersMonitor = new Object();

    private volatile Executor taskExecutor = new SimpleAsyncTaskExecutor();

    private final MessageHandler handler;

    private volatile Connection sharedConnection;

    private AtomicBoolean running = new AtomicBoolean(false);

    private final Object monitor = new Object();

    private volatile int prefetchCount = 10;

    public RabbitMessageListener(ConnectionFactory connectionFactory, Object endpoint, Method method, ErrorHandler errorHandler) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.handler = new InvokingConsumerHandler(connectionFactory, endpoint, method);
        this.queueName = method.getName();
        this.errorHandler = errorHandler;
        initialize();
    }


    @Override
    public String toString() {
        return new StringBuilder("queue=").append(queueName)
                .append(" sharedConnection=").append(sharedConnection).append(" consumers=")
                .append(consumers).append(" messageListener=").append(handler)
                .append(" workerQueues=").append(StringUtils.arrayToCommaDelimitedString(workerQueues)).toString();
    }

    public void setWorkerQueues(String... queues) {
        this.workerQueues = queues;
    }

    public void initialize() {
        Assert.notNull(channelTemplate, "Error creating ChannelTemplate - check the connection factory, credentials, broker.");
        Assert.notNull(errorHandler, "No ErrorHandler has been set.");

        try {
            running.set(true);

            if (sharedConnection == null) {
                sharedConnection = channelTemplate.createConnection();
                logger.debug("established a shared connection " + sharedConnection);
            }
            initializeConsumers();
        }
        catch (Exception e) {
            channelTemplate.closeConnection(sharedConnection);
            sharedConnection = null;
            throw new ConnectionException("Failed to create a shared connection.", e.getCause());
        }
    }

    private void initializeConsumers() throws IOException {
        synchronized (consumersMonitor) {
            if (consumers == null) {
                this.channels = new HashSet<Channel>(concurrentConsumers);
                this.consumers = new HashSet<QueueingConsumer>(concurrentConsumers);

                for (int i = 0; i < concurrentConsumers; i++) {
                    Channel channel = channelTemplate.createChannel();
                    QueueingConsumer consumer = createQueueingConsumer(channel);
                    channels.add(channel);
                    consumers.add(consumer);
                }
                //cancellationLock.release(consumers.size());
            }
        }

        for (QueueingConsumer consumer : consumers) {
            taskExecutor.execute(new ConsumingRunnable(consumer, receiveTimeout));
        }
    }

    /* TODO worker queues */

    private QueueingConsumer createQueueingConsumer(final Channel channel) throws IOException {
        QueueingConsumer consumer = new QueueingConsumer(channel);
        //channel.basicQos(prefetchCount);
        channel.basicConsume(queueName, false, consumer);
        return consumer;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    /**
     * Stop the shared Connection, and close this container.
     * Notify all invoker tasks and stop the shared Connection, if any.
     */
    protected void stop() {
        if (!running.get()) return;

        running.set(false);

        /*try {
            cancellationLock.acquire(consumers.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }*/

        synchronized (consumersMonitor) {
            try {
                for (Channel channel : channels) {
                    channelTemplate.releaseResources(channel);
                }
                channelTemplate.closeConnection(sharedConnection);
            } finally {
                cancellationLock.release(consumers.size());
            }

            sharedConnection = null;
            consumers = null;
            channels = null;
        }
    }


    /**
     * Determine whether this container is currently running,
     * that is, whether it has been started and not stopped yet
     * @return true if running
     */
    private boolean isRunning() {
        return running.get();
    }

    private class ConsumingRunnable implements Runnable {

        private QueueingConsumer consumer;

        private long receiveTimeout;

        public ConsumingRunnable(QueueingConsumer consumer, long receiveTimeout) {
            this.consumer = consumer;
            this.receiveTimeout = receiveTimeout;
        }

        public void run() {
            try {
                while (running.get()) {
                    try {
                        consume();
                    } catch (Exception e) {
                        //continue
                    }
                }
            } catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
            } catch (ShutdownSignalException e) {
                logger.debug("Consumer received ShutdownSignal, processing stopped.");
            } catch (Throwable t) {
                logger.debug("Consumer received fatal exception, processing stopped.", t);
            }
        }

        private void consume() throws Throwable {
            Channel channel = consumer.getChannel();
            QueueingConsumer.Delivery delivery = null;

            try {
                delivery = consumer.nextDelivery();//receiveTimeout
                if (delivery != null && delivery.getBody().length > 0) {
                    /* temporary output */
                    System.out.println("\ndelivery on queue=" + queueName + ": " + new String(delivery.getBody()) + "\n");
                    handle(channel, delivery);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }

            } catch (Throwable t) {
                logger.error("Unable to handle message", t.getCause());
                if (delivery != null) channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
            } finally {
                channel.basicCancel(consumer.getConsumerTag());
                channelTemplate.releaseResources(channel);
            }
        }
    }

    private void handle(Channel channel, QueueingConsumer.Delivery delivery) throws Exception {
        if (!isRunning()) throw new IllegalStateException("rejecting message - listener has stopped.");
        try {
            handler.handle(delivery, channel);
        } catch (Throwable t) {
            handleListenerException(t);
        }
    }

    private void handleListenerException(Throwable e) {
        if (isRunning()) errorHandler.handleError(e);
    }
}
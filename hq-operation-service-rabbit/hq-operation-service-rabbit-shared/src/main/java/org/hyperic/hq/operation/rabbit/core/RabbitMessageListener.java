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

    private final ConnectionFactory connectionFactory;

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

    protected final AtomicBoolean read = new AtomicBoolean(true);

    private final MessageHandler handler;

    private volatile Connection sharedConnection;

    private volatile boolean running = false;

    private final Object monitor = new Object();

    private volatile int prefetchCount = 10;

    public RabbitMessageListener(ConnectionFactory connectionFactory, Object endpoint,
                                          Method method, ErrorHandler errorHandler) {

        this.connectionFactory = connectionFactory;
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.handler = new InvokingConsumerHandler(connectionFactory, endpoint, method);
        this.queueName = extractQueue(method);
        this.errorHandler = errorHandler;
        initialize();
        logger.info("created listener for endpoint=" + endpoint + " to invoke handle method=" + method.getName());
    }


    @Override
    public String toString() {
        return new StringBuilder("connectionFactory=").append(connectionFactory).append(" queueName=").append(queueName)
                .append(" sharedConnection=").append(sharedConnection).append(" consumers=")
                .append(consumers).append(" messageListener=").append(handler)
                .append(" workerQueues=").append(StringUtils.arrayToCommaDelimitedString(workerQueues)).toString();
    }

    public void setWorkerQueues(String... queues) {
        this.workerQueues = queues;
    }

    public void initialize() {
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null.");

        try {
            synchronized (monitor) {
                running = true;
            }

            if (sharedConnection == null) {
                sharedConnection = channelTemplate.createConnection();
                logger.debug("established a shared " + sharedConnection);
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
        synchronized (this.consumersMonitor) {
            if (this.consumers == null) {
                this.channels = new HashSet<Channel>(concurrentConsumers);
                this.consumers = new HashSet<QueueingConsumer>(concurrentConsumers);

                for (int i = 0; i < concurrentConsumers; i++) {
                    Channel channel = channelTemplate.createChannel();
                    QueueingConsumer consumer = createQueueingConsumer(channel);

                    channels.add(channel);
                    consumers.add(consumer);
                }
                cancellationLock.release(consumers.size());
            }
        }

        for (QueueingConsumer consumer : consumers) {
            taskExecutor.execute(new AsyncConsumer(consumer, receiveTimeout));
        }
    }

    protected QueueingConsumer createQueueingConsumer(final Channel channel) throws IOException {
        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicQos(prefetchCount);
        /* TODO workerQueues... */
        channel.basicConsume(queueName, true, consumer);
 
        return consumer;
    }

    public String extractQueue(Method method) {
        String queue = method.getAnnotation(OperationEndpoint.class).queue();
        return hasValue(queue) ? queue : method.getName();
    }

    private boolean hasValue(String entry) {
        return entry != null && entry.length() > 0;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    /**
	 * Stop the shared Connection, and close this container.
     * Notify all invoker tasks and stop the shared Connection, if any.
	 */
    @PreDestroy
	protected void stop() {
        if (!this.isRunning()) return;

		synchronized (monitor) {
			running = false; 
		}

        try {
			cancellationLock.acquire(consumers.size());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		synchronized (consumersMonitor) {
			try {
				for (Channel channel : this.channels) {
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
        synchronized (monitor) {
            return (running);
        }
    }

    private class AsyncConsumer implements Runnable {

        private QueueingConsumer consumer;

        private long receiveTimeout;

        public AsyncConsumer(QueueingConsumer consumer, long receiveTimeout) {
            this.consumer = consumer;
            this.receiveTimeout = receiveTimeout;
        }

        public void run() {

            try {
                cancellationLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                while (isRunning()) {
                    try {
                        consume();
                        read.set(true);
                    } catch (Exception e) {
                        // Continue
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Consumer thread interrupted, processing stopped.");
                Thread.currentThread().interrupt();
            } catch (ShutdownSignalException e) {
                logger.debug("Consumer received ShutdownSignal, processing stopped.");
            } catch (Throwable t) {
                logger.debug("Consumer received fatal exception, processing stopped.", t);
            } finally {
                Channel channel = consumer.getChannel();
                /* TODO
                logger.debug("Closing consumer on " + channel);
                channel.basicCancel(consumer.getConsumerTag());  */

                channelTemplate.releaseResources(channel);
                cancellationLock.release();
            }
        }

        private boolean consume() throws Throwable {
            Channel channel = consumer.getChannel();

            while (read.get()) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(receiveTimeout);
                if (delivery == null) return true;

                logger.debug("consume = " + delivery.getEnvelope().getExchange() + ", " + delivery.getEnvelope().getRoutingKey());

                if (delivery.getBody().length > 0) {
                    invokeListener(channel, delivery);
                    
                    read.set(false);
                }
            }
            read.set(true);
            return true;
        }
    }

    private void invokeListener(Channel channel, QueueingConsumer.Delivery delivery) throws Exception {
        if (!isRunning()) throw new IllegalStateException("rejecting message - listener has stopped: " + delivery.getEnvelope());

        try {
            handler.handle(delivery, channel);
        }
        catch (Throwable t) {
            handleListenerException(t);
        }
    }

    private void handleListenerException(Throwable e) {
        if (isRunning()) {
            if (errorHandler != null) {
                errorHandler.handleError(e);
            } else {
                logger.warn("Execution failed and no ErrorHandler has been set.", e);
            }
        } else {
            logger.debug("Listener exception after container shutdown", e);
        }
    }
}
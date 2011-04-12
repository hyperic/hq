


package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Helena Edelson
 */
public class ListenerContainer {

    private final ConnectionFactory connectionFactory;

    private final int concurrentConsumers;

    private final TaskExecutor taskExecutor;

    private final ConsumerCallbackFactory consumingCallbackFactory;

    private final List<ConsumerCallback> callbacks = new ArrayList<ConsumerCallback>();

    private final Object monitor = new Object();


    public ListenerContainer(ConnectionFactory connectionFactory, int concurrentConsumers, TaskExecutor taskExecutor,
        ConsumerCallbackFactory consumingCallbackFactory) {
        this.connectionFactory = connectionFactory;
        this.concurrentConsumers = concurrentConsumers;
        this.taskExecutor = taskExecutor;
        this.consumingCallbackFactory = consumingCallbackFactory;
    }

    //@PostConstruct
    void startConsumers() {
        synchronized (this.monitor) {
            for (int i = 0; i < this.concurrentConsumers; i++) {
                ConsumerCallback consumingCallback = this.consumingCallbackFactory.create();
                this.callbacks.add(consumingCallback);
                this.taskExecutor.execute(new ConsumingRunnable(this.connectionFactory, consumingCallback));
            }
        }
    }

    //@PreDestroy
    void stopConsumers() {
        synchronized (this.monitor) {
            for (ConsumerCallback callback : this.callbacks) {
                callback.stop();
            }
        }
    }

    private static final class ConsumingRunnable implements Runnable {

        //private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final ChannelTemplate channelTemplate;

        private final ChannelCallback<?> channelCallback;

        ConsumingRunnable(ConnectionFactory connectionFactory, ChannelCallback<?> channelCallback) {
            this.channelTemplate = new ChannelTemplate(connectionFactory);
            this.channelCallback = channelCallback;
        }

        public void run() {
            try {
                this.channelTemplate.execute(this.channelCallback);
            } catch (Exception e) {
                //this.logger.error(e.getMessage());
            }
        }
    }
}

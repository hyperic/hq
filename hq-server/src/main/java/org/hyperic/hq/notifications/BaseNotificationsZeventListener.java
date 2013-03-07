package org.hyperic.hq.notifications;

import java.util.List;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public abstract class BaseNotificationsZeventListener<E extends Zevent,N extends BaseNotification>
implements ZeventListener<E> {
    private static final int BATCH_SIZE = 10000;
    @Autowired
    protected ConcurrentStatsCollector concurrentStatsCollector;
    @Autowired
    protected ZeventEnqueuer zEventManager;
    @Autowired
    protected EndpointQueue endpointQueue;
    @Autowired
    private ThreadPoolTaskScheduler notificationFilterExecutor;

    public abstract String getListenersBeanName();
    public abstract String getConcurrentStatsCollectorType();
    /**
     * @param events
     * @return data which is needed for filtering, extracted per the events data
     */
    public abstract List<N> extract(List<E> events);
    public abstract DestinationEvaluator<N> getEvaluator();
    
    public void processEvents(final List<E> events) {
        if (endpointQueue.getNumConsumers() == 0) {
            return;
        }
        // execute in batches to avoid session bloat.
        // to increase the throughput of this mechanism we could add more threads to the notificationFilterExecutor
        // bean.  Making it configurable would be nice too.
        final int size = events.size();
        for (int i=0; i<size; i+=BATCH_SIZE) {
            final int end = Math.min(size, i+BATCH_SIZE);
            @SuppressWarnings("unchecked")
            final NotificationFilterExecutorRunner<E,N> runner = Bootstrap.getBean(NotificationFilterExecutorRunner.class);
            runner.setEvents(events.subList(i, end));
            runner.setListener(this);
            notificationFilterExecutor.execute(runner);
        }
    }

}
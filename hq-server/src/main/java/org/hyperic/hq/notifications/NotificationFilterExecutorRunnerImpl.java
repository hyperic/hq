package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Scope("prototype")
public class NotificationFilterExecutorRunnerImpl<E extends Zevent,N extends BaseNotification>
implements NotificationFilterExecutorRunner<E,N> {
    
    private static final Log log = LogFactory.getLog(NotificationFilterExecutorRunnerImpl.class);
    
    @Autowired
    private ConcurrentStatsCollector concurrentStatsCollector;
    @Autowired
    private EndpointQueue endpointQueue;
    private List<E> events;
    private BaseNotificationsZeventListener<E, N> listener;

    @Transactional(readOnly=true)
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug(listener.getListenersBeanName() + " got events:\n" + events);
        }
        final long start = System.currentTimeMillis();
        final List<N> ns = listener.extract(events);
        final Map<NotificationEndpoint, Collection<N>> msgs = listener.getEvaluator().evaluate(ns);
        final long end = System.currentTimeMillis();
        concurrentStatsCollector.addStat(end-start, listener.getConcurrentStatsCollectorType());
        endpointQueue.publishAsync(msgs);
        if (log.isDebugEnabled()) {
            if (msgs==null) {
                log.debug(listener.getListenersBeanName() + " did not publish any notifications");
            } else {
                log.debug(listener.getListenersBeanName() + " published:\n" + msgs);
            }
        }
    }

    public void setEvents(List<E> events) {
        this.events = events;
    }
   
    public void setListener(BaseNotificationsZeventListener<E, N> listener) {
        this.listener = listener;
    }

}

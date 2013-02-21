package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseNotificationsZeventListener<E extends Zevent,N extends BaseNotification> implements ZeventListener<E> {
    private final Log log = LogFactory.getLog(BaseNotificationsZeventListener.class);
    @Autowired
    protected ConcurrentStatsCollector concurrentStatsCollector;
    @Autowired
    protected ZeventEnqueuer zEventManager;
    @Autowired
    protected EndpointQueue endpointQueue;

    protected abstract String getListenersBeanName();
    protected abstract String getConcurrentStatsCollectorType();
    /**
     * @param events
     * @return data which is needed for filtering, extracted per the events data
     */
    protected abstract List<N> extract(List<E> events);
    protected abstract DestinationEvaluator<N> getEvaluator();
    
    @Transactional(readOnly = true)
    public void processEvents(List<E> events) {
        if (log.isDebugEnabled()) {
            log.debug(getListenersBeanName() + " got events:\n" + events);
        }
        final long start = System.currentTimeMillis();
        final List<N> ns = extract(events);
        final Map<NotificationEndpoint, Collection<N>> msgs = getEvaluator().evaluate(ns);
        final long end = System.currentTimeMillis();
        concurrentStatsCollector.addStat(end-start, getConcurrentStatsCollectorType());
        endpointQueue.publishAsync(msgs);
        if (log.isDebugEnabled()) {
            if (msgs==null) {
                log.debug(getListenersBeanName() + " did not publish any notifications");
            } else {
                log.debug(getListenersBeanName() + " published:\n" + msgs);
            }
        }
    }
}

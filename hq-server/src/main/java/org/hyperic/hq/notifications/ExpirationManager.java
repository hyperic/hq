package org.hyperic.hq.notifications;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yakarn
 */
@Component
public class ExpirationManager {
    protected final static long EXPIRATION_DURATION = /*10*60*/10*1000;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    @Autowired
    MetricDestinationEvaluator metricEvaluator;
    @Autowired
    ResourceDestinationEvaluator resourceEvaluator;
    EndpointQueue endpointQueue;
    protected Map<NotificationEndpoint,ScheduledFuture<?>> expirationCandidates = new ConcurrentHashMap<NotificationEndpoint,ScheduledFuture<?>>();    
    
    @PostConstruct
    public void init() {
        endpointQueue = (EndpointQueue) Bootstrap.getBean("endpointQueue");
        Bootstrap.getBean(EndpointQueue.class);
    }
    /**
     * start a countdown process regarding the given endpoint
     * @param endpoint
     */
    public void startExpiration(final NotificationEndpoint endpoint) {
        if (this.expirationCandidates.containsKey(endpoint)) {
            return;
        }
        ScheduledFuture<?> future = executor.schedule(new Runnable() {
            public void run() {
                endpointQueue.unregister(endpoint.getRegistrationId());
                metricEvaluator.unregisterAll(endpoint);
                resourceEvaluator.unregisterAll(endpoint);
                expirationCandidates.remove(endpoint);
            }            
        },ExpirationManager.EXPIRATION_DURATION,TimeUnit.MILLISECONDS);
        this.expirationCandidates.put(endpoint,future);
    }
    public void abortExpiration(NotificationEndpoint endpoint) {
        ScheduledFuture<?> future = this.expirationCandidates.get(endpoint);
        if (future==null) {
            return;
        }
        if (future.cancel(false)) {
            this.expirationCandidates.remove(endpoint);
        }
    }
}
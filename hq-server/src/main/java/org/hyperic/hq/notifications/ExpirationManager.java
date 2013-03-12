package org.hyperic.hq.notifications;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yakarn
 * 
 * following are the assumptions I relied upon when I wrote this class:
 * 1. concurrency - only one thread will deal (expire/abort expiration of) 
 *    with a certain registration at a time
 * 2. a failure sending a notification would cause it to be added again to
 *    the endpoint queue of the relevant registration,
 *    hence, it is reasonable to assume that unless the abortExpiration method would be called,
 *    the next failure which will exceed the expiration duration taken from the 1st expiration, 
 *    would trigger the expiration of the relevant registration
 */
@Component
public class ExpirationManager {
    protected final static long EXPIRATION_DURATION = /*10*60*/10*1000;
    @Autowired
    MetricDestinationEvaluator metricEvaluator;
    @Autowired
    ResourceDestinationEvaluator resourceEvaluator;
    EndpointQueue endpointQueue;
    protected Map<NotificationEndpoint,Long> expirationCandidates = new ConcurrentHashMap<NotificationEndpoint,Long>();    
    
    @PostConstruct
    public void init() {
        endpointQueue = (EndpointQueue) Bootstrap.getBean("endpointQueue");
    }
    /**
     * start a countdown process regarding the given endpoint
     * @param endpoint
     */
    public void startExpiration(final NotificationEndpoint endpoint, final long sendingTime) {
        Long firstFailureTime = this.expirationCandidates.get(endpoint);
        if (firstFailureTime==null) {
            this.expirationCandidates.put(endpoint,new Long(sendingTime));
        } else if (sendingTime-firstFailureTime>=ExpirationManager.EXPIRATION_DURATION) {
            this.endpointQueue.unregister(endpoint.getRegistrationId());
            this.metricEvaluator.unregisterAll(endpoint);
            this.resourceEvaluator.unregisterAll(endpoint);
            this.expirationCandidates.remove(endpoint);
        }
    }
    public void abortExpiration(NotificationEndpoint endpoint) {
        this.expirationCandidates.remove(endpoint);
    }
}
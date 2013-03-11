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
    protected final static long EXPIRATION_DURATION = /*10*60*/30*1000;
    @Autowired
    private ScheduledThreadPoolExecutor executor;
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

    public void addExpiration(final NotificationEndpoint endpoint) {
        if (this.expirationCandidates.containsKey(endpoint)) {
            return;
        }
        ScheduledFuture<?> future = executor.schedule(new Runnable() {
            public void run() {
                expire(endpoint);
                expirationCandidates.remove(endpoint);
            }            
        },ExpirationManager.EXPIRATION_DURATION,TimeUnit.MILLISECONDS);
        this.expirationCandidates.put(endpoint,future);
    }
    public void removeExpiration(NotificationEndpoint endpoint) {
        ScheduledFuture<?> future = this.expirationCandidates.get(endpoint);
        if (future==null) {
            return;
        }
        if (future.cancel(false)) {
            this.expirationCandidates.remove(endpoint);
        }
    }

    protected void expire(NotificationEndpoint expirationCandidate) {
        this.endpointQueue.unregister(expirationCandidate.getRegistrationId());
        this.metricEvaluator.unregisterAll(expirationCandidate);
        this.resourceEvaluator.unregisterAll(expirationCandidate);
    }
    
    public static void main(String[] args) throws Throwable {
        /*final Long l0 = new Long(3);
        final AtomicReference<Long> l = new AtomicReference<Long>(l0);
        
        Runnable r0= new Runnable() {
            public void run() {
                synchronized (l0) {
                    int i=0;
                    while(true) {
                        i=1;
//                        System.out.println(".");
                    }
                }
            }
        };
        Runnable r1= new Runnable() {
            public void run() {
                l.set(new Long(4));
                System.out.println(l);
            }
        };
        new Thread(r0).start();
        Thread.sleep(1000*2);
        new Thread(r1).start();
        */
        
        ScheduledThreadPoolExecutor e = new ScheduledThreadPoolExecutor(1);
        ScheduledFuture<?> future = e.schedule(new Runnable() {
            public void run() {
                System.out.println("A");
                while(true) {}
            }
        }, 5, TimeUnit.SECONDS);
        System.out.println();
//        e.schedule(new Runnable() {
//            public void run() {
//                System.out.println("B");
//            }
//        }, 1, TimeUnit.SECONDS);
    }//EOM 
}
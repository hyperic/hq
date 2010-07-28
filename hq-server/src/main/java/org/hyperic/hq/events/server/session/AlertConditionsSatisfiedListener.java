package org.hyperic.hq.events.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Receives AlertConditionSatisfiedZEvents and forwards them to the AlertManager
 * for processing
 * @author jhickey
 * 
 */
@Component
public class AlertConditionsSatisfiedListener implements ZeventListener<AlertConditionsSatisfiedZEvent> {
    private AlertManager alertManager;
    private ZeventEnqueuer zEventManager;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private static final int MAX_RETRIES = 3;
    private final Log log = LogFactory.getLog(AlertConditionsSatisfiedListener.class);

    @Autowired
    public AlertConditionsSatisfiedListener(AlertManager alertManager, ZeventEnqueuer zEventManager, ConcurrentStatsCollector concurrentStatsCollector) {
        this.alertManager = alertManager;
        this.zEventManager = zEventManager;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void subscribe() {
        zEventManager.registerEventClass(AlertConditionsSatisfiedZEvent.class);
        Set<Class<?>> alertEvents = new HashSet<Class<?>>();
        alertEvents.add(AlertConditionsSatisfiedZEvent.class);
        zEventManager.addBufferedListener(alertEvents, this);
        concurrentStatsCollector.register(ConcurrentStatsCollector.FIRED_ALERT_TIME);
    }

    public void processEvents(List<AlertConditionsSatisfiedZEvent> events) {
        final long start = System.currentTimeMillis();
        for (AlertConditionsSatisfiedZEvent z : events) {
             // HQ-1905 need to retry due to potential StaleStateExceptions
             for (int i=0; i<MAX_RETRIES; i++) {
                 try {
                     alertManager.fireAlert(z);
                     break;
                 } catch (OptimisticLockingFailureException e) {
                     if ((i+1) < MAX_RETRIES) {
                         String times = (MAX_RETRIES - i == 1) ? "time" : "times";
                         log.warn("Warning, exception occurred while running fireAlert.  will retry "
                                                    + (MAX_RETRIES - (i+1)) + " more " + times + ".  errorMsg: " + e);
                         continue;
                     } else {
                         log.error("fireAlert threw an Exception, will not be retried",e);
                         break;
                     }
                 }catch(Throwable t) {
                     log.error("fireAlert threw an Exception, will not be retried",t);
                     break;
                 }
             }
        }
       
        concurrentStatsCollector.addStat(System.currentTimeMillis()-start, ConcurrentStatsCollector.FIRED_ALERT_TIME);
    }
}

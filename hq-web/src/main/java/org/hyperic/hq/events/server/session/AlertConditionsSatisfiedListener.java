package org.hyperic.hq.events.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public AlertConditionsSatisfiedListener(AlertManager alertManager, ZeventEnqueuer zEventManager) {
        this.alertManager = alertManager;
        this.zEventManager = zEventManager;
    }
    
    @PostConstruct
    public void subscribe() {
        zEventManager.registerEventClass(AlertConditionsSatisfiedZEvent.class);
        Set<Class<?>> alertEvents = new HashSet<Class<?>>();
        alertEvents.add(AlertConditionsSatisfiedZEvent.class);
        zEventManager.addBufferedListener(alertEvents, this);
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.FIRED_ALERT_TIME);
    }

    public void processEvents(List<AlertConditionsSatisfiedZEvent> events) {
        final long start = System.currentTimeMillis();
        for (AlertConditionsSatisfiedZEvent z : events) {
            alertManager.fireAlert(z);
        }
       
        ConcurrentStatsCollector.getInstance().addStat(
            System.currentTimeMillis()-start, ConcurrentStatsCollector.FIRED_ALERT_TIME);
    }

}

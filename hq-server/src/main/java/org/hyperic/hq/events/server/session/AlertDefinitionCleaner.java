package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AlertDefinitionCleaner implements ZeventListener<ResourceZevent> {
    private AlertDefinitionManager alertDefinitionManager;
    private ZeventEnqueuer zEventManager;
    private final Log log = LogFactory.getLog(AlertDefinitionCleaner.class);
    
    @Autowired
    public AlertDefinitionCleaner(AlertDefinitionManager alertDefinitionManager,
                                  ZeventEnqueuer zEventManager) {
        this.alertDefinitionManager = alertDefinitionManager;
        this.zEventManager = zEventManager;
    }
    
    @PostConstruct
    public void registerListener() {
        HashSet<Class<ResourceDeletedZevent>> events = new HashSet<Class<ResourceDeletedZevent>>();
        events.add(ResourceDeletedZevent.class);
        zEventManager.addBufferedListener(events,this);
    }
    
    public void processEvents(List<ResourceZevent> events) {
        final List<Integer> alertDefs = alertDefinitionManager.getAllDeletedAlertDefs();
        final int batchSize = 500;
        try {
            final int size = alertDefs.size();
            for (int i=0; i< size; i+=batchSize) {
                final int end = Math.min(size, i+batchSize);
                final List<Integer> defIds = alertDefs.subList(i, end);
                final StopWatch watch = new StopWatch();
                final boolean debug = log.isDebugEnabled();
                if (defIds.size() == 0) {
                    continue;
                }
                if (debug) watch.markTimeBegin("cleanupAlertDefs");
                alertDefinitionManager.cleanupAlertDefs(defIds);
                if (debug) watch.markTimeEnd("cleanupAlertDefs");
                if (debug) log.debug(watch);
            }
        }catch(Exception e) {
            log.error(e,e);
        }
       
    }

    public String toString() {
        return "AlertDefCleanupListener";
    }

}

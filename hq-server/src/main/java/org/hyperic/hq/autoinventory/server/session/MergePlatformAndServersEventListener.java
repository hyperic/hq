package org.hyperic.hq.autoinventory.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MergePlatformAndServersEventListener implements ZeventListener<MergePlatformAndServersZevent> {

    private RuntimePlatformAndServerMerger runtimePlatformAndServerMerger;
    private ZeventEnqueuer zeventManager;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private static final String RUNTIME_PLATFORM_AND_SERVER_MERGER = ConcurrentStatsCollector.RUNTIME_PLATFORM_AND_SERVER_MERGER;
    private final Log log = LogFactory.getLog(MergePlatformAndServersEventListener.class);
    
    @Autowired
    public MergePlatformAndServersEventListener(RuntimePlatformAndServerMerger runtimePlatformAndServerMerger,
                                                ZeventEnqueuer zeventManager, ConcurrentStatsCollector concurrentStatsCollector) {
        this.runtimePlatformAndServerMerger = runtimePlatformAndServerMerger;
        this.zeventManager = zeventManager;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }

    @PostConstruct
    public void subscribeForEvents() {
    	concurrentStatsCollector.register(ConcurrentStatsCollector.RUNTIME_PLATFORM_AND_SERVER_MERGER);
        Set<Class<?>>events = new HashSet<Class<?>>();
        events.add(MergePlatformAndServersZevent.class);
        zeventManager.addBufferedListener(events, this);
    }
    
    public void processEvents(List<MergePlatformAndServersZevent> events) {
        for (MergePlatformAndServersZevent event : events) {
            try {
            	concurrentStatsCollector.addStat(1, RUNTIME_PLATFORM_AND_SERVER_MERGER);
                runtimePlatformAndServerMerger.reportAIRuntimeReport(event.getAgentToken(), event.getCrrr());
            } catch (Exception e) {
                log.error("Error merging platform and servers with " + "agentToken=" + event.getAgentToken(), e);
            }
        }
    }

    
}

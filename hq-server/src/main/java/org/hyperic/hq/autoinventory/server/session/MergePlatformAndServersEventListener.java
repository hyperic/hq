/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.autoinventory.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
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
        Set<Class<? extends Zevent>>events = new HashSet<Class<? extends Zevent>>();
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

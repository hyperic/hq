/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.AgentConnections;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.ReportStatsCollector;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

@Service
@ManagedResource("hyperic.jmx:name=HQInternal")
public class HQInternalService implements HQInternalServiceMBean {

    private Log log = LogFactory.getLog(HQInternalService.class);
    private AgentManager agentManager;
    private MeasurementManager measurementManager;
    private PlatformManager platformManager;
    private ZeventEnqueuer zEventManager;
    private ReportStatsCollector reportStatsCollector;

    @Autowired
    public HQInternalService(AgentManager agentManager, MeasurementManager measurementManager,
                             PlatformManager platformManager, ZeventEnqueuer zEventManager,
                             ReportStatsCollector reportStatsCollector) {
        this.agentManager = agentManager;
        this.measurementManager = measurementManager;
        this.platformManager = platformManager;
        this.zEventManager = zEventManager;
        this.reportStatsCollector = reportStatsCollector;
    }

    public double getMetricInsertsPerMinute() {
        double val = reportStatsCollector.getCollector().valPerTimestamp();

        return val * 1000.0 * 60.0;
    }
    
    public int getAgentCount() {
        final ClassLoader cl = agentManager.getClass().getClassLoader();
        final AtomicInteger atInt = new AtomicInteger(-1);
        final AgentManager aMan = agentManager;
        final Runnable runner = new Runnable() {
           
            public void run() {
                atInt.set(aMan.getAgentCountUsed());
            }
        };
        runInContext(runner, cl);
        return atInt.get();
    }

    public double getMetricsCollectedPerMinute() {
        final ClassLoader cl = agentManager.getClass().getClassLoader();
        final List<CollectionSummary> vals = new ArrayList<CollectionSummary>();
        final Runnable runner = new Runnable() {
   
            public void run() {
                vals.addAll(measurementManager.findMetricCountSummaries());
            }
        };
        double total = -1.0;
        runInContext(runner, cl);
        total = 0.0;
        for (CollectionSummary s : vals) {
            int interval = s.getInterval();
            if (interval == 0) {
                continue;
            }
            total += (float) s.getTotal() / (float) interval;
        }
        return total;
    }

    public int getPlatformCount() {
        final ClassLoader cl = agentManager.getClass().getClassLoader();
        final AtomicInteger atInt = new AtomicInteger(-1);
        final PlatformManager pMan = platformManager;
        final Runnable runner = new Runnable() {
            public void run() {
                atInt.set(pMan.getPlatformCount().intValue());
            }
        };
        runInContext(runner, cl);
        return atInt.get();
    }
    
    // HE-394, need to run in the context that our web container is bound to
    // based on http://opensource.atlassian.com/projects/hibernate/browse/HHH-3529
    // when we upgrade to hibernate 3.5 this may not be necessary
    private void runInContext(Runnable runner, ClassLoader cl) {
        Thread thread = new Thread(runner);
        thread.setContextClassLoader(cl);
        thread.start();
        try {
            // 5 min timeout - should not take more than 5 mins to grab a value
            final StopWatch watch = new StopWatch();
            final long timeout = 5*60*1000;
            thread.join(timeout);
            if (watch.getElapsed() >= timeout) {
                log.warn("timeout when running job in a class loader context.  This could mean that the db is hung " + watch);
            }
        } catch (InterruptedException e) {
            log.error(e,e);
        }
    }

    public long getAgentRequests() {
        return AgentConnections.getInstance().getTotalConnections();
    }

    public int getAgentConnections() {
        return AgentConnections.getInstance().getNumConnected();
    }

    public long getZeventMaxWait() {
        return zEventManager.getMaxTimeInQueue();
    }

    public long getZeventsProcessed() {
        return zEventManager.getZeventsProcessed();
    }

    public long getZeventQueueSize() {
        return zEventManager.getQueueSize();
    }
}

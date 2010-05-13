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

package org.hyperic.hq.ha.server.session;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.server.session.AgentAIScanService;
import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.ha.HAService;
import org.hyperic.hq.measurement.server.session.AvailabilityCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.MethodInvokingRunnable;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link HAService} that gets started if we are in the .org
 * HQ (HA not available) or by the master node of an HQ EE cluster
 * implementation
 * @author jhickey
 * 
 */
@Service("haService")
public class HAServiceImpl implements HAService {
    private final Log log = LogFactory.getLog(HAServiceImpl.class);
    private TaskScheduler scheduler;
    private AvailabilityCheckService availabilityCheckService;
    private AgentAIScanService agentAIScanService;
    private ScheduledFuture<?> backfillTask;
    private ScheduledFuture<?> notifyAgentsTask;
    private RegisteredTriggerManager registeredTriggerManager;
   

    @Autowired
    public HAServiceImpl(TaskScheduler scheduler,
                         AvailabilityCheckService availabilityCheckService,
                         AgentAIScanService agentAIScanService,
                         RegisteredTriggerManager registeredTriggerManager) {
        this.scheduler = scheduler;
        this.availabilityCheckService = availabilityCheckService;
        this.agentAIScanService = agentAIScanService;
        this.registeredTriggerManager = registeredTriggerManager;
    }

    /**
     * These services can only run on a master node when HA is used, so they
     * have to be started programmatically once we know if HA is enabled
     */
    public void start() {
        initializeTriggers();
        // If this node was designated as a slave b/c of connectivity loss (not
        // server crash), then we don't want to schedule another round of tasks
        // once it becomes master again
        if (this.backfillTask == null) {
            MethodInvokingRunnable backfill = new MethodInvokingRunnable();
            backfill.setTargetObject(availabilityCheckService);
            backfill.setTargetMethod("backfill");
            try {
                backfill.prepare();
                this.backfillTask = scheduler.scheduleAtFixedRate(backfill, 120000);
            } catch (Exception e) {
                log.error("Unable to schedule availability backfill.", e);
            }
        }
        if (this.notifyAgentsTask == null) {
            MethodInvokingRunnable notifyAgents = new MethodInvokingRunnable();
            notifyAgents.setTargetObject(agentAIScanService);
            notifyAgents.setTargetMethod("notifyAgents");
            try {
                notifyAgents.prepare();
                this.notifyAgentsTask = scheduler.scheduleAtFixedRate(notifyAgents, 1800000);
            } catch (Exception e) {
                log.error("Unable to schedule agent AI scan.", e);
            }
        }
    }
    
    private void initializeTriggers() {
        //Asynchronously initialize triggers once (as this may be an expensive operation)
        MethodInvokingRunnable initTriggers = new MethodInvokingRunnable();
        initTriggers.setTargetObject(registeredTriggerManager);
        initTriggers.setTargetMethod("initializeTriggers");
        try {
            initTriggers.prepare();
            scheduler.schedule(initTriggers, new Date(System.currentTimeMillis() + 1000));
        } catch (Exception e) {
                log.error("Unable to intialize triggers", e);
         }
    }

    public boolean isMasterNode() {
        // HA not implemented in .org
        return true;
    }

    public void stop() {
        if (backfillTask != null) {
            backfillTask.cancel(false);
            this.backfillTask = null;
        }
        if (notifyAgentsTask != null) {
            notifyAgentsTask.cancel(false);
            this.notifyAgentsTask = null;
        }
    }

}

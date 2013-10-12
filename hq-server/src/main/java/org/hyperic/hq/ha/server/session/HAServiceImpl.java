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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.ha.HAService;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.AvailabilityCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
   
    private ScheduledFuture<?> backfillTask;
   
    private RegisteredTriggerManager registeredTriggerManager;
    private final AtomicBoolean triggersHaveInitialized = new AtomicBoolean(false);
    private final Thread triggerInitThread = new Thread() {
        public void run() {
            final long start = System.currentTimeMillis();
            registeredTriggerManager.initializeTriggers();
            final long finish = System.currentTimeMillis();
            triggersHaveInitialized.set(true);
            float elapsed = (finish-start)/1000/60;
            log.info("Trigger initialization completed in " + elapsed + " minutes");
            //Schedule backfill after triggers have been initialized
            if (backfillTask == null) {
                MethodInvokingRunnable backfill = new MethodInvokingRunnable();
                backfill.setTargetObject(availabilityCheckService);
                backfill.setTargetMethod("backfillPlatformAvailability");
                try {
                    backfill.prepare();
                    final long twoMins = 2 * MeasurementConstants.MINUTE;
                    backfillTask = scheduler.scheduleWithFixedDelay(backfill, new Date(System.currentTimeMillis()+twoMins), twoMins);
                } catch (Exception e) {
                    log.error("Unable to schedule availability backfill.", e);
                }
            }
        }
    };

    @Autowired
    public HAServiceImpl(@Value("#{scheduler}")TaskScheduler scheduler,
                         AvailabilityCheckService availabilityCheckService,
                         RegisteredTriggerManager registeredTriggerManager) {
        this.scheduler = scheduler;
        this.availabilityCheckService = availabilityCheckService;
        this.registeredTriggerManager = registeredTriggerManager;
    }

    /**
     * These services can only run on a master node when HA is used, so they
     * have to be started programmatically once we know if HA is enabled
     */
    
    public void start() {
        triggerInitThread.start();
    }
    
    
    public boolean alertTriggersHaveInitialized() {
        return triggersHaveInitialized.get();
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
    }

}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.NewResourceVerifiedZevent;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
@Component
public class MeasurementEnabler implements ZeventListener<ResourceZevent> {
    private final Log log = LogFactory.getLog(MeasurementEnabler.class.getName());
    private MeasurementManager measurementManager;
    private ZeventEnqueuer zEventManager;
    private TransactionRetry transactionRetry;
    private ThreadPoolTaskScheduler measurementEnablerExecutor;
    
    @Autowired
    public MeasurementEnabler(MeasurementManager measurementManager, ZeventEnqueuer zEventManager,
                              TransactionRetry transactionRetry, ThreadPoolTaskScheduler measurementEnablerExecutor) {
        this.measurementManager = measurementManager;
        this.zEventManager = zEventManager;
        this.transactionRetry = transactionRetry;
        this.measurementEnablerExecutor = measurementEnablerExecutor;
    }
    
    @PostConstruct
    public void subscribe() {
        /**
         * Add measurement enabler listener to enable metrics for newly created
         * resources or to reschedule when resources are updated.
         */
        Set<Class<? extends Zevent>> listenEvents = new HashSet<Class<? extends Zevent>>();
        listenEvents.add(ResourceCreatedZevent.class);
        listenEvents.add(ResourceUpdatedZevent.class);
        listenEvents.add(ResourceRefreshZevent.class);
        listenEvents.add(NewResourceVerifiedZevent.class);
        zEventManager.addBufferedListener(listenEvents, this);
    }

    public void processEvents(final List<ResourceZevent> e) {
        if (log.isDebugEnabled()) {
            log.debug("handling refresh event list size=" + e.size());
            log.debug("handling events: " + e);
        }
        final Runnable runner = new Runnable() {
            public void run() {
                measurementManager.handleCreateRefreshEvents(e);
            }
            @Override
            public String toString() {
                return "MeasurementEnabler.Runner";
            }
        };
        final Runnable toExecute = new Runnable() {
            public void run() {
                transactionRetry.runTransaction(runner, 5, 1000);
            }
        };
        measurementEnablerExecutor.execute(toExecute);
    }
    
    public String toString() {
        return "MeasurementEnabler";
    }
}

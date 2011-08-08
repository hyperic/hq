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
import org.hyperic.hq.zevents.Zevent;
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
        Set<Class<? extends Zevent>> alertEvents = new HashSet<Class<? extends Zevent>>();
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

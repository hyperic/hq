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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.zevents.Zevent;
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
        HashSet<Class<? extends Zevent>> events = new HashSet<Class<? extends Zevent>>();
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

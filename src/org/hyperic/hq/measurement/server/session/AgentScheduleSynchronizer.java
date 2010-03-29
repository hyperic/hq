/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
public class AgentScheduleSynchronizer {
    
    private static final Log _log = LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private static AgentScheduleSynchronizer SINGLETON = new AgentScheduleSynchronizer();
    
    public static AgentScheduleSynchronizer getInstance() {
        return AgentScheduleSynchronizer.SINGLETON;
    }

    void initialize() {
        ZeventListener l = new ZeventListener() {
            public void processEvents(List events) {
                final MeasurementProcessorLocal mProc = MeasurementProcessorEJBImpl.getOne();
                final List toSchedule = new ArrayList(events.size());
                final Map unscheduleMap = new HashMap(events.size());
                final boolean debug = _log.isDebugEnabled();
                for (final Iterator i=events.iterator(); i.hasNext(); ) {
                    final Zevent z = (Zevent)i.next();
                    if (z instanceof AgentScheduleSyncZevent) {
                        AgentScheduleSyncZevent event = (AgentScheduleSyncZevent) z;
                        toSchedule.addAll(event.getEntityIds());
                        if (debug) _log.debug("Schduling eids=[" + event.getEntityIds() + "]");
                    } else if (z instanceof AgentUnscheduleZevent) {
                        AgentUnscheduleZevent event = (AgentUnscheduleZevent) z;
                        String token = event.getAgentToken();
                        if (token == null) {
                            continue;
                        }
                        Collection tmp;
                        if (null == (tmp = (Collection) unscheduleMap.get(token))) {
                            tmp = new HashSet();
                            unscheduleMap.put(token, tmp);
                        }
                        tmp.addAll(event.getEntityIds());
                        if (debug) _log.debug("Unschduling eids=[" + event.getEntityIds() + "]");
                    }
                }
                if (!unscheduleMap.isEmpty()) {
                    for (Iterator it=unscheduleMap.entrySet().iterator(); it.hasNext();) {
                        Entry entry = (Entry) it.next();
                        String token = (String) entry.getKey();
                        List list = new ArrayList((Collection) entry.getValue());
                        try {
                            mProc.unschedule(token, list);
                        } catch (MeasurementUnscheduleException e) {
                            _log.error(e,e);
                        }
                    }
                }
                if (!toSchedule.isEmpty()) {
                    mProc.scheduleSynchronous(toSchedule);
                }
            }
            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };
        ZeventManager.getInstance().addBufferedListener(AgentScheduleSyncZevent.class, l);
        ZeventManager.getInstance().addBufferedListener(AgentUnscheduleZevent.class, l);
    }

}

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

package org.hyperic.hq.autoinventory.server.session;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;

public class RuntimePlatformAndServerMerger implements ZeventListener {
    
    private final Log _log =
        LogFactory.getLog(RuntimePlatformAndServerMerger.class);
    private static final ConcurrentStatsCollector _stats =
        ConcurrentStatsCollector.getInstance();
    private static final String RUNTIME_PLATFORM_AND_SERVER_MERGER =
        ConcurrentStatsCollector.RUNTIME_PLATFORM_AND_SERVER_MERGER;

    public void processEvents(List events) {
        AutoinventoryManager aMan = AutoinventoryManagerImpl.getOne();
        for (Iterator it=events.iterator(); it.hasNext(); ) {
            MergePlatformAndServersZevent event =
                (MergePlatformAndServersZevent)it.next();
            try {
                _stats.addStat(1, RUNTIME_PLATFORM_AND_SERVER_MERGER);
                aMan._reportAIRuntimeReport(event.getAgentToken(), event.getCrrr());
            } catch (Exception e) {
                _log.error("Error merging platform and servers with " +
                           "agentToken=" + event.getAgentToken(), e);
            }
        }
    }
    
    static void schedulePlatformAndServerMerges(
        String agentToken, CompositeRuntimeResourceReport crrr) {
        MergePlatformAndServersZevent event =
            new MergePlatformAndServersZevent(agentToken, crrr);
        ZeventManager.getInstance().enqueueEventAfterCommit(event);
    }

    public String toString() {
        return "RuntimePlatformAndServerMerger";
    }
}

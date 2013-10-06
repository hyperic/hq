/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

/**
 * This event is used within the measurement subsytem by the 
 * {@link AgentScheduleSynchronizer} to queue syncing agent's schedules.
 */
public class AgentScheduleSyncZevent extends Zevent {
    private static final Log log = LogFactory.getLog(AgentScheduleSyncZevent.class);

    static {
        ZeventManager.getInstance()
            .registerEventClass(AgentScheduleSyncZevent.class);
    }
    protected final static String EVENT_TYPE = "AgentScheduleSyncZevent";
    protected String getEventType() {
        return EVENT_TYPE;
    }

    private static class AgentScheduleSyncZeventSource
        implements ZeventSourceId
    {
        private static final long serialVersionUID = 4776356140503377728L;

        public AgentScheduleSyncZeventSource() {
        }
    }

    private static class AgentScheduleSyncZeventPayload
        implements ZeventPayload
    {
        // List<AppdefEntityID>
        private final Collection _entityIDs;

        public AgentScheduleSyncZeventPayload(Collection ids) {
            _entityIDs = ids;
        }

        public Collection getEntityIds() {
            return _entityIDs;
        }
    }

    public Collection<AppdefEntityID> getEntityIds() {
        return ((AgentScheduleSyncZeventPayload)getPayload()).getEntityIds();
    }

    /**
     * @param aeids {@link Collection} of {@link AppdefEntityID}
     */
    public AgentScheduleSyncZevent(Collection aeids) {
        super(new AgentScheduleSyncZeventSource(), new AgentScheduleSyncZeventPayload(aeids));
        if (log.isDebugEnabled()) {
            log.debug("scheduling aeids = " + aeids);
        }
    }
}

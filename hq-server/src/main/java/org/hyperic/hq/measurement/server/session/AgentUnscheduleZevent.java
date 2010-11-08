/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class AgentUnscheduleZevent extends Zevent {

    static {
        ZeventManager.getInstance().registerEventClass(AgentUnscheduleZevent.class);
    }

    private static class AgentUnscheduleZeventSource
        implements ZeventSourceId
    {
        private static final long serialVersionUID = 4776356140503377728L;

        public AgentUnscheduleZeventSource() {
        }
    }

    private static class AgentUnscheduleZeventPayload
        implements ZeventPayload
    {
        private final Collection<AppdefEntityID> _entityIDs;
        private final String _agentToken;

        public AgentUnscheduleZeventPayload(Collection<AppdefEntityID> ids, String agentToken) {
            _agentToken = agentToken;
            _entityIDs = ids;
        }

        public Collection<AppdefEntityID> getEntityIds() {
            return _entityIDs;
        }

        public String getAgentToken() {
            return _agentToken;
        }
    }

    public String getAgentToken() {
        return ((AgentUnscheduleZeventPayload)getPayload()).getAgentToken();
    }

    public Collection<AppdefEntityID> getEntityIds() {
        return ((AgentUnscheduleZeventPayload)getPayload()).getEntityIds();
    }

    /**
     * @param aeids {@link Collection} of {@link AppdefEntityID}
     * @param schedule or unschedule metrics
     */
    public AgentUnscheduleZevent(Collection<AppdefEntityID> aeids, String agentToken) {
        super(new AgentUnscheduleZeventSource(),
              new AgentUnscheduleZeventPayload(aeids, agentToken));
    }

}

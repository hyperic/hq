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

package org.hyperic.hq.autoinventory.server.session;

import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class MergePlatformAndServersZevent extends Zevent {
    static {
        ZeventManager.getInstance().
            registerEventClass(MergePlatformAndServersZevent.class);
    }
    
    private static class MergeServiceReportZeventPayload
        implements ZeventPayload
    {
        private final String _agentToken;
        private final CompositeRuntimeResourceReport _crrr;

        public MergeServiceReportZeventPayload(
            String agentToken, CompositeRuntimeResourceReport crrr) {
            _agentToken = agentToken;
            _crrr = crrr;
        }

        public CompositeRuntimeResourceReport getCrrr() {
            return _crrr;
        }

        public String getAgentToken() {
            return _agentToken;
        }
    }
    
    private static class MergePlatformAndServerReportZeventSource
        implements ZeventSourceId
    {
        private static final long serialVersionUID = 7526472295622776147L;
        public MergePlatformAndServerReportZeventSource() {
        }
    }

    public MergePlatformAndServersZevent(String agentToken,
                                        CompositeRuntimeResourceReport crrr) {
        super(new MergePlatformAndServerReportZeventSource(),
              new MergeServiceReportZeventPayload(agentToken, crrr));
    }
    
    CompositeRuntimeResourceReport getCrrr() {
        return ((MergeServiceReportZeventPayload)getPayload()).getCrrr();
    }

    String getAgentToken() {
        return ((MergeServiceReportZeventPayload)getPayload()).getAgentToken();
    }

}

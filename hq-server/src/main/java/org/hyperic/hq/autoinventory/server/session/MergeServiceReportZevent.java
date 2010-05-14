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

package org.hyperic.hq.autoinventory.server.session;

import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class MergeServiceReportZevent extends Zevent {
    static {
        ZeventManager.getInstance().
            registerEventClass(MergeServiceReportZevent.class);
    }

    private static class MergeServiceReportZeventSource
        implements ZeventSourceId
    {
        private static final long serialVersionUID = 5423507898667486049L;

        public MergeServiceReportZeventSource() {
        }
    }

    private static class MergeServiceReportZeventPayload
        implements ZeventPayload
    {
        private ServiceMergeInfo _sInfo;

        public MergeServiceReportZeventPayload(ServiceMergeInfo sInfo) {
            _sInfo = sInfo;
        }
        
        public ServiceMergeInfo getMergeInfo() {
            return _sInfo;
        }
    }
    
    public ServiceMergeInfo getMergeInfo() {
        return ((MergeServiceReportZeventPayload)getPayload()).getMergeInfo();
    }

    public MergeServiceReportZevent(ServiceMergeInfo sInfo) {
        super(new MergeServiceReportZeventSource(),
              new MergeServiceReportZeventPayload(sInfo));
    }
}

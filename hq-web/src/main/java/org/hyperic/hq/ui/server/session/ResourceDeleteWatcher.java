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

package org.hyperic.hq.ui.server.session;

import java.util.List;

import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class ResourceDeleteWatcher implements ZeventListener<ResourceDeletedZevent> {

   private DashboardManager dashboardManager;
   
   
   @Autowired
    public ResourceDeleteWatcher(DashboardManager dashboardManager) {
    
    this.dashboardManager = dashboardManager;
}

    public void processEvents(List<ResourceDeletedZevent> events) {
        AppdefEntityID[] ids = new AppdefEntityID[events.size()];
        for (int i = 0; i < events.size(); i++) {
            ResourceDeletedZevent e = events.get(i);
            ids[i] = e.getAppdefEntityID();
        }

        dashboardManager.handleResourceDelete(ids);
    }
    
    public String toString() {
        return "ResourceDeleteWatcher";
    }
}

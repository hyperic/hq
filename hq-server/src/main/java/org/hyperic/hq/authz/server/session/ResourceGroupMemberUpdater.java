/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

package org.hyperic.hq.authz.server.session;

import java.util.List;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listens for resource creations to update any groups that have
 * associated membership critieria
 * @author jhickey
 * 
 */
@Component
public class ResourceGroupMemberUpdater implements ZeventListener<ResourceCreatedZevent> {
    private final ZeventEnqueuer zEventManager;
    private final ResourceGroupManager resourceGroupManager;

    @Autowired
    public ResourceGroupMemberUpdater(ZeventEnqueuer zEventManager,
                                      ResourceGroupManager resourceGroupManager) {
        this.zEventManager = zEventManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    @PostConstruct
    public void subscribe() {
        zEventManager.addBufferedListener(ResourceCreatedZevent.class, this);
    }

    public void processEvents(List<ResourceCreatedZevent> events) {
        resourceGroupManager.updateGroupMembers(events);
    }
    
    public String toString() {
        return "GroupMemberUpdater";
    }

}

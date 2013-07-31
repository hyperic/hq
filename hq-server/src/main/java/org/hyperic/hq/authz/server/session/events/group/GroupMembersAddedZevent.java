/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2012], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.authz.server.session.events.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class GroupMembersAddedZevent extends GroupMembersChangedZevent {

    // don't use the Resource object.  Could lead to lazy initialization issues if the Resource object is a proxy
    private final List<Integer> addedResourceIds;

    @SuppressWarnings("serial")
    public GroupMembersAddedZevent(ResourceGroup group, Collection<Resource> added) {
        this(group, added, false);
    }
    

    public GroupMembersAddedZevent(ResourceGroup group, Collection<Resource> added, boolean isDuringCalculation) {
        super(group, isDuringCalculation);
        this.addedResourceIds = new ArrayList<Integer>(added.size());
        for (Resource r : added) {
            addedResourceIds.add(r.getId());
        }
    }

    public List<Integer> getAddedResourceIds() {
        return addedResourceIds;
    }

}

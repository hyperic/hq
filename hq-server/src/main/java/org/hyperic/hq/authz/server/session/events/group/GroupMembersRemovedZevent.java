package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupMembersRemovedZevent extends GroupMembersChangedZevent {

    public GroupMembersRemovedZevent(ResourceGroup group, boolean isDuringCalculation) {
        super(group, isDuringCalculation);
    }

}

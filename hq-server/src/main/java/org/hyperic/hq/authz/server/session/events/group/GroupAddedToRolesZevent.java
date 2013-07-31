package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupAddedToRolesZevent extends GroupRelatedZevent {

    public GroupAddedToRolesZevent(ResourceGroup group) {
        super(group);
    }

}

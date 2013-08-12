package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupAddedToRolesZevent extends GroupRelatedZevent {
    
    private final boolean isDuringCalculation;

    public GroupAddedToRolesZevent(ResourceGroup group, boolean isDuringCalculation) {
        super(group);
        this.isDuringCalculation = isDuringCalculation;
    }

    public boolean isDuringCalculation() {
        return isDuringCalculation;
    }

}

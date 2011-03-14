package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.auth.domain.AuthzSubject;

abstract public class ResourceGroupFactory {

    public static ResourceGroup create(org.hyperic.hq.inventory.domain.ResourceGroup group,
                                       AuthzSubject owner) {
        if(group == null) {
            return null;
        }
        ResourceGroup groupDto = new ResourceGroup();
        groupDto.setName(group.getName());
        groupDto.setId(group.getId());
        groupDto.setResource(ResourceFactory.create(group,owner));
        //TODO other fields
        return groupDto;
    }
}

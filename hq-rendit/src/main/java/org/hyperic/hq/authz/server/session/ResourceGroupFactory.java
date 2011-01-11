package org.hyperic.hq.authz.server.session;

abstract public class ResourceGroupFactory {

    public static ResourceGroup create(org.hyperic.hq.inventory.domain.ResourceGroup group) {
        if(group == null) {
            return null;
        }
        ResourceGroup groupDto = new ResourceGroup();
        groupDto.setName(group.getName());
        groupDto.setId(group.getId());
        //TODO other fields
        return groupDto;
    }
}

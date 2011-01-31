package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.appdef.shared.AppdefUtil;

abstract public class ResourceTypeFactory {

    public static ResourceType create(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        ResourceType resourceTypeDto = new org.hyperic.hq.authz.server.session.ResourceType();
        resourceTypeDto.setId(resourceType.getId());
        resourceTypeDto.setName(resourceType.getName());
        try {
            int appdefType = AppdefUtil.getAppdefType(resourceType);
            resourceTypeDto.setAppdefType(appdefType);
            //TODO other fields
            return resourceTypeDto;
        }catch(IllegalArgumentException e) {
            return null;
        }
    }
    
    public static Resource toPrototype(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        ResourceType type = ResourceTypeFactory.create(resourceType);
        if(type == null) {
            return null;
        }
        Resource prototype = new Resource();
        prototype.setName(resourceType.getName());
        //TODO for now, ID is the ID of the resource type
        prototype.setId(resourceType.getId());
        prototype.setInstanceId(resourceType.getId());
        prototype.setResourceType(type);
        return prototype;
    }
}

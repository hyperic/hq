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
        resourceTypeDto.setAppdefType(AppdefUtil.getAppdefType(resourceType));
        //TODO other fields
        return resourceTypeDto;
    }
    
    public static Resource toPrototype(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        Resource prototype = new Resource();
        prototype.setName(resourceType.getName());
        //TODO for now, ID is the ID of the resource type
        prototype.setId(resourceType.getId());
        prototype.setInstanceId(resourceType.getId());
        prototype.setResourceType(ResourceTypeFactory.create(resourceType));
        return prototype;
    }
}

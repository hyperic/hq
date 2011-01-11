package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.reference.RelationshipTypes;

abstract public class ResourceTypeFactory {

    public static ResourceType create(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        ResourceType resourceTypeDto = new org.hyperic.hq.authz.server.session.ResourceType();
        resourceTypeDto.setId(resourceType.getId());
        resourceTypeDto.setName(resourceType.getName());
        setAppdefType(resourceType, resourceTypeDto);
        //TODO other fields
        return resourceTypeDto;
    }
    
    private static void setAppdefType(org.hyperic.hq.inventory.domain.ResourceType resourceType,ResourceType resourceTypeDto) {
        if(resourceType.getName().equals(AppdefEntityConstants.getAppdefGroupTypeName(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP))) {
            resourceTypeDto.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_APPLICATION); 
        }else if(resourceType.getResourceTypeTo(RelationshipTypes.PLATFORM) != null) {
            resourceTypeDto.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        }else if(resourceType.getResourceTypeTo(RelationshipTypes.SERVER) != null) {
            resourceTypeDto.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_SERVER);
        }else if(resourceType.getResourceTypeTo(RelationshipTypes.SERVICE) != null) {
            resourceTypeDto.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        }else {
            throw new IllegalArgumentException("ResourceType not supported");
        }
    }
    
    public static Resource toPrototype(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        Resource prototype = new Resource();
        prototype.setName(resourceType.getName());
        //TODO for now, ID is the ID of the resource type
        prototype.setId(resourceType.getId());
        return prototype;
    }
}

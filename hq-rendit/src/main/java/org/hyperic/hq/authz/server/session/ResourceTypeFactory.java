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
        resourceTypeDto.setAppdefType(getAppdefType(resourceType));
        //TODO other fields
        return resourceTypeDto;
    }
    
    private static int getAppdefType(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType.getName().equals(AppdefEntityConstants.getAppdefGroupTypeName(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP))) {
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION; 
        }
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        for(int i=0;i< groupTypes.length;i++) {
            if(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]).equals(resourceType.getName())) {
                //TODO this wasn't in ResourceType.getAppdefType before.  Not sure what the type for a Group's Resource was
                return AppdefEntityConstants.APPDEF_TYPE_GROUP;
            }
        }
        if(resourceType.getResourceTypeTo(RelationshipTypes.PLATFORM) != null) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        }
        if(resourceType.getResourceTypeTo(RelationshipTypes.SERVER) != null) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        }
        if(resourceType.getResourceTypeTo(RelationshipTypes.SERVICE) != null) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        }
        throw new IllegalArgumentException("ResourceType not supported");
    }
    
    
    
    public static Resource toPrototype(org.hyperic.hq.inventory.domain.ResourceType resourceType) {
        if(resourceType == null) {
            return null;
        }
        Resource prototype = new Resource();
        prototype.setName(resourceType.getName());
        //TODO for now, ID is the ID of the resource type
        prototype.setId(resourceType.getId());
        prototype.setResourceType(ResourceTypeFactory.create(resourceType));
        return prototype;
    }
}

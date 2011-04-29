package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppdefConverterImpl implements AppdefConverter {
    
    @Autowired
    private ResourceDao resourceDao;
    
    @Autowired
    private ResourceTypeDao resourceTypeDao;

    public AppdefEntityID newAppdefEntityId(Resource rv) {   
        ResourceType resType = rv.getType();
        if (resType == null) {
                throw new IllegalArgumentException(rv.getName() + 
                    " does not have a Resource Type");
        }
        int entityID = rv.getId();
        return new AppdefEntityID(getAppdefType(resType), entityID);
    }
    
    public int getAppdefType(ResourceType rv) {
        if(!(rv.getResourceTypesTo(RelationshipTypes.PLATFORM).isEmpty())) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        }
        if(!(rv.getResourceTypesTo(RelationshipTypes.SERVER).isEmpty()) || !(rv.getResourceTypesTo(RelationshipTypes.VIRTUAL).isEmpty())) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        }
        if(!(rv.getResourceTypesTo(RelationshipTypes.SERVICE)).isEmpty()) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        }
        if(rv.getName().equals(AppdefEntityConstants.APPDEF_NAME_APPLICATION)){
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        }
    
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        for(int i=0;i< groupTypes.length;i++) {
            if(rv.getName().equals(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]))) {
                return AppdefEntityConstants.APPDEF_TYPE_GROUP;
            }
        }
        throw new IllegalArgumentException(rv.getName() + 
            " is not a valid Appdef Resource Type");
    }

    public AppdefEntityID newAppdefEntityId(Integer resourceId) {
       return newAppdefEntityId(resourceDao.findById(resourceId));
    }
    
    public AppdefEntityID newAppdefEntityIdForType(Integer resourceTypeId) {
        return newAppdefEntityId(resourceTypeDao.findById(resourceTypeId));
     }

    public AppdefEntityID newAppdefEntityId(ResourceType resourceType) {
        return new AppdefEntityID(getAppdefType(resourceType), resourceType.getId());
    }
    
    
}

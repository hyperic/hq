package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;

public interface AppdefConverter {

    AppdefEntityID newAppdefEntityId(Resource resource);
    
    int getAppdefType(ResourceType type);
    
    
}

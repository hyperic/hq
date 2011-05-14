package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.plugin.mgmt.data.PluginResourceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceFactory {

    static final String MODIFIED_TIME = "ModifiedTime";

    static final String CREATION_TIME = "CreationTime";

    static final String AUTO_INVENTORY_IDENTIFIER = "AutoInventoryIdentifier";

    private ServerFactory serverFactory;

    private PlatformFactory platformFactory;
    
    private PluginResourceTypeRepository pluginResourceTypeRepository;
    

    @Autowired
    public ServiceFactory(ServerFactory serverFactory, PlatformFactory platformFactory, 
                          PluginResourceTypeRepository pluginResourceTypeRepository) {
        this.serverFactory = serverFactory;
        this.platformFactory = platformFactory;
        this.pluginResourceTypeRepository = pluginResourceTypeRepository;
    }

    public Service createService(Resource resource) {
        Service service = new Service();
        service
            .setAutoinventoryIdentifier((String) resource.getProperty(AUTO_INVENTORY_IDENTIFIER));
        service.setCreationTime((Long) resource.getProperty(CREATION_TIME));
        service.setDescription(resource.getDescription());
        service.setId(resource.getId());
        service.setLocation(resource.getLocation());
        service.setModifiedBy(resource.getModifiedBy());
        service.setModifiedTime((Long) resource.getProperty(MODIFIED_TIME));
        service.setName(resource.getName());
        service.setOwnerName(resource.getOwner());
        service.setResource(resource);
        Resource parent = resource.getResourceTo(RelationshipTypes.SERVICE);
        if(parent.getProperty(AppdefResourceType.APPDEF_TYPE_ID).equals(AppdefEntityConstants.APPDEF_TYPE_SERVER)) {
            service.setParent(serverFactory.createServer(parent));
        }else {
            service.setParent(platformFactory.createPlatform(parent));
        }
        service.setServiceType(createServiceType(resource.getType()));
        service.setSortName(resource.getSortName());
        return service;
    }

    public ServiceType createServiceType(ResourceType resourceType) {
        ServiceType serviceType = new ServiceType();
        // TODO set ctime and mtime
        // serviceType.setCreationTime(creationTime);
        // serviceType.setModifiedTime(modifiedTime);
        serviceType.setDescription(resourceType.getDescription());
        serviceType.setId(resourceType.getId());
        serviceType.setName(resourceType.getName());
        serviceType.setPlugin(pluginResourceTypeRepository.findNameByResourceType(resourceType.getId()));
        serviceType.setSortName(resourceType.getSortName());
        return serviceType;
    }
}

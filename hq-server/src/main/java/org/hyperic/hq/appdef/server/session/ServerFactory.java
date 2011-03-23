package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.auth.data.AuthzSubjectRepository;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.plugin.mgmt.data.PluginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerFactory {
    
    static final String MODIFIED_TIME = "ModifiedTime";

    static final String CREATION_TIME = "CreationTime";

    static final String WAS_AUTODISCOVERED = "wasAutodiscovered";

    static final String RUNTIME_AUTODISCOVERY = "runtimeAutodiscovery";

    static final String SERVICES_AUTO_MANAGED = "servicesAutoManaged";

    static final String AUTODISCOVERY_ZOMBIE = "autodiscoveryZombie";

    static final String AUTO_INVENTORY_IDENTIFIER = "autoInventoryIdentifier";

    static final String INSTALL_PATH = "installPath";
    
    private PlatformFactory platformFactory;
    
    private PluginRepository pluginRepository;
    
    private AuthzSubjectRepository authzSubjectRepository;
    
    @Autowired
    public ServerFactory(PlatformFactory platformFactory, PluginRepository pluginRepository,
                         AuthzSubjectRepository authzSubjectRepository) {
        this.platformFactory = platformFactory;
        this.pluginRepository = pluginRepository;
        this.authzSubjectRepository = authzSubjectRepository;
    }
  
    public Server createServer(Resource serverResource) {
        Server server = new Server(serverResource.getId());
        server.setAutoinventoryIdentifier((String)serverResource.getProperty(AUTO_INVENTORY_IDENTIFIER));
        server.setCreationTime((Long)serverResource.getProperty(CREATION_TIME));
        server.setDescription(serverResource.getDescription());
        server.setInstallPath((String)serverResource.getProperty(INSTALL_PATH));
        server.setLocation(serverResource.getLocation());
        server.setModifiedBy(serverResource.getModifiedBy());
        server.setModifiedTime((Long)serverResource.getProperty(MODIFIED_TIME));
        server.setName(serverResource.getName());
        server.setResource(serverResource);
        server.setRuntimeAutodiscovery((Boolean)serverResource.getProperty(RUNTIME_AUTODISCOVERY));
        server.setServerType(createServerType(serverResource.getType()));
        server.setServicesAutomanaged((Boolean)serverResource.getProperty(SERVICES_AUTO_MANAGED));
        server.setWasAutodiscovered((Boolean)serverResource.getProperty(WAS_AUTODISCOVERED));
        server.setAutodiscoveryZombie((Boolean)serverResource.getProperty(AUTODISCOVERY_ZOMBIE));
        server.setSortName((String) serverResource.getProperty(AppdefResource.SORT_NAME));
        server.setOwnerName(authzSubjectRepository.findByOwnedResource(serverResource).getName());
        Resource platform = serverResource.getResourceTo(RelationshipTypes.SERVER);
        if(platform != null) {
            server.setPlatform(platformFactory.createPlatform(platform));
        }else {
            server.setPlatform(platformFactory.createPlatform(serverResource.getResourceTo(RelationshipTypes.VIRTUAL)));
        }
        return server;
    }
    
    public ServerType createServerType(ResourceType serverResType) {
        ServerType serverType = new ServerType();
        //TODO?
        //serverType.setCreationTime(creationTime)
        //serverType.setModifiedTime();
        serverType.setDescription(serverResType.getDescription());
        serverType.setId(serverResType.getId());
        serverType.setName(serverResType.getName());
        serverType.setPlugin(pluginRepository.findByResourceType(serverResType).getName());
        //TODO for types, we just fake out sort name for now.  Can't do setProperty on ResourceType
        serverType.setSortName(serverResType.getName().toUpperCase());
        if(!(serverResType.getResourceTypesTo(RelationshipTypes.VIRTUAL).isEmpty())) {
            serverType.setVirtual(true);
        }else {
            serverType.setVirtual(false);
        }
        return serverType;
    }
    
}

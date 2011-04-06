package org.hyperic.hq.appdef.server.session;

import java.util.Set;

import org.hyperic.hq.agent.mgmt.data.AgentRepository;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.plugin.mgmt.data.PluginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlatformFactory {

    static final String MODIFIED_TIME = "ModifiedTime";

    static final String CREATION_TIME = "CreationTime";

    static final String COMMENT_TEXT = "commentText";

    static final String NETMASK = "netmask";

    static final String CPU_COUNT = "cpuCount";

    static final String MAC_ADDRESS = "macAddress";

    static final String IP_ADDRESS = "address";

    static final String CERT_DN = "certDN";

    static final String FQDN = "FQDN";
    
    @Autowired
    private PluginRepository pluginRepository;
    
    @Autowired
    private AgentRepository agentRepository;

    public Platform createPlatform(Resource resource) {
        Platform platform = new Platform();
        platform.setAgent(agentRepository.findByManagedResource(resource));
        platform.setCertdn((String) resource.getProperty(CERT_DN));
        platform.setCommentText((String) resource.getProperty(COMMENT_TEXT));
        platform.setCpuCount((Integer) resource.getProperty(CPU_COUNT));
        platform.setCreationTime((Long) resource.getProperty(CREATION_TIME));
        platform.setDescription(resource.getDescription());
        platform.setFqdn((String) resource.getProperty(FQDN));
        platform.setId(resource.getId());
        platform.setModifiedTime((Long) resource.getProperty(MODIFIED_TIME));
        platform.setModifiedBy(resource.getModifiedBy());
        platform.setLocation(resource.getLocation());
        platform.setName(resource.getName());
        platform.setResource(resource);
        platform.setPlatformType(createPlatformType(resource.getType()));
        platform.setSortName((String) resource.getProperty(AppdefResource.SORT_NAME));
        platform.setOwnerName(resource.getOwner());
        Set<Resource> ips = resource.getResourcesFrom(RelationshipTypes.IP);
        for (Resource ip : ips) {
            platform.addIp(createIp(ip));
        }
        return platform;
    }

    public PlatformType createPlatformType(ResourceType resourceType) {
        PlatformType platformType = new PlatformType();
        platformType.setDescription(resourceType.getDescription());
        // TODO modifiedTime and creationTime on types?
        // platformType.setCreationTime(creationTime)
        // platformType.setModifiedTime(modifiedTime)
        platformType.setId(resourceType.getId());
        platformType.setName(resourceType.getName());
        platformType.setPlugin(pluginRepository.findByResourceType(resourceType).getName());
        // TODO for types, we just fake out sort name for now. Can't do
        // setProperty on ResourceType
        platformType.setSortName(resourceType.getName().toUpperCase());
        return platformType;
    }

    public Ip createIp(Resource resource) {
        Ip ip = new Ip();
        ip.setAddress((String) resource.getProperty(IP_ADDRESS));
        ip.setMacAddress((String) resource.getProperty(MAC_ADDRESS));
        ip.setNetmask((String) resource.getProperty(NETMASK));
        ip.setCreationTime((Long) resource.getProperty(CREATION_TIME));
        ip.setId(resource.getId());
        ip.setModifiedTime((Long) resource.getProperty(MODIFIED_TIME));
        return ip;
    }
}

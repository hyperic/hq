package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.auth.domain.AuthzSubject;

abstract public class ResourceFactory {

    
    
    public static Resource create(org.hyperic.hq.inventory.domain.Resource resource, AuthzSubject owner) {
        if(resource == null) {
            return null;
        }
        ResourceType type = ResourceTypeFactory.create(resource.getType());
        Resource prototype  = ResourceTypeFactory.toPrototype(resource.getType());
        Resource resourceDto = new Resource(type,prototype,resource.getName(),owner,resource.getId(),false);
        resourceDto.setId(resource.getId());
        //TODO fill in the rest of the fields
        return resourceDto;
    }
    
    public static List<Resource> create(Set<org.hyperic.hq.inventory.domain.Resource> resources, AuthzSubject owner) {
        List<Resource> resourceDtos = new ArrayList<Resource>();
        for(org.hyperic.hq.inventory.domain.Resource resource: resources) {
            resourceDtos.add(ResourceFactory.create(resource,owner));
        }
        return resourceDtos;
    }
}

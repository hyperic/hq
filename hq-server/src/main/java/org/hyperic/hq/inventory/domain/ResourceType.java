package org.hyperic.hq.inventory.domain;

import java.util.Set;

import org.hyperic.hq.product.Plugin;

public interface ResourceType {

    Integer getId();

    String getName();
    
    String getDescription();
    
    Plugin getPlugin();
    
    Set<Resource> getResources();
    
    Set<ResourceType> getResourceTypesFrom(String relationName);
    
    Set<ResourceType> getResourceTypesTo(String relationName);
    
    ResourceType getResourceTypeFrom(String relationName);
    
    ResourceType getResourceTypeTo(String relationName);
    
    ResourceTypeRelationship relateTo(ResourceType entity, String relationName);
    
    boolean isRelatedTo(ResourceType entity, String name);
    
    void removeRelationship(ResourceType entity, String relationName);
    
    Set<OperationType> getOperationTypes();
    
    PropertyType getPropertyType(String name);
    
    void addPropertyType(PropertyType propertyType);
    
    Set<PropertyType> getPropertyTypes();
    
    OperationType getOperationType(String name);
    
    boolean hasResources();
    
    ResourceType merge();
    
    void persist();
    
    void remove();
}

package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.Plugin;

public interface ResourceTypeDao extends GenericDao<ResourceType> {
    public ResourceType findRoot();
    PropertyType createPropertyType(String name,Class<?> type);
    PropertyType createPropertyType(org.hyperic.hq.pdk.domain.PropertyType propertyType);
    OperationType createOperationType(org.hyperic.hq.pdk.domain.OperationType operationType);
    OperationType createOperationType(String name, ResourceType resourceType);
    ResourceType create(String name, Plugin plugin);
    ResourceType create(org.hyperic.hq.pdk.domain.ResourceType resourceType, Plugin plugin);
    ResourceType create(String name);
}

package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.Plugin;

public interface ResourceTypeDao extends GenericDao<ResourceType> {
    public ResourceType findRoot();
    PropertyType createPropertyType(String name,Class<?> type);
    ResourceType create(String name, Plugin plugin);
    ResourceType create(String name);
}

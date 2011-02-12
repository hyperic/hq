package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.ResourceType;

public interface ResourceTypeDao extends GenericDao<ResourceType> {
    ResourceType findRoot();

    void persist(ResourceType resourceType);

    ResourceType merge(ResourceType resourceType);
}

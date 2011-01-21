package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;

public interface ResourceGroupDao extends GenericDao<ResourceGroup> {
    ResourceGroup create(String name, ResourceType type, boolean privateGroup);

    ResourceGroup create(String name, ResourceType type);
}

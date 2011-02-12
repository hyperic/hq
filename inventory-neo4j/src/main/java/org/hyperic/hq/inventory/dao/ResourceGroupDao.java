package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.ResourceGroup;

public interface ResourceGroupDao extends GenericDao<ResourceGroup> {
    void persist(ResourceGroup resourceGroup);

    ResourceGroup merge(ResourceGroup resourceGroup);
}

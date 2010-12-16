package org.hyperic.hq.inventory.dao;

import java.util.Set;

import org.hyperic.hq.inventory.domain.ResourceType;

public interface ResourceTypeDao extends GenericDao<ResourceType> {
	public Set<ResourceType> findByPlugin(String plugin);
	public ResourceType findRoot();
}


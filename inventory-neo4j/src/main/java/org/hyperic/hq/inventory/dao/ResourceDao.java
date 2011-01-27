package org.hyperic.hq.inventory.dao;

import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;

public interface ResourceDao extends GenericDao<Resource> {
	public List<Resource> findByOwner(AuthzSubject owner);
	public List<Resource> findByTypeName(String name);
	public Resource findRoot();
	Resource create(String name, ResourceType type);
	Config createConfig();
}
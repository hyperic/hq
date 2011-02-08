package org.hyperic.hq.inventory.dao;

import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.util.pager.PageList;

public interface ResourceDao extends GenericDao<Resource> {
	public List<Resource> findByOwner(AuthzSubject owner);
	public Resource findRoot();
	Resource create(String name, ResourceType type);
	Config createConfig();
	PageList<Resource> findByIndexedProperty(String propertyName, Object propertyValue, PageInfo pageInfo);
}
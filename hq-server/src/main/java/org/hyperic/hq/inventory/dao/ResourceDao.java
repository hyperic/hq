package org.hyperic.hq.inventory.dao;

import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;

public interface ResourceDao extends GenericDao<Resource> {
	public List<Resource> findByOwner(AuthzSubject owner);
	public Resource findRoot();
}
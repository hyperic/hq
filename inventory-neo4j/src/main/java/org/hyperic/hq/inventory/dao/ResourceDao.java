package org.hyperic.hq.inventory.dao;

import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.util.pager.PageList;

public interface ResourceDao extends GenericDao<Resource> {
    List<Resource> findByOwner(AuthzSubject owner);

    Resource findRoot();

    PageList<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                             PageInfo pageInfo);

    void persist(Resource resource);

    Resource merge(Resource resource);
}
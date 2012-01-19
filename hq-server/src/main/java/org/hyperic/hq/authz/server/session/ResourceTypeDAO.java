/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.server.session;

import java.util.Collections;

import org.hibernate.SessionFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceTypeDAO
    extends HibernateDAO<ResourceType> {

    @Autowired
    public ResourceTypeDAO(SessionFactory f) {
        super(ResourceType.class, f);
    }

    ResourceType create(AuthzSubject creator, String name, boolean system) {
        ResourceType resType = new ResourceType(name, null, system);

        save(resType);

        // ResourceTypes also have Resources associated with them, so create
        // that and link 'em up.
        // TODO resolve circular deps so can remove Bootstrap

        ResourceType typeResType = findTypeResourceType();
        Resource prototype = Bootstrap.getBean(ResourceDAO.class).findById(
            AuthzConstants.rootResourceId);
        Resource res = Bootstrap.getBean(ResourceDAO.class).create(typeResType, prototype,
            resType.getName(), creator, resType.getId(), false);

        resType.setResource(res);

        ResourceGroup authzGroup = Bootstrap.getBean(ResourceGroupDAO.class).findByName(
            AuthzConstants.authzResourceGroupName);
        if (authzGroup == null) {
            throw new IllegalArgumentException("Resource Group not found: " +
                                               AuthzConstants.authzResourceGroupName);
        }
        // TODO resolve circular deps so can remove Bootstrap
        Bootstrap.getBean(ResourceGroupDAO.class)
            .addMembers(authzGroup, Collections.singleton(res));
        return resType;
    }

    public ResourceType findById(Integer id) {
        return (ResourceType) super.findById(id);
    }

    public void save(ResourceType entity) {
        super.save(entity);
    }

    public ResourceType findTypeResourceType() {
        return findByName(AuthzConstants.typeResourceTypeName);
    }

    public void remove(AuthzSubject whoami, ResourceType entity) {
        // XXX need to check against owner
        super.remove(entity);
    }

    protected boolean cacheFindAll() {
        return true;
    }

    public ResourceType findByName(String name) {
        String sql = "from ResourceType where name=?";
        return (ResourceType) getSession().createQuery(sql).setString(0, name).setCacheable(true)
            .setCacheRegion("ResourceType.findByName").uniqueResult();
    }

}

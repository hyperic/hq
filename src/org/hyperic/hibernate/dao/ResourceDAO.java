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

package org.hyperic.hibernate.dao;

import java.util.Collection;

import org.hibernate.Session;
import org.hyperic.hq.authz.AuthzSubject;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.shared.ResourceValue;

/**
 * CRUD methods, finders, etc. for Resource
 */
public class ResourceDAO extends HibernateDAO
{
    public ResourceDAO(Session session) {
        super(Resource.class, session);
    }

    public Resource create(AuthzSubject creator, ResourceValue createInfo) {
        Resource res = new Resource(createInfo);
        res.setOwner(creator);
        // XXX create resource for owner
        save(res);
        return res;
    }

    public Collection findAll() {
        return (Collection) super.findAll();
    }

    public Resource findById(Integer id) {
        return (Resource) super.findById(id);
    }

    public void save(Resource entity) {
        super.save(entity);
    }

    public Resource merge(Resource entity) {
        return (Resource) super.merge(entity);
    }

    public void remove(Resource entity) {
        super.remove(entity);
    }

    public void evict(Resource entity) {
        super.evict(entity);
    }

    public Resource findByInstanceId(ResourceType type, Integer id) {            
        String sql = "from Resource where instanceId = ? and" +
                     " resourceType = ?";
        return (Resource)getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setEntity(1, type)
            .uniqueResult();
    }
    
    public Collection findByOwner(AuthzSubject owner) {
        String sql = "from Resource where owner = ?";
        return getSession().createQuery(sql)
                .setEntity(0, owner)
                .list();
    }
    
    public Collection findByOwnerAndType(AuthzSubject owner,
                                         ResourceType type ) {
        String sql ="from Resource where owner = ? and resourceType = ?";
        return getSession().createQuery(sql)
            .setEntity(0, owner)
            .setEntity(0, type)
            .list();
    }
}

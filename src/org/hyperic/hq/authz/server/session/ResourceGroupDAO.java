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

import java.util.Collection;

import org.hibernate.Session;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.dao.DAOFactory;

/**
 * CRUD methods, finders, etc. for Resource
 */
public class ResourceGroupDAO extends HibernateDAO
{
    public ResourceGroupDAO(DAOFactory f) {
        super(ResourceGroup.class, f);
    }

    public ResourceGroup create(AuthzSubject creator,
                                ResourceGroupValue createInfo) {
        return create(creator, createInfo, false);
    }

    public ResourceGroup create(AuthzSubject creator,
                                ResourceGroupValue createInfo,
                                boolean isSystem) {
        ResourceGroup resGrp = new ResourceGroup(createInfo);
        save(resGrp);

        // We have to create a new resource
        ResourceValue resValue = new ResourceValue();

        ResourceType resType = 
            DAOFactory.getDAOFactory().getResourceTypeDAO()
            .findByName(AuthzConstants.groupResourceTypeName);
        if (resType == null) {
            throw new IllegalArgumentException("ResourceType not found " +
                                               AuthzConstants.groupResourceTypeName);
        }
        resValue.setResourceTypeValue(resType.getResourceTypeValue());

        resValue.setInstanceId(resGrp.getId());
        resValue.setName(resGrp.getName());
        resValue.setSystem(isSystem);
        Resource resource = 
            DAOFactory.getDAOFactory().getResourceDAO().create(creator, 
                                                               resValue);
        resGrp.setResource(resource);

        return resGrp;
    }

    public ResourceGroup findById(Integer id) {
        return (ResourceGroup) super.findById(id);
    }

    public ResourceGroup get(Integer id) {
        return (ResourceGroup) super.get(id);
    }

    public void save(ResourceGroup entity) {
        super.save(entity);
    }

    public ResourceGroup merge(ResourceGroup entity) {
        return (ResourceGroup) super.merge(entity);
    }

    public void remove(ResourceGroup entity) {
        // remove all roles
        entity.getRoles().clear();
        // remove all resources
        entity.getResourceSet().clear();

        // remove this resourceGroup itself
        DAOFactory.getDAOFactory().
            getResourceDAO().remove(entity.getResource());
        
        super.remove(entity);
    }
    
    public void addResource(ResourceGroup entity, Resource res) {
        entity.getResources().add(res);
    }
    
    public void removeAllResources(ResourceGroup entity) {
        entity.getResources().clear();
    }

    public void removeResources(ResourceGroup entity, Resource[] resources) {
        Collection resCol = entity.getResourceSet();
        for (int i = 0; i < resources.length; i++) {
            resCol.remove(resources[i]);
        }
    }

    public void evict(ResourceGroup entity) {
        super.evict(entity);
    }

    public ResourceGroup findByName(String name) {            
        String sql = "from ResourceGroup where lower(name) = lower(?)";
        return (ResourceGroup)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }
    
    public Collection findByRoleIdAndSystem_orderName(Integer roleId,
                                                         boolean system,
                                                         boolean asc) {            
        String sql = "select g from ResourceGroup g join fetch g.roles r " +
                     "where r.id = ? and g.system = ? order by g.sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, roleId.intValue())
            .setBoolean(1, system)
            .list();
    }

    public Collection findWithNoRoles_orderName(boolean asc) {            
        String sql = "from ResourceGroup g " +
                     "where g.roles.size = 0 and g.system = false " +
                     "order by sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public Collection findByNotRoleId_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("select disctinct g from ResourceGroup g " +
                         "where g.id not in " +
                         "(select g2.id from ResourceGroup g2 join " +
                         "fetch g2.roles r where r.id = ? ) and " +
                         "g.system = false order by g.sortName " +
                         (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findContaining_orderName(Integer instanceId,
                                               Integer typeId,
                                               boolean asc)
    {

        String sql="select distinct rg from ResourceGroup rg " +
                   " join fetch rg.resourceSet r " +
                   "where r.instanceId=? and " +
                   " r.resourceType.id=? " +
                   "order by rg.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, instanceId.intValue())
            .setInteger(1, typeId.intValue())
            .list();
    }

}

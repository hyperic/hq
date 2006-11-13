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

package org.hyperic.hq.dao;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.Session;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.ResourceGroupValue;

/**
 * CRUD methods, finders, etc. for Role
 */
public class RoleDAO extends HibernateDAO {
    public RoleDAO(Session session) {
        super(Role.class, session);
    }

    public Role create(AuthzSubject creator, RoleValue createInfo) {
        Role role = new Role(createInfo);
        // Save it at this point to get an ID
        save(role);

        ResourceType resType = (new ResourceTypeDAO(getSession()))
            .findByName(AuthzConstants.roleResourceTypeName);
        if (resType == null) {
            throw new IllegalArgumentException(
                "resource type not found "+AuthzConstants.roleResourceTypeName
            );
        }
        ResourceValue rValue = new ResourceValue();
        rValue.setResourceTypeValue(resType.getResourceTypeValue());
        rValue.setInstanceId(role.getId());
        Resource myResource =
            (new ResourceDAO(getSession())).create(creator, rValue);
        role.setResource(myResource);

        ResourceGroupDAO resourceGroupDAO = new ResourceGroupDAO(getSession());
        HashSet groups = new HashSet(2);

        /**
         Add the Authz Resource Group to every role.
         This is done here so that the roles are always able
         to operate on root types such as Subjects, Roles, and Groups
        **/
        ResourceGroup authzGroup = resourceGroupDAO
            .findByName(AuthzConstants.authzResourceGroupName);
        if (authzGroup == null) {
            throw new IllegalArgumentException(
                "resource group not found "+AuthzConstants.authzResourceGroupName
            );
        }
        groups.add(authzGroup);

        /**
         Create a group which will contain only the resource for the
         Role we're creating, this is done so that role permissions
         can be granted to members of the role.
         Fix for Bug #5219
        **/
        ResourceGroupValue grpVal = new ResourceGroupValue();
        grpVal.setName(AuthzConstants.privateRoleGroupName + role.getId());
        grpVal.setSystem(true);
        ResourceGroup group = resourceGroupDAO.create(creator, grpVal, true);
        // add our resource
        group.addResource(myResource);
        groups.add(group);

        role.setResourceGroups(groups);

        return role;
    }

    public Role findById(Integer id) {
        return (Role) super.findById(id);
    }
    
    public void save(Role entity) {
        super.save(entity);
    }

    public Role merge(Role entity) {
        return (Role) super.merge(entity);
    }

    public void remove(Role entity) {
        super.remove(entity);
    }

    public void evict(Role entity) {
        super.evict(entity);
    }

    public Role findByName(String name)
    {            
        String sql = "from Role where name=?";
        return (Role)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }
    
    public Collection findAll_orderName(boolean asc) {
        return getSession()
            .createQuery("from Role order by sortName " +
                         (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findBySystem_orderName(boolean system, boolean asc) {
        return getSession()
            .createQuery("from Role where system = ? order by sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .list();
    }

    public Collection findBySystemAndSubject_orderName(boolean system,
                                                       Integer sid, boolean asc)
    {
        return getSession()
            .createQuery("from Role r join fetch r.subjects s " +
                         "where r.system = ? and s.subject.id = ? " +
                         "order by r.sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .setInteger(1, sid.intValue())
            .list();
    }


    public Collection findBySystemAndSubject_orderMember(boolean system,
                                                         Integer sid,
                                                         boolean asc)
    {
        return getSession()
            .createQuery("from Role r join fetch r.subjects s " +
                         "where r.system = ? and s.subject.id = ? " +
                         "order by r.sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .setInteger(1, sid.intValue())
            .list();
    }

    public Collection findBySystemAndAvailableForSubject_orderName(
        boolean system, Integer sid, boolean asc) {
        return getSession()
            .createQuery("from Role r join fetch r.subjects s " +
                         "where system = ? and s.subject.id = ? and " +
                         "r.id != s.role.id order by r.sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .setInteger(1, sid.intValue())
            .list();
    }

    public Role findAvailableRoleForSubject(Integer roleId,
                                            Integer subjectid) {
        return (Role)getSession()
            .createQuery("from Role r join fetch r.subjects s " +
                         "where r.id = ? and s.subject.id = ? and " +
                         "r.id != s.role.id ")
            .setInteger(0, roleId.intValue())
            .setInteger(1, subjectid.intValue())
            .uniqueResult();
    }

    public Collection findAvailableForGroup(boolean system, Integer groupId)
    {
        return getSession()
            .createQuery("from Role r join fetch r.resourceGroups rg " +
                         "where r.system = ? and rg.resourceGroup.id = ? and " +
                         "r.id != rg.role.id order by r.sortName ")
            .setBoolean(0, system)
            .setInteger(1, groupId.intValue())
            .list();
    }
}

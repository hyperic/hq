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
import java.util.Collections;
import java.util.HashSet;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.HibernateDAO;

public class RoleDAO extends HibernateDAO {
    public RoleDAO(DAOFactory f) {
        super(Role.class, f);
    }

    Role create(AuthzSubject creator, RoleValue createInfo) {
        Role role = new Role(createInfo);
        // Save it at this point to get an ID
        save(role);

        ResourceType resType = 
            DAOFactory.getDAOFactory().getResourceTypeDAO()
            .findByName(AuthzConstants.roleResourceTypeName);
        if (resType == null) {
            throw new IllegalArgumentException(
                "resource type not found "+AuthzConstants.roleResourceTypeName
            );
        }
        
        ResourceDAO rDao = DAOFactory.getDAOFactory().getResourceDAO();
        Resource proto = rDao.findRootResource();
        Resource myResource = rDao.create(resType, proto, null /* No name? */, 
                                          creator, role.getId(), false); 
            
        role.setResource(myResource);

        ResourceGroupDAO resourceGroupDAO = 
            DAOFactory.getDAOFactory().getResourceGroupDAO();
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
        String groupName = AuthzConstants.privateRoleGroupName + role.getId(); 
        grpVal.setSystem(true);
        ResourceGroupCreateInfo cInfo = 
            new ResourceGroupCreateInfo(groupName,
                                        "",    // Description
                                        0,     // Group type
                                        null,  // The Resource prototype
                                        null,  // Location
                                        0,     // clusterId
                                        true); // system

        ResourceGroup group;
        try {
            group = resourceGroupDAO.create(creator, cInfo,
                                            Collections.EMPTY_LIST,
                                            Collections.EMPTY_LIST);
        } catch(GroupCreationException e) {
            throw new SystemException("Should always be able to create a " +
                                      "group for roles, but got exceptin", e);
        }
        // add our resource
        group.addResource(myResource);
        groups.add(group);

        role.setResourceGroups(groups);

        return role;
    }

    public Role get(Integer id) {
        return (Role) super.get(id);
    }
    
    public Role findById(Integer id) {
        return (Role) super.findById(id);
    }
    
    void save(Role entity) {
        super.save(entity);
    }

    void remove(Role entity) {
        entity.clearCalendars();
        entity.clearResourceGroups();
        entity.clearSubjects();
        super.remove(entity);
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
                         "where r.system = ? and s.id = ? " +
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
                         "where r.system = ? and s.id = ? " +
                         "order by r.sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .setInteger(1, sid.intValue())
            .list();
    }

    public Collection findBySystemAndAvailableForSubject_orderName(
        boolean system, Integer sid, boolean asc) {
        return getSession()
            .createQuery("select distinct r from Role r, AuthzSubject s " +
                         "where r.system = ? and s.id = ? and " +
                         "r.id not in (select id from s.roles) " +
                         "order by r.sortName " +
                         (asc ? "asc" : "desc"))
            .setBoolean(0, system)
            .setInteger(1, sid.intValue())
            .list();
    }

    public Role findAvailableRoleForSubject(Integer roleId,
                                            Integer subjectid) {
        return (Role)getSession()
            .createQuery("from Role r where r.id = ? and ? not in " +
                         "(select id from r.subjects) ")
            .setInteger(0, roleId.intValue())
            .setInteger(1, subjectid.intValue())
            .uniqueResult();
    }

    public Collection findAvailableForGroup(boolean system, Integer groupId)
    {
        return getSession()
            .createQuery("from Role r " +
                         "where r.system = ? and " +
                               "? not in (select id from r.resourceGroups) " +
                         "order by r.sortName ")
            .setBoolean(0, system)
            .setInteger(1, groupId.intValue())
            .list();
    }
}

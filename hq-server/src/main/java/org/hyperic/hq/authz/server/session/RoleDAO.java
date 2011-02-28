/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class RoleDAO {

    private ResourceTypeDao resourceTypeDao;

    @PersistenceContext
    private EntityManager entityManager;
    
    protected RoleDAO() {
        super();
    }
    
    @Autowired
    public RoleDAO(ResourceTypeDao resourceTypeDao) {
        this.resourceTypeDao = resourceTypeDao;
    }

    Role create(AuthzSubject creator, String name, String description, boolean system) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setSystem(system);
        // Save it at this point to get an ID
        entityManager.persist(role);

        ResourceType resType = resourceTypeDao.findByName(AuthzConstants.roleResourceTypeName);
        if (resType == null) {
            throw new IllegalArgumentException("resource type not found " +
                                               AuthzConstants.roleResourceTypeName);
        }

        //TODO resource for Role?
        //Resource proto = resourceDAO.findRootResource();
        //Resource myResource = resourceDAO.create(resType, proto, null /*
          //                                                             * No
            //                                                           * name?
             //                                                          */, creator, role.getId(),
            //false);

        //role.setResource(myResource);

        HashSet groups = new HashSet(2);

        /**
         * Add the Authz Resource Group to every role. This is done here so that
         * the roles are always able to operate on root types such as Subjects,
         * Roles, and Groups
         **/
        //TODO
//        ResourceGroup authzGroup = ResourceGroup
//            .findResourceGroupByName(AuthzConstants.authzResourceGroupName);
//        if (authzGroup == null) {
//            throw new IllegalArgumentException("resource group not found " +
//                                               AuthzConstants.authzResourceGroupName);
//        }
//        groups.add(authzGroup);

        /**
         * Create a group which will contain only the resource for the Role
         * we're creating, this is done so that role permissions can be granted
         * to members of the role. Fix for Bug #5219
         **/
        ResourceGroupValue grpVal = new ResourceGroupValue();
        String groupName = AuthzConstants.privateRoleGroupName + role.getId();
        grpVal.setSystem(true);
        //TODO
//        ResourceGroupCreateInfo cInfo = new ResourceGroupCreateInfo(groupName, "", // Description
//            0, // Group type
//            null, // The Resource prototype
//            null, // Location
//            0, // clusterId
//            true, false);// system
//
//        ResourceGroup group;
//        try {
//            group = resourceGroupDAO.create(creator, cInfo, Collections.EMPTY_LIST,
//                Collections.EMPTY_LIST);
//        } catch (GroupCreationException e) {
//            throw new SystemException("Should always be able to create a "
//                                      + "group for roles, but got exceptin", e);
//        }
//
//        resourceGroupDAO.addMembers(group, Collections.singleton(myResource));
//
//        role.setResourceGroups(groups);
        role.getId();
        return role;
    }

    public void remove(Role entity) {
        entity.clearCalendars();
        //TODO impl?
        //entity.clearResourceGroups();
        entity.clearSubjects();
        entityManager.remove(entity);
    }
    
    public Role findById(Integer id) {
        if (id == null) return null;
        //We aren't allowing lazy fetching of Node-Backed objects, so while you may have gotten a proxy here before, now you don't
        //You also may have been expecting an ObjectNotFoundException.  Now you get back null.
        Role result = entityManager.find(Role.class, id);
        if(result != null) {
            result.getId();
        }    
        return result;
    }
    
    public Role get(Integer id) {
        //You are getting exactly what you expected from Hibernate
        return findById(id);
    }
    
    public int size() {
        return ((Number)entityManager.createQuery("select count(r) from Role r").getSingleResult()).intValue();
    }
    
    public List<Role> findAll() {
        List<Role> roles = entityManager.createQuery("select r from Agent r",Role.class).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Role findByName(String name) {
        String sql = "select r from Role r where r.name=?";
        try {
            Role role= entityManager.createQuery(sql,Role.class).setParameter(1, name).getSingleResult();
            role.getId();
            return role;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }

    public Collection<Role> findAll_orderName(boolean asc) {
        Collection<Role> roles =  entityManager.createQuery("select r from Role r order by r.sortName " + (asc ? "asc" : "desc"),Role.class)
            .getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Collection<Role> findBySystem_orderName(boolean system, boolean asc) {
        Collection<Role> roles =   entityManager.createQuery(
            "select r from Role r where r.system = ? order by r.sortName " + (asc ? "asc" : "desc"),Role.class).setParameter(1,
            system).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Collection<Role> findBySystemAndSubject_orderName(boolean system, Integer sid,
                                                             boolean asc) {
        Collection<Role> roles = entityManager.createQuery(
            "select r from Role r join fetch r.subjects s " + "where r.system = ? and s.id = ? " +
                "order by r.sortName " + (asc ? "asc" : "desc"),Role.class).setParameter(1, system).setParameter(
            2, sid.intValue()).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Collection<Role> findBySystemAndSubject_orderMember(boolean system, Integer sid,
                                                               boolean asc) {
        Collection<Role> roles = entityManager.createQuery(
            "select r from Role r join fetch r.subjects s " + "where r.system = ? and s.id = ? " +
                "order by r.sortName " + (asc ? "asc" : "desc"),Role.class).setParameter(1, system).setParameter(
            2, sid.intValue()).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Collection<Role> findBySystemAndAvailableForSubject_orderName(boolean system,
                                                                         Integer sid, boolean asc) {
        Collection<Role> roles = entityManager.createQuery(
            "select distinct r from Role r, AuthzSubject s " +
                "where r.system = ? and s.id = ? and " + "r not in (select r2 from s.roles r2) " +
                "order by r.sortName " + (asc ? "asc" : "desc"),Role.class).setParameter(1, system).setParameter(
            2, sid.intValue()).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }

    public Role findAvailableRoleForSubject(Integer roleId, Integer subjectid) {
        try {
            Role role =  entityManager.createQuery(
            "select r from Role r where r.id = ? and ? not in " + "(select id from r.subjects) ",Role.class)
            .setParameter(0, roleId.intValue()).setParameter(1, subjectid.intValue()).getSingleResult();
            role.getId();
            return role;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }

    public Collection<Role> findAvailableForGroup(boolean system, Integer groupId) {
        Collection<Role> roles = entityManager.createQuery(
            "select r from Role r " + "where r.system = ? and "
                + "? not in (select id from r.resourceGroups) " + "order by r.sortName ",Role.class)
            .setParameter(0, system).setParameter(1, groupId.intValue()).getResultList();
        for(Role role: roles) {
            role.getId();
        }
        return roles;
    }
}

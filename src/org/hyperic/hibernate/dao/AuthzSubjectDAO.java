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

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.hibernate.Session;
import org.hyperic.hq.authz.AuthzSubject;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceValue;

/**
 * CRUD methods, finders, etc. for AuthzSubject
 */
public class AuthzSubjectDAO extends HibernateDAO
{
    public AuthzSubjectDAO(Session session)
    {
        super(AuthzSubject.class, session);
    }

    public AuthzSubject create(AuthzSubject creator,
                               AuthzSubjectValue createInfo) {
        AuthzSubject subject = new AuthzSubject(createInfo);
        save(subject);

        ResourceType resType = (new ResourceTypeDAO(getSession()))
            .findByName(AuthzConstants.subjectResourceTypeName);

        ResourceValue resValue = new ResourceValue();   
        resValue.setResourceTypeValue(resType.getResourceTypeValue());
        resValue.setInstanceId(subject.getId());
        Resource resource =
            new ResourceDAO(getSession()).create(creator, resValue);
        subject.setResource(resource);
        
        // add the resource creator role to the user
        Role creatorRole = new RoleDAO(getSession())
            .findByName(AuthzConstants.creatorRoleName);
        subject.addRole(creatorRole);

        return subject;
    }

    public AuthzSubject findById(Integer id)
    {
        return (AuthzSubject)super.findById(id);
    }

    public void save(AuthzSubject entity)
    {
        super.save(entity);
    }

    public AuthzSubject merge(AuthzSubject entity)
    {
        return (AuthzSubject)super.merge(entity);
    }

    public void remove(AuthzSubject entity)
    {
        super.remove(entity);
    }

    public void evict(AuthzSubject entity)
    {
        super.evict(entity);
    }

    public AuthzSubject findByAuth(String name, String dsn)
    {
        String sql = "from AuthzSubject s where s.name=? and s.authDsn=?";
        return (AuthzSubject)getSession().createQuery(sql)
            .setString(0, name)
            .setString(1, dsn)
            .uniqueResult();
    }

    public AuthzSubject findByName(String name)
    {
        String sql = "from AuthzSubject where name=?";
        return (AuthzSubject)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    public Collection findAll_orderName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject WHERE system = false " +
                         "order by sortName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findAll_orderFirstName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject WHERE system = false " +
                         "order by firstName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findAll_orderLastName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject WHERE system = false " +
                         "order by lastName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findAllRoot_orderName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject " +
                         "where system = false or id = 1 " +
                         "order by sortName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findAllRoot_orderFirstName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject " +
                         "where system = false or id = 1 " +
                         "order by firstName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findAllRoot_orderLastName(boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject " +
                         "where system = false or id = 1 " +
                         "order by lastName " + (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findByRoleId_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject join fetch roles r " +
                         "where r.id = ? and system = false " +
                         "order by sortName " + (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findByNotRoleId_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject s where s.userId not in " +
                         "(select s2.userId from AuthzSubject s2 join " +
                         "fetch s2.roles r where r.id = ? ) and " +
                         "s.system = false order by s.sortName " +
                         (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findWithNoRoles_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("from AuthzSubject join fetch roles r " +
                         "WHERE r.size = 0 and system = false " +
                         "order by sortName " + (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }
}

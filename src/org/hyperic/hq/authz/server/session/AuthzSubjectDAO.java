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

import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.dao.HibernateDAO;

/**
 * CRUD methods, finders, etc. for AuthzSubject
 */
public class AuthzSubjectDAO extends HibernateDAO
{
    public AuthzSubjectDAO(DAOFactory f) {
        super(AuthzSubject.class, f);
    }

    public AuthzSubject create(AuthzSubject creator,
                               AuthzSubjectValue createInfo) {
        AuthzSubject subject = new AuthzSubject(createInfo);
        save(subject);

        DAOFactory daoFactory = DAOFactory.getDAOFactory();

        // XXX create resource for owner
        ResourceTypeDAO rtdao = daoFactory.getResourceTypeDAO();
        ResourceType rt =
            rtdao.findByName(AuthzConstants.subjectResourceTypeName);
        if (rt == null) {
            throw new IllegalArgumentException("resource type not found " +
                                               AuthzConstants.subjectResourceTypeName);
        }
        ResourceValue rValue = new ResourceValue();
        rValue.setResourceTypeValue(rt.getResourceTypeValue());
        rValue.setInstanceId(subject.getId());
        subject.setResource(daoFactory.getResourceDAO().create(creator, rValue));
        Role role = daoFactory.getRoleDAO().findByName(
            AuthzConstants.creatorRoleName);
        if (role == null) {
            throw new IllegalArgumentException("role not found " +
                                               AuthzConstants.creatorRoleName);
        }
        subject.getRoles().add(role);
        save(subject);
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

    public Collection findById_orderName(Integer[] ids, boolean asc)
    {
        return getSession().createCriteria(AuthzSubject.class)
            .add(Expression.in("id", ids))
            .add(Expression.eq("system", Boolean.FALSE))
            .addOrder( asc ? Order.asc("sortName") : Order.desc("sortName"))
            .list();
    }
    
    public Collection findAll_order(String col, boolean asc) {
        return getSession().createCriteria(AuthzSubject.class)
            .add(Expression.eq("system", Boolean.FALSE))
            .addOrder( asc ? Order.asc(col) : Order.desc(col))
            .list();
    }

    public Collection findAll_orderName(boolean asc)
    {
        return findAll_order("sortName", asc);
    }

    public Collection findAll_orderFirstName(boolean asc)
    {
        return findAll_order("firstName", asc);
    }

    public Collection findAll_orderLastName(boolean asc)
    {
        return findAll_order("lastName", asc);
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
            .createQuery("select s from AuthzSubject s join fetch s.roles r " +
                         "where r.id = ? and s.system = false " +
                         "order by s.sortName " + (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findByNotRoleId_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("select distinct s from AuthzSubject s, Role r " +
                         "where r.id = ? and s.id not in " +
                         "(select id from r.subjects) and " +
                         "s.system = false order by s.sortName " +
                         (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }
}

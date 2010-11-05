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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * Hibernate Data Access Object
 */
public abstract class HibernateDAO<T> {
    private Class<T> _persistentClass;
    protected static final int BATCH_SIZE = 1000;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    protected HibernateDAO(Class<T> persistentClass, SessionFactory f) {
        _persistentClass = persistentClass;
    }

    public HQDialect getHQDialect() {

        return (HQDialect) ((SessionFactoryImplementor) getSession().getSessionFactory())
            .getDialect();
    }

    public Class<T> getPersistentClass() {
        return _persistentClass;
    }

    public Session getSession() {
        return ((EntityManagerImpl) EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory)).getSession();
    }

    public IdentifierGenerator getIdentifierGenerator(String className) {
        return ((SessionFactoryImplementor) getSession().getSessionFactory()).getEntityPersister(
            className).getIdentifierGenerator();
    }

    public void flushSession() {
        getSession().flush();
    }

    public T findById(Serializable id) {
        return findById(id, false);
    }

    @SuppressWarnings("unchecked")
    public T get(Serializable id) {
        return (T) getSession().get(getPersistentClass(), id);
    }

    @SuppressWarnings("unchecked")
    public T findById(Serializable id, boolean lock) {
        return lock ? (T) getSession().load(getPersistentClass(), id, LockMode.UPGRADE)
                   : (T) getSession().load(getPersistentClass(), id);
    }

    protected Criteria createCriteria() {
        return getSession().createCriteria(_persistentClass);
    }

    protected Query createQuery(String s) {
        return getSession().createQuery(s);
    }

    /**
     * This method is intended for sub-classes to specify whether or not their
     * 'find-all' finder should be automatically added to the query-cache.
     * 
     * The findAll query will use the persistent class specified in the
     * constructor, and use the following cache region:
     * 
     * com.my.Persistent.findAll
     * 
     * @return true to indicate that the finder should be cached
     */
    protected boolean cacheFindAll() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        if (cacheFindAll()) {
            String region = getPersistentClass().getName() + ".findAll";
            return getSession().createCriteria(getPersistentClass()).setCacheable(true)
                .setCacheRegion(region).list();
        }
        return getSession().createCriteria(getPersistentClass()).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<T> findAllOrderByName() {
        return getSession().createCriteria(getPersistentClass()).addOrder(Order.asc("name")).list();
    }

    public int size() {
        return ((Number) getSession().createQuery(
            "select count(*) from " + getPersistentClass().getName()).uniqueResult()).intValue();
    }

    public int size(Collection<T> coll) {
        return ((Number) getSession().createFilter(coll, "select count(*)").uniqueResult())
            .intValue();
    }

    public void save(T entity) {
        getSession().saveOrUpdate(entity);
    }

    public void remove(T entity) {
        getSession().delete(entity);
    }

    @SuppressWarnings("unchecked")
    protected PageList<T> getPagedResult(Query q, Integer total, PageControl pc) {
        if (pc.getPagesize() != PageControl.SIZE_UNLIMITED) {
            q.setMaxResults(pc.getPagesize());
        }

        if (pc.getPageEntityIndex() != 0) {
            q.setFirstResult(pc.getPageEntityIndex());
        }

        return new PageList<T>(q.list(), total.intValue());
    }

    @SuppressWarnings("unchecked")
    protected PageList<T> getPagedResult(Criteria crit, Integer total, PageControl pc) {
        if (pc.getPagesize() != PageControl.SIZE_UNLIMITED) {
            crit.setMaxResults(pc.getPagesize());
        }

        if (pc.getPageEntityIndex() != 0) {
            crit.setFirstResult(pc.getPageEntityIndex());
        }

        return new PageList<T>(crit.list(), total.intValue());
    }

}

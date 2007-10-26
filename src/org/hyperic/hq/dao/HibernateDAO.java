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

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Hibernate Data Access Object
 * The actual DAO is subclass of this object.
 * This class should actually be implemented with J2SE 5 Generics,
 * but we have to support JDK 1.4, :(
 */
public abstract class HibernateDAO {
    private Class      _persistentClass;
    private DAOFactory _daoFactory;

    protected HibernateDAO(Class persistentClass, DAOFactory f) {
        _persistentClass = persistentClass;
        _daoFactory      = f;
    }

    public Class getPersistentClass() {
        return _persistentClass;
    }

    public Session getSession() {
        return _daoFactory.getCurrentSession();
    }
            
    public void flushSession() {
        getSession().flush();
    }

    protected Object findById(Serializable id) {
        return findById(id, false);
    }

    protected Object get(Serializable id) {
        return getSession().get(getPersistentClass(), id);
    }

    protected Object findById(Serializable id, boolean lock) {
        return lock
               ? getSession().load(getPersistentClass(), id, LockMode.UPGRADE)
               : getSession().load(getPersistentClass(), id);
    }
    
    protected Criteria createCriteria() {
        return getSession().createCriteria(_persistentClass);
    }

    /**
     * This method is intended for sub-classes to specify whether or not
     * their 'find-all' finder should be automatically added to the query-cache.
     *
     * The findAll query will use the persistent class specified in the
     * constructor, and use the following cache region:
     * 
     *     com.my.Persistent.findAll
     * 
     * @return true to indicate that the finder should be cached
     */
    protected boolean cacheFindAll() {
        return false;
    }
    
    public Collection findAll() {
        if (cacheFindAll()) {
            String region = getPersistentClass().getName() + ".findAll";
            return getSession().createCriteria(getPersistentClass())
                               .setCacheable(true)
                               .setCacheRegion(region)
                               .list();
        }
        return getSession().createCriteria(getPersistentClass()).list();
    }

    public Collection findAllOrderByName() {
        return getSession()
            .createCriteria(getPersistentClass())
            .addOrder(Order.asc("name"))
            .list();
    }

    public int size() {
        return ((Integer)getSession()
            .createQuery("select count(*) from "+getPersistentClass().getName())
            .uniqueResult())
            .intValue();
    }

    public int size(Collection coll) {
        return ((Integer)getSession()
            .createFilter(coll, "select count(*)")
            .uniqueResult())
            .intValue();
    }

    protected void save(Object entity) {
        getSession().saveOrUpdate(entity);
    }

    protected void remove(Object entity) {
        getSession().delete(entity);
    }

    protected PageList getPagedResult(Query q, Integer total, PageControl pc) {
        if (pc.getPagesize() != PageControl.SIZE_UNLIMITED) {
            q.setMaxResults(pc.getPagesize());
        }
        
        if (pc.getPageEntityIndex() != 0) {
            q.setFirstResult(pc.getPageEntityIndex());
        }
        
        return new PageList(q.list(), total.intValue());
    }

    protected PageList getPagedResult(Criteria crit, Integer total,
                                      PageControl pc) {
        if (pc.getPagesize() != PageControl.SIZE_UNLIMITED) {
            crit.setMaxResults(pc.getPagesize());
        }
        
        if (pc.getPageEntityIndex() != 0) {
            crit.setFirstResult(pc.getPageEntityIndex());
        }
        
        return new PageList(crit.list(), total.intValue());
    }
}

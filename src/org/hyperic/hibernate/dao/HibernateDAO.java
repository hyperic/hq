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

import org.hibernate.LockMode;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.Collection;

/**
 * Hibernate Data Access Object
 * The actual DAO is subclass of this object.
 * This class should actually be implemented with J2SE 5 Generics,
 * but we have to support JDK 1.4, :(
 */
public abstract class HibernateDAO
{
    private Class persistentClass;
    private Session session;

    protected HibernateDAO(Class persistentClass, Session session)
    {
        this.persistentClass = persistentClass;
        this.session = session;
    }

    public Class getPersistentClass()
    {
        return persistentClass;
    }

    protected void setSession(Session session)
    {
        this.session = session;
    }

    public Session getSession()
    {
        if (session == null) {
            throw new IllegalStateException("Session not set.");
        }
        return session;
    }

    protected Object findById(Serializable id)
    {
        return findById(id, false);
    }

    protected Object findById(Serializable id, boolean lock)
    {
        return lock
               ? getSession().load(getPersistentClass(), id, LockMode.UPGRADE)
               : getSession().load(getPersistentClass(), id);
    }

    public Collection findAll()
    {
        return getSession().createCriteria(getPersistentClass()).list();
    }

    protected void evict(Object entity)
    {
        getSession().evict(entity);
    }

    protected Object merge(Object entity)
    {
        return getSession().merge(entity);
    }

    protected void save(Object entity)
    {
        getSession().saveOrUpdate(entity);
    }

    protected void remove(Object entity)
    {
        getSession().delete(entity);
    }
}

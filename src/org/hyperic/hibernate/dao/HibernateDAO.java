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
        getSession().flush();
    }

    protected void remove(Object entity)
    {
        getSession().delete(entity);
    }
}

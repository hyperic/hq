package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Application;

/**
 * CRUD methods, finders, etc. for Application
 */
public class ApplicationDAO extends HibernateDAO
{
    public ApplicationDAO(Session session)
    {
        super(Application.class, session);
    }

    public Application findById(Integer id)
    {
        return (Application)super.findById(id);
    }

    public void evict(Application entity)
    {
        super.evict(entity);
    }

    public Application merge(Application entity)
    {
        return (Application)super.merge(entity);
    }

    public void save(Application entity)
    {
        super.save(entity);
    }

    public void remove(Application entity)
    {
        super.remove(entity);
    }
}

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Platform;

/**
 * CRUD methods, finders, etc. for Platform
 */
public class PlatformDAO extends HibernateDAO
{
    public PlatformDAO(Session session)
    {
        super(Platform.class, session);
    }

    public Platform findById(Integer id)
    {
        return (Platform)super.findById(id);
    }

    public void evict(Platform entity)
    {
        super.evict(entity);
    }

    public Platform merge(Platform entity)
    {
        return (Platform)super.merge(entity);
    }

    public void save(Platform entity)
    {
        super.save(entity);
    }

    public void remove(Platform entity)
    {
        super.remove(entity);
    }
}

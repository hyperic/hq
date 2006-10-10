package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.CpropKey;

/**
 * CRUD methods, finders, etc. for CpropKey
 */
public class CpropKeyDAO extends HibernateDAO
{
    public CpropKeyDAO(Session session)
    {
        super(CpropKey.class, session);
    }

    protected CpropKey findById(Integer id)
    {
        return (CpropKey)super.findById(id);
    }

    public void evict(CpropKey entity)
    {
        super.evict(entity);
    }

    public CpropKey merge(CpropKey entity)
    {
        return (CpropKey)super.merge(entity);
    }

    public void save(CpropKey entity)
    {
        super.save(entity);
    }

    public void remove(CpropKey entity)
    {
        super.remove(entity);
    }
}

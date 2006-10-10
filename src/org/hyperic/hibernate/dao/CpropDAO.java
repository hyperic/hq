package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Cprop;

/**
 * CRUD methods, finders, etc. for Cprop
 */
public class CpropDAO extends HibernateDAO
{
    public CpropDAO(Session session)
    {
        super(Cprop.class, session);
    }

    public Cprop findById(Integer Id)
    {
        return (Cprop)super.findById(Id);
    }

    public void save(Cprop entity)
    {
        super.save(entity);
    }

    public Cprop merge(Cprop entity)
    {
        return (Cprop)super.merge(entity);
    }

    public void remove(Cprop entity)
    {
        super.remove(entity);
    }

    public void evict(Cprop entity)
    {
        super.evict(entity);
    }
}

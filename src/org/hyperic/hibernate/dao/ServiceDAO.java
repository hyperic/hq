package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Service;

/**
 * CRUD methods, finders, etc. for Service
 */
public class ServiceDAO extends HibernateDAO
{
    public ServiceDAO(Session session)
    {
        super(Service.class, session);
    }

    public Service findById(Service id)
    {
        return (Service)super.findById(id);
    }

    public void evict(Service entity)
    {
        super.evict(entity);
    }

    public Service merge(Service entity)
    {
        return (Service)super.merge(entity);
    }

    public void save(Service entity)
    {
        super.save(entity);
    }

    public void remove(Service entity)
    {
        super.remove(entity);
    }
}

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Server;

/**
 * CRUD methods, finders, etc. for Server
 */
public class ServerDAO extends HibernateDAO
{
    public ServerDAO(Session session)
    {
        super(Server.class, session);
    }

    public Server findById(Integer id)
    {
        return (Server)super.findById(id);
    }

    public void evict(Server entity)
    {
        super.evict(entity);
    }

    public Server merge(Server entity)
    {
        return (Server)super.merge(entity);
    }

    public void save(Server entity)
    {
        super.save(entity);
    }

    public void remove(Server entity)
    {
        super.remove(entity);
    }
}

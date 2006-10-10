package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Agent;

/**
 * CRUD methods, finders, etc. for Agent
 */
public class AgentDAO extends HibernateDAO
{
    public AgentDAO(Session session)
    {
        super(Agent.class, session);
    }

    public void evict(Agent entity)
    {
        super.evict(entity);
    }

    public Agent merge(Agent entity)
    {
        return (Agent)super.merge(entity);
    }

    public void save(Agent entity)
    {
        super.save(entity);
    }

    public void remove(Agent entity)
    {
        super.remove(entity);
    }

    public Agent findById(Integer id)
    {
        return (Agent)super.findById(id);
    }
}

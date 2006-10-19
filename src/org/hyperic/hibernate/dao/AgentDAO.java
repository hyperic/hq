package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentValue;

import java.util.Collection;

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

    public Agent create(AgentValue av, AgentType type)
    {
        Agent ag = new Agent();
        ag.setAddress(av.getAddress());
        ag.setPort(new Integer(av.getPort()));
        ag.setVersion(av.getVersion());
        ag.setAuthToken(av.getAuthToken());
        ag.setAgentToken(av.getAgentToken());
        AgentType at = new AgentType();
        at.setId(type.getId());
        ag.setAgentType(at);
        save(ag);
        // TODO: remove when migration to hibernate is complete
        getSession().flush();
        return ag;
    }

    public Agent findByIpAndPort(String address, int port)
    {
        String sql = "from Agent where address=? and port=?";
        return (Agent)getSession().createQuery(sql)
            .setString(0, address)
            .setInteger(1, port)
            .uniqueResult();
    }

    public Agent findByAgentToken(String token)
    {
        String sql = "from Agent where agentToken=?";
        return (Agent)getSession().createQuery(sql)
            .setString(0, token)
            .uniqueResult();
    }

    public Collection findUnusedAgents(Integer platformId)
    {
        String sql = "from Agent where id not in (" +
                     "select agent.id from Platform where " +
                     "id != ?and agent.id is not null)";
        return getSession().createQuery(sql)
            .setInteger(0, platformId.intValue())
            .list();
    }

    public Agent findByPrimaryKey(AgentPK pk)
    {
        return findById(pk.getId());
    }
}

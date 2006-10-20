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

import org.hibernate.Session;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.AgentType;

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

    /**
     * @deprecated use findById()
     * @param pk
     * @return
     */
    public Agent findByPrimaryKey(AgentPK pk)
    {
        return findById(pk.getId());
    }
}

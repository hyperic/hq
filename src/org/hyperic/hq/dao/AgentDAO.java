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

package org.hyperic.hq.dao;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.shared.AgentValue;

public class AgentDAO extends HibernateDAO
{
    public AgentDAO(DAOFactory f) {
        super(Agent.class, f);
    }

    public void save(Agent entity) {
        super.save(entity);
    }

    public void remove(Agent entity) {
        super.remove(entity);
    }

    public Agent findById(Integer id) {
        return (Agent)super.findById(id);
    }

    public Agent create(AgentType type, String address, Integer port, 
                        String authToken, String agentToken, String version)
    {
        Agent ag = new Agent(type, address, port, authToken, agentToken, 
                             version); 
        save(ag);
        return ag;
    }

    public Agent findByIpAndPort(String address, int port) {
        String sql = "from Agent where address=? and port=?";
        return (Agent)getSession().createQuery(sql)
            .setString(0, address)
            .setInteger(1, port)
            .uniqueResult();
    }

    public Agent findByAgentToken(String token) {
        String sql = "from Agent where agentToken=?";
        return (Agent)getSession().createQuery(sql)
            .setString(0, token)
            .setCacheRegion("Agent.findByAgentToken")
            .setCacheable(true)
            .uniqueResult();
    }

    public Collection findUnusedAgents(Integer platformId) {
        String sql = "from Agent where id not in (" +
                     "select agent.id from Platform where " +
                     "id != ?and agent.id is not null)";
        return getSession().createQuery(sql)
            .setInteger(0, platformId.intValue())
            .list();
    }

}

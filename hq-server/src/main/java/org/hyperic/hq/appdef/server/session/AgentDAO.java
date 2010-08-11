/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AgentDAO
    extends HibernateDAO<Agent> {
    @Autowired
    public AgentDAO(SessionFactory f) {
        super(Agent.class, f);
    }

    public Agent create(AgentType type, String address, Integer port, boolean unidirectional,
                        String authToken, String agentToken, String version) {
        Agent ag = new Agent(type, address, port, unidirectional, authToken, agentToken, version);
        save(ag);
        return ag;
    }
    
    @SuppressWarnings("unchecked")
    public List<Agent> findAll() {
        return getSession().createCriteria(Agent.class).addOrder(Order.asc("address")).addOrder(Order.asc("port"))
        .list();
    }

    @SuppressWarnings("unchecked")
    public List<Agent> findByIP(String ip) {
        String hql = "from Agent where address=:address";
        return (List<Agent>) getSession().createQuery(hql).setString("address", ip).list();
    }

    public int countUsed() {
        return ((Number) getSession().createQuery(
            "select count(distinct a) from Platform p " + "join p.agent a").uniqueResult())
            .intValue();

    }

    public Agent findByIpAndPort(String address, int port) {
        String sql = "from Agent where address=? and port=?";
        return (Agent) getSession().createQuery(sql).setString(0, address).setInteger(1, port)
            .uniqueResult();
    }

    public Agent findByAgentToken(String token) {
        String sql = "from Agent where agentToken=?";
        return (Agent) getSession().createQuery(sql).setString(0, token).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<Agent> findAgents(PageInfo pInfo) {
        final AgentSortField sort = (AgentSortField) pInfo.getSort();
        final StringBuilder sql = new StringBuilder().append("select distinct a from Platform p ")
            .append(" JOIN p.agent a").append(" JOIN p.resource r").append(
                " WHERE r.resourceType is not null").append(" ORDER BY ").append(
                sort.getSortString("a")).append((pInfo.isAscending() ? "" : " DESC"));

        // Secondary sort by CTime
        if (!sort.equals(AgentSortField.CTIME)) {
            sql.append(", ").append(AgentSortField.CTIME.getSortString("a")).append(" DESC");
        }

        final Query q = getSession().createQuery(sql.toString());

        return (List<Agent>) pInfo.pageResults(q).list();
    }
}

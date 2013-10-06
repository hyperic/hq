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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentDAO extends HibernateDAO<Agent> {
    private static final Log log = LogFactory.getLog(AgentDAO.class);
    private static final String LIMIT_A_TO_CURRENT_AGENTS = "a.version >= :serverVersion ";
    private static final String LIMIT_A_TO_OLD_AGENTS = "a.version < :serverVersion ";
    private final Map<String, Integer> agentTokenToId = new HashMap<String, Integer>();
    
    private final ServerConfigManager serverConfigManager;
    
    @Autowired
    public AgentDAO(SessionFactory f, ServerConfigManager serverConfigManager) {
        super(Agent.class, f);
        this.serverConfigManager = serverConfigManager;
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void preloadQueryCache() {
        new HibernateTemplate(sessionFactory, true).execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                List<String> tokens = session.createQuery("select agentToken from Agent").list();
                log.info("preloading Agent.findByAgentToken cache");
                for(String token : tokens) {
                    //HQ-2575 Preload findByAgentToken query cache to minimize DB connections when multiple agents
                    //send measurement reports to a restarted server 
                    findByAgentToken(token, session);
                }
                // pre-fetch platforms bag to optimize ReportProcessor.handleMeasurementData
                log.info("preloading Agent.platforms bag");
                String hql = "from Agent a left outer join fetch a.platforms";
                session.createQuery(hql).list();
                return null;
            }
        });
    }
    
    @PreDestroy
    public void cleanup() {
        synchronized (agentTokenToId) {
            agentTokenToId.clear();
        }
    }

    public Agent create(AgentType type, String address, Integer port, boolean unidirectional,
                        String authToken, String agentToken, String version) {
        Agent ag = new Agent(type, address, port, unidirectional, authToken, agentToken, version);
        save(ag);
        return ag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Agent> findAll() {
        return getSession().createCriteria(Agent.class).addOrder(Order.asc("address")).addOrder(
            Order.asc("port")).list();
    }

    @SuppressWarnings("unchecked")
    public List<Agent> findByIP(String ip) {
        String hql = "from Agent where address=:address";
        return getSession().createQuery(hql)
                                         .setString("address", ip)
                                         .setCacheable(true)
                                         .setCacheRegion("Agent.findByIP")
                                         .list();
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
    
    private Agent findByAgentToken(String token, Session session) {
        Integer agentId = null;
        synchronized (agentTokenToId) {
            agentId = agentTokenToId.get(token);
        }
        Agent rtn = (agentId != null) ? get(agentId) : null;
        if (rtn == null) {
            rtn = (Agent) session.createCriteria(Agent.class)
                              .add(Restrictions.eq("agentToken", token))
                              .uniqueResult();
            if (rtn != null) {
                agentId = rtn.getId();
                synchronized (agentTokenToId) {
                    agentTokenToId.put(token, rtn.getId());
                }
            }
        }
        return rtn;
    }

    public Agent findByAgentToken(String token) {
        return findByAgentToken(token, getSession());
    }

    @SuppressWarnings("unchecked")
    public List<Agent> findAgents(PageInfo pInfo) {
        final AgentSortField sort = (AgentSortField) pInfo.getSort();
        final StringBuilder sql = new StringBuilder()
            .append("select distinct a from Platform p ")
            .append(" JOIN p.agent a")
            .append(" JOIN p.resource r")
            .append(" WHERE r.resourceType is not null")
            .append(" ORDER BY ").append(sort.getSortString("a"))
            .append((pInfo.isAscending() ? "" : " DESC"));
        // Secondary sort by CTime
        if (!sort.equals(AgentSortField.CTIME)) {
            sql.append(", ").append(AgentSortField.CTIME.getSortString("a")).append(" DESC");
        }
        final Query q = getSession().createQuery(sql.toString());
        return pInfo.pageResults(q).list();
    }

    @SuppressWarnings("unchecked")
    public List<Agent> findOldAgents() {
        String sql = "from Agent a where " + LIMIT_A_TO_OLD_AGENTS;
        final Query query = getSession().createQuery(sql);
        query.setParameter("serverVersion", serverConfigManager.getServerMajorVersion());
        return query.list();
    }
    
    /**
     * Returns the agent's installation path 
     * @param agentToken
     */
    public String getAgentInstallationPath(String agentToken) {
    	final StringBuilder sql = new StringBuilder()
    	.append("select distinct s from Server s ")
    	.append(" JOIN s.platform p")
    	.append(" JOIN p.agent a")
    	.append(" WHERE a.agentToken='" + agentToken + "' and s.description like '%Hyperic%Agent%'");
    	final Query q = getSession().createQuery(sql.toString());
    	try{
    		return ((Server)q.uniqueResult()).getInstallPath();
    	}catch (Exception e) {
    	}
    	return null;
    }

    /**
     * Get a list of all agents in the system, whose version is older than server's version and 
     *  which are actually used (e. have platforms)
     */
    @SuppressWarnings("unchecked")
    public List<Agent> findOldAgentsUsed() {
        final String sql = new StringBuilder(150)
        .append("from Agent a where ")
        .append(LIMIT_A_TO_OLD_AGENTS)
        .append("and exists (select 1 from Platform p where p.agent.id = a.id)")
        .toString();
        final Query query = getSession().createQuery(sql);
        query.setParameter("serverVersion", serverConfigManager.getServerMajorVersion());
        return query.list();
    }
  
    
    /**
     * Get a list of all agents in the system, whose version is equal to or newer than server's version and 
     *  which are actually used (e. have platforms)
     */
    @SuppressWarnings("unchecked")
    public List<Agent> findUpToDateAgentsUsed() {
        final String sql = new StringBuilder(150)
        .append("from Agent a where ")
        .append(LIMIT_A_TO_CURRENT_AGENTS)
        .append("and exists (select 1 from Platform p where p.agent.id = a.id)")
        .toString();
        final Query query = getSession().createQuery(sql);
        query.setParameter("serverVersion", serverConfigManager.getServerMajorVersion());
        return query.list();
    }
    
    /**
     * 
     * @return number of agents, whose version is older than that of the server
     */
    public long getNumOldAgents() {
        final String sql = "select count(a) from Agent a where " + LIMIT_A_TO_OLD_AGENTS;        
        final Query query = getSession().createQuery(sql);
        query.setParameter("serverVersion", serverConfigManager.getServerMajorVersion());        
        return ((Number) query.uniqueResult()).longValue();
    }
    

    public long getNumVersionUpToDateAgents() {
        final String sql = new StringBuilder(150)
        .append("select count(a) from Agent a where ")
        .append(LIMIT_A_TO_CURRENT_AGENTS)
        .append("and exists (select 1 from Platform p where p.agent.id = a.id)")
        .toString();
	    final Query query = getSession().createQuery(sql);
	    query.setParameter("serverVersion", serverConfigManager.getServerMajorVersion());
	    return ((Number) query.uniqueResult()).longValue();    	
    }
    
    /**
     * 
     * @param agent
     * @return true if agent's version is older than that of the server
     */
    public boolean isAgentOld(Agent agent) {
        String serverVersion = serverConfigManager.getServerMajorVersion();     
        return agent.getVersion().compareTo(serverVersion) < 0;
    }


}

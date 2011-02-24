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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.agent.domain.Agent;
import org.hyperic.hq.agent.domain.AgentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentDAO {
     
    @PersistenceContext
    private EntityManager entityManager; 
    
    private JpaTemplate jpaTemplate;
    
    @Autowired
    public AgentDAO(JpaTemplate jpaTemplate) {
        this.jpaTemplate = jpaTemplate;
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void preloadQueryCache() {
        jpaTemplate.execute(new JpaCallback() {
            public Object doInJpa(EntityManager entityManager)  {
                List<String> tokens = entityManager.createQuery("select a.agentToken from Agent a",String.class).getResultList();
                for(String token : tokens) {
                    //HQ-2575 Preload findByAgentToken query cache to minimize DB connections when multiple agents
                    //send measurement reports to a restarted server 
                    entityManager.createQuery("SELECT a FROM Agent a WHERE a.agentToken = :agentToken",Agent.class).
                    setHint("org.hibernate.cacheable", true).setHint("org.hibernate.cacheRegion", "Agent.findByAgentToken").
                    setParameter("agentToken", token).
                    getSingleResult();
                }
                return null;
            }
        });
    }

    public Agent create(AgentType type, String address, Integer port, boolean unidirectional,
                        String authToken, String agentToken, String version) {
        Agent ag = new Agent(type, address, port, unidirectional, authToken, agentToken, version);
        entityManager.persist(ag);
        ag.getId();
        return ag;
    }

    public List<Agent> findAll() {
        //TODO previously ordered by Address, then Port
        List<Agent> agents = entityManager.createQuery("select a from Agent a",Agent.class).getResultList();
        for(Agent agent: agents) {
            agent.getId();
        }
        return agents;
    }

  
    public List<Agent> findByIP(String ip) {
        List<Agent> agents = entityManager.createQuery("SELECT a FROM Agent a WHERE a.address = :address",Agent.class).
            setParameter("address", ip).getResultList();
        for(Agent agent: agents) {
            agent.getId();
        }
        return agents;
    }
    
    public int countUsed() {
        //TODO move this to ResourceDao and do graph traversal to find Agents.  Below won't work b/c nothing in RDBMS linking Resource to Agent
//        return ((Number) entityManager.createQuery(
//            "select count(distinct a) from Resource r " + "join r.agent a").getSingleResult())
//            .intValue();
        return 0;
    }

    public Agent findByIpAndPort(String address, int port) {
        try {
            Agent agent = entityManager.createQuery("SELECT a FROM Agent a WHERE a.address = :address and a.port = :port",Agent.class).
                setParameter("address", address).setParameter("port", port).getSingleResult();
            agent.getId();
            return agent;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }

    public Agent findByAgentToken(String token) {
        try {
            Agent agent = entityManager.createQuery("SELECT a FROM Agent a WHERE a.agentToken = :agentToken",Agent.class).
                setHint("org.hibernate.cacheable", true).setHint("org.hibernate.cacheRegion", "Agent.findByAgentToken").
                setParameter("agentToken", token).
                getSingleResult();
            agent.getId();
            return agent;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }
    
    public Agent findById(Integer id) {
        if (id == null) return null;
        //We aren't allowing lazy fetching of Node-Backed objects, so while you may have gotten a proxy here before, now you don't
        //You also may have been expecting an ObjectNotFoundException.  Now you get back null.
        Agent result = entityManager.find(Agent.class, id);
        if(result != null) {
            result.getId();
        }    
        return result;
    }
    
    public Agent get(Integer id) {
        //You are getting exactly what you expected from Hibernate
        return findById(id);
    }
    
    public void remove(Agent agent) {
        entityManager.remove(agent);
    }
    
    public int size() {
        return ((Number)entityManager.createQuery("select count(a) from Agent a").getSingleResult()).intValue();
    }
}

package org.hyperic.hq.agent.mgmt.data;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

public class AgentRepositoryImpl implements AgentRepositoryCustom {

    @Autowired
    private JpaTemplate jpaTemplate;
    
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
}

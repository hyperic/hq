package org.hyperic.hq.agent.mgmt.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.agent.mgmt.domain.ManagedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ManagedResourceRepository extends JpaRepository<ManagedResource, Integer>,
    ManagedResourceRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("select count(distinct m.agent) from ManagedResource m")
    long countUsedAgents();

    @Transactional(readOnly = true)
    @Query("select m.agent from ManagedResource m where m.resourceId=:resource")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Agent.findByManagedResource") })
    Agent findAgentByResource(@Param("resource") Integer resourceId);
    
    List<ManagedResource> findByAgent(Agent agent);
}

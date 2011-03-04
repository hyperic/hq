package org.hyperic.hq.agent.mgmt.data;

import org.hyperic.hq.agent.domain.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTypeRepository extends JpaRepository<AgentType, Integer> {

    AgentType findByName(String name);

}

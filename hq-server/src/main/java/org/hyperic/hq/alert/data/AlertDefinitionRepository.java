package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlertDefinitionRepository extends JpaRepository<AlertDefinition, Integer> {

    @Transactional(readOnly = true)
    @Query("select d.enabled from AlertDefinition d where d.id = :alertDefId")
    boolean isEnabled(@Param("alertDefId") Integer alertDefId);
    
    List<AlertDefinition> findByEscalation(Escalation escalation);
}

package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

//TODO 2 of these methods used to filter out deleted alerts
public interface ResourceAlertDefinitionRepository extends
    JpaRepository<ResourceAlertDefinition, Integer>, ResourceAlertDefinitionRepositoryCustom {

    List<ResourceAlertDefinition> findByEscalation(Escalation escalation);

    List<ResourceAlertDefinition> findByResource(Integer resource, Sort sort);

    ResourceAlertDefinition findByResourceAndResourceTypeAlertDefinition(Integer resource,
                                                                         Integer typeAlertDefId);

    @Transactional(readOnly = true)
    @Query("select d.enabled from ResourceAlertDefinition d where d.id = :alertDefId")
    boolean isEnabled(@Param("alertDefId") Integer alertDefId);
}

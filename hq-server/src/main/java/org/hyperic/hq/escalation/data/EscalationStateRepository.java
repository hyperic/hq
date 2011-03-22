package org.hyperic.hq.escalation.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EscalationStateRepository extends JpaRepository<EscalationState, Integer> {

    @Transactional
    @Modifying
    @Query("delete from EscalationState s where s.id in (:ids)")
    void deleteByIds(@Param("ids") List<Integer> escalationStateIds);

    @Transactional(readOnly = true)
    @Query("select e from EscalationState e where e.alertDefId=:alertDefId and e.alertType=:alertType")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "EscalationState.findByTypeAndDef") })
    EscalationState findByAlertDefAndAlertType(@Param("alertDefId") Integer alertDefinitionId,
                                               @Param("alertType") Integer alertType);

    EscalationState findByAlertIdAndAlertType(Integer alertId, Integer alertType);

    List<EscalationState> findByEscalation(Escalation escalation);

    @Transactional
    @Modifying
    @Query("update EscalationState e set e.acknowledgedBy = null where e.acknowledgedBy = :subject")
    void removeAcknowledgedBy(@Param("subject") AuthzSubject subject);
}

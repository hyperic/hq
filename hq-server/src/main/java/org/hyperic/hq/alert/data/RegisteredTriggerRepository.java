package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredTriggerRepository extends JpaRepository<RegisteredTrigger, Integer>,
    RegisteredTriggerRepositoryCustom {

    List<RegisteredTrigger> findByAlertDefinition(Integer alertDefinitionId);
}

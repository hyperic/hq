package org.hyperic.hq.alert.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.springframework.transaction.annotation.Transactional;

public interface RegisteredTriggerRepositoryCustom {

    @Transactional(readOnly = true)
    Set<RegisteredTrigger> findAllEnabledTriggers();

    @Transactional(readOnly = true)
    Map<Integer, List<Integer>> findTriggerIdsByAlertDefinitionIds(List<Integer> alertDefIds);
}

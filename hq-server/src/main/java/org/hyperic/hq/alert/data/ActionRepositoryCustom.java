package org.hyperic.hq.alert.data;

import org.hyperic.hq.events.server.session.AlertDefinition;
import org.springframework.transaction.annotation.Transactional;

public interface ActionRepositoryCustom {

    @Transactional
    void deleteByAlertDefinition(AlertDefinition def);
}

package org.hyperic.hq.alert.data;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.server.session.ResourceTypeAlertDefinition;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceAlertDefinitionRepositoryCustom {

    @Transactional
    int setChildrenActive(ResourceTypeAlertDefinition def, boolean active);

    @Transactional
    int setChildrenEscalation(ResourceTypeAlertDefinition def, Escalation esc);
}

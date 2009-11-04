package org.hyperic.hq.events.server.session;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.events.shared.RegisteredTriggerValue;
/**
 * DAO for interacting with {@link RegisteredTrigger}s
 * @author jhickey
 *
 */
public interface TriggerDAOInterface {

    RegisteredTrigger create(RegisteredTriggerValue createInfo);

    List findAll();

    List findByAlertDefinitionId(Integer id);
    
    Map findTriggerIdsByAlertDefinitionIds(List alertDefIds);

    RegisteredTrigger findById(Integer id);

    RegisteredTrigger get(Integer id);

    Set findAllEnabledTriggers();
}

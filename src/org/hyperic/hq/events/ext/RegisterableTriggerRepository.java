package org.hyperic.hq.events.ext;

import java.util.Collection;
/**
 * Repository of in-memory representations of alert triggers
 * @author jhickey
 *
 */
public interface RegisterableTriggerRepository {

    /**
     *
     * @param eventClass The event class
     * @param instanceId The id of the source instance of the event
     * @return The {@link RegisterableTriggerInterface}s interested in the event
     */
    Collection getInterestedTriggers(Class eventClass, Integer instanceId);

    /**
     *
     * @param trigger The trigger to add to the repository
     */
    void addTrigger(RegisterableTriggerInterface trigger);

    /**
     *
     * @param triggerId The trigger to remove from the repository
     */
    void removeTrigger(Integer triggerId);


}

package org.hyperic.hq.events.ext;

/**
 * Key object used in {@link RegisteredTrigger}'s internal Map. Indicates the
 * combination of event type and instance a trigger is listening for
 * @author jhickey
 *
 */
public class TriggerEventKey {

    private final Class<?> eventClass;

    private final int instanceId;

    /**
     *
     * @param eventClass The event type
     * @param instanceId The instance the event occurred against, or RegisteredTriggers.KEY_ALL if interested in all instances
     */
    public TriggerEventKey(Class<?> eventClass, int instanceId) {
        this.eventClass = eventClass;
        this.instanceId = instanceId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof TriggerEventKey)) {
            return false;
        }
        TriggerEventKey other = (TriggerEventKey) obj;
        if (eventClass == null) {
            if (other.eventClass != null) {
                return false;
            }
        } else if (!eventClass.getName().equals(other.eventClass.getName())) {
            return false;
        }
        if (instanceId != other.instanceId) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventClass == null) ? 0 : eventClass.getName().hashCode());
        result = prime * result + instanceId;
        return result;
    }

}

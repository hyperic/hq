package org.hyperic.hq.events.server.session;

import org.hyperic.hq.zevents.ZeventSourceId;
/**
 * Source ID object for a TriggersCreatedZevent. This represents the
 * Alert Definition whose triggers were created.
 * @author jhickey
 *
 */
public class TriggersCreatedZeventSource implements ZeventSourceId {

    private static final long serialVersionUID = 3480600667010718596L;

    private final Integer id;

    /**
     *
     * @param id The id of the AlertDefinition that the event is for.
     */
    public TriggersCreatedZeventSource(Integer id)
    {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof TriggersCreatedZeventSource)) {
            return false;
        }

        return ((TriggersCreatedZeventSource) o).getId().equals(getId());
    }

    /**
     * @return The id of the AlertDefinition that the event is for.
     */
    public Integer getId() {
        return id;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.intValue();
        return result;
    }

    public String toString() {
        return "Triggers Created for Alert Def ID[" + id + "]";
    }
}

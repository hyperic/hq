package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.zevents.ZeventSourceId;

/**
 * Source ID object for an AlertConditionsSatisifiedZEvent. This represents the
 * Alert Definition whose conditions were satisfied.
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedZEventSource implements ZeventSourceId {

    private static final long serialVersionUID = 8416378224728973400L;

    private final int id;

    /**
     *
     * @param id The id of the AlertDefinition that the event is for.
     */
    public AlertConditionsSatisfiedZEventSource(int id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof AlertConditionsSatisfiedZEventSource)) {
            return false;
        }

        return ((AlertConditionsSatisfiedZEventSource) o).getId() == getId();
    }

    /**
     * @return The id of the AlertDefinition that the event is for.
     */
    public int getId() {
        return id;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    public String toString() {
        return "Alert Def ID[" + id + "]";
    }

}

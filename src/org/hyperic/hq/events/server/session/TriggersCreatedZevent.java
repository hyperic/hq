package org.hyperic.hq.events.server.session;

import org.hyperic.hq.zevents.Zevent;

/**
 * Represents that triggers have been created in the database (due to create or
 * update of an alert definition) and signals to listeners that the in-memory
 * representations of these triggers can be created. Note this event does not
 * have a payload.
 * @author jhickey
 *
 */
public class TriggersCreatedZevent
    extends Zevent
{

    public TriggersCreatedZevent(Integer alertDefId) {
        super(new TriggersCreatedZeventSource(alertDefId), null);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof TriggersCreatedZevent)) {
            return false;
        }
        TriggersCreatedZevent other = (TriggersCreatedZevent) obj;
        return getSourceId().equals(other.getSourceId());
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getSourceId().hashCode();
        return result;
    }

}

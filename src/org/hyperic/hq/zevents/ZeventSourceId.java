package org.hyperic.hq.zevents;

import java.io.Serializable;

/**
 * This class encapsulates the ID of the originating resource.  This ID should
 * properly implement {@link Object#equals(Object)} and 
 * {@link Object#hashCode()}, and should take the event type into consideration
 * so that events of different types are different in ID as well.
 * 
 * Since these classes are used to index into maps for calling triggers, they
 * must be immutable.
 * 
 * @see Zevent#getSourceId()
 */
public interface ZeventSourceId extends Serializable {
}

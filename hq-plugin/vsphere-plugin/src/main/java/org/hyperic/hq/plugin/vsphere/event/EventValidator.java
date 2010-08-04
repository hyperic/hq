package org.hyperic.hq.plugin.vsphere.event;

import com.vmware.vim25.Event;

/**
 * EventValidator
 *
 * @author Helena Edelson
 */
public interface EventValidator {

    boolean hasValidEvents(Event[] events);
    
}

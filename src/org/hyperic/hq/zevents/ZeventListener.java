package org.hyperic.hq.zevents;

import java.util.List;

/**
 * Implementors of this interface may listen for events coming from the
 * {@link ZeventManager}
 * 
 * @see ZeventManager#addGlobalListener(ZeventListener)
 * @see ZeventManager#addListener(Class, ZeventListener)
 */
public interface ZeventListener {
    /**
     * Called by the {@link ZeventManager} to process events coming off the
     * event queue
     * 
     * @param events a List of {@link Zevent} events
     */
    void processEvents(List events);
}

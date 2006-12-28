package org.hyperic.hq.application;

public interface StartupListener {
    /**
     * Called by the HQ Application when the full application has started.
     */
    void hqStarted(); 
}

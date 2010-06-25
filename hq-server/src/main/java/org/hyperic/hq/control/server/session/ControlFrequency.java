package org.hyperic.hq.control.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
/**
 * DTO to hold the frequency of invocations of a specific control action
 * @author jhickey
 *
 */
public class ControlFrequency {
    private AppdefEntityID id;
    private String action;
    private long count;

    public ControlFrequency(AppdefEntityID id, String action, long count) {
        this.id = id;
        this.action = action;
        this.count = count;
    }

    /**
     * 
     * @return The entity against which the control action was executed
     */
    public AppdefEntityID getId() {
        return id;
    }

    /**
     * 
     * @return The name of the action
     */
    public String getAction() {
        return action;
    }

    /**
     * 
     * @return The number of times the action was invoked
     */
    public long getCount() {
        return count;
    }

}

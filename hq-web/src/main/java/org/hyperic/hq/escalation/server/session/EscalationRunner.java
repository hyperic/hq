package org.hyperic.hq.escalation.server.session;

import org.hyperic.hq.context.Bootstrap;

/**
 * This class actually performs the execution of a given segment of an
 * escalation. These are queued up and run by the executor thread pool.
 */
public class EscalationRunner implements Runnable {
    
    private Integer stateId;
    private EscalationRuntime escalationRuntime = Bootstrap.getBean(EscalationRuntime.class);
    
    public EscalationRunner(Integer stateId) {
        this.stateId = stateId;
    }

    public void run() {
        escalationRuntime.executeState(stateId);
    }
}
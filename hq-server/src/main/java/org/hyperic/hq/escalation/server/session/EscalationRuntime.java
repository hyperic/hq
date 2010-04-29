package org.hyperic.hq.escalation.server.session;

public interface EscalationRuntime {

    void acquireMutex() throws InterruptedException;

    void releaseMutex();

    void endEscalation(EscalationState escalationState);

    Escalatable getEscalatable(EscalationState escalationState);

    boolean addToUncommittedEscalationStateCache(PerformsEscalations def);

    void removeFromUncommittedEscalationStateCache(final PerformsEscalations def, boolean postTxnCommit);
    
    void scheduleEscalation(final EscalationState state);
    
    void unscheduleAllEscalationsFor(PerformsEscalations def);
    
    void executeState(Integer stateId);
}

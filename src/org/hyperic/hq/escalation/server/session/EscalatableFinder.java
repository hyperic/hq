package org.hyperic.hq.escalation.server.session;

/**
 * The MEscalationState only contains information about the 'type' of the
 * alert that triggered the escalation.  For the MEscalationManager to
 * find the Escalatables that are associated with an alert 'type', those
 * subsystems must be registered -- this is the interface that subsystems
 * must register as to find 'em.
 */
public interface EscalatableFinder {
    /**
     * Find an escalatable (alert) given its ID.
     */
    Escalatable findEscalatable(Integer alertId);
    
    /**
     * Find an alert definition (or something that performs escalations)
     * given its id.
     */
    PerformsEscalations findDefinition(Integer defId);
}

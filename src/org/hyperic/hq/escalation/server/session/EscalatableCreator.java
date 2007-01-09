package org.hyperic.hq.escalation.server.session;

/**
 * Implementors of this interface can create escalatable objects.  
 * 
 * This abstraction is needed because of the following problem:
 * 
 * Group Alert System -[starts escalation] -> Escalation System
 * 
 * The Escalation system must create an escalatable object ONLY if the
 * escalation has not already been started.  So we essentially need to call
 * back into caller to create that object for us.  
 */
public interface EscalatableCreator {
    Escalatable createEscalatable();
}

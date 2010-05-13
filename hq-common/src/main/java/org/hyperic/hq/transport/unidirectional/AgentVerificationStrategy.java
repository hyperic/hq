package org.hyperic.hq.transport.unidirectional;

/**
 * A strategy for verifying an agent.
 */
public interface AgentVerificationStrategy {
    
    /**
     * Check existence of an agent with the given agent token.
     * 
     * @param agentToken The agent token.
     * @return <code>true</code> if the agent exists; <code>false</code> otherwise.
     */
    boolean agentExists(String agentToken);

}

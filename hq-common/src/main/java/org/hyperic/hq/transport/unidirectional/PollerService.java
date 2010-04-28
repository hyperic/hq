package org.hyperic.hq.transport.unidirectional;

import java.util.List;

import org.hyperic.hq.measurement.MeasurementConstants;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;

import org.hyperic.hq.transport.unidirectional.AgentVerificationStrategy;

/**
 * The remote interface invoked by the HQ agent to handle requests/responses 
 * from unidirectional clients residing on the HQ server.
 */
public interface PollerService {
    
    public static final long REQUEST_TIMEOUT = MeasurementConstants.HOUR;
    
    /**
     * Retrieve the requests assigned to a specific agent.
     * 
     * @param agentToken The agent token identifying the agent for which we 
     *                   are receiving invocation requests.
     * @return The invocation requests.
     */
    List<InvocationRequest> retrieveRequests(String agentToken);
    
    /**
     * Removes the old agent messages
     * @param verifier
     * @return
     */
    boolean removeOldAgentMessages(AgentVerificationStrategy verifier);
    
    /**
     * Send responses.
     * 
     * @param responses The responses to send.
     */
    void sendReponses(List<InvocationResponse> responses);
    
    /**
     * Invoke a remote call on an HQ agent service and retrieve the response.
     * 
     * @param agentToken The agent token identifying the agent we are calling.
     * @param invocationRequest The invocation request sent to the agent.
     * @return The invocation response or <code>null</code> if the invocation 
     *         request is asynchronous (one-way). 
     * @throws NullPointerException if the agent token is <code>null</code>.        
     * @throws InterruptedException if the current thread is interrupted.
     * @throws TimeoutException if a response has not been received before 
     *                          the {@link ResponseHandler#getResponse() timeout}.
     */
    InvocationResponse invoke(String agentToken, InvocationRequest invocationRequest)
        throws InterruptedException;


}

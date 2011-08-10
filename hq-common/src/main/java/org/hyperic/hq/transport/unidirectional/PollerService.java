/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.transport.unidirectional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * Remove aged out requests
     * 
     * @param now time the method uses as a marker to age out old requests
     * @return {@link Map} of {@link String} of agentToken to {@link InvocationRequest}
     * representing the requests which were removed from the queue.
     */
    Map<String, Collection<InvocationRequest>> removeAgedOutRequests(long now);

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

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.transport.util;

import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.InvokerLocator;

/**
 * The invoker locator containing configuration specific to the HQ transport.
 */
public class HQInvokerLocator
    extends InvokerLocator {

    /**
     * The default value for the agent token when the agent token is not yet
     * known.
     */
    public static final String UNKNOWN_AGENT_TOKEN = "UNKNOWN-AGENT-TOKEN";

    private final String _agentToken;

    private static final ThreadLocal<MessageDeliveryOptions> THREADLOCAL_MESSAGE_DELIVERY_OPTIONS = new ThreadLocal<MessageDeliveryOptions>() {
        protected MessageDeliveryOptions initialValue() {
            return MessageDeliveryOptions.newSynchronousInstance();
        }
    };

    /**
     * Creates an instance.
     * 
     * @param protocol The protocol.
     * @param host The remote host.
     * @param port The port on the remote host.
     * @param path The invoker locator path or <code>null</code>.
     * @param parameters The invoker locator parameters or <code>null</code>.
     * @param agentToken The agent token.
     * @throws NullPointerException if the agent token is <code>null</code>.
     * @throws IllegalArgumentException if the agent token is assigned the value
     *         {@link #UNKNOWN_AGENT_TOKEN}.
     */
    public HQInvokerLocator(String protocol, String host, int port, String path, Map parameters, String agentToken) {
        super(protocol, host, port, path, parameters);

        if (agentToken == null) {
            throw new NullPointerException("agent token is null");
        }

        if (agentToken.equals(UNKNOWN_AGENT_TOKEN)) {
            throw new IllegalArgumentException("illegal token name: " + UNKNOWN_AGENT_TOKEN);
        }

        _agentToken = agentToken;
    }

    /**
     * Creates an instance where the agent token is not yet known.
     * 
     * @param protocol The protocol.
     * @param host The remote host.
     * @param port The port on the remote host.
     * @param path The invoker locator path or <code>null</code>.
     * @param parameters The invoker locator parameters or <code>null</code>.
     */
    public HQInvokerLocator(String protocol, String host, int port, String path, Map parameters) {
        super(protocol, host, port, path, parameters);

        _agentToken = UNKNOWN_AGENT_TOKEN;
    }

    /**
     * @return An invoker locator instance with the same connection info as this
     *         HQ invoker locator.
     */
    public InvokerLocator toInvokerLocator() {
        return new InvokerLocator(this.getProtocol(), this.getHost(), this.getPort(), this.getPath(), this
            .getParameters());
    }

    /**
     * Clone this instance of HQ invoker locator, setting the agent token to a
     * new value. Note that the {@link MessageDeliveryOptions} are not
     * guaranteed to be passed to the cloned instance.
     * 
     * @param agentToken The new agent token value.
     * @return The cloned instance.
     * @throws NullPointerException if the agent token is <code>null</code>.
     * @throws IllegalArgumentException if the agent token is assigned the value
     *         {@link #UNKNOWN_AGENT_TOKEN}.
     */
    public HQInvokerLocator cloneWithNewAgentToken(String agentToken) {
        Map parameters = null;

        if (this.getParameters() != null) {
            parameters = new HashMap(this.getParameters());
        }

        return new HQInvokerLocator(this.getProtocol(), this.getHost(), this.getPort(), this.getPath(), parameters,
            agentToken);
    }

    /**
     * @return The agent token or {@link #UNKNOWN_AGENT_TOKEN} if the agent
     *         token is not yet known.
     */
    public String getAgentToken() {
        return _agentToken;
    }

    /**
     * @return <code>true</code> if the agent token is known; <code>false</code>
     *         otherwise.
     */
    public boolean isAgentTokenKnown() {
        return !UNKNOWN_AGENT_TOKEN.equals(_agentToken);
    }

    /**
     * Set the message delivery options on a thread local.
     * 
     * @param options The message delivery options.
     * @throws NullPointerException if the message delivery options is
     *         <code>null</code>.
     */
    public void setMessageDeliveryOptions(MessageDeliveryOptions options) {
        if (options == null) {
            throw new NullPointerException("message delivery options is null");
        }

        THREADLOCAL_MESSAGE_DELIVERY_OPTIONS.set(options);
    }

    /**
     * Retrieve the thread local message delivery options. By default returns a
     * synchronous message delivery options.
     * 
     * @return The message delivery options.
     */
    public MessageDeliveryOptions getMessageDeliveryOptions() {
        return (MessageDeliveryOptions) THREADLOCAL_MESSAGE_DELIVERY_OPTIONS.get();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof HQInvokerLocator) {
            HQInvokerLocator locator = (HQInvokerLocator) obj;

            return super.equals(obj) && locator.getAgentToken().equals(this.getAgentToken());
        }

        return false;
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 17 * result + this.getAgentToken().hashCode();

        return result;
    }

}

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
public class HQInvokerLocator extends InvokerLocator {
    
    private final String _agentToken;
    
    private boolean _oneWay;
    
    private boolean _guaranteed;
    
    private AsynchronousInvoker _invoker;
    
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
     */
    public HQInvokerLocator(String protocol, 
                            String host, 
                            int port, 
                            String path, 
                            Map parameters, 
                            String agentToken) {
        super(protocol, host, port, path, parameters);
        
        if (agentToken == null) {
            throw new NullPointerException("agent token is null");
        }
        
        _agentToken = agentToken;
    }
    
    /**
     * Clone this instance of HQ invoker locator, setting the agent token 
     * to a new value.
     * 
     * @param agentToken The new agent token value.
     * @return The cloned instance.
     * @throws NullPointerException if the agent token is <code>null</code>.
     */
    public HQInvokerLocator cloneWithNewAgentToken(String agentToken) {
        Map parameters = null;
        
        if (this.getParameters() != null) {
            parameters = new HashMap(this.getParameters());
        }
        
        HQInvokerLocator locator = new HQInvokerLocator(this.getProtocol(), 
                                                        this.getHost(), 
                                                        this.getPort(), 
                                                        this.getPath(), 
                                                        parameters, 
                                                        agentToken);
        
        locator._invoker = this._invoker;
        locator._oneWay = this._oneWay;
        locator._guaranteed = this._guaranteed;
        
        return locator;
    }
    
    /**
     * Set the asynchronous invoker.
     * 
     * @param invoker The asynchronous invoker.
     * @throws NullPointerException if the asychronous invoker is <code>null</code>.
     */
    public void setAsynchronousInvoker(AsynchronousInvoker invoker) {
        if (invoker == null) {
            throw new NullPointerException("async invoker is null");
        }
        
        _invoker = invoker;
    }
    
    /**
     * @return The asynchronous invoker or <code>null</code>.
     */
    public AsynchronousInvoker getAsynchronousInvoker() {
        return _invoker;
    }
    
    /**
     * @return The agent token.
     */
    public String getAgentToken() {
        return _agentToken;
    }
    
    /**
     * Set invocations as one-way meaning that the client invoker will 
     * not block on the remote invocation. This also means the client 
     * application should not expect a return value.
     */
    public void setOneWay() {
        _oneWay = true;
    }
    
    /**
     * @return <code>true</code> if the invocation is one-way.
     */
    public boolean isOneWay() {
        return _oneWay;
    }
    
    /**
     * if invocations are already set as one-way, delivery may be specified 
     * as guaranteed.
     * 
     * @throws IllegalStateException if invocations are not already set as 
     *                               {@link #setOneWay() one-way}.
     */
    public void setDeliveryGuaranteed() {
        if (!isOneWay()) {
            throw new IllegalStateException("guranteed delivery is only applicable " +
            		                        "for one-way invocation");
        }
        
        _guaranteed = true;
    }
    
    /**
     * @return <code>true</code> if delivery is guaranteed.
     */
    public boolean isDeliveryGuaranteed() {
        return _guaranteed;
    }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj instanceof HQInvokerLocator) {
            HQInvokerLocator locator = (HQInvokerLocator)obj;
            
            return super.equals(obj) && locator.getAgentToken().equals(this.getAgentToken());            
        }
        
        return false;
    }
    
    public int hashCode() {
        int result = super.hashCode();
        
        result = 17*result+this.getAgentToken().hashCode();
        
        return result;
    }

}

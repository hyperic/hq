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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;

/**
 * Provides utility methods for the transport layer.
 */
public class TransportUtils {
    
    /**
     * Private constructor for utility class.
     */
    private TransportUtils() {}
    
    /**
     * Determine if the invocation request represents a one-way (asynchronous) 
     * invocation.
     * 
     * @param invocation The invocation request.
     * @return <code>true</code> if this is a one-way invocation.
     */
    public static boolean isOneWayInvocation(InvocationRequest invocation) {
        Map requestPayload = invocation.getRequestPayload();
        
        if (requestPayload != null) {
            String value = (String)requestPayload.get(Client.ONEWAY_FLAG);
            return Boolean.valueOf(value).booleanValue();
        } else {
            return false;            
        }
    }
    
    /**
     * Set the invocation request as one-way (asynchronous).
     * 
     * @param invocation The invocation request.
     */
    public static void setOneWayInvocation(InvocationRequest invocation) {
        Map requestPayload = invocation.getRequestPayload();
        
        if (requestPayload == null) {
            requestPayload = new HashMap();
            invocation.setRequestPayload(requestPayload);
        }
        
        requestPayload.put(Client.ONEWAY_FLAG, Boolean.TRUE.toString());        
    }
    
    /**
     * Return the HTTP transport name.
     * 
     * @param encrypted <code>true</code> if the transport is encrypted.
     * @return The HTTP transport name.
     */
    public static String getHttpTransport(boolean encrypted) {
        if (encrypted) {
            return "https";
        } else {
            return "http";
        }
    }
    
    /**
     * Assert that the interface only contains operations with void return types.
     * 
     * @param serviceInterface The interface to inspect.
     * @throws IllegalArgumentException if a non-void return type operation is found.
     */
    public static void assertVoidReturnTypes(Class serviceInterface) {
        Method[] methods = serviceInterface.getMethods();
        
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            
            if (!method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException(
                        "found a non-void return type operation: "+method+
                        " in "+serviceInterface);
            }
        }
    }
    
    /**
     * Attempt to load the poller client implementation from the context 
     * class loader.
     * 
     * @return The class object.
     * @throws ClassNotFoundException if the poller client implementation cannot 
     *                                be found. This can happen if invoked from 
     *                                a .ORG instance.
     */
    public static Class tryLoadUnidirectionalTransportPollerClient() 
        throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(
                "com.hyperic.hq.transport.PollerClientImpl");          
    }
    
}

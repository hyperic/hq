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

/**
 * Passes the message delivery options specified when creating a service proxy 
 * down to the client invoker.
 */
public class MessageDeliveryOptions {
    
    private static final MessageDeliveryOptions SYNCHRONOUS = 
        new MessageDeliveryOptions(false, false);

    private static final MessageDeliveryOptions ASYNC_GUARANTEED =
        new MessageDeliveryOptions(true, true);
    
    private static final MessageDeliveryOptions ASYNC_NON_GUARANTEED =
        new MessageDeliveryOptions(true, false);
        
    
    private final boolean _asynchronous;
    
    private final boolean _guaranteed;
    
    private MessageDeliveryOptions(boolean asynchronous, boolean guaranteed) {
        if (asynchronous == false && guaranteed == true) {
            throw new IllegalArgumentException("guaranteed delivery only " +
            		"available for asynchronous delivery");
        }
        
        _asynchronous = asynchronous;
        _guaranteed = guaranteed;
    }
    
    /**
     * Create an instance specifying synchronous delivery.
     * 
     * @return The message delivery options.
     */
    public static MessageDeliveryOptions newSynchronousInstance() {
        return SYNCHRONOUS;
    }
        
    /**
     * Create an instance specifying asynchronous delivery.
     * 
     * @param guaranteed <code>true</code> if message delivery is guaranteed.
     * @return The message delivery options.
     */
    public static MessageDeliveryOptions newAsynchronousInstance(boolean guaranteed) {
        if (guaranteed) {
            return ASYNC_GUARANTEED;
        } else {
            return ASYNC_NON_GUARANTEED;
        }
    }
    
    /**
     * Check if the message delivery is asynchronous meaning that the client 
     * invoker will not block on the remote invocation. This means that the 
     * client should not expect a response from the remote end point.
     * 
     * @return <code>true</code> if the invocation is asynchronous.
     */
    public boolean isAsynchronous() {
        return _asynchronous;
    }
    
    /**
     * If the message delivery is asynchronous, will the client invoker guarantee 
     * message delivery? Guaranteed delivery does not apply for synchronous delivery.
     * 
     * @return <code>true</code> if message delivery is guaranteed.
     */
    public boolean isGuaranteed() {
        return _guaranteed;
    }

}

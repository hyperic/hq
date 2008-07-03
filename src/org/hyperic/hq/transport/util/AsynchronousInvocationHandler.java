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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The handler for making asynchronous invocations. If the invocations must 
 * be guaranteed, then any state associated with the invocation must be 
 * made {@link Externalizable}.
 */
public abstract class AsynchronousInvocationHandler 
    implements Runnable, Externalizable {
    
    private static final Log _log = LogFactory.getLog(AsynchronousInvocationHandler.class);
    
    private boolean _guaranteed;
    
    /**
     * Public no-arg constructor required for externalization.
     */
    public AsynchronousInvocationHandler() {}
    
    /**
     * Creates an instance.
     *
     * @param guaranteed <code>true</code> if the invocation is guaranteed.
     */
    public AsynchronousInvocationHandler(boolean guaranteed) {
        _guaranteed = guaranteed;
    }

    /**
     * Delegates to {@link #handleInvocation()} but swallows any exceptions 
     * throw during invocation handling.
     */
    public final void run() {
        try {
            handleInvocation();
        } catch (Exception e) {
            _log.error("Error while handling invocation", e);
        }
    }
    
    /**
     * @return <code>true</code> if the invocation is guaranteed.
     */
    public final boolean isInvocationGuaranteed() {
        return _guaranteed;
    }

    /**
     * Subclasses should only extend this method, never override it.
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        _guaranteed = in.readBoolean();
    }

    /**
     * Subclasses should only extend this method, never override it.
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(_guaranteed);
    }

    /**
     * Handle the invocation.
     * 
     * @throws Exception if invocation handling fails.
     */
    public abstract void handleInvocation() throws Exception;
    
}

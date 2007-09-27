/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.agent.server;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentConfig;

import java.io.InterruptedIOException;

/**
 * The AgentConnectionListener is an interface for plugins which wish
 * to handle incoming connections & pass them off to the agent.
 */
public abstract class AgentConnectionListener {
    private AgentConfig cfg;

    /**
     * Setup the connection listener.
     */
    public AgentConnectionListener(AgentConfig cfg){
        this.cfg = cfg;
    }

    protected AgentConfig getConfig(){
        return this.cfg;
    }

    /**
     * Fetch a new incoming connection and return it to the agent.
     * 
     * @return the incoming connection to the agent
     * @throws AgentConnectionException on generic connection failures
     * @throws InterruptedIOException if a timeout occurred waiting for the
     *                                connection
     */
    public abstract AgentServerConnection getNewConnection()
        throws AgentConnectionException, InterruptedIOException;

    /**
     * Initialize the listener.  This should perform functions such as
     * creating the socket to listen on, etc.
     *
     * @param timeout Timeout in milliseconds to wait for a connection
     */
    public abstract void setup(int timeout)
        throws AgentStartException;


    /**
     * Called by the agent when the connection listener should cleanup 
     * all resources such as sockets, open files, etc.
     */
    public abstract void cleanup();
}

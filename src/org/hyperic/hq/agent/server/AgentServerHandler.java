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

import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface which must be used by any libraries wanting to 
 * be hooked into the CommandDispatcher.
 */

public interface AgentServerHandler {
    /**
     * Get an array of strings with the commands that this handler
     * recognizes.
     */

    public String[] getCommandSet();

    /**
     * Get information about the API, including the version number,
     * which is used to ensure that remote APIs match up with local
     * APIs.
     *
     * @return an AgentAPIInfo object, valid for this server handler.
     */

    public AgentAPIInfo getAPIInfo();

    /**
     * dispatchCommand is the method used to invoke any command, 
     * previously retrieved via getCommandSet.  Note that if the
     * inStream and outStream are used, they must be left in a state
     * which the agent can use to communicate exceptions and results
     * back correctly.
     *
     * @param cmd       name of the command to execute
     * @param arg       argument to pass to the command
     * @param inStream  Input stream which can be used to read special
     *                  command specific data from the remote entity 
     * @param outStream Output stream which can be used to write special
     *                  command specific data to the remote entity.
     *
     * @return The object which was the result of the method invocation
     *
     * @throws AgentRemoteException indicating an exception occurred 
     *                              during execution
     * @see #getCommandSet
     */

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue arg,
                                            InputStream inStream, 
                                            OutputStream outStream)
        throws AgentRemoteException;


    /**
     * inform the plugin that it should startup.  When this method is invoked,
     * a plugin should setup all the internal resources it needs (like 
     * helper threads, etc.)
     * 
     * @param agent The agent.
     */

    public void startup(AgentDaemon agent) 
        throws AgentStartException;

    /**
     * inform the plugin that it should shutdown.  When this method is invoked,
     * a plugin should cleanup all resources (such as open sockets, threads,
     * etc.).
     */

    public void shutdown();
}

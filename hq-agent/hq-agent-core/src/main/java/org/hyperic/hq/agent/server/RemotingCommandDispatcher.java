/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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
 */

package org.hyperic.hq.agent.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The object which manages all libraries wanting to have their
 * commands remotely dispatched.  Libraries can register their
 * server handler with a CommandDispatcher, and when a request
 * is processed, their registered methods will be invoked.
 */
@Component
public class RemotingCommandDispatcher implements CommandDispatcher {

    private Log log = LogFactory.getLog(this.getClass());

    private Map<String, AgentServerHandler> commandHandlers = new ConcurrentHashMap<String, AgentServerHandler>();

    private AgentService agentService;

    /**
     * Register a server handler with the dispatcher.  The server
     * handler will be queried as to what commands it knows about,
     * and that information will be saved locally.
     * @param startedHandlers the handlers implementing the AgentServerHandler interface
     * @see AgentServerHandler
     */
    public void addServerHandlers(List<AgentServerHandler> startedHandlers) {
        for (AgentServerHandler handler : startedHandlers) {
            for (String command : handler.getCommandSet()) {
                commandHandlers.put(command, handler);
            }
        }
    }

    /**
     * Dispatch a method after verifying that the version APIs match up.
     * @param agentCommand Method to invoke
     * @param inStream     Stream which can read from the client
     * @param outStream    Stream which can write to the client
     * @return the return value from the dispatched method
     * @throws AgentRemoteException indicating some error occurred dispatching the method.
     */
    public AgentRemoteValue processRequest(AgentCommand agentCommand, InputStream inStream, OutputStream outStream) throws AgentRemoteException {
        if (commandHandlers.get(agentCommand.getCommand()) == null)
            throw new AgentRemoteException("Unknown command, '" + agentCommand.getCommand() + "'");

        try {

            AgentServerHandler agentServerHandler = commandHandlers.get(agentCommand.getCommand());
            AgentAPIInfo apiInfo = agentServerHandler.getAPIInfo();

            if (!apiInfo.isCompatible(agentCommand.getCommandVersion())) {
                throw new AgentRemoteException(new StringBuilder("Client API mismatch: ")
                        .append(agentCommand.getCommandVersion()).append(" vs. ").append(apiInfo.getVersion()).toString());
            }

            /* each handler is handed an instance of AgentService on startup() prior to this */
            return agentServerHandler.dispatchCommand(agentCommand.getCommand(), agentCommand.getCommandArg(), inStream, outStream);

        }
        catch (AgentRemoteException exc) {
            throw exc;
        }
        catch (Exception exc) {
            log.error("Error while processing request", exc);
            throw new AgentRemoteException(exc.toString());
        }
        catch (LinkageError err) {
            throw new AgentRemoteException(err.toString());
        }
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }
}

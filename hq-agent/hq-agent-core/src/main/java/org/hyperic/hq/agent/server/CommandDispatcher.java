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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.stats.AgentStatsCollector;


/**
 * The object which manages all libraries wanting to have their
 * commands remotely dispatched.  Libraries can register their
 * server handler with a CommandDispatcher, and when a request
 * is processed, their registered methods will be invoked.
 */
public class CommandDispatcher {
    private final Log log = LogFactory.getLog(CommandDispatcher.class);
    private final HashMap<String, AgentServerHandler> commands = new HashMap<String, AgentServerHandler>();
    private final AgentStatsCollector statsCollector = AgentStatsCollector.getInstance();
    private static final String COMMAND_DISPATCHER_INCOMING_COMMAND = "COMMAND_DISPATCHER_INCOMING_COMMAND";
    // this should never happen but the agent is in such an unknown state so I didn't want to leave it out
    private static final String COMMAND_DISPATCHER_ILLEGAL_COMMAND = "COMMAND_DISPATCHER_ILLEGAL_COMMAND";

    CommandDispatcher(){
        statsCollector.register(COMMAND_DISPATCHER_ILLEGAL_COMMAND);
        statsCollector.register(COMMAND_DISPATCHER_INCOMING_COMMAND);
    }

    /**
     * Register a server handler with the dispatcher.  The server
     * handler will be queried as to what commands it knows about,
     * and that information will be saved locally.
     *
     * @param handler an object implementing the AgentServerHandler 
     *                interface
     *
     * @see AgentServerHandler
     */
    void addServerHandler(AgentServerHandler handler){
        String[] cmds = handler.getCommandSet();
        for(int i=0; i<cmds.length; i++){
            commands.put(cmds[i], handler);
            statsCollector.register(COMMAND_DISPATCHER_INCOMING_COMMAND + "_" + cmds[i].toUpperCase());
        }
    }

    /**
     * Dispatch a method after verifying that the version APIs
     * match up.
     *
     * @param cmd        Method to invoke
     * @param inStream   Stream which can read from the client
     * @param outStream  Stream which can write to the client
     *
     * @return the return value from the dispatched method
     * @throws AgentRemoteException indicating some error occurred dispatching
     *                              the method.
     */
    public AgentRemoteValue processRequest(AgentCommand cmd, InputStream inStream, OutputStream outStream)
    throws AgentRemoteException {
        final long start = now();
        final String command = cmd.getCommand();
        boolean legalCommand = false;
        try {
            AgentServerHandler handler;
            AgentAPIInfo apiInfo;
            Object val;
            if((val = commands.get(cmd.getCommand())) == null){
                throw new AgentRemoteException("Unknown command, '" + cmd.getCommand() + "'");
            }
            handler = (AgentServerHandler) val;        
            apiInfo = handler.getAPIInfo();
            if(!apiInfo.isCompatible(cmd.getCommandVersion())){
                throw new AgentRemoteException("Client API mismatch: " + cmd.getCommandVersion() + 
                                               " vs. " + apiInfo.getVersion());
            }
            legalCommand = true;
            if (log.isDebugEnabled()) {
                log.debug("processing cmd=" + cmd.getCommand() + ", arg=" + cmd.getCommandArg());
            }
            return handler.dispatchCommand(cmd.getCommand(), cmd.getCommandArg(), inStream, outStream);
        } catch(AgentRemoteException exc){
            throw exc;
        } catch(Exception exc){
            log.error("Error while processing request", exc);
            throw new AgentRemoteException(exc.toString(), exc);
        } catch(LinkageError err){
            AgentRemoteException e = new AgentRemoteException(err.toString());
            e.initCause(err);
            throw e;
        } finally {
            long end = now();
            if (legalCommand) {
                statsCollector.addStat(end-start, COMMAND_DISPATCHER_INCOMING_COMMAND);
                statsCollector.addStat(end-start, COMMAND_DISPATCHER_INCOMING_COMMAND + "_" + command.toUpperCase());
            } else {
                statsCollector.addStat(1, COMMAND_DISPATCHER_ILLEGAL_COMMAND);
            }
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
}

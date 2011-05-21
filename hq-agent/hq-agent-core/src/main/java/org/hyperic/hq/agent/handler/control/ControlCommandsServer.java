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

package org.hyperic.hq.agent.handler.control;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.bizapp.client.ControlCallback;
import org.hyperic.hq.agent.bizapp.client.StorageProviderFetcher; 
import org.hyperic.hq.agent.server.*;
import org.hyperic.hq.control.agent.ControlCommandsAPI;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.agent.commands.*;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;

@Component
public class ControlCommandsServer implements AgentServerHandler {
    // possibly make this configurable at some point
    private static final String PROP_BACKUPDIR = "file_backup";

    private ControlCommandsAPI verAPI = new ControlCommandsAPI();

    private Log log = LogFactory.getLog(ControlCommandsServer.class);

    private AgentStorageProvider storage;        // Our storage provider

    private ControlCommandsService controlCommandsService;

    private AgentService agentService;

    public AgentAPIInfo getAPIInfo() {
        return this.verAPI;
    }

    public String[] getCommandSet() {
        return ControlCommandsAPI.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream in, OutputStream out) throws AgentRemoteException {
        if (cmd.equals(this.verAPI.command_controlPluginAdd)) {
            ControlPluginAdd_args ca = new ControlPluginAdd_args(args);

            return this.controlPluginAdd(ca);
        } else if (cmd.equals(this.verAPI.command_controlPluginCommand)) {
            ControlPluginCommand_args ca =
                    new ControlPluginCommand_args(args);

            return this.controlPluginCommand(ca);
        } else if (cmd.equals(this.verAPI.command_controlPluginRemove)) {
            ControlPluginRemove_args ca =
                    new ControlPluginRemove_args(args);

            return this.controlPluginRemove(ca);
        } else {
            throw new AgentRemoteException("Unexpected command: " + cmd);
        }
    }

    public void startup(AgentService agentService) throws AgentStartException {
        this.agentService = agentService;
        
        try {
            this.storage = agentService.getStorageProvider();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get storage " +
                    "provider: " + e.getMessage());
        }

        // setup control manager
        ControlPluginManager controlManager;
        ControlCallback client;

        try {
            controlManager =
                    (ControlPluginManager) agentService.
                            getPluginManager(ProductPlugin.TYPE_CONTROL);

            client = setupClient();
        } catch (Exception e) {
            // Problem loading the plugin jars
            throw new AgentStartException("Unable to load control jars: " +
                    e.getMessage());
        }

        controlCommandsService = new ControlCommandsService(controlManager, client);

        AgentTransportLifecycle agentTransportLifecycle;

        try {
            agentTransportLifecycle = agentService.getAgentTransportLifecycle();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get agent transport lifecycle: " +
                    e.getMessage());
        }

        log.info("Registering Control Commands Service with Agent Transport");

        try {
            agentTransportLifecycle.registerService(ControlCommandsClient.class, controlCommandsService);
        } catch (Exception e) {
            throw new AgentStartException("Failed to register Control Commands Service.", e);
        }

        this.log.info("Control Commands Server started up");
    }

    public void shutdown() {
        this.log.info("Control Commands Server shut down");
    }

    private ControlCallback setupClient()
            throws AgentStartException {
        StorageProviderFetcher fetcher;

        fetcher = new StorageProviderFetcher(this.storage);
        return new ControlCallback(fetcher);
    }

    private ControlPluginAdd_result
    controlPluginAdd(ControlPluginAdd_args args)
            throws AgentRemoteException {
        ConfigResponse config = args.getConfigResponse();
        String name = args.getName();
        String type = args.getType();

        controlCommandsService.controlPluginAdd(name, type, config);

        ControlPluginAdd_result result = new ControlPluginAdd_result();

        return result;
    }

    private ControlPluginCommand_result
    controlPluginCommand(ControlPluginCommand_args args)
            throws AgentRemoteException {
        String pluginName = args.getPluginName();
        String pluginType = args.getPluginType();
        String id = args.getId();
        String pluginAction = args.getPluginAction();
        String[] pluginArgs = args.getArgs();

        controlCommandsService.controlPluginCommand(pluginName,
                pluginType,
                id,
                pluginAction,
                pluginArgs);

        return new ControlPluginCommand_result();
    }

    private ControlPluginRemove_result
    controlPluginRemove(ControlPluginRemove_args args)
            throws AgentRemoteException {
        String pluginName = args.getPluginName();

        controlCommandsService.controlPluginRemove(pluginName);

        return new ControlPluginRemove_result();
    }
}

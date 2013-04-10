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

package org.hyperic.hq.control.agent.server;

import java.util.NoSuchElementException;


import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.ControlCallbackClient;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ControlPluginManager;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class to run control actions.  Control actions should happen infrequently
 * enough that the simplicity of spawning a new thread each time should
 * outweigh the performance benefits of using a thread pool.  We can always
 * modify this behavior later.
 */
class ActionThread extends Thread {
    private ControlPluginManager  manager;
    private ControlCallbackClient client;
    private String                pluginName;
    private String                pluginType;
    private String                action;
    private String[]              args;
    private String                id;

    // Time to wait before requeuing a control job that failed because
    // the plugin was busy with a previous action
    protected static final int REQUEUE_INTERVAL = 2 * 1000;

    protected Log log = LogFactory.getLog(ActionThread.class.getName());

    ActionThread(String pluginName, String pluginType, String id,
                 String action, String[] args, ControlCallbackClient client,
                 ControlPluginManager manager)
    {
        this.pluginName = pluginName;
        this.pluginType = pluginType;
        this.id         = id;
        this.action     = action;
        this.args       = args;
        this.client     = client;
        this.manager    = manager;
    }
    
    public void run()
    {
        int result = -1;
        String errMsg = null;

        // add ourselves to the job queue
        this.log.debug("Adding job " + id + " to " + pluginName + " queue");
        this.manager.addJob(pluginName, id);

        // wait for our turn
        while (true) {
            String nextJob;

            try {
                nextJob = this.manager.getNextJob(pluginName);
                if (nextJob.equals(id))
                    break;
            } catch (NoSuchElementException e) {
                // should never happen
                this.log.error("Job queue empty");
                return;
            }

            this.log.debug("Plugin busy with job " + nextJob +
                           " requeueing in " + REQUEUE_INTERVAL + "ms");
            try {
                Thread.sleep(REQUEUE_INTERVAL);
            } catch (InterruptedException e) {}
        }

        this.log.debug("Running job " + id);
        long startTime = System.currentTimeMillis();
        
        final ControlSendCommandResult_args resultsMetadata = 
                this.client.newResultsMetadata(this.pluginName,
                        Integer.parseInt(id), startTime) ;

        try {
            this.manager.doAction(this.pluginName, this.action, this.args, resultsMetadata);
            result = this.manager.getResult(this.pluginName);
            errMsg = this.manager.getMessage(this.pluginName);
        } catch (PluginNotFoundException e) {

            // The plugin has not been configured locally, or the plugin
            // type is not found on this agent.  Try to get the config
            // from the server
            try {

                this.log.info("Fetching plugin configuration from server");
                byte[] configBytes = 
                    this.client.controlGetPluginConfiguration(this.pluginName);

                if (configBytes == null || configBytes.length == 0) {
                    // log something or send a message back to the server
                    errMsg = "Plugin configuration not found";
                    this.log.error(errMsg);
                    return;
                }

                ConfigResponse config = ConfigResponse.decode(configBytes);

                this.log.info("Config finished, running control action");
                this.manager.createControlPlugin(this.pluginName,
                                                 this.pluginType, config);
                this.manager.doAction(this.pluginName, this.action, this.args, resultsMetadata);
                result = this.manager.getResult(this.pluginName);
                errMsg = this.manager.getMessage(this.pluginName);
            } catch (Exception exc) {
                errMsg = "Unable to fetch plugin configuration: " +
                    exc.getMessage();
                this.log.error(errMsg, exc);
            }
        } catch (PluginException e) {
            errMsg = "Unable to run control action:  " +e.getMessage();
            this.log.error(errMsg, e);
        } finally {

            // Remove this job from the queue
            try {
                this.manager.removeNextJob(pluginName);
                this.log.debug("Removed job " + id + " from the queue");
            } catch (NoSuchElementException e) {
                // will never happen
            }

            // Lastly, send the status back to the server
            try {
                this.client.controlSendCommandResult(resultsMetadata, result, errMsg);
            } catch (AgentCallbackClientException e) {
                this.log.error("Unable to send command result: " +
                               e.getMessage());
            }
        }
    }
}

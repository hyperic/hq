/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
 *
 */
package org.hyperic.hq.control.server.session;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.agent.client.ControlCommandsClientFactory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ControlActionExecutor}
 * @author jhickey
 * 
 */
@Component
public class ControlActionExecutorImpl implements ControlActionExecutor {

    private final Log log = LogFactory.getLog(ControlActionExecutorImpl.class);

    private ControlScheduleManager controlScheduleManager;
    private PlatformManager platformManager;

    private ControlCommandsClientFactory controlCommandsClientFactory;

    @Autowired
    public ControlActionExecutorImpl(ControlScheduleManager controlScheduleManager,
                                     PlatformManager platformManager,
                                     ControlCommandsClientFactory controlCommandsClientFactory) {
        this.controlScheduleManager = controlScheduleManager;
        this.platformManager = platformManager;
        this.controlCommandsClientFactory = controlCommandsClientFactory;
    }

    public Integer executeControlAction(AppdefEntityID id, String subjectName, Date dateScheduled,
                                        Boolean scheduled, String description, String action,
                                        String args) throws PluginException {
        return executeControlAction(id, null, null, subjectName, dateScheduled, scheduled,
            description, action, args);
    }

    public Integer executeControlAction(AppdefEntityID id, AppdefEntityID gid, Integer batchId,
                                        String subjectName, Date dateScheduled, Boolean scheduled,
                                        String description, String action, String args)
        throws PluginException {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();

        long startTime = System.currentTimeMillis();
        Integer commandHistoryId = null;
        String errorMsg = null;
        Integer groupId = (gid != null) ? gid.getId() : null;

        try {
            ControlCommandsClient client = controlCommandsClientFactory.getClient(id);
            String pluginName = id.toString();
            String productName = platformManager.getPlatformPluginName(id);

            // Regular command

            if (debug)
                watch.markTimeBegin("createHistory");

            commandHistoryId = controlScheduleManager.createHistory(id, groupId, batchId,
                subjectName, action, args, scheduled, startTime, startTime,
                dateScheduled.getTime(), ControlConstants.STATUS_INPROGRESS, description, null);

            if (debug) {
                watch.markTimeEnd("createHistory");
                watch.markTimeBegin("controlPluginCommand");
            }

            client.controlPluginCommand(pluginName, productName, commandHistoryId, action, args);

            if (debug)
                watch.markTimeEnd("controlPluginCommand");

        } catch (AgentNotFoundException e) {
            errorMsg = "Agent not found: " + e.getMessage();
        } catch (AgentConnectionException e) {
            errorMsg = "Error getting agent connection: " + e.getMessage();
        } catch (AgentRemoteException e) {
            errorMsg = "Agent error: " + e.getMessage();
        } catch (SystemException e) {
            errorMsg = "System error";
        } catch (AppdefEntityNotFoundException e) {
            errorMsg = "System error";
        } finally {
            if (debug) {
                log
                    .debug("executeControlAction: " + watch + " {resource=" + id + ", action=" +
                           action + ", dateScheduled=" + dateScheduled + ", error=" + errorMsg +
                           "}");
            }

            if (errorMsg != null) {
                this.log.error("Unable to execute command: " + errorMsg);

                // Add the failed job to the history
                try {
                    AppdefEntityID aid = (gid != null) ? gid : id;

                    // Remove the old history
                    if (commandHistoryId != null) {
                        controlScheduleManager.removeHistory(commandHistoryId);
                    }

                    // Add the new one
                    controlScheduleManager.createHistory(aid, groupId, batchId, subjectName,
                        action, args, scheduled, startTime, System.currentTimeMillis(),
                        dateScheduled.getTime(), ControlConstants.STATUS_FAILED, description,
                        errorMsg);
                } catch (Exception exc) {
                    this.log.error("Unable to create history entry for " + "failed control action");
                }

                throw new PluginException(errorMsg);
            }
        }

        return commandHistoryId;
    }

    /**
     * For testing without the agent
     * @param controlCommandsClientFactory An implementation of @{link
     *        {@link ControlCommandsClientFactory} to use for invoking control
     *        actions
     */
    void setControlCommandsClientFactory(ControlCommandsClientFactory controlCommandsClientFactory) {
        this.controlCommandsClientFactory = controlCommandsClientFactory;
    }

}

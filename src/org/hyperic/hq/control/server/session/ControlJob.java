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

package org.hyperic.hq.control.server.session;

import java.util.Date;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.agent.client.ControlCommandsClientFactory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.control.shared.ControlScheduleManagerUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.server.session.BaseJob;

public abstract class ControlJob extends BaseJob {

    // The time to wait inbetween checking if a control action has
    // finished.  This is used to synchronize control calls to the
    // agent.
    protected static final int JOB_WAIT_INTERVAL = 500;
    
    // Configuration paramaters
    public static final String PROP_ACTION         = "action";
    public static final String PROP_ARGS           = "args";

    protected Log log = LogFactory.getLog(ControlJob.class);

    /**
     * Do a control command on a single appdef entity
     *
     * @return The job id
     */
    protected Integer doAgentControlCommand(AppdefEntityID id,
                                            AppdefEntityID gid,
                                            Integer batchId,
                                            AuthzSubjectValue subject,
                                            Date dateScheduled,
                                            Boolean scheduled,
                                            String description,
                                            String action,
                                            String args)
        throws PluginException
    {
        long startTime = System.currentTimeMillis();
        ControlHistory commandHistory = null;
        String errorMsg = null;
        Integer groupId = (gid != null) ? gid.getId() : null;
        
        try {
            ControlCommandsClient client =
                ControlCommandsClientFactory.getInstance().getClient(id);
            String pluginName  = id.toString();
            String productName = PlatformManagerUtil.getLocalHome().create()
                .getPlatformPluginName(id);

            ControlScheduleManagerLocal cLocal =
                ControlScheduleManagerUtil.getLocalHome().create();

            // Regular command

            commandHistory = 
                cLocal.createHistory(id, groupId, batchId,
                                     subject.getName(), action, args,
                                     scheduled, startTime, startTime, 
                                     dateScheduled.getTime(),
                                     ControlConstants.STATUS_INPROGRESS, 
                                     description, null);

            client.controlPluginCommand(pluginName, 
                                        productName,
                                        commandHistory.getId(),
                                        action, args);

        } catch (AgentNotFoundException e) {
            errorMsg = "Agent not found: " + e.getMessage();
        } catch (AgentConnectionException e) {
            errorMsg = "Error getting agent connection: " + e.getMessage();
        } catch (AgentRemoteException e) {
            errorMsg = "Agent error: " + e.getMessage();
        } catch (SystemException e) {
            errorMsg = "System error";
        } catch (NamingException e) {
            errorMsg = "System error";
        } catch (CreateException e) {
            errorMsg = "System error";
        } catch (AppdefEntityNotFoundException e) {
            errorMsg = "System error";
        } finally {
        
            if (errorMsg != null) {
                this.log.error("Unable to execute command: " + errorMsg);
            
                // Add the failed job to the history
                try {
                    AppdefEntityID aid = (gid != null) ? gid : id;

                    ControlScheduleManagerLocal cLocal =
                        ControlScheduleManagerUtil.getLocalHome().create();

                    // Remove the old history
                    if (commandHistory != null)
                        cLocal.removeHistory(commandHistory.getId());

                    // Add the new one
                    cLocal.createHistory(aid, groupId, batchId,
                                         subject.getName(), action, args,
                                         scheduled, startTime, 
                                         System.currentTimeMillis(),
                                         dateScheduled.getTime(),
                                         ControlConstants.STATUS_FAILED,
                                         description, errorMsg);
                } catch (Exception exc) {
                    this.log.error("Unable to create history entry for " +
                                   "failed control action");
                }

                throw new PluginException(errorMsg);
            }
        }

        return commandHistory.getId();
    }
}

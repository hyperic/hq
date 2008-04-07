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

package org.hyperic.hq.autoinventory.server.session;

import java.io.IOException;
import java.util.Date;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.scheduler.server.session.BaseJob;
import org.quartz.JobDataMap;

public abstract class AIJob extends BaseJob {

    // The time to wait in between checking if an AI scan has
    // finished.  This is used to synchronize calls to the
    // agent.
    protected static final int JOB_WAIT_INTERVAL = 10000;
    
    // Configuration parameters
    public static final String PROP_CONFIG = "scanConfig";
    public static final String PROP_SCAN_OS = "scanOs";

    public static final String PROP_SCANNAME = "scanName";
    public static final String PROP_SCANDESC = "scanDesc";

    protected Log log = LogFactory.getLog(AIJob.class);

    /**
     * Do a control command on a single appdef entity
     *
     * @return The job id
     */
    protected Integer doAgentScan(AppdefEntityID id,
                                  AppdefEntityID gid,
                                  Integer groupId,
                                  Integer batchId,
                                  AuthzSubject subject,
                                  Date dateScheduled,
                                  Boolean scheduled,
                                  ScanConfigurationCore scanConfig,
                                  String scanName,
                                  String scanDesc)
        throws AutoinventoryException
    {
        long startTime = System.currentTimeMillis();
        AIHistory commandHistory = null;
        String errorMsg = null;

        try {
            AICommandsClient client = AIUtil.getClient(id);
            commandHistory =
                createHistory(id, groupId, batchId,
                              subject.getName(),
                              scanConfig, scanName, scanDesc,
                              scheduled, startTime, startTime,
                              dateScheduled.getTime(),
                              AIScheduleManagerEJBImpl.STATUS_STARTED,
                              null);
            client.startScan(scanConfig);

        } catch (AutoinventoryException e) {
            errorMsg = "AI exception: " + e.getMessage();
        } catch (PermissionException e) {
            errorMsg = "Permission denied: " + e.getMessage();
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
        } finally {
        
            if (errorMsg != null) {
                this.log.error("Unable to execute command: " + errorMsg);

                try {
                    // Add the failed job to the history
                    if (commandHistory != null) {
                        removeHistory(commandHistory);
                    }
                    createHistory(id, groupId, batchId,
                                  subject.getName(), 
                                  scanConfig, scanName, scanDesc,
                                  scheduled, startTime, 
                                  System.currentTimeMillis(),
                                  dateScheduled.getTime(),
                                  AIScheduleManagerEJBImpl.STATUS_COMPLETED,
                                  errorMsg);
                } catch (Exception exc) {
                    this.log.error("Unable to create history entry for " +
                                   "failed autoinventory scan");
                }

                throw new AutoinventoryException(errorMsg);
            }

        }

        return commandHistory.getId();
    }

    protected void removeHistory(AIHistory history)
        throws NamingException, CreateException
    {
        AutoinventoryManagerLocal alocal = getAutoInventoryManager();
        alocal.removeHistory(history);
    }

    protected AIHistory createHistory(AppdefEntityID id,
                                      Integer groupId,
                                      Integer batchId,
                                      String subjectName,
                                      ScanConfigurationCore config,
                                      String scanName,
                                      String scanDesc,
                                      Boolean scheduled,
                                      long startTime,
                                      long stopTime,
                                      long scheduleTime,
                                      String status,
                                      String errorMessage)
        throws CreateException, NamingException, AutoinventoryException
    {
        AutoinventoryManagerLocal alocal = getAutoInventoryManager();
        return alocal.createAIHistory(id, groupId, batchId, subjectName,
                                      config, scanName, scanDesc,
                                      scheduled, startTime,
                                      stopTime, scheduleTime,
                                      status,
                                      errorMessage);
    }

    protected void updateHistory(Integer jobId, long endTime,
                                 String status, String message)
        throws FinderException, CreateException, NamingException
    {
        AutoinventoryManagerLocal alocal = getAutoInventoryManager();
        alocal.updateAIHistory(jobId, endTime, status, message);
    }

    private AutoinventoryManagerLocal aimanager = null;
    protected AutoinventoryManagerLocal getAutoInventoryManager()
        throws NamingException, CreateException {
        if (aimanager == null) {
            aimanager = AutoinventoryManagerUtil.getLocalHome().create();
        }
        return aimanager;
    }

    /**
     * loads the scan config object
     */
    protected ScanConfigurationCore getScanConfig(JobDataMap dataMap)
        throws IOException
    {
        String config = (String)dataMap.get(PROP_CONFIG);
        
        try {
            return ScanConfigurationCore.decode(config);
        } catch (AutoinventoryException e) {
            throw new IOException(e.getMessage());
        }
    }
}

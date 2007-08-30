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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.shared.ControlActionTimeoutException;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlManagerLocal;
import org.hyperic.hq.control.shared.ControlManagerUtil;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.control.shared.ControlScheduleManagerUtil;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * A quartz job class for handling control actions on a group entity
 */
public class ControlActionGroupJob extends ControlJob {

    // Default timeout is 10 minutes.  If a plugin does not define its
    // own timeout, this is the value that will be used.
    private static final int DEFAULT_TIMEOUT = 10 * 60 * 1000;

    private ControlManagerLocal manager = null;

    protected Log log = 
        LogFactory.getLog(ControlActionGroupJob.class.getName());    

    public void executeInSession(JobExecutionContext context)
        throws JobExecutionException
    {
        // Job configuration
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Integer idVal = new Integer(dataMap.getString(PROP_ID));
        Integer type = new Integer(dataMap.getString(PROP_TYPE));
        AppdefEntityID id = new AppdefEntityID(type.intValue(), idVal.intValue());
        Integer subjectId = new Integer(dataMap.getString(PROP_SUBJECT));
        AuthzSubjectValue subject = getSubject(subjectId);
        
        String action     = dataMap.getString(PROP_ACTION);
        String args = dataMap.getString(PROP_ARGS);
        Boolean scheduled = Boolean.valueOf(dataMap.getString(PROP_SCHEDULED));
        
        int[] order   = getOrder(dataMap.getString(PROP_ORDER));
        String description = dataMap.getString(PROP_DESCRIPTION);

        long startTime = System.currentTimeMillis();

        Integer jobId = null;
        Integer groupId = null;
        String status = ControlConstants.STATUS_COMPLETED;
        String errMsg = null;

        try {
            Trigger trigger = context.getTrigger();
            Date dateScheduled = trigger.getPreviousFireTime();
            int longestTimeout = 0;

            // create group entry in the history
            ControlScheduleManagerLocal cLocal =
                ControlScheduleManagerUtil.getLocalHome().create();
            ControlHistory historyValue =
                cLocal.createHistory(id, null, null, subject.getName(), 
                                     action, args, scheduled, startTime,
                                     startTime, dateScheduled.getTime(),
                                     ControlConstants.STATUS_INPROGRESS,
                                     description, null);
            groupId = historyValue.getId();

            // get the group members and iterate over them
            List groupMembers = GroupUtil.getCompatGroupMembers(
                subject, id, order, PageControl.PAGE_ALL);

            if (groupMembers.isEmpty()) {
                errMsg = "Group contains no resources";
                return;
            }

            ArrayList jobIds = new ArrayList();
            
            for (Iterator i = groupMembers.iterator(); i.hasNext();) {
                AppdefEntityID entity = (AppdefEntityID) i.next();

                int timeout = getTimeout(subject, entity);
                if (timeout > longestTimeout)
                    longestTimeout = timeout;

                jobId = doAgentControlCommand(entity,
                                              id,
                                              historyValue.getId(),
                                              subject,
                                              dateScheduled,
                                              scheduled, 
                                              description,
                                              action, args);
                
                // Keep a reference to all the job ids in case we need to
                // verify later they have completed successfully.

                jobIds.add(jobId);

                // If the job is ordered, synchronize the commands
                if (order.length > 0) {
                    waitForJob(jobId, timeout);
                }
            }

            if (order.length == 0) {
                waitForAllJobs(jobIds, longestTimeout);
            }               

        } catch (GroupNotCompatibleException e) {
            errMsg = e.getMessage();
        } catch (ControlActionTimeoutException e) {
            errMsg = e.getMessage();
        } catch (PluginException e) {
            errMsg = e.getMessage();
        } catch (PermissionException e) { 
            // This will only happen if the permisions on a resource change
            // after the job was scheudled.
            errMsg = "Permission denied: " + e.getMessage();
        } catch (AppdefEntityNotFoundException e) {
            // Shouldnt happen
            errMsg = "System error, resource not found: " + e.getMessage();
        } catch (ApplicationException e) {
            // Shouldnt happen
            errMsg = "Application error: " + e.getMessage();
        } catch (CreateException e) {
            // Shouldnt happen
            errMsg = "System error";
        } catch (NamingException e) {
            // Shouldt happen
            errMsg = "System error";
        } finally {

            if (groupId != null) {
          
                if (errMsg != null)
                    status = ControlConstants.STATUS_FAILED;

                try {
                    // Update group history entry
                    ControlScheduleManagerLocal cLocal =
                        ControlScheduleManagerUtil.getLocalHome().create();
                    cLocal.updateHistory(groupId, System.currentTimeMillis(),
                                         status, errMsg);
                
                    ControlHistory cv = cLocal.getJobHistoryValue(groupId);
                    
                    // Send a control event
                    ControlEvent event = 
                        new ControlEvent(cv.getSubject(),
                                         cv.getEntityType().intValue(),
                                         cv.getEntityId(),
                                         cv.getAction(),
                                         cv.getScheduled().booleanValue(),
                                         cv.getDateScheduled(),
                                         status);
        
                    Messenger sender = new Messenger();
                    sender.publishMessage("topic/eventsTopic", event);
                } catch (Exception e) {
                    this.log.error("Unable to update control history: " +
                                   e.getMessage());
                }
            }
        }
    }

    // Private helper methods

    private void waitForJob(Integer jobId, int timeout)
        throws ControlActionTimeoutException,
               ApplicationException
    {
        ControlScheduleManagerLocal cLocal;
        
        try {
            cLocal = ControlScheduleManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }

        long start = System.currentTimeMillis();
        
        while (System.currentTimeMillis() < start + timeout) {
            ControlHistory cValue = cLocal.getJobHistoryValue(jobId);
            String status = cValue.getStatus();
            if (status.equals(ControlConstants.STATUS_COMPLETED))
                return;

            if (status.equals(ControlConstants.STATUS_FAILED)) {
                String err = "Job id " + jobId + " failed: " +
                             cValue.getMessage();
                throw new ControlActionTimeoutException(err);
            }

            // else wait some more
            try {
                Thread.sleep(JOB_WAIT_INTERVAL);
            } catch (InterruptedException e) {}
        }

        throw new ControlActionTimeoutException("Timeout waiting for job id " +
                                                jobId);
    }

    private void waitForAllJobs(List ids, int timeout)
        throws ControlActionTimeoutException,
               ApplicationException
    {
        ControlScheduleManagerLocal cLocal;

        try {
            cLocal = ControlScheduleManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }

        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() < start + timeout) {
            
            // Run through all the jobs, removing each job that completes
            // successfully.  If we run across any job that fails, the entire
            // group action fails
            
            if(ids.isEmpty())
                return;

            for (Iterator i = ids.iterator(); i.hasNext(); ) {
                Integer jobId = (Integer)i.next();
                
                ControlHistory cValue = cLocal.getJobHistoryValue(jobId);
                String status = cValue.getStatus();
                    
                if (status.equals(ControlConstants.STATUS_COMPLETED)) {
                    i.remove();
                    continue;
                }

                if (status.equals(ControlConstants.STATUS_FAILED)) {
                    String err = cValue.getMessage();
                    throw new ControlActionTimeoutException(err);
                }
            }

            // else wait some more
            try {
                Thread.sleep(JOB_WAIT_INTERVAL);
            } catch (InterruptedException e) {}
        }

        throw new ControlActionTimeoutException("Timeout waiting for all " +
                                                "jobs to complete");
    }

    /**
     * Utility to return the configured timeout of a resource in milliseconds
     */
    private int getTimeout(AuthzSubjectValue subject, AppdefEntityID id)
    {
        ConfigResponse config;

        // Get the control manager
        if (this.manager == null) {
            try { 
                manager = ControlManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                this.log.error("Unable to get control manager, using default " +
                               "timeout value of " + DEFAULT_TIMEOUT);
                return DEFAULT_TIMEOUT;
            }
        }
     
        int timeout;
        try {
            config = manager.getConfigResponse(subject, id);
            String strTimeout = config.getValue(ControlPlugin.PROP_TIMEOUT);
            if (strTimeout == null)
                return DEFAULT_TIMEOUT;
            timeout = Integer.parseInt(strTimeout);
        } catch (Exception e) {
            return DEFAULT_TIMEOUT;
        }

        return timeout * 1000;
    }    
}

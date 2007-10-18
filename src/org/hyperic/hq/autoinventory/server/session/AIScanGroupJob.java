/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.util.pager.PageControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * A quartz job class for handling AI scans on a group entity
 */
public class AIScanGroupJob extends AIJob {

    protected Log log = 
        LogFactory.getLog(AIScanGroupJob.class.getName());    

    public void executeInSession(JobExecutionContext context)
        throws JobExecutionException
    {
        // Job configuration
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Integer idVal = new Integer(dataMap.getString(PROP_ID));
        Integer type = new Integer(dataMap.getString(PROP_TYPE));
        AppdefEntityID id = new AppdefEntityID(type.intValue(), idVal.intValue());
        Integer subjectId = new Integer(dataMap.getString(PROP_SUBJECT));
        AuthzSubject subject = getSubject(subjectId);
        
        Boolean scheduled = new Boolean(dataMap.getString(PROP_SCHEDULED));
        
        long startTime = System.currentTimeMillis();

        // AIHistoryLocal historyLocal = null; 
        Integer jobId = null;
        // String status = ControlConstants.STATUS_COMPLETED;
        String errMsg = null;


        try {
            int[] order = getOrder(dataMap.getString(PROP_ORDER));
        
            ScanConfigurationCore scanConfig;
            scanConfig = getScanConfig(dataMap);
            
            String scanName = dataMap.getString(PROP_SCANNAME);
            String scanDesc = dataMap.getString(PROP_SCANDESC);
            
            Trigger trigger = context.getTrigger();
            Date dateScheduled = trigger.getPreviousFireTime();
            int longestTimeout = 0;

            // create group entry in the history
            /*
            historyLocal =
                createHistory(id, null, null, subject.getName(), action, scheduled,
                              startTime, startTime, dateScheduled.getTime(),
                              ControlConstants.STATUS_INPROGRESS, null);
            */
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

                int timeout = 0; // getTimeout(subject, entity);
                if (timeout > longestTimeout)
                    longestTimeout = timeout;

                jobId = doAgentScan(entity,
                                    id,
                                    id.getId(),
                                    new Integer(42), // historyLocal.getId(),
                                    subject.getAuthzSubjectValue(),
                                    dateScheduled, 
                                    scheduled,
                                    scanConfig,
                                    scanName,
                                    scanDesc);
                
                // Keep a reference to all the job ids in case we need to
                // verify later they have completed successfully.

                jobIds.add(jobId);

                // If the job is ordered, synchronize the commands
                if (order != null) {
                    // waitForJob(jobId, timeout);
                }
            }

            if (order == null) {
                // waitForAllJobs(jobIds, longestTimeout);
            }               

        } catch (IOException e) {
            errMsg = e.getMessage();
        } catch (AutoinventoryException e) {
            errMsg = e.getMessage();
        } catch (GroupNotCompatibleException e) {
            errMsg = e.getMessage();
        } catch (PermissionException e) { 
            // This will only happen if the permisions on a resource change
            // after the job was scheudled.
            errMsg = "Permission denied: " + e.getMessage();
        } catch (AppdefEntityNotFoundException e) {
            // Shouldnt happen
            errMsg = "System error, resource not found: " + e.getMessage();
        } catch (SystemException e) {
            // Shouldnt happen
            errMsg = "System error";
            /*
        } catch (FinderException e) {
            // Shouldnt happen
            errMsg = "System error";
            */
        } finally {
            /*
            if (historyLocal != null) {
          
                if (errMsg != null)
                    status = ControlConstants.STATUS_FAILED;
                
                // Update group history entry
                long endTime = System.currentTimeMillis();
                historyLocal.setEndTime(endTime);
                historyLocal.setDuration(endTime - 
                                         historyLocal.getStartTime());
                historyLocal.setStatus(status);
                historyLocal.setMessage(errMsg);


                // Send a control event
                ControlEvent event = 
                    new ControlEvent(historyLocal.getSubject(),
                                     historyLocal.getEntityType().intValue(),
                                     historyLocal.getId(),
                                     historyLocal.getAction(),
                                     historyLocal.getScheduled().booleanValue(),
                                     historyLocal.getDateScheduled(),
                                     status);
        
                Messenger sender = new Messenger();
                sender.publishMessage("topic/eventsTopic", event);
            }
            */
        }
    }
    
    // Private helper methods
    /*
    private AIHistoryValue getJobHistoryValue(Integer jobId)
        throws NamingException, FinderException, CreateException
    {
        AIHistoryPK pk = new AIHistoryPK(jobId);
        AIHistoryLocalHome home = getHistoryDAO(); 
        AIHistoryLocal local = home.findByPrimaryKey(pk);

        return local.getAIHistoryValue();
    }

    private void waitForJob(Integer jobId, int timeout)
        throws ControlActionTimeoutException,
               NamingException, CreateException, FinderException
    {
        long start = System.currentTimeMillis();
        
        while (System.currentTimeMillis() < start + timeout) {

            AIHistoryValue cValue = getJobHistoryValue(jobId);
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
               CreateException, NamingException, FinderException
    {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() < start + timeout) {
            
            // Run through all the jobs, removing each job that completes
            // successfully.  If we run across any job that fails, the entire
            // group action fails
            
            if(ids.isEmpty())
                return;

            for (Iterator i = ids.iterator(); i.hasNext(); ) {
                Integer jobId = (Integer)i.next();
                
                AIHistoryValue cValue = getJobHistoryValue(jobId);
                String status = cValue.getStatus();
                    
                if (status.equals(ControlConstants.STATUS_COMPLETED)) {
                    i.remove();
                    continue;
                }

                if (status.equals(ControlConstants.STATUS_FAILED)) {
                    String msg = cValue.getMessage();
                    throw new ControlActionTimeoutException(msg);
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
    */

    /**
     * Utility to return the configured timeout of a resource in milliseconds
     */
    /*
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
    */
}

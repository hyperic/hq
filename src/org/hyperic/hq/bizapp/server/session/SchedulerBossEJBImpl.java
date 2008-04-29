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

package org.hyperic.hq.bizapp.server.session;

import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.scheduler.server.session.SchedulerEJBImpl;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.utils.Key;


/** 
 * The BizApp's interface to the Scheduler Subsystem.
 *
 * @ejb:bean name="SchedulerBoss"
 *      jndi-name="ejb/bizapp/SchedulerBoss"
 *      local-jndi-name="LocalSchedulerBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 */
public class SchedulerBossEJBImpl implements SessionBean {
    private SessionManager manager;

    /**
     * Constructor.
     */
    public SchedulerBossEJBImpl() {
        this.manager = SessionManager.getInstance();
    }

    //-------------------------------------------------------------------------
    //-- interface methods
    //-------------------------------------------------------------------------
    /**
     * Get a list of all job groups in the scheduler.
     *
     * @ejb:interface-method
     */
    public String[] getJobGroupNames(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().getJobGroupNames();
    }

    /**
     * Get a list of all trigger groups in the scheduler.
     *
     * @ejb:interface-method
     */
    public String[] getTriggerGroupNames(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().getTriggerGroupNames();
    }

    /**
     * Get a list of all jobs in a given group.
     *
     * @param jobGroup the group whose jobs should be listed
     *
     * @ejb:interface-method
     */
    public String[] getJobNames(int sessionID, String jobGroup)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().getJobNames(jobGroup);
    }

    /**
     * Get a list of all triggers in a given group.
     *
     * @param triggerGroup the group whose triggers should be listed
     *
     * @ejb:interface-method
     */
    public String[] getTriggerNames(int sessionID, String triggerGroup)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().getTriggerNames(triggerGroup);
    }

    /**
     * Get a list of all currently-executing jobs.
     *
     * @ejb:interface-method
     */
    public Key[] getCurrentlyExecutingJobs(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        List execJobs = getSched().getCurrentlyExecutingJobs();
        Key[] jobKeys = new Key[execJobs.size()];
        for (int i=0; i<jobKeys.length; ++i) {
            JobExecutionContext jec = (JobExecutionContext)execJobs.get(i);
            JobDetail jd = jec.getJobDetail();
            jobKeys[i] = new Key( jd.getName(), jd.getGroup() );
        }

        return jobKeys;
    }

    /**
     * Remove a previously-scheduled job and all of its associated
     * triggers.
     *
     * @param jobName the name of the job to be removed
     * @param groupName the name of the group
     * @return true if a job was removed, false otherwise
     *
     * @ejb:interface-method
     */
    public boolean deleteJob(int sessionID, String jobName, String groupName)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().deleteJob(jobName, groupName);
    }

    /**
     * Remove the given schedule from the scheduler.
     *
     * @param scheduleName the name of the schedule to delete
     * @param groupName the name of the group
     * @return true if a schedule was deleted, false otherwise
     *
     * @ejb:interface-method
     */
    public boolean deleteSchedule(int sessionID, String scheduleName,
                                  String groupName)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        return getSched().unscheduleJob(scheduleName, groupName);
    }

    /**
     * Remove the given group of schedules from the scheduler.
     *
     * @param groupName the name of the group
     * @return number of schedules deleted
     *
     * @ejb:interface-method
     */
    public int deleteScheduleGroup(int sessionID, String groupName)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        String[] triggersInGroup = getSched().getTriggerNames(groupName);
        int numDeleted = 0;
        for (int i=0; i<triggersInGroup.length; ++i) {
            if ( getSched().unscheduleJob(triggersInGroup[i], groupName) ) {
                ++numDeleted;
            }
        }

        return numDeleted;
    }

    /**
     * Remove the given group of jobs from the scheduler.
     *
     * @param groupName the name of the group
     * @return number of jobs deleted
     *
     * @ejb:interface-method
     */
    public int deleteJobGroup(int sessionID, String groupName)
        throws SessionNotFoundException, SessionTimeoutException,
               SchedulerException
    {
        manager.getSubjectPojo(sessionID);
        String[] jobsInGroup = getSched().getJobNames(groupName);
        int numDeleted = 0;
        for (int i=0; i<jobsInGroup.length; ++i) {
            if ( getSched().deleteJob(jobsInGroup[i], groupName) ) { ++numDeleted; }
        }

        return numDeleted;
    }

    //-------------------------------------------------------------------------
    //-- session-bean methods
    //-------------------------------------------------------------------------
    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}


    //-------------------------------------------------------------------------
    //-- private helpers
    //-------------------------------------------------------------------------
    private SchedulerLocal getSched() {
        return SchedulerEJBImpl.getOne();
    }
}

// EOF

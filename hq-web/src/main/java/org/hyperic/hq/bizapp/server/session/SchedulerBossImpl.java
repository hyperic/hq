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

import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.SchedulerBoss;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Scheduler Subsystem.
 * 
 */
@Service
@Transactional
public class SchedulerBossImpl implements SchedulerBoss {

    private SessionManager sessionManager;
    private Scheduler scheduler;

    @Autowired
    public SchedulerBossImpl(SessionManager sessionManager, Scheduler scheduler) {
        this.sessionManager = sessionManager;
        this.scheduler = scheduler;
    }

    // -------------------------------------------------------------------------
    // -- interface methods
    // -------------------------------------------------------------------------
    /**
     * Get a list of all job groups in the scheduler.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public String[] getJobGroupNames(int sessionID) throws SessionNotFoundException, SessionTimeoutException,
        SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.getJobGroupNames();
    }

    /**
     * Get a list of all trigger groups in the scheduler.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public String[] getTriggerGroupNames(int sessionID) throws SessionNotFoundException, SessionTimeoutException,
        SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.getTriggerGroupNames();
    }

    /**
     * Get a list of all jobs in a given group.
     * 
     * @param jobGroup the group whose jobs should be listed
     * 
     * 
     */
    @Transactional(readOnly=true)
    public String[] getJobNames(int sessionID, String jobGroup) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.getJobNames(jobGroup);
    }

    /**
     * Get a list of all triggers in a given group.
     * 
     * @param triggerGroup the group whose triggers should be listed
     * 
     * 
     */
    @Transactional(readOnly=true)
    public String[] getTriggerNames(int sessionID, String triggerGroup) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.getTriggerNames(triggerGroup);
    }

    /**
     * Get a list of all currently-executing jobs.
     * 
     * 
     */
    @Transactional(readOnly=true)
    @SuppressWarnings("unchecked")
    public Key[] getCurrentlyExecutingJobs(int sessionID) throws SessionNotFoundException, SessionTimeoutException,
        SchedulerException {
        sessionManager.authenticate(sessionID);
        List<JobExecutionContext> execJobs = scheduler.getCurrentlyExecutingJobs();
        Key[] jobKeys = new Key[execJobs.size()];
        for (int i = 0; i < jobKeys.length; ++i) {
            JobExecutionContext jec = execJobs.get(i);
            JobDetail jd = jec.getJobDetail();
            jobKeys[i] = new Key(jd.getName(), jd.getGroup());
        }

        return jobKeys;
    }

    /**
     * Remove a previously-scheduled job and all of its associated triggers.
     * 
     * @param jobName the name of the job to be removed
     * @param groupName the name of the group
     * @return true if a job was removed, false otherwise
     * 
     * 
     */
    public boolean deleteJob(int sessionID, String jobName, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.deleteJob(jobName, groupName);
    }

    /**
     * Remove the given schedule from the scheduler.
     * 
     * @param scheduleName the name of the schedule to delete
     * @param groupName the name of the group
     * @return true if a schedule was deleted, false otherwise
     * 
     * 
     */
    public boolean deleteSchedule(int sessionID, String scheduleName, String groupName)
        throws SessionNotFoundException, SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        return scheduler.unscheduleJob(scheduleName, groupName);
    }

    /**
     * Remove the given group of schedules from the scheduler.
     * 
     * @param groupName the name of the group
     * @return number of schedules deleted
     * 
     * 
     */
    public int deleteScheduleGroup(int sessionID, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        String[] triggersInGroup = scheduler.getTriggerNames(groupName);
        int numDeleted = 0;
        for (int i = 0; i < triggersInGroup.length; ++i) {
            if (scheduler.unscheduleJob(triggersInGroup[i], groupName)) {
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
     * 
     */
    public int deleteJobGroup(int sessionID, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException {
        sessionManager.authenticate(sessionID);
        String[] jobsInGroup = scheduler.getJobNames(groupName);
        int numDeleted = 0;
        for (int i = 0; i < jobsInGroup.length; ++i) {
            if (scheduler.deleteJob(jobsInGroup[i], groupName)) {
                ++numDeleted;
            }
        }

        return numDeleted;
    }

}

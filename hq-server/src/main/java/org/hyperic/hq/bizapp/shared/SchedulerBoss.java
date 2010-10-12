/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.bizapp.shared;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.quartz.SchedulerException;

/**
 * Local interface for SchedulerBoss.
 */
public interface SchedulerBoss {
    /**
     * Get a list of all job groups in the scheduler.
     */
    public java.lang.String[] getJobGroupNames(int sessionID) throws SessionNotFoundException, SessionTimeoutException,
        SchedulerException;

    /**
     * Get a list of all trigger groups in the scheduler.
     */
    public java.lang.String[] getTriggerGroupNames(int sessionID) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Get a list of all jobs in a given group.
     * @param jobGroup the group whose jobs should be listed
     */
    public java.lang.String[] getJobNames(int sessionID, String jobGroup) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Get a list of all triggers in a given group.
     * @param triggerGroup the group whose triggers should be listed
     */
    public java.lang.String[] getTriggerNames(int sessionID, String triggerGroup) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Get a list of all currently-executing jobs.
     */
    public org.quartz.utils.Key[] getCurrentlyExecutingJobs(int sessionID) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Remove a previously-scheduled job and all of its associated triggers.
     * @param jobName the name of the job to be removed
     * @param groupName the name of the group
     * @return true if a job was removed, false otherwise
     */
    public boolean deleteJob(int sessionID, String jobName, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Remove the given schedule from the scheduler.
     * @param scheduleName the name of the schedule to delete
     * @param groupName the name of the group
     * @return true if a schedule was deleted, false otherwise
     */
    public boolean deleteSchedule(int sessionID, String scheduleName, String groupName)
        throws SessionNotFoundException, SessionTimeoutException, SchedulerException;

    /**
     * Remove the given group of schedules from the scheduler.
     * @param groupName the name of the group
     * @return number of schedules deleted
     */
    public int deleteScheduleGroup(int sessionID, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

    /**
     * Remove the given group of jobs from the scheduler.
     * @param groupName the name of the group
     * @return number of jobs deleted
     */
    public int deleteJobGroup(int sessionID, String groupName) throws SessionNotFoundException,
        SessionTimeoutException, SchedulerException;

}

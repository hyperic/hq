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
package org.hyperic.hq.control.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.quartz.SchedulerException;

/**
 * Local interface for ControlScheduleManager.
 */
public interface ControlScheduleManager {
    /**
     * Get a list of recent control actions in decending order
     */
    public PageList<ControlHistory> getRecentControlActions(AuthzSubject subject, int rows, long window)
        throws ApplicationException;

    /**
     * Get a list of pending control actions in decending order
     */
    public PageList<ControlSchedule> getPendingControlActions(AuthzSubject subject, int rows)
        throws ApplicationException;

    /**
     * Get a list of most active control operations
     */
    public PageList<ControlFrequencyValue> getOnDemandControlFrequency(AuthzSubject subject, int numToReturn)
        throws ApplicationException;

    /**
     * Get a list of scheduled jobs based on appdef id
     */
    public PageList<ControlSchedule> findScheduledJobs(AuthzSubject subject, AppdefEntityID id, PageControl pc)
        throws ScheduledJobNotFoundException;

    /**
     * Get a job history based on appdef id
     */
    public PageList<ControlHistory> findJobHistory(AuthzSubject subject, AppdefEntityID id, PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException, GroupNotCompatibleException;

    /**
     * Get a batch job history based on batchJobId and appdef id
     */
    public PageList<ControlHistory> findGroupJobHistory(AuthzSubject subject, int batchId, AppdefEntityID id,
                                                        PageControl pc) throws ApplicationException;

    /**
     * Remove an entry from the control history
     */
    public void deleteJobHistory(AuthzSubject subject, java.lang.Integer[] ids) throws ApplicationException;

    /**
     * Obtain the current action that is being executed. If there is no current
     * running action, null is returned.
     */
    public ControlHistory getCurrentJob(AuthzSubject whoami, AppdefEntityID id) throws ApplicationException;

    /**
     * Obtain a control history object based on the history id
     */
    public ControlHistory getJobByJobId(AuthzSubject subject, Integer id) throws ApplicationException;

    /**
     * Obtain the last control action that fired. Returns null if there are no
     * previous events. This ignores jobs that are in progress.
     */
    public ControlHistory getLastJob(AuthzSubject subject, AppdefEntityID id) throws ApplicationException;

    /**
     * Obtain a scheduled control action based on an id
     */
    public ControlSchedule getControlJob(AuthzSubject subject, Integer id) throws PluginException;

    /**
     * Delete a scheduled control actions based on id
     */
    public void deleteControlJob(AuthzSubject subject, java.lang.Integer[] ids) throws PluginException;

    /**
     * Removes all jobs associated with an appdef entity
     */
    public void removeScheduledJobs(AuthzSubject subject, AppdefEntityID id) throws ScheduledJobRemoveException;

    /**
     * Schedule an action on an appdef entity
     */
    public void scheduleAction(AppdefEntityID id, AuthzSubject subject, String action, ScheduleValue schedule,
                                  int[] order) throws PluginException, SchedulerException;

    /**
     * Create a control history entry
     * @return The ID of the created history
     */
    public Integer createHistory(AppdefEntityID id, Integer groupId, Integer batchId, String subjectName,
                                        String action, String args, Boolean scheduled, long startTime, long stopTime,
                                        long scheduleTime, String status, String description, String errorMessage);

    /**
     * Update a control history entry
     */
    public void updateHistory(Integer jobId, long endTime, String status, String message) throws ApplicationException;

    /**
     * Get a control history value based on primary key
     */
    public ControlHistory getJobHistoryValue(Integer jobId) throws ApplicationException;

    /**
     * Get a control history value based on primary key
     */
    public void removeHistory(Integer id) throws ApplicationException;

}

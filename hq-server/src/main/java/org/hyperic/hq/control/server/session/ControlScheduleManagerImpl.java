/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.control.shared.ScheduledJobNotFoundException;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.ScheduleParseException;
import org.hyperic.hq.scheduler.ScheduleParser;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.scheduler.server.session.BaseScheduleManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control manager for dealing with scheduled actions
 * 
 */
@Service
public class ControlScheduleManagerImpl
    extends BaseScheduleManager implements ControlScheduleManager {

    private static final int MAX_HISTORY_TEXT_SIZE = 500;

    private Log log = LogFactory.getLog(ControlScheduleManagerImpl.class);

    private static final String JOB_PREFIX = "action";
    private static final String SCHEDULE_PREFIX = "interval";
    private static final String GROUP = "control";

    private static final String PAGER_BASE = "org.hyperic.hq.control.server.session.";
    private static final String HISTORY_PAGER = PAGER_BASE + "PagerProcessor_control_history";
    private static final String SCHEDULE_PAGER = PAGER_BASE + "PagerProcessor_control_schedule";

    private ControlHistoryDAO controlHistoryDAO;
    private ControlScheduleDAO controlScheduleDAO; 
    private PermissionManager permissionManager;
  

    @Autowired
    public ControlScheduleManagerImpl(Scheduler scheduler, DBUtil dbUtil, ControlHistoryDAO controlHistoryDAO,
                                      ControlScheduleDAO controlScheduleDAO, 
                                      PermissionManager permissionManager) {
        super(scheduler, dbUtil);
        this.controlHistoryDAO = controlHistoryDAO;
        this.controlScheduleDAO = controlScheduleDAO;
        this.permissionManager = permissionManager;
    }

    protected String getHistoryPagerClass() {
        return HISTORY_PAGER;
    }

    protected String getSchedulePagerClass() {
        return SCHEDULE_PAGER;
    }

    protected String getJobPrefix() {
        return JOB_PREFIX;
    }

    protected String getSchedulePrefix() {
        return SCHEDULE_PREFIX;
    }

    protected void setupJobData(JobDetail jobDetail, AuthzSubject subject, AppdefEntityID id, String action,
                                String args, String scheduleString, Boolean scheduled, String description, int[] order) {
        super.setupJobData(jobDetail, subject, id, scheduleString, scheduled, order);

        JobDataMap dataMap = jobDetail.getJobDataMap();

        // Quartz 1.5 requires Strings in the JobDataMap to be non-null
        if (description == null) {
            description = "";
        }

        if (args == null) {
            args = "";
        }

        dataMap.put(ControlJob.PROP_DESCRIPTION, description);
        dataMap.put(ControlJob.PROP_ACTION, action);
        dataMap.put(ControlJob.PROP_ARGS, args);
    }

    /**
     * Get a list of recent control actions in decending order
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> getRecentControlActions(AuthzSubject subject, int rows, long window)
        throws ApplicationException {
        StopWatch watch = new StopWatch();

        // this routine ignores sort attribute!

        try {
            Collection<ControlHistory> recent = controlHistoryDAO.findByStartTime(System.currentTimeMillis() - window,
                false);

            // Run through the list only returning entities the user
            // has the ability to see
            int count = 0;
            for (Iterator<ControlHistory> i = recent.iterator(); i.hasNext();) {
                ControlHistory cLocal = i.next();
                AppdefEntityID entity = new AppdefEntityID(cLocal.getEntityType().intValue(), cLocal.getEntityId());
                try {
                    checkControlPermission(subject, entity);

                    AppdefEntityValue aVal = new AppdefEntityValue(entity, subject);
                    cLocal.setEntityName(aVal.getName());

                    if (++count > rows)
                        break;
                } catch (PermissionException e) {
                    i.remove();
                } catch (AppdefEntityNotFoundException e) {
                    // Resource not found, skip it and move on
                    i.remove();
                }
            }

            PageList<ControlHistory> list = historyPager.seek(recent, 0, rows);
            list.setTotalSize(recent.size());
            return list;
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        } finally {
            if (log.isDebugEnabled())
                log.debug("getRecentControlActions(): " + watch.getElapsed());
        }
    }

    /**
     * Get a list of pending control actions in decending order
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ControlSchedule> getPendingControlActions(AuthzSubject subject, int rows)
        throws ApplicationException {
        StopWatch watch = new StopWatch();

        // this routine ignores sort attribute!

        try {
            Collection<ControlSchedule> pending = controlScheduleDAO.findByFireTime(false);
            // Run through the list only returning entities the user
            // has the ability to see
            int count = 0;
            for (Iterator<ControlSchedule> i = pending.iterator(); i.hasNext();) {
                ControlSchedule sLocal = i.next();
                AppdefEntityID entity = new AppdefEntityID(sLocal.getEntityType().intValue(), sLocal.getEntityId());
                try {
                    checkControlPermission(subject, entity);
                    if (++count > rows)
                        break;
                } catch (PermissionException e) {
                    i.remove();
                }
            }

            // This will remove stale data and update fire times which
            // may result in the list being out of order. We should
            // probably sort a second time
            PageList<ControlSchedule> list = schedulePager.seek(pending, 0, rows);
            return list;

        } finally {
            if (log.isDebugEnabled())
                log.debug("getPendingControlActions(): " + watch.getElapsed());
        }
    }

    /**
     * Get a list of most active control operations
     * 
     * 
     * 
     * 
     * XXX: This could also take a page control, although we would ignore
     * everything except for the size
     */
    @Transactional(readOnly=true)
    public PageList<ControlFrequencyValue> getOnDemandControlFrequency(AuthzSubject subject, int numToReturn)
        throws ApplicationException {
    
        PageList<ControlFrequencyValue> list = new PageList<ControlFrequencyValue>();

        try {
              List<ControlFrequency> frequencies = controlHistoryDAO.getControlFrequencies(numToReturn);
              for (ControlFrequency frequency: frequencies) {
                 try {
                     checkControlPermission(subject, frequency.getId());
                     AppdefEntityValue aVal = new AppdefEntityValue(frequency.getId(), subject);
                     String name = aVal.getName();
                     ControlFrequencyValue cv =
                         new ControlFrequencyValue(name, frequency.getId().getType(), frequency.getId().getID(),
                             frequency.getAction(), (int)frequency.getCount());
                    list.add(cv);
                 } catch (AppdefEntityNotFoundException e) {
                     log.debug(e,e);
                     continue;
                 } catch (PermissionException e) {
                     log.debug(e,e);
                     continue;
                 }
            }
        } catch (Exception e) {
            throw new ApplicationException(e);
        } 

        return list;
    }

    /**
     * Get a list of scheduled jobs based on appdef id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ControlSchedule> findScheduledJobs(AuthzSubject subject, AppdefEntityID id, PageControl pc)
        throws ScheduledJobNotFoundException {

        Collection<ControlSchedule> schedule;
        try {

            // default the sorting to the next fire time
            pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_NEXTFIRE);

            int sortAttr = pc.getSortattribute();
            switch (sortAttr) {
                case SortAttribute.CONTROL_NEXTFIRE:
                    schedule = controlScheduleDAO.findByEntityFireTime(id.getType(), id.getID(), pc.isAscending());
                    break;

                case SortAttribute.CONTROL_ACTION:
                    schedule = controlScheduleDAO.findByEntityAction(id.getType(), id.getID(), pc.isAscending());
                    break;
                default:
                    throw new NotFoundException("Unknown sort attribute: " + sortAttr);
            }
        } catch (NotFoundException e) {
            throw new ScheduledJobNotFoundException(e);
        }

        // This will remove stale data and update fire times which
        // may result in the list being out of order. We should
        // probably sort a second time
        PageList<ControlSchedule> list = schedulePager.seek(schedule, pc.getPagenum(), pc.getPagesize());
        list.setTotalSize(schedule.size());

        return list;
    }

    /**
     * Get a job history based on appdef id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> findJobHistory(AuthzSubject subject, AppdefEntityID id, PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException, GroupNotCompatibleException {
        if (id.isGroup()) {
            List<AppdefEntityID> groupMembers = GroupUtil
                .getCompatGroupMembers(subject, id, null, PageControl.PAGE_ALL);

            // For each entity in the list, sanity check permissions
            for (AppdefEntityID entity : groupMembers) {
                checkControlPermission(subject, entity);
            }
        } else {
            checkControlPermission(subject, id);
        }

        if (pc == null) {
            pc = new PageControl();
        }

        Collection<ControlHistory> hist;

        int sortAttr = pc.getSortattribute();
        switch (sortAttr) {
            case SortAttribute.CONTROL_ACTION:
                hist = controlHistoryDAO.findByEntityAction(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_STATUS:
                hist = controlHistoryDAO.findByEntityStatus(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_STARTED:
            case SortAttribute.DEFAULT: // default the sorting to the start
                hist = controlHistoryDAO.findByEntityStartTime(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_ELAPSED:
                hist = controlHistoryDAO.findByEntityDuration(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_DATESCHEDULED:
                hist = controlHistoryDAO.findByEntityDateScheduled(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_ENTITYNAME:
                // No need to sort since all will have the same name
                hist = controlHistoryDAO.findByEntity(id.getType(), id.getID());
                break;
            default:
                throw new SystemException("Unknown sort attribute: " + sortAttr);
        }
        PageList<ControlHistory> list = historyPager.seek(hist, pc.getPagenum(), pc.getPagesize());
        list.setTotalSize(hist.size());

        return list;
    }

    /**
     * Get a batch job history based on batchJobId and appdef id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> findGroupJobHistory(AuthzSubject subject, int batchId, AppdefEntityID id,
                                                        PageControl pc) throws ApplicationException {

        // default the sorting to the date started
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_STARTED);
        pc.setSortorder(PageControl.SORT_DESC);

        Collection<ControlHistory> hist;

        int sortAttr = pc.getSortattribute();
        switch (sortAttr) {
            case SortAttribute.CONTROL_ACTION:
                hist = controlHistoryDAO.findByGroupAction(id.getID(), batchId, pc.isAscending());
                break;
            case SortAttribute.CONTROL_STATUS:
                hist = controlHistoryDAO.findByGroupStatus(id.getID(), batchId, pc.isAscending());
                break;
            case SortAttribute.CONTROL_STARTED:
                hist = controlHistoryDAO.findByGroupStartTime(id.getID(), batchId, pc.isAscending());
                break;
            case SortAttribute.CONTROL_ELAPSED:
                hist = controlHistoryDAO.findByGroupDuration(id.getID(), batchId, pc.isAscending());
                break;
            case SortAttribute.CONTROL_DATESCHEDULED:
                hist = controlHistoryDAO.findByGroupDateScheduled(id.getID(), batchId, pc.isAscending());
                break;
            case SortAttribute.CONTROL_ENTITYNAME:
                hist = controlHistoryDAO.findByEntity(id.getType(), id.getID());
                break;
            default:
                throw new ApplicationException("Unknown sort attribute: " + sortAttr);
        }

        // The the entity names
        for (ControlHistory ch : hist) {
            AppdefEntityValue aev = new AppdefEntityValue(new AppdefEntityID(ch.getEntityType().intValue(), ch
                .getEntityId()), subject);
            ch.setEntityName(aev.getName());
        }

        PageList<ControlHistory> list = historyPager.seek(hist, pc.getPagenum(), pc.getPagesize());
        list.setTotalSize(hist.size());

        // Sort the list if we are sorting by entity name
        if (sortAttr == SortAttribute.CONTROL_ENTITYNAME) {
            if (pc.isAscending()) {
                Collections.sort(list, new ControlHistoryLocalComparatorAsc());
            } else {
                Collections.sort(list, new ControlHistoryLocalComparatorDesc());
            }
        }

        return list;
    }

    /**
     * Remove an entry from the control history
     * 
     * 
     * 
     */
    @Transactional
    public void deleteJobHistory(AuthzSubject subject, Integer[] ids) throws ApplicationException {
        // SpringSource: require admin privileges

        if (!permissionManager.hasAdminPermission(subject.getId())) {
            throw new PermissionException("Admin permission is required to delete job history");
        }
        // END SpringSource

        for (int i = 0; i < ids.length; i++) {
            try {
                ControlHistory historyLocal = controlHistoryDAO.findById(ids[i]);
                controlHistoryDAO.remove(historyLocal);
            } catch (ObjectNotFoundException e) {
                throw new ApplicationException(e);
            }
        }
    }

    /**
     * Obtain the current action that is being executed. If there is no current
     * running action, null is returned.
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public ControlHistory getCurrentJob(AuthzSubject whoami, AppdefEntityID id) throws ApplicationException {
        Collection<ControlHistory> historyLocals = controlHistoryDAO.findByEntityStartTime(id.getType(), id.getID(),
            false);
        for (ControlHistory history : historyLocals) {
            if (history.getStatus().equals(ControlConstants.STATUS_INPROGRESS)) {
                return history;
            }
        }

        return null;
    }

    /**
     * Obtain a control history object based on the history id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public ControlHistory getJobByJobId(AuthzSubject subject, Integer id) throws ApplicationException {

        try {
            return controlHistoryDAO.findById(id);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }

    }

    /**
     * Obtain the last control action that fired. Returns null if there are no
     * previous events. This ignores jobs that are in progress.
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public ControlHistory getLastJob(AuthzSubject subject, AppdefEntityID id) throws ApplicationException {
        Collection<ControlHistory> historyLocals = controlHistoryDAO.findByEntityStartTime(id.getType(), id.getID(),
            false);

        for (ControlHistory cLocal : historyLocals) {
            if (!cLocal.getStatus().equals(ControlConstants.STATUS_INPROGRESS))
                return cLocal;
        }

        return null;
    }

    /**
     * Obtain a scheduled control action based on an id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public ControlSchedule getControlJob(AuthzSubject subject, Integer id) throws PluginException {

        try {
            return controlScheduleDAO.findById(id);

            // TODO: validate the job in the scheduler?
        } catch (Exception e) {
            log.error("Unable to get control job info: " + e.getMessage());
            throw new PluginException(e);
        }

    }

    /**
     * Delete a scheduled control actions based on id
     * 
     * 
     * 
     */
    @Transactional
    public void deleteControlJob(AuthzSubject subject, Integer ids[]) throws PluginException {

        for (int i = 0; i < ids.length; i++) {
            try {
                ControlSchedule cScheduleLocal = controlScheduleDAO.findById(ids[i]);
                scheduler.deleteJob(cScheduleLocal.getJobName(), GROUP);
                controlScheduleDAO.remove(cScheduleLocal);
            } catch (Exception e) {
                log.error("Unable to remove job: " + e.getMessage());
                throw new PluginException(e);
            }
        }
    }

    /**
     * Removes all jobs associated with an appdef entity
     * 
     * 
     */
    @Transactional
    public void removeScheduledJobs(AuthzSubject subject, AppdefEntityID id) throws ScheduledJobRemoveException {

        // Any associated triggers will be automatically removed by Quartz.
        Collection<ControlSchedule> jobs = controlScheduleDAO.findByEntity(id.getType(), id.getID());
        for (ControlSchedule cSched : jobs) {
            try {
                scheduler.deleteJob(cSched.getJobName(), GROUP);
            } catch (SchedulerException e) {
                log.error("Unable to remove job " + cSched.getJobName() + ": " + e.getMessage());
            }
            controlScheduleDAO.remove(cSched);
        }
    }
    

    /**
     * Schedule an action on an appdef entity
     * 
     * 
     * 
     */
    @Transactional
    public void scheduleAction(AppdefEntityID id, AuthzSubject subject, String action, ScheduleValue schedule,
                                  int[] order) throws PluginException, SchedulerException {
        // Scheduled jobs are persisted in the control subsystem
        String jobName = getJobName(subject, id, action);
        String triggerName = getTriggerName(subject, id, action);

        // Setup the quartz job class that will handle this control action.
        Class<?> jobClass = id.isGroup() ? ControlActionGroupJob.class : ControlActionJob.class;

        JobDetail jobDetail = new JobDetail(jobName, GROUP, jobClass);

        setupJobData(jobDetail, subject, id, action, null, schedule.getScheduleString(), Boolean.TRUE, schedule
            .getDescription(), order);

        String cronStr;
        try {
            cronStr = ScheduleParser.getCronString(schedule);
        } catch (ScheduleParseException e) {
            log.error("Unable to get cron string: " + e.getMessage());
            throw new PluginException(e);
        }

        // Single scheduled actions do not have cron strings
        if (cronStr == null) {
            SimpleTrigger trigger = new SimpleTrigger(triggerName, GROUP, schedule.getStart());
            scheduler.scheduleJob(jobDetail, trigger);
            Date nextFire = trigger.getFireTimeAfter(new Date());
            if (nextFire == null) {
                throw new SchedulerException();
            }

            try {
                controlScheduleDAO.create(id, subject.getName(), action, schedule, nextFire.getTime(), triggerName,
                    jobName, null);
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new PluginException(e);
            }
        } else {
            try {
                CronTrigger trigger = new CronTrigger(triggerName, GROUP, jobName, GROUP, schedule.getStart(), schedule
                    .getEnd(), cronStr);
                trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);

                scheduler.scheduleJob(jobDetail, trigger);

                // Quartz used to throw an exception on scheduleJob if the
                // job would never fire. Guess that is not the case anymore
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }

                String stringOrder = null;
                if (order != null)
                    stringOrder = StringUtil.arrayToString(order);

                controlScheduleDAO.create(id, subject.getName(), action, schedule, nextFire.getTime(), triggerName,
                    jobName, stringOrder);
            } catch (ParseException e) {
                log.error("Unable to setup cron trigger: " + e.getMessage());
                throw new PluginException(e);
            } catch (SchedulerException e) {
                throw new PluginException(e);
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new PluginException(e);
            }
        }
    }

    private String truncateText(int maxSize, String text) {
        String truncatedText = text;
        if (text != null) {
            if (text.length() > maxSize) {
                truncatedText = text.substring(0, maxSize - 3) + "...";
            }
        }
        return truncatedText;
    }

    /**
     * Create a control history entry
     * 
     * 
     * 
     */
    @Transactional
    public Integer createHistory(AppdefEntityID id, Integer groupId, Integer batchId, String subjectName,
                                        String action, String args, Boolean scheduled, long startTime, long stopTime,
                                        long scheduleTime, String status, String description, String errorMessage) {
        return controlHistoryDAO.create(id, groupId, batchId, subjectName, action, truncateText(MAX_HISTORY_TEXT_SIZE,
            args), scheduled, startTime, stopTime, scheduleTime, status, truncateText(MAX_HISTORY_TEXT_SIZE,
            description), truncateText(MAX_HISTORY_TEXT_SIZE, errorMessage)).getId();
    }

    /**
     * Update a control history entry
     * 
     * 
     * 
     */
    @Transactional
    public void updateHistory(Integer jobId, long endTime, String status, String message) throws ApplicationException {
        ControlHistory local;

        try {
            local = controlHistoryDAO.findById(jobId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }

        local.setEndTime(endTime);
        local.setStatus(status);
        local.setMessage(truncateText(MAX_HISTORY_TEXT_SIZE, message));
    }

    /**
     * Get a control history value based on primary key
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public ControlHistory getJobHistoryValue(Integer jobId) throws ApplicationException {
        try {
            return controlHistoryDAO.findByIdAndPopulate(jobId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Get a control history value based on primary key
     * 
     * 
     * 
     */
    @Transactional
    public void removeHistory(Integer id) throws ApplicationException {
        try {
            ControlHistory local = controlHistoryDAO.findById(id);
            controlHistoryDAO.remove(local);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Check view permission for an appdef entity
     * 
     * @throws PermissionException if the user does not have the modify
     *         Operation for the given resource.
     */
    private void checkControlPermission(AuthzSubject caller, AppdefEntityID id) throws PermissionException {
        permissionManager.checkControlPermission(caller, id);
    }
    
    private class ControlHistoryLocalComparatorAsc implements Comparator<ControlHistory> {

        public int compare(ControlHistory o1, ControlHistory o2) {
            return o1.getEntityName().compareTo(o2.getEntityName());
        }

        public boolean equals(Object other) {
            return false;
        }
    }

    private class ControlHistoryLocalComparatorDesc implements Comparator<ControlHistory> {

        public int compare(ControlHistory o1, ControlHistory o2) {
            return -(o1.getEntityName().compareTo(o2.getEntityName()));
        }

        public boolean equals(Object other) {
            return false;
        }
    }
}

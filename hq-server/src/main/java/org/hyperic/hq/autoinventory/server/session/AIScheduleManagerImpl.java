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

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.shared.AIScheduleManager;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.dao.AIHistoryDAO;
import org.hyperic.hq.dao.AIScheduleDAO;
import org.hyperic.hq.scheduler.ScheduleParseException;
import org.hyperic.hq.scheduler.ScheduleParser;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.scheduler.server.session.BaseScheduleManager;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
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
 * Manager for dealing with scheduled autoinventory scans.
 * 
 */
@Service
public class AIScheduleManagerImpl
    extends BaseScheduleManager implements AIScheduleManager {

    private static final String GROUP = "autoinventory";
    private final Log log = LogFactory.getLog(AIScheduleManagerImpl.class.getName());

    private static final String JOB_PREFIX = "aiScan";
    private static final String SCHEDULE_PREFIX = "interval";

    private static final String PAGER_BASE = "org.hyperic.hq.autoinventory.server.session.";
    private static final String HISTORY_PAGER = PAGER_BASE + "PagerProcessor_ai_history";
    private static final String SCHEDULE_PAGER = PAGER_BASE + "PagerProcessor_ai_schedule";

    private AIScheduleDAO aiScheduleDao;
    private PlatformManager platformManager;
    private AIHistoryDAO aiHistoryDao;

    @Autowired
    public AIScheduleManagerImpl(Scheduler scheduler, DBUtil dbUtil, AIScheduleDAO aiScheduleDao,
                                 PlatformManager platformManager, AIHistoryDAO aiHistoryDAO) {
        super(scheduler, dbUtil);
        this.aiScheduleDao = aiScheduleDao;
        this.platformManager = platformManager;
        this.aiHistoryDao = aiHistoryDAO;
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

    protected void setupJobData(JobDetail jobDetail, AuthzSubject subject, AppdefEntityID id,
                                ScanConfigurationCore scanConfig, String scanName, String scanDesc, String os,
                                ScheduleValue schedule) {
        String scheduleString;
        Boolean scheduled = new Boolean(schedule != null);
        if (scheduled.booleanValue()) {
            scheduleString = schedule.getScheduleString();
        } else {
            scheduleString = "Single execution";
        }

        // Quartz 1.5 requires Strings in the JobDataMap to be non-null
        if (scanName == null) {
            scanName = "";
        }
        if (scanDesc == null) {
            scanDesc = "";
        }

        super.setupJobData(jobDetail, subject, id, scheduleString, scheduled, null);

        JobDataMap dataMap = jobDetail.getJobDataMap();

        try {
            dataMap.put(AIScanJob.PROP_CONFIG, scanConfig.encode());
            dataMap.put(AIScanJob.PROP_SCAN_OS, os);
            dataMap.put(AIScanJob.PROP_SCANNAME, scanName);
            dataMap.put(AIScanJob.PROP_SCANDESC, scanDesc);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Schedule an AI scan on an appdef entity (platform or group of platforms)
     * 
     * 
     */
    @Transactional
    public void doScheduledScan(AuthzSubject subject, AppdefEntityID id, ScanConfigurationCore scanConfig,
                                String scanName, String scanDesc, ScheduleValue schedule)
        throws AutoinventoryException, DuplicateAIScanNameException, ScheduleWillNeverFireException {
        // find the os for the platform
        Platform pValue = null;
        try {
            pValue = platformManager.findPlatformById(id.getId());
        } catch (PlatformNotFoundException e) {
            throw new AutoinventoryException(e);
        }

        String configid = "config-" + String.valueOf(scanConfig.hashCode());
        String jobName = getJobName(subject, id, configid);
        String triggerName = getTriggerName(subject, id, configid);

        // Setup the quartz job class that will handle this ai action.
        Class<?> jobClass = id.isGroup() ? AIScanGroupJob.class : AIScanJob.class;

        JobDetail jobDetail = new JobDetail(jobName, GROUP, jobClass);

        setupJobData(jobDetail, subject, id, scanConfig, scanName, scanDesc, pValue.getPlatformType().getName(),
            schedule);

        // On-demand scans will have no schedule
        if (schedule == null) {
            jobDetail.setVolatility(true);
            SimpleTrigger trigger = new SimpleTrigger(triggerName, GROUP);
            trigger.setVolatility(true);

            try {
                log.info("Scheduling job for immediate execution: " + jobDetail);
                scheduler.scheduleJob(jobDetail, trigger);
                return;
            } catch (SchedulerException e) {
                log.error("Unable to schedule job: " + e.getMessage());
                return;
            }
        }

        String cronStr;
        try {
            cronStr = ScheduleParser.getCronString(schedule);
        } catch (ScheduleParseException e) {
            log.error("Unable to get cron string: " + e.getMessage());
            throw new AutoinventoryException(e);
        }

        // Single scheduled actions do not have cron strings
        if (cronStr == null) {
            try {
                SimpleTrigger trigger = new SimpleTrigger(triggerName, GROUP, schedule.getStart());
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }
                scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(aiScheduleDao, scanName);
                AISchedule aiLoc = aiScheduleDao.create(id, subject.getName(), scanName, scanDesc, schedule, nextFire
                    .getTime(), triggerName, jobName);
                aiLoc.setConfig(scanConfig.serialize());
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new AutoinventoryException(e);
            }
        } else {
            try {
                CronTrigger trigger = new CronTrigger(triggerName, GROUP, jobName, GROUP, schedule.getStart(), schedule
                    .getEnd(), cronStr);
                // Quartz used to throw an exception on scheduleJob if the
                // job would never fire. Guess that is not the case anymore
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }
                scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(aiScheduleDao, scanName);
                AISchedule aiLoc = aiScheduleDao.create(id, subject.getName(), scanName, scanDesc, schedule, nextFire
                    .getTime(), triggerName, jobName);
                aiLoc.setConfig(scanConfig.serialize());
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (ParseException e) {
                log.error("Unable to setup cron trigger: " + e.getMessage());
                throw new AutoinventoryException(e);
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new AutoinventoryException(e);
            }
        }
    }

    /**
     * Get a list of scheduled scans based on appdef id
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AIScheduleValue> findScheduledJobs(AuthzSubject subject, AppdefEntityID id, PageControl pc) throws NotFoundException {

        // default the sorting to the next fire time
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_NEXTFIRE);

        Collection<AISchedule> schedule;
        int sortAttr = pc.getSortattribute();
        switch (sortAttr) {
            case SortAttribute.RESOURCE_NAME:
                schedule = aiScheduleDao.findByEntityScanName(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_NEXTFIRE:
                schedule = aiScheduleDao.findByEntityFireTime(id.getType(), id.getID(), pc.isAscending());
                break;

            default:
                throw new NotFoundException("Unknown sort attribute: " + sortAttr);
        }

        // The pager will remove any stale data
        // TODO: G
        PageList<AIScheduleValue> list = this.schedulePager.seek(schedule, pc.getPagenum(), pc.getPagesize());
        list.setTotalSize(schedule.size());

        return list;
    }

    /**
     * 
     * 
     */
    @Transactional(readOnly=true)
    public AISchedule findScheduleByID(AuthzSubject subject, Integer id) {

        return aiScheduleDao.findById(id);
    }

    /**
     * Get a job history based on appdef id
     * 
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AIHistory> findJobHistory(AuthzSubject subject, AppdefEntityID id, PageControl pc) throws NotFoundException {

        // default the sorting to the date started
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_STARTED);

        Collection<AIHistory> hist;
        int sortAttr = pc.getSortattribute();
        switch (sortAttr) {
            case SortAttribute.CONTROL_STATUS:
                hist = aiHistoryDao.findByEntityStatus(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_STARTED:
                hist = aiHistoryDao.findByEntityStartTime(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_ELAPSED:
                hist = aiHistoryDao.findByEntityDuration(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_DATESCHEDULED:
                hist = aiHistoryDao.findByEntityDateScheduled(id.getType(), id.getID(), pc.isAscending());
                break;
            case SortAttribute.CONTROL_ENTITYNAME:
                // No need to sort since all will have the same name
                hist = aiHistoryDao.findByEntity(id.getType(), id.getID());
                break;
            default:
                throw new NotFoundException("Unknown sort attribute: " + sortAttr);
        }

        PageList<AIHistory> list = this.historyPager.seek(hist, pc.getPagenum(), pc.getPagesize());
        list.setTotalSize(hist.size());

        return list;
    }

    /**
     * 
     * 
     */
    @Transactional
    public void deleteAIJob(AuthzSubject subject, Integer ids[]) throws AutoinventoryException {
        for (int i = 0; i < ids.length; i++) {
            try {
                AISchedule aiScheduleLocal = aiScheduleDao.findById(ids[i]);
                scheduler.deleteJob(aiScheduleLocal.getJobName(), GROUP);
                aiScheduleDao.remove(aiScheduleLocal);
            } catch (Exception e) {
                throw new AutoinventoryException("Unable to remove job: " + e.getMessage());
            }
        }
    }

    @Transactional(readOnly=true)
    public void checkUniqueName(AIScheduleDAO aiScheduleLocalHome, String scanName) throws DuplicateAIScanNameException {

        // Ensure that the name is not a duplicate.
        AISchedule aisl = aiScheduleLocalHome.findByScanName(scanName);
        if (aisl != null) {
            throw new DuplicateAIScanNameException(scanName);
        }
        return;
    }
}

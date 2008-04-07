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

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.shared.AIScheduleManagerLocal;
import org.hyperic.hq.autoinventory.shared.AIScheduleManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.AIHistoryDAO;
import org.hyperic.hq.dao.AIScheduleDAO;
import org.hyperic.hq.scheduler.ScheduleParseException;
import org.hyperic.hq.scheduler.ScheduleParser;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.scheduler.server.session.BaseScheduleManagerEJB;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

/** 
 * Manager for dealing with scheduled autoinventory scans.
 *
 * @ejb:bean name="AIScheduleManager"
 *      jndi-name="ejb/autoinventory/AIScheduleManager"
 *      local-jndi-name="LocalAIScheduleManager"
 *      view-type="local"
 *      type="Stateless"
 */
public class AIScheduleManagerEJBImpl 
    extends BaseScheduleManagerEJB implements SessionBean {

    public static final String GROUP = "autoinventory";
    public static final String STATUS_STARTED   = "scan running";
    public static final String STATUS_COMPLETED = "scan completed";
    public static final String AI_SCHED_TABLE = "EAM_AUTOINV_SCHEDULE";
    public static final String AI_SCHED_TABLE_COL_ID = "ID";
    public static final String AI_SCHED_TABLE_COL_BL = "CONFIG";

    private static final Log _log = 
        LogFactory.getLog(AIScheduleManagerEJBImpl.class.getName());

    private final String JOB_PREFIX      = "aiScan";
    private final String SCHEDULE_PREFIX = "interval";

    private final String PAGER_BASE =
        "org.hyperic.hq.autoinventory.server.session.";
    private final String HISTORY_PAGER =
        PAGER_BASE + "PagerProcessor_ai_history";
    private final String SCHEDULE_PAGER =
        PAGER_BASE + "PagerProcessor_ai_schedule";

    protected String getHistoryPagerClass  () { return HISTORY_PAGER; }
    protected String getSchedulePagerClass () { return SCHEDULE_PAGER; }
    protected String getJobPrefix          () { return JOB_PREFIX; }
    protected String getSchedulePrefix     () { return SCHEDULE_PREFIX; }

    protected void setupJobData(JobDetail jobDetail, 
                                AuthzSubject subject,
                                AppdefEntityID id, 
                                ScanConfigurationCore scanConfig,
                                String scanName,
                                String scanDesc,
                                String os,
                                ScheduleValue schedule)
    {
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

        super.setupJobData(jobDetail, subject, id,
                           scheduleString, scheduled, null);

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
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void doScheduledScan(AuthzSubject subject,
                                AppdefEntityID id, 
                                ScanConfigurationCore scanConfig,
                                String scanName,
                                String scanDesc,
                                ScheduleValue schedule)
        throws AutoinventoryException, CreateException,
               DuplicateAIScanNameException, ScheduleWillNeverFireException
    {
        // Scheduled jobs are persisted in the autoinventory subsystem
        AIScheduleDAO asdao = DAOFactory.getDAOFactory().getAIScheduleDAO();

        // find the os for the platform
        Platform pValue = null;
        try {
            pValue =
                PlatformManagerEJBImpl.getOne().findPlatformById(id.getId());
        } catch (PlatformNotFoundException e) {
            throw new AutoinventoryException(e);
        }
                    
        String configid = "config-" + String.valueOf(scanConfig.hashCode());
        String jobName = getJobName(subject, id, configid);
        String triggerName = getTriggerName(subject, id, configid);

        // Setup the quartz job class that will handle this ai action.
        Class jobClass = 
            (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) ?
            AIScanGroupJob.class : AIScanJob.class;

        JobDetail jobDetail = new JobDetail(jobName, GROUP, jobClass);

        setupJobData(jobDetail, subject, id, scanConfig, scanName, scanDesc, 
                     pValue.getPlatformType().getName(), schedule);

        // On-demand scans will have no schedule
        if (schedule == null) {
            jobDetail.setVolatility(true);
            SimpleTrigger trigger = new SimpleTrigger(triggerName, GROUP);
            trigger.setVolatility(true);
            
            try {
                _log.info("Scheduling job for immediate execution: " + 
                          jobDetail);
                _scheduler.scheduleJob(jobDetail, trigger);
                return;
            } catch (SchedulerException e) {
                _log.error("Unable to schedule job: " + e.getMessage());
                return;
            }
        }

        String cronStr;
        try {
            cronStr = ScheduleParser.getCronString(schedule);
        } catch (ScheduleParseException e) {
            _log.error("Unable to get cron string: " + e.getMessage());
            throw new AutoinventoryException(e);
        }

        // Single scheduled actions do not have cron strings
        if (cronStr == null) {
            try {
                SimpleTrigger trigger = new SimpleTrigger(triggerName, 
                                                          GROUP, 
                                                          schedule.getStart());
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }
                _scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(asdao, scanName);
                AISchedule aiLoc =
                    asdao.create(id,
                                 subject.getName(),
                                 scanName, scanDesc,
                                 schedule,
                                 nextFire.getTime(),
                                 triggerName,
                                 jobName);
                aiLoc.setConfig(scanConfig.serialize());
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (Exception e) {
                _log.error("Unable to schedule job: " + e.getMessage());
                throw new AutoinventoryException(e);
            }
        } else {        
            try {
                CronTrigger trigger =
                    new CronTrigger(triggerName, GROUP, jobName, GROUP,
                                    schedule.getStart(), schedule.getEnd(),
                                    cronStr);
                // Quartz used to throw an exception on scheduleJob if the
                // job would never fire.  Guess that is not the case anymore
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }
                _scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(asdao, scanName);
                AISchedule aiLoc =
                    asdao.create(id, subject.getName(),
                                 scanName, scanDesc,
                                 schedule,
                                 nextFire.getTime(),
                                 triggerName,
                                 jobName);
                aiLoc.setConfig(scanConfig.serialize());
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (ParseException e) {
                _log.error("Unable to setup cron trigger: " +
                               e.getMessage());
                throw new AutoinventoryException(e);
            } catch (Exception e) {
                _log.error("Unable to schedule job: " + e.getMessage());
                throw new AutoinventoryException(e);
            }
        }
    }

    /**
     * Get a list of scheduled scans based on appdef id
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList findScheduledJobs(AuthzSubject subject, 
                                      AppdefEntityID id, PageControl pc)
        throws FinderException {
        AIScheduleDAO sl = DAOFactory.getDAOFactory().getAIScheduleDAO();
        Collection schedule;

        // default the sorting to the next fire time
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_NEXTFIRE);

        int sortAttr = pc.getSortattribute();
        switch (sortAttr) {
        case SortAttribute.RESOURCE_NAME:
            schedule = sl.findByEntityScanName(id.getType(), id.getID(),
                                               pc.isAscending());
            break;
        case SortAttribute.CONTROL_NEXTFIRE:
            schedule = sl.findByEntityFireTime(id.getType(), id.getID(),
                                               pc.isAscending());
            break;

        default:
            throw new FinderException("Unknown sort attribute: " + sortAttr);
        }

        // The pager will remove any stale data
        PageList list = this.schedulePager.seek(schedule,
                                                pc.getPagenum(),
                                                pc.getPagesize());
        list.setTotalSize(schedule.size());

        return list;
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AISchedule findScheduleByID(AuthzSubject subject, 
                                            Integer id)
        throws FinderException, CreateException
    {
        AIScheduleDAO sl = new AIScheduleDAO(DAOFactory.getDAOFactory());
        return sl.findById(id);
    }

    /**
     * Get a job history based on appdef id
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     *
     */
    public PageList findJobHistory(AuthzSubject subject, 
                                   AppdefEntityID id, PageControl pc)
        throws FinderException
    {
        AIHistoryDAO histLH = DAOFactory.getDAOFactory().getAIHistoryDAO();
        Collection hist;

        // default the sorting to the date started
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_STARTED);

        int sortAttr = pc.getSortattribute();
        switch(sortAttr) {
          case SortAttribute.CONTROL_STATUS:
              hist = histLH.findByEntityStatus(id.getType(),
                                               id.getID(),
                                               pc.isAscending());
            break;
          case SortAttribute.CONTROL_STARTED:
              hist = histLH.findByEntityStartTime(id.getType(),
                                                  id.getID(),
                                                  pc.isAscending());
            break;
          case SortAttribute.CONTROL_ELAPSED:
              hist = histLH.findByEntityDuration(id.getType(),
                                                 id.getID(),
                                                 pc.isAscending());
            break;
          case SortAttribute.CONTROL_DATESCHEDULED:
              hist = histLH.findByEntityDateScheduled(id.getType(),
                                                      id.getID(),
                                                      pc.isAscending());
            break;
          case SortAttribute.CONTROL_ENTITYNAME:
            // No need to sort since all will have the same name
            hist = histLH.findByEntity(id.getType(), id.getID());
            break;
          default:
            throw new FinderException("Unknown sort attribute: " +
                                      sortAttr);
        }

        PageList list = this.historyPager.seek(hist,
                                               pc.getPagenum(),
                                               pc.getPagesize());
        list.setTotalSize(hist.size());

        return list;
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteAIJob(AuthzSubject subject, Integer ids[])
        throws AutoinventoryException
    {
        AIScheduleDAO asdao = DAOFactory.getDAOFactory().getAIScheduleDAO();
        AISchedule aiScheduleLocal;

        for (int i = 0; i < ids.length; i++) { 
            try {
                aiScheduleLocal = asdao.findById(ids[i]);
                _scheduler.deleteJob(aiScheduleLocal.getJobName(), GROUP);
                asdao.remove(aiScheduleLocal);
            } catch (Exception e) {
                throw new AutoinventoryException("Unable to remove job: " +
                                                 e.getMessage());
            }
        }
    }

    public void checkUniqueName ( AIScheduleDAO aiScheduleLocalHome,
                                  String scanName ) 
        throws DuplicateAIScanNameException {

        // Ensure that the name is not a duplicate.
        AISchedule aisl = aiScheduleLocalHome.findByScanName(scanName);
        if ( aisl != null ) {
            throw new DuplicateAIScanNameException(scanName);
        }
        return;
    }

    public static AIScheduleManagerLocal getOne() {
        try {
            return AIScheduleManagerUtil.getLocalHome().create();    
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    /** @ejb:create-method */
    public void ejbCreate() {
        super.ejbCreate();
    }
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}

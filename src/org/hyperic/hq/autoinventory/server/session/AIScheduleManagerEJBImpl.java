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

package org.hyperic.hq.autoinventory.server.session;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.SchedulerException;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformManagerLocalHome;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.autoinventory.shared.AIScheduleLocal;
import org.hyperic.hq.autoinventory.shared.AIScheduleLocalHome;
import org.hyperic.hq.autoinventory.shared.AISchedulePK;
import org.hyperic.hq.autoinventory.shared.AIScheduleUtil;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.autoinventory.shared.AIHistoryLocalHome;
import org.hyperic.hq.autoinventory.shared.AIHistoryUtil;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleParser;
import org.hyperic.hq.scheduler.ScheduleParseException;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.scheduler.server.session.BaseScheduleManagerEJB;

import org.hyperic.util.jdbc.BlobColumn;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;

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

    private static final Log log = 
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
                                AuthzSubjectValue subject,
                                AppdefEntityID id, 
                                ScanConfigurationCore scanConfig,
                                String scanName,
                                String scanDesc,
                                String os,
                                ScheduleValue schedule)
    {
        String scheduleString;
        Boolean scheduled;
        if (schedule == null) {
            scheduled = Boolean.FALSE;
            scheduleString = "Single execution";
        } else {
            scheduled = Boolean.TRUE;
            scheduleString = schedule.getScheduleString();
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
     * @ejb:transaction-type="REQUIRED"
     */
    public void doScheduledScan(AuthzSubjectValue subject,
                                AppdefEntityID id, 
                                ScanConfigurationCore scanConfig,
                                String scanName,
                                String scanDesc,
                                ScheduleValue schedule)
        throws AutoinventoryException, NamingException, CreateException,
               DuplicateAIScanNameException, ScheduleWillNeverFireException
    {
        // Scheduled jobs are persisted in the autoinventory subsystem
        AIScheduleLocalHome aiScheduleLocalHome =
            AIScheduleUtil.getLocalHome();

        // find the os for the platform
        PlatformValue pValue = null;
        try {
            PlatformManagerLocalHome platformManagerLocalHome =
                PlatformManagerUtil.getLocalHome();

            PlatformManagerLocal platformManagerLocal = null;
            platformManagerLocal = platformManagerLocalHome.create();
            pValue = platformManagerLocal.getPlatformById(subject, id.getId());
        } catch (PermissionException e) {
            throw new AutoinventoryException(e);
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
                     pValue.getPlatformType().getName(),
                     schedule);

        // On-demand scans will have no schedule
        if (schedule == null) {
            jobDetail.setVolatility(true);
            SimpleTrigger trigger = new SimpleTrigger(triggerName, 
                                                      GROUP);
            trigger.setVolatility(true);
            
            try {
                this.log.info("Scheduling job for immediate execution: " + 
                              jobDetail);
                this.scheduler.scheduleJob(jobDetail, trigger);
                return;
            } catch (SchedulerException e) {
                this.log.error("Unable to schedule job: " + e.getMessage());
                return;
            }
        }

        String cronStr;
        try {
            cronStr = ScheduleParser.getCronString(schedule);
        } catch (ScheduleParseException e) {
            this.log.error("Unable to get cron string: " + e.getMessage());
            throw new AutoinventoryException(e);
        }

        BlobColumn configBlob = 
            DBUtil.getBlobColumn( getDbType(),
                                  HQConstants.DATASOURCE,
                                  AI_SCHED_TABLE, 
                                  AI_SCHED_TABLE_COL_ID, 
                                  AI_SCHED_TABLE_COL_BL);

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
                this.scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(aiScheduleLocalHome, scanName);
                AIScheduleLocal aiLoc = 
                    aiScheduleLocalHome.create(id,
                                           subject.getName(),
                                           scanName, scanDesc, 
                                           schedule,
                                           nextFire.getTime(),
                                           triggerName,
                                           jobName);
                configBlob.setId(aiLoc.getId());
                configBlob.setBlobData(scanConfig.serialize());
                configBlob.update();
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (Exception e) {
                this.log.error("Unable to schedule job: " + e.getMessage());
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
                this.scheduler.scheduleJob(jobDetail, trigger);

                checkUniqueName(aiScheduleLocalHome, scanName);
                AIScheduleLocal aiLoc = 
                    aiScheduleLocalHome.create(id, subject.getName(), 
                                           scanName, scanDesc,
                                           schedule,
                                           nextFire.getTime(),
                                           triggerName,
                                           jobName);
                configBlob.setId(aiLoc.getId());
                configBlob.setBlobData(scanConfig.serialize());
                configBlob.update();
            } catch (DuplicateAIScanNameException e) {
                throw e;
            } catch (ScheduleWillNeverFireException e) {
                throw e;
            } catch (ParseException e) {
                this.log.error("Unable to setup cron trigger: " +
                               e.getMessage());
                throw new AutoinventoryException(e);
            } catch (Exception e) {
                this.log.error("Unable to schedule job: " + e.getMessage());
                throw new AutoinventoryException(e);
            }
        }
    }

    /**
     * Get a list of scheduled scans based on appdef id
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList findScheduledJobs(AuthzSubjectValue subject, 
                                      AppdefEntityID id, PageControl pc)
        throws NamingException, FinderException, CreateException
    {
        AIScheduleLocalHome sl =
            AIScheduleUtil.getLocalHome();
        Collection schedule;

        // default the sorting to the next fire time
        pc = PageControl.initDefaults(pc, 
                                      SortAttribute.CONTROL_NEXTFIRE);

        int sortAttr = pc.getSortattribute();
        switch(sortAttr) {
          case SortAttribute.CONTROL_NEXTFIRE:
            if (pc.isAscending())
                schedule = sl.findByEntityFireTimeAsc(id.getType(),
                                                      id.getID());
            else
                schedule = sl.findByEntityFireTimeDesc(id.getType(),
                                                       id.getID());
            break;

          default:
            throw new FinderException("Unknown sort attribute: " +
                                      sortAttr);
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
    public AIScheduleValue findScheduleByID(AuthzSubjectValue subject, 
                                            Integer id)
        throws NamingException, FinderException, CreateException
    {
        AIScheduleLocalHome sl = AIScheduleUtil.getLocalHome();

        AIScheduleValue aiVo = sl.findById(id).getAIScheduleValue();

        BlobColumn configBlob =
            DBUtil.getBlobColumn( getDbType(),
                                  HQConstants.DATASOURCE,
                                  AI_SCHED_TABLE,
                                  AI_SCHED_TABLE_COL_ID,
                                  AI_SCHED_TABLE_COL_BL);
        try {
            configBlob.setId(aiVo.getId());
            configBlob.select();
            aiVo.setConfig(configBlob.getBlobData());
        } catch (SQLException e) {
            throw new SystemException(e);
        }
        return aiVo;
    }

    /**
     * Get a job history based on appdef id
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     *
     */
    public PageList findJobHistory(AuthzSubjectValue subject, 
                                   AppdefEntityID id, PageControl pc)
        throws NamingException, FinderException
    {
        AIHistoryLocalHome histLH = AIHistoryUtil.getLocalHome();
        Collection hist;

        // default the sorting to the date started
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_STARTED);

        int sortAttr = pc.getSortattribute();
        switch(sortAttr) {
          case SortAttribute.CONTROL_STATUS:
            if (pc.isAscending())
                hist = histLH.findByEntityStatusAsc(id.getType(),
                                                    id.getID());
            else
                hist = histLH.findByEntityStatusDesc(id.getType(),
                                                     id.getID());
            break;
          case SortAttribute.CONTROL_STARTED:
            if (pc.isAscending())
                hist = histLH.findByEntityStartTimeAsc(id.getType(),
                                                       id.getID());
            else
                hist = histLH.findByEntityStartTimeDesc(id.getType(),
                                                        id.getID());
            break;
          case SortAttribute.CONTROL_ELAPSED:
            if (pc.isAscending())
                hist = histLH.findByEntityDurationAsc(id.getType(),
                                                      id.getID());
            else
                hist = histLH.findByEntityDurationDesc(id.getType(),
                                                       id.getID());
            break;
          case SortAttribute.CONTROL_DATESCHEDULED:
            if (pc.isAscending())
                hist = histLH.findByEntityDateScheduledAsc(id.getType(),
                                                           id.getID());
            else
                hist = histLH.findByEntityDateScheduledDesc(id.getType(),
                                                            id.getID());
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
    public void deleteAIJob(AuthzSubjectValue subject,
                            Integer ids[])
        throws NamingException, AutoinventoryException
    {
        AIScheduleLocalHome aiScheduleLocalHome
            = AIScheduleUtil.getLocalHome();
        AIScheduleLocal aiScheduleLocal;

        for (int i = 0; i < ids.length; i++) { 
            try {
                AISchedulePK pk = new AISchedulePK(ids[i]);
                aiScheduleLocal = aiScheduleLocalHome.findByPrimaryKey(pk);
                this.scheduler.deleteJob(aiScheduleLocal.getJobName(), GROUP);
                aiScheduleLocal.remove();
            } catch (Exception e) {
                throw new AutoinventoryException("Unable to remove job: " +
                                                 e.getMessage());
            }
        }
    }

    public void checkUniqueName ( AIScheduleLocalHome aiScheduleLocalHome,
                                  String scanName ) 
        throws DuplicateAIScanNameException {

        // Ensure that the name is not a duplicate.
        try {
            AIScheduleLocal aisl = aiScheduleLocalHome.findByScanName(scanName);
            if ( aisl != null ) {
                throw new DuplicateAIScanNameException(scanName);
            }
        } catch (FinderException e) {
            // it's ok, then unique constraint will be OK
        }
        return;
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

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AppdefManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.control.shared.ControlScheduleManagerUtil;
import org.hyperic.hq.control.shared.ControlScheduleValue;
import org.hyperic.hq.control.shared.ScheduledJobNotFoundException;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.ScheduleParseException;
import org.hyperic.hq.scheduler.ScheduleParser;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.scheduler.server.session.BaseScheduleManagerEJB;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

/** 
 * Control manager for dealing with scheduled actions
 *
 * @ejb:bean name="ControlScheduleManager"
 *      jndi-name="ejb/control/ControlScheduleManager"
 *      local-jndi-name="LocalControlScheduleManager"
 *      view-type="local"
 *      type="Stateless"
 */

public class ControlScheduleManagerEJBImpl 
    extends BaseScheduleManagerEJB implements SessionBean {

    private Log log = 
        LogFactory.getLog(ControlScheduleManagerEJBImpl.class.getName());

    private final String JOB_PREFIX      = "action";
    private final String SCHEDULE_PREFIX = "interval";
    private final String GROUP           = "control";
    private InitialContext ic            = null;
    
    private final String PAGER_BASE =
        "org.hyperic.hq.control.server.session.";
    private final String HISTORY_PAGER =
        PAGER_BASE + "PagerProcessor_control_history";
    private final String SCHEDULE_PAGER =
        PAGER_BASE + "PagerProcessor_control_schedule";

    protected String getHistoryPagerClass ()  { return HISTORY_PAGER; }
    protected String getSchedulePagerClass () { return SCHEDULE_PAGER; }
    protected String getJobPrefix ()          { return JOB_PREFIX; }
    protected String getSchedulePrefix ()     { return SCHEDULE_PREFIX; }

    private ControlScheduleDAO getControlScheduleDAO() {
        return DAOFactory.getDAOFactory().getControlScheduleDAO();
    }

    private ControlHistoryDAO getControlHistoryDAO() {
        return DAOFactory.getDAOFactory().getControlHistoryDAO();
    }

    public static ControlScheduleManagerLocal getOne() {
        try {
            return ControlScheduleManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    private AppdefManagerLocal appdefMan = null;
    private AppdefManagerLocal getAppdefMan() {
        if (appdefMan == null) {
            appdefMan = AppdefManagerEJBImpl.getOne();
        }
        return appdefMan;
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {   
        super.ejbCreate();
    }
    
    protected void setupJobData(JobDetail jobDetail, 
                                AuthzSubjectValue subject,
                                AppdefEntityID id, String action,
                                String args,
                                String scheduleString,
                                Boolean scheduled,
                                String description,
                                int[] order)
    {
        super.setupJobData(jobDetail, subject, id,
                           scheduleString, scheduled, order);

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

    protected InitialContext getInitialContext() {
        if (this.ic == null) {
            try {
                this.ic = new InitialContext();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return this.ic;
    }

    private ControlHistoryDAO getHistoryDAO()
    {
        return getControlHistoryDAO();
    }

    // Public EJB methods (local)

    // Dashboard routines
    
    /**
     * Get a list of recent control actions in decending order
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PageList getRecentControlActions(AuthzSubject subject,
                                            int rows, long window)
        throws ApplicationException {
        StopWatch watch = new StopWatch();
        
        // this routine ignores sort attribute!
        Collection recent;

        try {
            ControlHistoryDAO histLH = getControlHistoryDAO();
            recent =
                histLH.findByStartTime(System.currentTimeMillis() - window,
                                       false);

            // Run through the list only returning entities the user
            // has the ability to see
            int count = 0;
            for (Iterator i = recent.iterator(); i.hasNext(); ) {
                ControlHistory cLocal = (ControlHistory) i.next();
                AppdefEntityID entity = 
                    new AppdefEntityID(cLocal.getEntityType().intValue(),
                                       cLocal.getEntityId());
                try {
                    checkViewPermission(subject, entity);
                    
                    AppdefEntityValue aVal =
                        new AppdefEntityValue(entity, subject);
                    cLocal.setEntityName(aVal.getName());

                    if (++count > rows)
                        break;
                } catch (PermissionException e) {
                    i.remove();
                }
            }

            PageList list = this.historyPager.seek(recent, 0, rows);
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
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PageList getPendingControlActions(AuthzSubject subject, int rows)
        throws ApplicationException
    {
        StopWatch watch = new StopWatch();

        // this routine ignores sort attribute!
        Collection pending;

        try {
            ControlScheduleDAO scheduleLH = getControlScheduleDAO();

            pending = scheduleLH.findByFireTime(false);
            // Run through the list only returning entities the user
            // has the ability to see
            int count = 0;
            for (Iterator i = pending.iterator(); i.hasNext();) {
                ControlSchedule sLocal = (ControlSchedule)i.next();
                AppdefEntityID entity = 
                    new AppdefEntityID(sLocal.getEntityType().intValue(),
                                       sLocal.getEntityId().intValue());
                try {
                    checkViewPermission(subject, entity);
                    if (++count > rows)
                        break;
                } catch (PermissionException e) {
                    i.remove();
                }
            }

            // This will remove stale data and update fire times which
            // may result in the list being out of order.  We should
            // probably sort a second time
            PageList list = this.schedulePager.seek(pending, 0, rows);
            list.setTotalSize(pending.size());
            return list;

        } finally {
            if (log.isDebugEnabled())
                log.debug("getPendingControlActions(): " + watch.getElapsed());
        }
    }

    /**
     * Get a list of most active control operations
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     *
     * XXX: This could also take a page control, although we would ignore
     *      everything except for the size
     */
    public PageList getOnDemandControlFrequency(AuthzSubject subject,
                                                int numToReturn)
        throws ApplicationException
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PageList list = new PageList();

        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);
            
            String sqlStr = 
                "SELECT entity_type, entity_id, action, COUNT(id) AS num " +
                "FROM EAM_CONTROL_HISTORY " +
                "WHERE scheduled = " + DBUtil.getBooleanValue(false, conn) +
                " GROUP BY entity_type, entity_id, action " +
                "ORDER by num DESC ";

            stmt = conn.prepareStatement(sqlStr);
            rs = stmt.executeQuery();

            int i = 0;
            while (rs.next() && i++ < numToReturn) {
                
                AppdefEntityID id;
                String name;
                try {
                    AppdefEntityValue aVal;
                    id = new AppdefEntityID(rs.getInt(1), rs.getInt(2));
                    
                    try {
                        checkViewPermission(subject, id);
                    } catch (PermissionException e) {
                        continue;
                    }

                    aVal = new AppdefEntityValue(id, subject);
                    name = aVal.getName();

                } catch (Exception e) {
                    // One of NamingException, FinderException, CreateException.
                    // Should never happen
                    log.error("Error looking up appdef name for type=" +
                                   rs.getInt(1) + " id=" + rs.getInt(2));
                    continue;
                }
                
                ControlFrequencyValue cv = 
                    new ControlFrequencyValue(name,
                                              id.getType(),
                                              id.getID(),
                                              rs.getString(3),
                                              rs.getInt(4));
                list.add(cv);
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (SQLException e) {
            throw new ApplicationException(e);
        } finally {
            DBUtil.closeStatement(log, stmt);
            DBUtil.closeConnection(log, conn);        
        }

        return list;
    }

    /**
     * Get a list of scheduled jobs based on appdef id
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList findScheduledJobs(AuthzSubjectValue subject, 
                                      AppdefEntityID id, PageControl pc)
        throws ScheduledJobNotFoundException
    {
        ControlScheduleDAO sl;
        Collection schedule;

        try {
            sl = getControlScheduleDAO();

            // default the sorting to the next fire time
            pc = PageControl.initDefaults(pc, 
                                          SortAttribute.CONTROL_NEXTFIRE);

            int sortAttr = pc.getSortattribute();
            switch(sortAttr) {
            case SortAttribute.CONTROL_NEXTFIRE:
                schedule = sl.findByEntityFireTime(id.getType(),
                                                   id.getID(),
                                                   pc.isAscending());
                break;

            case SortAttribute.CONTROL_ACTION:
                schedule = sl.findByEntityAction(id.getType(),
                                                 id.getID(),
                                                 pc.isAscending());
                break;
            default:
                throw new FinderException("Unknown sort attribute: " +
                                          sortAttr);
            }
        } catch (FinderException e) {
            throw new ScheduledJobNotFoundException(e);
        }

        // This will remove stale data and update fire times which
        // may result in the list being out of order.  We should
        // probably sort a second time
        PageList list = this.schedulePager.seek(schedule,
                                                pc.getPagenum(),
                                                pc.getPagesize());
        list.setTotalSize(schedule.size());

        return list;
    }

    /**
     * Get a job history based on appdef id
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PageList findJobHistory(AuthzSubjectValue subject, 
                                   AppdefEntityID id, PageControl pc)
        throws ApplicationException
    {
        ControlHistoryDAO histLH;
        Collection hist;

        histLH = getControlHistoryDAO();

        if (pc == null)
            pc = new PageControl();
            
        int sortAttr = pc.getSortattribute();
        switch(sortAttr) {
        case SortAttribute.CONTROL_ACTION:
            hist = histLH.findByEntityAction(id.getType(),
                                             id.getID(),
                                             pc.isAscending());
            break;
        case SortAttribute.CONTROL_STATUS:
            hist = histLH.findByEntityStatus(id.getType(),
                                             id.getID(),
                                             pc.isAscending());
            break;
        case SortAttribute.CONTROL_STARTED:
        case SortAttribute.DEFAULT: // default the sorting to the start
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
            throw new SystemException("Unknown sort attribute: " +
                                      sortAttr);
        }
        PageList list = this.historyPager.seek(hist,
                                               pc.getPagenum(),
                                               pc.getPagesize());
        list.setTotalSize(hist.size());

        return list;
    }

    /**
     * Get a batch job history based on batchJobId and appdef id
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList findGroupJobHistory(AuthzSubject subject, int batchId,
                                        AppdefEntityID id, PageControl pc)
        throws ApplicationException 
    {
        ControlHistoryDAO histLH;
        Collection hist;
        int sortAttr;

        histLH = getControlHistoryDAO();

        // default the sorting to the date started
        pc = PageControl.initDefaults(pc, SortAttribute.CONTROL_STARTED);
        pc.setSortorder(PageControl.SORT_DESC);

        sortAttr = pc.getSortattribute();
        switch(sortAttr) {
        case SortAttribute.CONTROL_ACTION:
            hist = histLH.findByGroupAction(id.getID(), batchId,
                                            pc.isAscending());
            break;
        case SortAttribute.CONTROL_STATUS:
            hist = histLH.findByGroupStatus(id.getID(), batchId,
                                            pc.isAscending());
            break;
        case SortAttribute.CONTROL_STARTED:
            hist = histLH.findByGroupStartTime(id.getID(), batchId,
                                               pc.isAscending());
            break;
        case SortAttribute.CONTROL_ELAPSED:
            hist = histLH.findByGroupDuration(id.getID(), batchId,
                                              pc.isAscending());
            break;
        case SortAttribute.CONTROL_DATESCHEDULED:
            hist = histLH.findByGroupDateScheduled(id.getID(),
                                                   batchId,
                                                   pc.isAscending());
            break;
        case SortAttribute.CONTROL_ENTITYNAME:
            hist = histLH.findByEntity(id.getType(), id.getID());
            break;
        default:
            throw new ApplicationException("Unknown sort attribute: " +
                                           sortAttr);
        }
        
        // The the entity names
        for (Iterator it = hist.iterator(); it.hasNext(); ) {
            ControlHistory ch = (ControlHistory) it.next();
            AppdefEntityValue aev = new AppdefEntityValue(
                new AppdefEntityID(ch.getEntityType().intValue(),
                                   ch.getEntityId()), subject);
            ch.setEntityName(aev.getName());
        }
        
        PageList list = this.historyPager.seek(hist,
                                               pc.getPagenum(),
                                               pc.getPagesize());
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
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteJobHistory(AuthzSubjectValue subject,
                                 Integer[] ids)
        throws ApplicationException
    {
        ControlHistoryDAO hdao = getControlHistoryDAO();

        for (int i=0; i<ids.length; i++) {
            try {
                ControlHistory historyLocal = hdao.findById(ids[i]);
                hdao.remove(historyLocal);
            } catch (ObjectNotFoundException e) {
                throw new ApplicationException(e);
            }
        }
    }

    /**
     * Obtain the current action that is being executed.  If there is
     * no current running action, null is returned.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
     public ControlHistory getCurrentJob(AuthzSubjectValue subject, 
                                              AppdefEntityID id)      
         throws ApplicationException
    {
        ControlHistoryDAO historyLocalHome;
        Collection historyLocals;

        historyLocalHome = getControlHistoryDAO();
        historyLocals =
            historyLocalHome.findByEntityStartTime(id.getType(),
                                                   id.getID(), false);
        Iterator i = historyLocals.iterator();
        if (!i.hasNext())
            return null;

        while (i.hasNext()) {
            ControlHistory history = (ControlHistory) i.next();
            if (history.getStatus().equals(ControlConstants.STATUS_INPROGRESS)){
                return history;
            }
        }

        return null;
    }

    /**
     * Obtain a control history object based on the history id
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ControlHistory getJobByJobId(AuthzSubjectValue subject, Integer id)
        throws ApplicationException
    {
        ControlHistoryDAO hLocalHome;
        ControlHistory local;

        try {
            hLocalHome = getControlHistoryDAO();
            local = hLocalHome.findById(id);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }

        return local;
    }

    /**
     * Obtain the last control action that fired.  Returns null if there
     * are no previous events.  This ignores jobs that are in progress.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ControlHistory getLastJob(AuthzSubjectValue subject,
                                     AppdefEntityID id)
        throws ApplicationException
    {
        ControlHistoryDAO hLocalHome;
        Collection historyLocals;

        hLocalHome = getControlHistoryDAO();
        historyLocals = hLocalHome.findByEntityStartTime(id.getType(),
                                                         id.getID(), false);
        Iterator i = historyLocals.iterator();
        while (i.hasNext()) {
            ControlHistory cLocal = (ControlHistory)i.next();
            if (!cLocal.getStatus().equals(ControlConstants.STATUS_INPROGRESS))
                return cLocal;
        }

        return null;
    }

    /**
     * Obtain a scheduled control action based on an id
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ControlScheduleValue getControlJob(AuthzSubjectValue subject,
                                              Integer id)
        throws PluginException
    {
        ControlScheduleDAO cScheduleLocalHome;
        ControlSchedule cScheduleLocal;

        cScheduleLocalHome = getControlScheduleDAO();

        try {
            cScheduleLocal = cScheduleLocalHome.findById(id);

            // validate the job in the scheduler?

        } catch (Exception e) {
            log.error("Unable to get control job info: " +
                           e.getMessage());
            throw new PluginException(e);
        }

        return cScheduleLocal.getControlScheduleValue();
    }

    /**
     * Delete a scheduled control actions based on id
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteControlJob(AuthzSubjectValue subject,
                                 Integer ids[])
        throws PluginException
    {
        ControlScheduleDAO cScheduleLocalHome;
        ControlSchedule cScheduleLocal;

        cScheduleLocalHome = getControlScheduleDAO();

        for (int i = 0; i < ids.length; i++) {
            try {
                cScheduleLocal = cScheduleLocalHome.findById(ids[i]);
                _scheduler.deleteJob(cScheduleLocal.getJobName(), GROUP);
                cScheduleLocalHome.remove(cScheduleLocal);
            } catch (Exception e) {
                log.error("Unable to remove job: " + e.getMessage());
                throw new PluginException(e);
            }
        }
    }

    /**
     * Removes all jobs associated with an appdef entity
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void removeScheduledJobs(AuthzSubjectValue subject,
                                    AppdefEntityID id)
        throws ScheduledJobRemoveException
    {
        ControlScheduleDAO cScheduleLocalHome = getControlScheduleDAO();
        
        // Any associated triggers will be automatically removed by Quartz.
        Collection jobs = cScheduleLocalHome.findByEntity(id.getType(),
                                                          id.getID());
        for (Iterator i = jobs.iterator(); i.hasNext();) {
            ControlSchedule cScheduleLocal = (ControlSchedule)i.next();
            try {
                _scheduler.deleteJob(cScheduleLocal.getJobName(),
                                         GROUP);
            } catch (SchedulerException e) {
                log.error("Unable to remove job " +
                               cScheduleLocal.getJobName() + ": " +
                               e.getMessage());
            }
            cScheduleLocalHome.remove(cScheduleLocal);
        }
    }

    /**
     * Execute a single action on an appdef entity
     *
     * @ejb:interface-method
     */
    public void doSingleAction(AppdefEntityID id,
                               AuthzSubjectValue subject, String action,
                               String args, int order[])
        throws PluginException
    {
        // Even one time actions go through the scheduler, but there
        // is no need to keep track of them in the ScheduleEntity
        
        String jobName = getJobName(subject, id, action);
        
        // Setup the quartz job class that will handle this control action.
        Class jobClass = 
            (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) ?
            ControlActionGroupJob.class : ControlActionJob.class;

        JobDetail jobDetail = new JobDetail(jobName, GROUP, jobClass);
        jobDetail.setVolatility(true);

        setupJobData(jobDetail, subject, id, action, args, "Single execution",
                     Boolean.FALSE, null, order);

        // All single actions use cron triggers
        SimpleTrigger trigger = 
            new SimpleTrigger(getTriggerName(subject, id, action),
                              GROUP);
        trigger.setVolatility(true);

        try {
            log.debug("Scheduling job for immediate execution: " + 
                           jobDetail);
            _scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error("Unable to schedule job: " + e.getMessage(), e);
        }
    }

    /**
     * Schedule an action on an appdef entity
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void doScheduledAction(AppdefEntityID id, 
                                  AuthzSubjectValue subject, String action,
                                  ScheduleValue schedule,
                                  int[] order)
        throws PluginException, SchedulerException
    {
        // Scheduled jobs are persisted in the control subsystem
        ControlScheduleDAO cScheduleLocalHome;

        cScheduleLocalHome = getControlScheduleDAO();

        String jobName = getJobName(subject, id, action);
        String triggerName = getTriggerName(subject, id, action);

        // Setup the quartz job class that will handle this control action.
        Class jobClass = 
            (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) ?
            ControlActionGroupJob.class : ControlActionJob.class;

        JobDetail jobDetail = new JobDetail(jobName, GROUP, jobClass);

        setupJobData(jobDetail, subject, id, action, null,
                     schedule.getScheduleString(), Boolean.TRUE,
                     schedule.getDescription(), order);

        String cronStr;
        try {
            cronStr = ScheduleParser.getCronString(schedule);
        } catch (ScheduleParseException e) {
            log.error("Unable to get cron string: " + e.getMessage());
            throw new PluginException(e);
        }

        // Single scheduled actions do not have cron strings
        if (cronStr == null) {
            SimpleTrigger trigger = new SimpleTrigger(triggerName, 
                                                      GROUP, 
                                                      schedule.getStart());
            _scheduler.scheduleJob(jobDetail, trigger);
            Date nextFire = trigger.getFireTimeAfter(new Date());
            if (nextFire == null) {
                throw new SchedulerException();
            }

            try {
                cScheduleLocalHome.create(id,
                                          subject.getName(),
                                          action, schedule,
                                          nextFire.getTime(),
                                          triggerName,
                                          jobName,null);
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new PluginException(e);
            }
        } else {        
            try {
                CronTrigger trigger =
                    new CronTrigger(triggerName, GROUP, jobName, GROUP,
                                    schedule.getStart(), schedule.getEnd(),
                                    cronStr);
                trigger.setMisfireInstruction(CronTrigger.
                    MISFIRE_INSTRUCTION_DO_NOTHING);

                _scheduler.scheduleJob(jobDetail, trigger);

                // Quartz used to throw an exception on scheduleJob if the
                // job would never fire.  Guess that is not the case anymore
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire == null) {
                    throw new ScheduleWillNeverFireException();
                }
 
                String stringOrder = null;
                if (order != null)
                    stringOrder = StringUtil.arrayToString(order);

                cScheduleLocalHome.create(id, subject.getName(), action,
                                          schedule,
                                          nextFire.getTime(),
                                          triggerName,
                                          jobName, stringOrder);
            } catch (ParseException e) {
                log.error("Unable to setup cron trigger: " +
                               e.getMessage());
                throw new PluginException(e);
            } catch (SchedulerException e) {
                throw new PluginException(e);
            } catch (Exception e) {
                log.error("Unable to schedule job: " + e.getMessage());
                throw new PluginException(e);
            }
        }
    }

    /**
     * Create a control history entry
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ControlHistory createHistory(AppdefEntityID id,
                                             Integer groupId,
                                             Integer batchId,
                                             String subjectName,
                                             String action,
                                             String args,
                                             Boolean scheduled,
                                             long startTime,
                                             long stopTime,
                                             long scheduleTime,
                                             String status,
                                             String description,
                                             String errorMessage)
    {
        return
            getHistoryDAO().create(id, groupId, batchId, subjectName,
                                   action, args, scheduled,
                                   startTime, stopTime, scheduleTime,
                                   status, description,
                                   errorMessage);    
    }

    /**
     * Update a control history entry
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updateHistory(Integer jobId, long endTime,
                              String status, String message)
        throws ApplicationException
    {
        ControlHistory local;

        try {
            local = getHistoryDAO().findById(jobId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }

        local.setEndTime(endTime);
        local.setStatus(status);
        local.setMessage(message);
    }

    /**
     * Get a control history value based on primary key
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ControlHistory getJobHistoryValue(Integer jobId)
        throws ApplicationException
    {
        try {
            ControlHistoryDAO home = getHistoryDAO();
            return home.findById(jobId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Get a control history value based on primary key
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeHistory(Integer id)
        throws ApplicationException
    {
        try {
            ControlHistoryDAO home = getHistoryDAO();
            ControlHistory local = home.findById(id);
            home.remove(local);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationException(e);
        }
    }
    
    /**
     * Check view permission for an appdef entity
     *
     * @throws PermissionException if the user does not have the
     *         modify Operation for the given resource.
     */
    private void checkViewPermission(AuthzSubject caller, AppdefEntityID id)
        throws PermissionException {
        getAppdefMan().checkViewPermission(caller, id);
    }
    
    private class ControlHistoryLocalComparatorAsc implements Comparator {

        public int compare(Object o1, Object o2) {
            return ((ControlHistory)o1).getEntityName().
                compareTo(((ControlHistory)o2).getEntityName());
        }

        public boolean equals(Object other) {
            return false;
        }
    }

    private class ControlHistoryLocalComparatorDesc implements Comparator {

        public int compare(Object o1, Object o2) {
            return -(((ControlHistory)o1).getEntityName().
                     compareTo(((ControlHistory)o2).getEntityName()));
        }

        public boolean equals(Object other) {
            return false;
        }
    }
}

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

package org.hyperic.hq.scheduler.server.session;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.ObjectName;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.mx.util.ObjectNameFactory;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.spi.JobFactory;

/**
 * The Scheduler session bean is a proxy to the Quartz scheduler MBean that is 
 * used for scheduling jobs to be executed within the application server.
 *
 * @ejb.bean
 *      name            = "Scheduler"
 *      jndi-name       = "ejb/scheduler/Scheduler"
 *      local-jndi-name = "LocalQuartzScheduler"
 *      view-type       = "local"
 *      type            = "Stateless"
 * @ejb.util
 *      generate = "physical"
 * @ejb.transaction
 *      type = "REQUIRED"
 */
public class SchedulerEJBImpl
   implements SessionBean,
              SchedulerServiceMBean
{
   private static final long serialVersionUID     = 1962110997028005340L;
   private static final ObjectName SCHEDULER_MBEAN_NAME 
       = ObjectNameFactory.create("hyperic.jmx:type=Service,name=Scheduler");
   private Log log = LogFactory.getLog(SchedulerEJBImpl.class.getName());
   private SessionContext sessCtx;

   /**
    * Returns a reference to a proxy to the scheduler service MBean, which
    * itself is a proxy that delegates to the Quartz scheduler.
    *
    * @return proxy to the scheduler service MBean
    *
    * @throws SchedulerException if failed to find the MBean
    */
   private SchedulerServiceMBean getSchedulerService()
       throws SchedulerException
   {
      try {
          return (SchedulerServiceMBean)
              MBeanProxy.get(SchedulerServiceMBean.class,
                             SCHEDULER_MBEAN_NAME,
                             MBeanServerLocator.locateJBoss());
      } catch (Exception e) {
         throw new SchedulerException("Failed to get a proxy to the " +
                                      "scheduler service MBean", e);
      }
   }

   /**
    * Delegates to the Scheduler Service MBean.
    *
    * @see SchedulerServiceMBean#getQuartzProperties()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Properties getQuartzProperties()
   {
      try {
         return getSchedulerService().getQuartzProperties();
      } catch (SchedulerException e) {
         log.error("Failed to get the Quartz properties", e);
         return null;
      }
   }

   /**
    * Delegates to the Scheduler Service MBean in order to set the properties
    * for Quartz and reinitialize th Quartz scheduler factory.
    *
    * @see SchedulerServiceMBean#setQuartzProperties(Properties)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void setQuartzProperties(final Properties quartzProps)
       throws SchedulerException
   {
      getSchedulerService().setQuartzProperties(quartzProps);
   }

   //---------------------------------------------------------------------
   //-- Scheduler interface methods
   //---------------------------------------------------------------------

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerName()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String getSchedulerName()
       throws SchedulerException
   {
      return getSchedulerService().getSchedulerName();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerInstanceId()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String getSchedulerInstanceId()
       throws SchedulerException
   {
      return getSchedulerService().getSchedulerInstanceId();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getContext()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public SchedulerContext getContext()
       throws SchedulerException
   {
      return getSchedulerService().getContext();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getMetaData()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public SchedulerMetaData getMetaData()
       throws SchedulerException
   {
      return getSchedulerService().getMetaData();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#start()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void start()
       throws SchedulerException
   {
      getSchedulerService().start();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#startScheduler()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void startScheduler()
       throws SchedulerException
   {
      getSchedulerService().startScheduler();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see        org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pause()
    * @deprecated
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pause()
       throws SchedulerException
   {
      getSchedulerService().pause();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see        org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#isPaused()
    * @deprecated
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean isPaused()
       throws SchedulerException
   {
      return getSchedulerService().isPaused();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#shutdown()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void shutdown()
       throws SchedulerException
   {
      getSchedulerService().shutdown();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#shutdown(boolean)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void shutdown(boolean waitForJobsToComplete)
       throws SchedulerException
   {
      getSchedulerService().shutdown(waitForJobsToComplete);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#isShutdown()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean isShutdown()
       throws SchedulerException
   {
      return getSchedulerService().isShutdown();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCurrentlyExecutingJobs()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public List getCurrentlyExecutingJobs()
       throws SchedulerException
   {
      return getSchedulerService().getCurrentlyExecutingJobs();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail,org.quartz.Trigger)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Date scheduleJob(JobDetail jobDetail,
                            Trigger   trigger)
       throws SchedulerException
   {
      if (log.isDebugEnabled()) {
         log.debug("Job details: " + jobDetail);
      }

      return getSchedulerService().scheduleJob(jobDetail, trigger);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#scheduleJob(org.quartz.Trigger)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Date scheduleJob(Trigger trigger)
       throws SchedulerException
   {
      return getSchedulerService().scheduleJob(trigger);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addJob(org.quartz.JobDetail, boolean)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addJob(JobDetail jobDetail,
                       boolean   replace)
       throws SchedulerException
   {
      getSchedulerService().addJob(jobDetail, replace);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#deleteJob(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean deleteJob(String jobName,
                             String groupName)
       throws SchedulerException
   {
      return getSchedulerService().deleteJob(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#unscheduleJob(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean unscheduleJob(String triggerName,
                                 String groupName)
       throws SchedulerException
   {
      return getSchedulerService().unscheduleJob(triggerName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#triggerJob(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void triggerJob(String jobName,
                           String groupName)
       throws SchedulerException
   {
      getSchedulerService().triggerJob(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#triggerJobWithVolatileTrigger(java.lang.String,
    *      java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void triggerJobWithVolatileTrigger(String jobName,
                                              String groupName)
       throws SchedulerException
   {
      getSchedulerService().triggerJobWithVolatileTrigger(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseTrigger(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pauseTrigger(String triggerName,
                             String groupName)
       throws SchedulerException
   {
      getSchedulerService().pauseTrigger(triggerName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseTriggerGroup(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pauseTriggerGroup(String groupName)
       throws SchedulerException
   {
      getSchedulerService().pauseTriggerGroup(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseJob(java.lang.String, java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pauseJob(String jobName,
                         String groupName)
       throws SchedulerException
   {
      getSchedulerService().pauseJob(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseJobGroup(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pauseJobGroup(String groupName)
       throws SchedulerException
   {
      getSchedulerService().pauseJobGroup(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeTrigger(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void resumeTrigger(String triggerName,
                              String groupName)
       throws SchedulerException
   {
      getSchedulerService().resumeTrigger(triggerName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeTriggerGroup(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void resumeTriggerGroup(String groupName)
       throws SchedulerException
   {
      getSchedulerService().resumeTriggerGroup(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeJob(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void resumeJob(String jobName,
                          String groupName)
       throws SchedulerException
   {
      getSchedulerService().resumeJob(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeJobGroup(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void resumeJobGroup(String groupName)
       throws SchedulerException
   {
      getSchedulerService().resumeJobGroup(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobGroupNames()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String[] getJobGroupNames()
       throws SchedulerException
   {
      return getSchedulerService().getJobGroupNames();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobNames(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String[] getJobNames(String groupName)
       throws SchedulerException
   {
      return getSchedulerService().getJobNames(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggersOfJob(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Trigger[] getTriggersOfJob(String jobName,
                                      String groupName)
       throws SchedulerException
   {
      return getSchedulerService().getTriggersOfJob(jobName, groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerGroupNames()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String[] getTriggerGroupNames()
       throws SchedulerException
   {
      return getSchedulerService().getTriggerGroupNames();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerNames(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String[] getTriggerNames(String groupName)
       throws SchedulerException
   {
      return getSchedulerService().getTriggerNames(groupName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobDetail(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public JobDetail getJobDetail(String jobName,
                                  String jobGroup)
       throws SchedulerException
   {
      return getSchedulerService().getJobDetail(jobName, jobGroup);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTrigger(java.lang.String,java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Trigger getTrigger(String triggerName,
                              String triggerGroup)
       throws SchedulerException
   {
      return getSchedulerService().getTrigger(triggerName, triggerGroup);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#deleteCalendar(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean deleteCalendar(String calName)
       throws SchedulerException
   {
      return getSchedulerService().deleteCalendar(calName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCalendar(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Calendar getCalendar(String calName)
       throws SchedulerException
   {
      return getSchedulerService().getCalendar(calName);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCalendarNames()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public String[] getCalendarNames()
       throws SchedulerException
   {
      return getSchedulerService().getCalendarNames();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addGlobalJobListener(org.quartz.JobListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addGlobalJobListener(JobListener jobListener)
       throws SchedulerException
   {
      getSchedulerService().addGlobalJobListener(jobListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addJobListener(org.quartz.JobListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addJobListener(JobListener jobListener)
       throws SchedulerException
   {
      getSchedulerService().addJobListener(jobListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeGlobalJobListener(org.quartz.JobListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean removeGlobalJobListener(JobListener jobListener)
       throws SchedulerException
   {
      return getSchedulerService().removeGlobalJobListener(jobListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeJobListener(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean removeJobListener(String name)
       throws SchedulerException
   {
      return getSchedulerService().removeJobListener(name);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getGlobalJobListeners()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public List getGlobalJobListeners()
       throws SchedulerException
   {
      return getSchedulerService().getGlobalJobListeners();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobListenerNames()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Set getJobListenerNames()
       throws SchedulerException
   {
      return getSchedulerService().getJobListenerNames();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobListener(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public JobListener getJobListener(String name)
       throws SchedulerException
   {
      return getSchedulerService().getJobListener(name);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addGlobalTriggerListener(org.quartz.TriggerListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addGlobalTriggerListener(TriggerListener triggerListener)
       throws SchedulerException
   {
      getSchedulerService().addGlobalTriggerListener(triggerListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addTriggerListener(org.quartz.TriggerListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addTriggerListener(TriggerListener triggerListener)
       throws SchedulerException
   {
      getSchedulerService().addTriggerListener(triggerListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeGlobalTriggerListener(org.quartz.TriggerListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean removeGlobalTriggerListener(TriggerListener triggerListener)
       throws SchedulerException
   {
      return getSchedulerService().removeGlobalTriggerListener(triggerListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeTriggerListener(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean removeTriggerListener(String name)
       throws SchedulerException
   {
      return getSchedulerService().removeTriggerListener(name);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getGlobalTriggerListeners()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public List getGlobalTriggerListeners()
       throws SchedulerException
   {
      return getSchedulerService().getGlobalTriggerListeners();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerListenerNames()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Set getTriggerListenerNames()
       throws SchedulerException
   {
      return getSchedulerService().getTriggerListenerNames();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerListener(java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public TriggerListener getTriggerListener(String name)
       throws SchedulerException
   {
      return getSchedulerService().getTriggerListener(name);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addSchedulerListener(org.quartz.SchedulerListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addSchedulerListener(SchedulerListener schedulerListener)
       throws SchedulerException
   {
      getSchedulerService().addSchedulerListener(schedulerListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeSchedulerListener(org.quartz.SchedulerListener)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean removeSchedulerListener(SchedulerListener schedulerListener)
       throws SchedulerException
   {
      return getSchedulerService().removeSchedulerListener(schedulerListener);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerListeners()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public List getSchedulerListeners()
       throws SchedulerException
   {
      return getSchedulerService().getSchedulerListeners();
   }

   // Quartz methods that are new in 1.5.1 that were not in 1.0.7

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#addCalendar(java.lang.String, org.quartz.Calendar, boolean, boolean)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void addCalendar(String   calName,
                            Calendar calendar,
                            boolean  replace,
                            boolean  updateTriggers)
       throws SchedulerException
   {
      getSchedulerService().addCalendar(calName, calendar, replace, updateTriggers);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#getPausedTriggerGroups()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Set getPausedTriggerGroups()
       throws SchedulerException
   {
      return getSchedulerService().getPausedTriggerGroups();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#getTriggerState(java.lang.String, java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public int getTriggerState(String triggerName,
                               String triggerGroup)
       throws SchedulerException
   {
      return getSchedulerService().getTriggerState(triggerName, triggerGroup);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#interrupt(java.lang.String, java.lang.String)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean interrupt(String jobName,
                             String groupName)
   throws UnableToInterruptJobException
   {
      try
      {
         return getSchedulerService().interrupt(jobName, groupName);
      }
      catch (SchedulerException e)
      {
         throw new UnableToInterruptJobException(e);
      }
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#isInStandbyMode()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public boolean isInStandbyMode()
       throws SchedulerException
   {
      return getSchedulerService().isInStandbyMode();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#pauseAll()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void pauseAll()
       throws SchedulerException
   {
      getSchedulerService().pauseAll();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#rescheduleJob(java.lang.String, java.lang.String, org.quartz.Trigger)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public Date rescheduleJob(String  triggerName,
                              String  groupName,
                              Trigger newTrigger)
       throws SchedulerException
   {
      return getSchedulerService().rescheduleJob(triggerName, groupName, newTrigger);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#resumeAll()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void resumeAll()
       throws SchedulerException
   {
      getSchedulerService().resumeAll();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void setJobFactory(JobFactory factory)
       throws SchedulerException
   {
      getSchedulerService().setJobFactory(factory);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#standby()
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void standby()
       throws SchedulerException
   {
      getSchedulerService().standby();
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#triggerJob(java.lang.String, java.lang.String, org.quartz.JobDataMap)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void triggerJob(String     jobName,
                           String     groupName,
                           JobDataMap data)
       throws SchedulerException
   {
      getSchedulerService().triggerJob(jobName, groupName, data);
   }

   /**
    * Delegates to the Quartz scheduler.
    *
    * @see org.quartz.Scheduler#triggerJobWithVolatileTrigger(String, String, org.quartz.JobDataMap)
    *
    * @ejb.interface-method
    * @ejb.transaction
    *      type = "REQUIRED"
    */
   public void triggerJobWithVolatileTrigger(String     jobName,
                                              String     groupName,
                                              JobDataMap data)
       throws SchedulerException
   {
      getSchedulerService().triggerJob(jobName, groupName, data);
   }

   //---------------------------------------------------------------------
   //-- SessionBean interface methods
   //---------------------------------------------------------------------
   /**
    * no-op.
    *
    * @throws CreateException
    */
   public void ejbCreate()
   throws CreateException
   {
   }

   /**
    * no-op.
    */
   public void ejbRemove()
   {
   }

   /**
    * no-op.
    */
   public void ejbActivate()
   {
   }

   /**
    * no-op.
    */
   public void ejbPassivate()
   {
   }

   /**
    * Sets the EJB session context.
    *
    * @param ctx
    */
   public void setSessionContext(SessionContext ctx)
   {
      sessCtx = ctx;
   }

   /**
    * Returns the EJB session context.
    *
    * @return the context
    */
   public SessionContext getSessionContext()
   {
      return sessCtx;
   }
   
   public static SchedulerLocal getOne() {
       try {
           return SchedulerUtil.getLocalHome().create();
       } catch(Exception e) {
           throw new SystemException(e);
       }
   }
}

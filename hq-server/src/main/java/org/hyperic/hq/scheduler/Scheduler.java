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

package org.hyperic.hq.scheduler;


import org.quartz.Trigger;

public interface Scheduler {

    /**
     * Delegates to the Scheduler Service MBean.
     * @see SchedulerServiceMBean#getQuartzProperties()
     */
    public java.util.Properties getQuartzProperties(  ) ;

    /**
     * Delegates to the Scheduler Service MBean in order to set the properties for Quartz and reinitialize th Quartz scheduler factory.
     * @see SchedulerServiceMBean#setQuartzProperties(Properties)
     */
    public void setQuartzProperties( java.util.Properties quartzProps ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerName()
     */
    public java.lang.String getSchedulerName(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerInstanceId()
     */
    public java.lang.String getSchedulerInstanceId(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getContext()
     */
    public org.quartz.SchedulerContext getContext(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getMetaData()
     */
    public org.quartz.SchedulerMetaData getMetaData(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#start()
     */
    public void start(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#startScheduler()
     */
    public void startScheduler(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pause()
     * @deprecated 
     */
    public void pause(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#isPaused()
     * @deprecated 
     */
    public boolean isPaused(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#shutdown()
     */
    public void shutdown(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#shutdown(boolean)
     */
    public void shutdown( boolean waitForJobsToComplete ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#isShutdown()
     */
    public boolean isShutdown(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCurrentlyExecutingJobs()
     */
    public java.util.List getCurrentlyExecutingJobs(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail,org.quartz.Trigger)
     */
    public java.util.Date scheduleJob( org.quartz.JobDetail jobDetail,org.quartz.Trigger trigger ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#scheduleJob(org.quartz.Trigger)
     */
    public java.util.Date scheduleJob( org.quartz.Trigger trigger ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addJob(org.quartz.JobDetail, boolean)
     */
    public void addJob( org.quartz.JobDetail jobDetail,boolean replace ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#deleteJob(java.lang.String,java.lang.String)
     */
    public boolean deleteJob( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#unscheduleJob(java.lang.String,java.lang.String)
     */
    public boolean unscheduleJob( java.lang.String triggerName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#triggerJob(java.lang.String,java.lang.String)
     */
    public void triggerJob( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#triggerJobWithVolatileTrigger(java.lang.String, java.lang.String)
     */
    public void triggerJobWithVolatileTrigger( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseTrigger(java.lang.String,java.lang.String)
     */
    public void pauseTrigger( java.lang.String triggerName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseTriggerGroup(java.lang.String)
     */
    public void pauseTriggerGroup( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseJob(java.lang.String, java.lang.String)
     */
    public void pauseJob( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#pauseJobGroup(java.lang.String)
     */
    public void pauseJobGroup( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeTrigger(java.lang.String,java.lang.String)
     */
    public void resumeTrigger( java.lang.String triggerName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeTriggerGroup(java.lang.String)
     */
    public void resumeTriggerGroup( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeJob(java.lang.String,java.lang.String)
     */
    public void resumeJob( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#resumeJobGroup(java.lang.String)
     */
    public void resumeJobGroup( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobGroupNames()
     */
    public java.lang.String[] getJobGroupNames(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobNames(java.lang.String)
     */
    public java.lang.String[] getJobNames( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggersOfJob(java.lang.String,java.lang.String)
     */
    public org.quartz.Trigger[] getTriggersOfJob( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerGroupNames()
     */
    public java.lang.String[] getTriggerGroupNames(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerNames(java.lang.String)
     */
    public java.lang.String[] getTriggerNames( java.lang.String groupName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobDetail(java.lang.String,java.lang.String)
     */
    public org.quartz.JobDetail getJobDetail( java.lang.String jobName,java.lang.String jobGroup ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTrigger(java.lang.String,java.lang.String)
     */
    public org.quartz.Trigger getTrigger( java.lang.String triggerName,java.lang.String triggerGroup ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#deleteCalendar(java.lang.String)
     */
    public boolean deleteCalendar( java.lang.String calName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCalendar(java.lang.String)
     */
    public org.quartz.Calendar getCalendar( java.lang.String calName ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getCalendarNames()
     */
    public java.lang.String[] getCalendarNames(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addGlobalJobListener(org.quartz.JobListener)
     */
    public void addGlobalJobListener( org.quartz.JobListener jobListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addJobListener(org.quartz.JobListener)
     */
    public void addJobListener( org.quartz.JobListener jobListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeGlobalJobListener(org.quartz.JobListener)
     */
    public boolean removeGlobalJobListener( org.quartz.JobListener jobListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeJobListener(java.lang.String)
     */
    public boolean removeJobListener( java.lang.String name ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getGlobalJobListeners()
     */
    public java.util.List getGlobalJobListeners(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobListenerNames()
     */
    public java.util.Set getJobListenerNames(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getJobListener(java.lang.String)
     */
    public org.quartz.JobListener getJobListener( java.lang.String name ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addGlobalTriggerListener(org.quartz.TriggerListener)
     */
    public void addGlobalTriggerListener( org.quartz.TriggerListener triggerListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addTriggerListener(org.quartz.TriggerListener)
     */
    public void addTriggerListener( org.quartz.TriggerListener triggerListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeGlobalTriggerListener(org.quartz.TriggerListener)
     */
    public boolean removeGlobalTriggerListener( org.quartz.TriggerListener triggerListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeTriggerListener(java.lang.String)
     */
    public boolean removeTriggerListener( java.lang.String name ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getGlobalTriggerListeners()
     */
    public java.util.List getGlobalTriggerListeners(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerListenerNames()
     */
    public java.util.Set getTriggerListenerNames(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getTriggerListener(java.lang.String)
     */
    public org.quartz.TriggerListener getTriggerListener( java.lang.String name ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#addSchedulerListener(org.quartz.SchedulerListener)
     */
    public void addSchedulerListener( org.quartz.SchedulerListener schedulerListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#removeSchedulerListener(org.quartz.SchedulerListener)
     */
    public boolean removeSchedulerListener( org.quartz.SchedulerListener schedulerListener ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.hyperic.hq.scheduler.server.mbean.SchedulerServiceMBean#getSchedulerListeners()
     */
    public java.util.List getSchedulerListeners(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#addCalendar(java.lang.String, org.quartz.Calendar, boolean, boolean)
     */
    public void addCalendar( java.lang.String calName,org.quartz.Calendar calendar,boolean replace,boolean updateTriggers ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#getPausedTriggerGroups()
     */
    public java.util.Set getPausedTriggerGroups(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#getTriggerState(java.lang.String, java.lang.String)
     */
    public int getTriggerState( java.lang.String triggerName,java.lang.String triggerGroup ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#interrupt(java.lang.String, java.lang.String)
     */
    public boolean interrupt( java.lang.String jobName,java.lang.String groupName ) throws org.quartz.UnableToInterruptJobException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#isInStandbyMode()
     */
    public boolean isInStandbyMode(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#pauseAll()
     */
    public void pauseAll(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#rescheduleJob(java.lang.String, java.lang.String, org.quartz.Trigger)
     */
    public java.util.Date rescheduleJob( java.lang.String triggerName,java.lang.String groupName,org.quartz.Trigger newTrigger ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#resumeAll()
     */
    public void resumeAll(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    public void setJobFactory( org.quartz.spi.JobFactory factory ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#standby()
     */
    public void standby(  ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#triggerJob(java.lang.String, java.lang.String, org.quartz.JobDataMap)
     */
    public void triggerJob( java.lang.String jobName,java.lang.String groupName,org.quartz.JobDataMap data ) throws org.quartz.SchedulerException;

    /**
     * Delegates to the Quartz scheduler.
     * @see org.quartz.Scheduler#triggerJobWithVolatileTrigger(String, String, org.quartz.JobDataMap)
     */
    public void triggerJobWithVolatileTrigger( java.lang.String jobName,java.lang.String groupName,org.quartz.JobDataMap data ) throws org.quartz.SchedulerException;
    
    
}

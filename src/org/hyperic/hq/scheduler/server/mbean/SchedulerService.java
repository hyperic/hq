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

package org.hyperic.hq.scheduler.server.mbean;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

/**
 * Scheduler service.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=Scheduler"
 *            extends="org.quartz.Scheduler"
 */
public class SchedulerService implements SchedulerServiceMBean, MBeanRegistration {

    protected Log log = LogFactory.getLog( SchedulerService.class.getName() );
    private Properties quartzProps;

    private StdSchedulerFactory schedFact = new StdSchedulerFactory();
    private Scheduler _sched;

    public SchedulerService() {
    }

    /**
     * Get the properties for Quartz.
     *
     * @jmx:managed-attribute
     */
    public Properties getQuartzProperties() {
        return quartzProps;
    }

    /**
     * Set the properties for Quartz and reinitialize the Quartz
     * scheduler factory.
     *
     * @jmx:managed-attribute
     */
    public void setQuartzProperties(final Properties quartzProps)
        throws SchedulerException
    {
        this.quartzProps = quartzProps;
        schedFact.initialize(quartzProps);
    }

    /**
     * start() is also part of the Scheduler interface, so it is found below
     */

    public void stop() throws SchedulerException {
        log.info("Stopping " + _sched );
        shutdown();
        _sched = null;
    }

    /**
     * @jmx:managed-attribute
     */
    public String getSchedulerName() throws SchedulerException {
        return _sched.getSchedulerName();
    }

    /**
     * @jmx:managed-attribute
     */
    public String getSchedulerInstanceId() throws SchedulerException {
        return _sched.getSchedulerInstanceId();
    }

    /**
     * @jmx:managed-attribute
     */
    public SchedulerContext getContext() throws SchedulerException {
        return _sched.getContext();
    }

    /**
     * @jmx:managed-attribute
     */
    public SchedulerMetaData getMetaData() throws SchedulerException {
        return _sched.getMetaData();
    }

    /**
     * @jmx:managed-operation
     */
    public void start() throws SchedulerException {}

    /**
     * @jmx:managed-operation
     */
    public void startScheduler() throws SchedulerException {
        log.info("Starting Scheduler");
        _sched = schedFact.getScheduler();
        _sched.start();
    }
    
    /**
     * @jmx:managed-operation
     */
    public void pause() throws SchedulerException {
        _sched.pause();
    }

    /**
     * @jmx:managed-attribute
     */
    public boolean isPaused() throws SchedulerException {
        return _sched.isPaused();
    }

    /**
     * @jmx:managed-operation
     */
    public void shutdown() throws SchedulerException {
        _sched.shutdown();
    }

    /**
     * @jmx:managed-operation
     */
    public void shutdown(boolean waitForJobsToComplete) 
        throws SchedulerException
    {
        _sched.shutdown(waitForJobsToComplete);
    }

    /**
     * @jmx:managed-attribute
     */
    public boolean isShutdown() throws SchedulerException {
        return _sched.isShutdown();
    }

    /**
     * @jmx:managed-operation
     */
    public List getCurrentlyExecutingJobs() throws SchedulerException {
        return _sched.getCurrentlyExecutingJobs();
    }

    /**
     * @jmx:managed-operation
     */
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger)
        throws SchedulerException
    { 
        if ( log.isDebugEnabled() ) {
            log.debug("Job details: " + jobDetail);
        }

        return _sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * @jmx:managed-operation
     */
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return _sched.scheduleJob(trigger);
    }

    /**
     * @jmx:managed-operation
     */
    public void addJob(JobDetail jobDetail, boolean replace)
        throws SchedulerException
    {
        _sched.addJob(jobDetail, replace);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean deleteJob(String jobName, String groupName)
        throws SchedulerException
    {
        return _sched.deleteJob(jobName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean unscheduleJob(String triggerName, String groupName)
        throws SchedulerException
    {
        return _sched.unscheduleJob(triggerName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void triggerJob(String jobName, String groupName)
        throws SchedulerException
    {
        _sched.triggerJob(jobName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void triggerJob(String jobName, String groupName, JobDataMap map)
        throws SchedulerException
    {
        _sched.triggerJob(jobName, groupName, map);
    }

    /**
     * @jmx:managed-operation
     */
    public void triggerJobWithVolatileTrigger(String jobName,
                                              String groupName)
        throws SchedulerException
    {
        _sched.triggerJobWithVolatileTrigger(jobName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void triggerJobWithVolatileTrigger(String jobName,
                                              String groupName,
                                              JobDataMap map)
        throws SchedulerException
    {
        _sched.triggerJobWithVolatileTrigger(jobName, groupName, map);
    }

    /**
     * @jmx:managed-operation
     */
    public void pauseTrigger(String triggerName, String groupName)
        throws SchedulerException
    {
        _sched.pauseTrigger(triggerName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void pauseTriggerGroup(String groupName) throws SchedulerException {
        _sched.pauseTriggerGroup(groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void pauseJob(String jobName, String groupName)
        throws SchedulerException
    {
        _sched.pauseJob(jobName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void pauseJobGroup(String groupName) throws SchedulerException {
        _sched.pauseJobGroup(groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void resumeTrigger(String triggerName, String groupName)
        throws SchedulerException
    {
        _sched.resumeTrigger(triggerName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void resumeTriggerGroup(String groupName)
        throws SchedulerException
    {
        _sched.resumeTriggerGroup(groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void resumeJob(String jobName, String groupName)
        throws SchedulerException
    {
        _sched.resumeJob(jobName, groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void resumeJobGroup(String groupName) throws SchedulerException {
        _sched.resumeJobGroup(groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public void pauseAll() throws SchedulerException {
        _sched.pauseAll();
    }

    /**
     * @jmx:managed-operation
     */
    public void resumeAll() throws SchedulerException {
        _sched.resumeAll();
    }

    /**
     * @jmx:managed-attribute
     */
    public String[] getJobGroupNames() throws SchedulerException {
        return _sched.getJobGroupNames();
    }

    /**
     * @jmx:managed-operation
     */
    public String[] getJobNames(String groupName) throws SchedulerException {
        return _sched.getJobNames(groupName);
    }

    /**
     * @jmx:managed-attribute
     */
    public Trigger[] getTriggersOfJob(String jobName, String groupName)
        throws SchedulerException
    {
        return _sched.getTriggersOfJob(jobName, groupName);
    }

    /**
     * @jmx:managed-attribute
     */
    public String[] getTriggerGroupNames() throws SchedulerException {
        return _sched.getTriggerGroupNames();
    }

    /**
     * @jmx:managed-attribute
     */
    public String[] getTriggerNames(String groupName)
        throws SchedulerException
    {
        return _sched.getTriggerNames(groupName);
    }

    /**
     * @jmx:managed-operation
     */
    public JobDetail getJobDetail(String jobName, String jobGroup)
        throws SchedulerException
    {
        return _sched.getJobDetail(jobName, jobGroup);
    }

    /**
     * @jmx:managed-operation
     */
    public Trigger getTrigger(String triggerName, String triggerGroup)
        throws SchedulerException
    {
        return _sched.getTrigger(triggerName, triggerGroup);
    }

    /**
     * @jmx:managed-operation
     */
    public void addCalendar(String calName, Calendar calendar,
                            boolean replace, boolean updateTriggers) 
        throws SchedulerException
    {
        _sched.addCalendar(calName, calendar, replace, updateTriggers);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean deleteCalendar(String calName) throws SchedulerException {
        return _sched.deleteCalendar(calName);
    }

    /**
     * @jmx:managed-operation
     */
    public Calendar getCalendar(String calName) throws SchedulerException {
        return _sched.getCalendar(calName);
    }

    /**
     * @jmx:managed-attribute
     */
    public String[] getCalendarNames() throws SchedulerException {
        return _sched.getCalendarNames();
    }

    /**
     * @jmx:managed-operation
     */
    public void addGlobalJobListener(JobListener jobListener)
        throws SchedulerException
    {
        _sched.addGlobalJobListener(jobListener);
    }

    /**
     * @jmx:managed-operation
     */
    public void addJobListener(JobListener jobListener)
        throws SchedulerException
    {
        _sched.addJobListener(jobListener);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean removeGlobalJobListener(JobListener jobListener)
        throws SchedulerException
    {
        return _sched.removeGlobalJobListener(jobListener);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean removeJobListener(String name) throws SchedulerException {
        return _sched.removeJobListener(name);
    }

    /**
     * @jmx:managed-attribute
     */
    public List getGlobalJobListeners() throws SchedulerException {
        return _sched.getGlobalJobListeners();
    }

    /**
     * <p>Calls the equivalent method on the 'proxied'
     * @jmx:managed-attribute
     */
    public Set getJobListenerNames() throws SchedulerException {
        return _sched.getJobListenerNames();
    }

    /**
     * @jmx:managed-operation
     */
    public JobListener getJobListener(String name) throws SchedulerException {
        return _sched.getJobListener(name);
    }

    /**
     * @jmx:managed-operation
     */
    public void addGlobalTriggerListener(TriggerListener triggerListener)
        throws SchedulerException
    {
        _sched.addGlobalTriggerListener(triggerListener);
    }

    /**
     * @jmx:managed-operation
     */
    public void addTriggerListener(TriggerListener triggerListener)
        throws SchedulerException
    {
        _sched.addTriggerListener(triggerListener);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean removeGlobalTriggerListener(TriggerListener triggerListener)
        throws SchedulerException 
    {
        return _sched.removeGlobalTriggerListener(triggerListener);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean removeTriggerListener(String name)
        throws SchedulerException
    {
        return _sched.removeTriggerListener(name);
    }

    /**
     * @jmx:managed-attribute
     */
    public List getGlobalTriggerListeners() throws SchedulerException {
        return _sched.getGlobalTriggerListeners();
    }

    /**
     * @jmx:managed-attribute
     */
    public Set getTriggerListenerNames() throws SchedulerException {
        return _sched.getTriggerListenerNames();
    }

    /**
     * @jmx:managed-operation
     */
    public TriggerListener getTriggerListener(String name)
        throws SchedulerException
    {
        return _sched.getTriggerListener(name);
    }

    /**
     * @jmx:managed-operation
     */
    public void addSchedulerListener(SchedulerListener schedulerListener) 
        throws SchedulerException 
    {
        _sched.addSchedulerListener(schedulerListener);
    }

    /**
     * @jmx:managed-operation
     */
    public boolean removeSchedulerListener(SchedulerListener schedulerListener)
        throws SchedulerException 
    {
        return _sched.removeSchedulerListener(schedulerListener);
    }

    /**
     * @jmx:managed-attribute
     */
    public List getSchedulerListeners() throws SchedulerException {
        return _sched.getSchedulerListeners();
    }

    /**
     * @jmx:managed-attribute
     */
    public boolean interrupt(String jobName, String groupName)
        throws UnableToInterruptJobException
    {
        return _sched.interrupt(jobName, groupName);
    }

    /**
     * @jmx:managed-attribute
     */
    public int getTriggerState(String triggerName, String triggerGroup) 
        throws SchedulerException
    {
        return _sched.getTriggerState(triggerName, triggerGroup);
    }

    /**
     * @jmx:managed-attribute
     */
    public Set getPausedTriggerGroups()
        throws SchedulerException
    {
        return _sched.getPausedTriggerGroups();
    }

    /**
     * @jmx:managed-attribute
     */
    public Date rescheduleJob(String triggerName, String groupName, 
                              Trigger newTriggerName)
        throws SchedulerException
    {
        return _sched.rescheduleJob(triggerName, groupName,
                                        newTriggerName);
    }

    /**
     * @jmx:managed-attribute
     */
    public void setJobFactory(JobFactory factory)
        throws SchedulerException
    {
        _sched.setJobFactory(factory);
    }

    /**
     * @jmx:managed-attribute
     */
    public boolean isInStandbyMode()
        throws SchedulerException
    {
        return _sched.isInStandbyMode();
    }

    /**
     * @jmx:managed-attribute
     */
    public void standby()
        throws SchedulerException
    {
        _sched.standby();
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister()
            throws Exception {
        log.error("Deregister ");
        if( _sched!=null )
            shutdown();
    }

    public void postDeregister() {
    }

    public JobListener getGlobalJobListener(String arg0)
        throws SchedulerException {
        return _sched.getGlobalJobListener(arg0);
    }

    public TriggerListener getGlobalTriggerListener(String arg0)
        throws SchedulerException {
        return _sched.getGlobalTriggerListener(arg0);
    }

    public boolean isStarted()
        throws SchedulerException {
        return _sched.isStarted();
    }

    public boolean removeGlobalJobListener(String arg0)
        throws SchedulerException {
        return this.removeGlobalJobListener(arg0);
    }

    public boolean removeGlobalTriggerListener(String arg0)
        throws SchedulerException {
        return _sched.removeGlobalTriggerListener(arg0);
    }

    public void startDelayed(int arg0)
        throws SchedulerException {
        _sched.startDelayed(arg0);
    }
}


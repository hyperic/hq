/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.scheduler.server.mbean;

/**
 * MBean interface.
 */
public interface SchedulerServiceMBean extends org.quartz.Scheduler {

   /**
    * Get the properties for Quartz.
    */
  java.util.Properties getQuartzProperties() ;

   /**
    * Set the properties for Quartz and reinitialize the Quartz scheduler factory.
    */
  void setQuartzProperties(java.util.Properties quartzProps) throws org.quartz.SchedulerException;

  java.lang.String getSchedulerName() throws org.quartz.SchedulerException;

  java.lang.String getSchedulerInstanceId() throws org.quartz.SchedulerException;

  org.quartz.SchedulerContext getContext() throws org.quartz.SchedulerException;

  org.quartz.SchedulerMetaData getMetaData() throws org.quartz.SchedulerException;

  void start() throws org.quartz.SchedulerException;

  void startScheduler() throws org.quartz.SchedulerException;

  void pause() throws org.quartz.SchedulerException;

  boolean isPaused() throws org.quartz.SchedulerException;

  void shutdown() throws org.quartz.SchedulerException;

  void shutdown(boolean waitForJobsToComplete) throws org.quartz.SchedulerException;

  boolean isShutdown() throws org.quartz.SchedulerException;

  java.util.List getCurrentlyExecutingJobs() throws org.quartz.SchedulerException;

  java.util.Date scheduleJob(org.quartz.JobDetail jobDetail,org.quartz.Trigger trigger) throws org.quartz.SchedulerException;

  java.util.Date scheduleJob(org.quartz.Trigger trigger) throws org.quartz.SchedulerException;

  void addJob(org.quartz.JobDetail jobDetail,boolean replace) throws org.quartz.SchedulerException;

  boolean deleteJob(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  boolean unscheduleJob(java.lang.String triggerName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void triggerJob(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void triggerJob(java.lang.String jobName,java.lang.String groupName,org.quartz.JobDataMap map) throws org.quartz.SchedulerException;

  void triggerJobWithVolatileTrigger(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void triggerJobWithVolatileTrigger(java.lang.String jobName,java.lang.String groupName,org.quartz.JobDataMap map) throws org.quartz.SchedulerException;

  void pauseTrigger(java.lang.String triggerName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void pauseTriggerGroup(java.lang.String groupName) throws org.quartz.SchedulerException;

  void pauseJob(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void pauseJobGroup(java.lang.String groupName) throws org.quartz.SchedulerException;

  void resumeTrigger(java.lang.String triggerName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void resumeTriggerGroup(java.lang.String groupName) throws org.quartz.SchedulerException;

  void resumeJob(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  void resumeJobGroup(java.lang.String groupName) throws org.quartz.SchedulerException;

  void pauseAll() throws org.quartz.SchedulerException;

  void resumeAll() throws org.quartz.SchedulerException;

  java.lang.String[] getJobGroupNames() throws org.quartz.SchedulerException;

  java.lang.String[] getJobNames(java.lang.String groupName) throws org.quartz.SchedulerException;

  org.quartz.Trigger[] getTriggersOfJob(java.lang.String jobName,java.lang.String groupName) throws org.quartz.SchedulerException;

  java.lang.String[] getTriggerGroupNames() throws org.quartz.SchedulerException;

  java.lang.String[] getTriggerNames(java.lang.String groupName) throws org.quartz.SchedulerException;

  org.quartz.JobDetail getJobDetail(java.lang.String jobName,java.lang.String jobGroup) throws org.quartz.SchedulerException;

  org.quartz.Trigger getTrigger(java.lang.String triggerName,java.lang.String triggerGroup) throws org.quartz.SchedulerException;

  void addCalendar(java.lang.String calName,org.quartz.Calendar calendar,boolean replace,boolean updateTriggers) throws org.quartz.SchedulerException;

  boolean deleteCalendar(java.lang.String calName) throws org.quartz.SchedulerException;

  org.quartz.Calendar getCalendar(java.lang.String calName) throws org.quartz.SchedulerException;

  java.lang.String[] getCalendarNames() throws org.quartz.SchedulerException;

  void addGlobalJobListener(org.quartz.JobListener jobListener) throws org.quartz.SchedulerException;

  void addJobListener(org.quartz.JobListener jobListener) throws org.quartz.SchedulerException;

  boolean removeGlobalJobListener(org.quartz.JobListener jobListener) throws org.quartz.SchedulerException;

  boolean removeJobListener(java.lang.String name) throws org.quartz.SchedulerException;

  java.util.List getGlobalJobListeners() throws org.quartz.SchedulerException;

   /**
    * <p>Calls the equivalent method on the 'proxied'
    */
  java.util.Set getJobListenerNames() throws org.quartz.SchedulerException;

  org.quartz.JobListener getJobListener(java.lang.String name) throws org.quartz.SchedulerException;

  void addGlobalTriggerListener(org.quartz.TriggerListener triggerListener) throws org.quartz.SchedulerException;

  void addTriggerListener(org.quartz.TriggerListener triggerListener) throws org.quartz.SchedulerException;

  boolean removeGlobalTriggerListener(org.quartz.TriggerListener triggerListener) throws org.quartz.SchedulerException;

  boolean removeTriggerListener(java.lang.String name) throws org.quartz.SchedulerException;

  java.util.List getGlobalTriggerListeners() throws org.quartz.SchedulerException;

  java.util.Set getTriggerListenerNames() throws org.quartz.SchedulerException;

  org.quartz.TriggerListener getTriggerListener(java.lang.String name) throws org.quartz.SchedulerException;

  void addSchedulerListener(org.quartz.SchedulerListener schedulerListener) throws org.quartz.SchedulerException;

  boolean removeSchedulerListener(org.quartz.SchedulerListener schedulerListener) throws org.quartz.SchedulerException;

  java.util.List getSchedulerListeners() throws org.quartz.SchedulerException;

  boolean interrupt(java.lang.String jobName,java.lang.String groupName) throws org.quartz.UnableToInterruptJobException;

  int getTriggerState(java.lang.String triggerName,java.lang.String triggerGroup) throws org.quartz.SchedulerException;

  java.util.Set getPausedTriggerGroups() throws org.quartz.SchedulerException;

  java.util.Date rescheduleJob(java.lang.String triggerName,java.lang.String groupName,org.quartz.Trigger newTriggerName) throws org.quartz.SchedulerException;

  void setJobFactory(org.quartz.spi.JobFactory factory) throws org.quartz.SchedulerException;

  boolean isInStandbyMode() throws org.quartz.SchedulerException;

  void standby() throws org.quartz.SchedulerException;

}

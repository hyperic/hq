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

package org.hyperic.hq.events.server.session;

import java.util.Date;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.MaintenanceEventJob;
import org.hyperic.hq.events.shared.MaintenanceEventManagerLocal;
import org.hyperic.hq.events.shared.MaintenanceEventManagerUtil;
import org.hyperic.hq.scheduler.server.session.SchedulerEJBImpl;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * <p> Stores Events to and deletes Events from storage
 *
 * </p>
 * @ejb:bean name="MaintenanceEventManager"
 *      jndi-name="ejb/events/MaintenanceEventManager"
 *      local-jndi-name="LocalMaintenanceEventManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 *
 */
public class MaintenanceEventManagerEJBImpl 
    extends SessionBase
    implements SessionBean
{
	private Log _log = LogFactory.getLog(MaintenanceEventManagerEJBImpl.class);

    private static final String JOB_NAME_PREFIX = MaintenanceEventJob.class.getName();
    private static final String JOB_GROUP  = "MaintenanceEventJobGroup";
    private static final String TRIGGER_GROUP = "MaintenanceEventTriggerGroup";
    private static final Object SCHEDULER_LOCK = new Object();
 
    /**
     * Get the maintenance event for the group
     * 
     * @ejb:interface-method 
     */
    public MaintenanceEvent getMaintenanceEvent(int groupId) throws SchedulerException {
    	MaintenanceEvent event = null;
    	JobDetail jobDetail = SchedulerEJBImpl.getOne().getJobDetail(
        													getJobName(groupId),
        													JOB_GROUP);
        
        if (jobDetail == null) {
        	event = new MaintenanceEvent();
        	event.setGroupId(groupId);
        } else {
        	event = buildMaintenanceEvent(jobDetail);
        }
        
        return event;
    }
        
    /**
     * Unschedule a maintenance event
     * 
     * @ejb:interface-method 
     */
    public void unschedule(MaintenanceEvent event) throws SchedulerException {
    	if (event == null) {
    		throw new IllegalArgumentException();
    	}
    	
    	SchedulerEJBImpl.getOne().deleteJob(
    									getJobName(event.getGroupId()), 
    									JOB_GROUP);	
    }

    /**
     * Schedule or reschedule a maintenance event
     * 
     * @ejb:interface-method 
     */    
    public MaintenanceEvent schedule(MaintenanceEvent event) throws SchedulerException {        
    	if (event == null) {
        	throw new IllegalArgumentException();
        }            		
       
        SchedulerLocal scheduler = SchedulerEJBImpl.getOne();

        synchronized (SCHEDULER_LOCK) {
        	Trigger[] triggers = scheduler.getTriggersOfJob(
            									getJobName(event.getGroupId()), 
            									JOB_GROUP);
            Trigger newTrigger = null;
        	JobDetail jobDetail = null; 
            Date nextFire = null;
            
            if (triggers.length == 0) {
                // schedule new job
            	jobDetail = buildJobDetail(event);
                newTrigger = buildTrigger(event, true);
                nextFire = scheduler.scheduleJob(jobDetail, newTrigger);
                
                _log.info(event + " : scheduling new job to start " + nextFire);              
            } else {
        		Trigger existingTrigger = triggers[0];

        		// update existing job
            	MaintenanceEvent currentEvent = buildMaintenanceEvent(
            										scheduler.getJobDetail(
            														getJobName(event.getGroupId()),
            														JOB_GROUP));
            	
            	if (existingTrigger.getPreviousFireTime() != null) {
            		// if the first trigger has already fired,
            		// then only the end time can be updated
            		currentEvent.setEndTime(event.getEndTime()); 
            		event = currentEvent;
            	}
            	
            	jobDetail = buildJobDetail(event);
            	scheduler.addJob(jobDetail, true);

        		// update existing trigger
        		newTrigger = buildTrigger(
        						event, 
        						(existingTrigger.getPreviousFireTime() == null));
        		
        		newTrigger.setJobName(existingTrigger.getJobName());
        		newTrigger.setJobGroup(existingTrigger.getJobGroup());

        		// reschedule job
        		nextFire = scheduler.rescheduleJob(
        								existingTrigger.getName(), 
        								existingTrigger.getGroup(), 
        								newTrigger);
        		
                _log.info(event + " : rescheduling job to start " + nextFire);                      		
            }
        }
        return event;
    }
     
    /**
     * Create a MaintenanceEvent object from a JobDetail
     * 
     * @ejb:interface-method 
     */    
    public MaintenanceEvent buildMaintenanceEvent(JobDetail jobDetail) {
    	MaintenanceEvent event = new MaintenanceEvent();
        JobDataMap jdMap = jobDetail.getJobDataMap();
        
        event.setGroupId(jdMap.getIntFromString(MaintenanceEventJob.GROUP_ID));
        event.setStartTime(jdMap.getLongValue(MaintenanceEventJob.START_TIME));
        event.setEndTime(jdMap.getLongValue(MaintenanceEventJob.END_TIME));
        event.setModifiedTime(jdMap.getLongValue(MaintenanceEventJob.MODIFIED_TIME));
    	
    	return event;
    }
    
    /*
     * Create a JobDetail object from a MaintenanceEvent
     * 
     */
    private JobDetail buildJobDetail(MaintenanceEvent event) {
    	long currentTime = System.currentTimeMillis();
    	event.setModifiedTime(currentTime);
    	
    	if (event.getEndTime() <= event.getStartTime()) {
        	throw new IllegalArgumentException(event 
        							+ " : End time cannot be before the start time.");
        }

    	JobDetail jobDetail = new JobDetail(
        							getJobName(event.getGroupId()), 
        							JOB_GROUP,
        							MaintenanceEventJob.class);

        JobDataMap jdMap = jobDetail.getJobDataMap();
        
        jdMap.putAsString(MaintenanceEventJob.GROUP_ID, event.getGroupId());
        jdMap.putAsString(MaintenanceEventJob.START_TIME, event.getStartTime());
        jdMap.putAsString(MaintenanceEventJob.END_TIME, event.getEndTime());
        jdMap.putAsString(MaintenanceEventJob.MODIFIED_TIME, event.getModifiedTime());
            	
    	return jobDetail;
    }

    /*
     * Create trigger to deactivate and/or activate alerts
     */
    private Trigger buildTrigger(MaintenanceEvent event, boolean deactivateAndActivate) {
        SimpleTrigger trigger = new SimpleTrigger(
										getTriggerName(event.getGroupId()),
										TRIGGER_GROUP);
        
        if (deactivateAndActivate) {
        	// set an additional trigger to re-activate the alerts
        	trigger.setStartTime(new Date(event.getStartTime()));
        	trigger.setRepeatCount(1);
        	trigger.setRepeatInterval(event.getEndTime() - event.getStartTime());
        } else {
        	trigger.setStartTime(new Date(event.getEndTime()));
        }
    	
		return trigger;
    }
    
    /*
     * Create new job name with the groupId
     */
    private String getJobName(int groupId) {
    	return JOB_NAME_PREFIX + "." + groupId + ".Job";
    }
    
    /*
     * Create new trigger name with the groupId
     */
    private String getTriggerName(int groupId) {
    	return JOB_NAME_PREFIX + "." + groupId + ".Trigger";
    } 

    public static MaintenanceEventManagerLocal getOne() {
        try {
            return MaintenanceEventManagerUtil.getLocalHome().create();
        } catch(Exception e) { 
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}

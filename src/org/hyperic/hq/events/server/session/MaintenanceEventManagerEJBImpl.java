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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.MaintenanceEventJob;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.MaintenanceEventManagerLocal;
import org.hyperic.hq.events.shared.MaintenanceEventManagerUtil;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.scheduler.server.session.SchedulerEJBImpl;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.util.pager.PageControl;
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
    public MaintenanceEvent getMaintenanceEvent(Integer groupId) throws SchedulerException {
    	MaintenanceEvent event = null;
    	JobDetail jobDetail = SchedulerEJBImpl.getOne().getJobDetail(
        													getJobName(groupId),
        													JOB_GROUP);
        
        if (jobDetail == null) {
        	event = new MaintenanceEvent(groupId);
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
    public void unschedule(AuthzSubject subject, MaintenanceEvent event)
    	throws PermissionException, SchedulerException
    {	
    	checkPermission(subject, event);
        
        SchedulerLocal scheduler = SchedulerEJBImpl.getOne();
    	
    	synchronized (SCHEDULER_LOCK) {
    		// Get the latest info from the scheduler
    		event = getMaintenanceEvent(event.getGroupId());
    		Trigger[] triggers = scheduler.getTriggersOfJob(
    											getJobName(event.getGroupId()), 
    											JOB_GROUP);

    		if ((triggers.length > 0) 
    				&& (triggers[0].getNextFireTime().getTime() == event.getEndTime())) {
    			// Maintenance window is already in progress
    			// so reschedule the last trigger to end now
    			event.setEndTime(System.currentTimeMillis());
    			schedule(subject, event);
    		} else {
    			// Otherwise, delete the job
    			scheduler.deleteJob(getJobName(event.getGroupId()), JOB_GROUP);
    		
    			_log.info(event + " : job has been deleted.");        		
    		}
    	}
    }

    /**
     * Schedule or reschedule a maintenance event
     * 
     * @ejb:interface-method 
     */    
    public MaintenanceEvent schedule(AuthzSubject subject, MaintenanceEvent event) 
    	throws PermissionException, SchedulerException 
    {	
    	checkPermission(subject, event);
    	
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
            	MaintenanceEvent existingEvent = buildMaintenanceEvent(
            										scheduler.getJobDetail(
            														getJobName(event.getGroupId()),
            														JOB_GROUP));
            	
        		// if the maintenance event has already started,
        		// then only the end time can be updated
            	if (existingTrigger.getNextFireTime().getTime() == existingEvent.getEndTime()) {
            		existingEvent.setEndTime(event.getEndTime()); 
            		event = existingEvent;
            	}
            	
            	jobDetail = buildJobDetail(event);
            	scheduler.addJob(jobDetail, true);

        		// update existing trigger
        		newTrigger = buildTrigger(
        						event, 
        						(existingTrigger.getNextFireTime().getTime() == existingEvent.getStartTime()));
        		
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
     * Disable or enable alerts for the group and its resources.
     * 
     * @ejb:interface-method 
     */        
    public void manageAlerts(AuthzSubject admin, MaintenanceEvent event, boolean activate) 
		throws PermissionException
	{		
		GalertManagerLocal gam = GalertManagerEJBImpl.getOne();
		AlertDefinitionManagerLocal adm = AlertDefinitionManagerEJBImpl.getOne();
		ResourceGroupManagerLocal rgm = ResourceGroupManagerEJBImpl.getOne();   	    	
		ResourceGroup group = rgm.findResourceGroupById(event.getGroupId());
		
		Collection resources = rgm.getMembers(group);
		Resource resource = null;
    	GalertDef galertDef = null;
		AlertDefinition alertDef = null;
		Collection alertDefinitions = null;
		Iterator adIter = null;
		String status = (activate ? "enabled" : "disabled") + " for " + event;
		
    	// Get the alerts for the group
        alertDefinitions = gam.findAlertDefs(group, PageControl.PAGE_ALL);

        for (adIter = alertDefinitions.iterator(); adIter.hasNext(); ) {
        	galertDef = (GalertDef) adIter.next();	
        	gam.enable(galertDef, activate);
			_log.info("Group Alert [" + galertDef + "] " + status);
        }	    
	    
		// Get the alerts for the resources of the group
		for (Iterator rIter = resources.iterator(); rIter.hasNext(); ) {
			resource = (Resource) rIter.next();
			alertDefinitions = adm.findRelatedAlertDefinitions(admin, resource);
			    		
    		for (adIter = alertDefinitions.iterator(); adIter.hasNext(); ) {
    			alertDef = (AlertDefinition) adIter.next();
    			    			
				// Disable and re-enable only the active alerts
				if (alertDef.isActive()) {
					adm.updateAlertDefinitionInternalEnable(admin, alertDef, activate);
    				log.info("Resource Alert [" + alertDef + "] " + status);
				}
    		}
		}
	}
    
    /**
     * Create a MaintenanceEvent object from a JobDetail
     * 
     * @ejb:interface-method 
     */    
    public MaintenanceEvent buildMaintenanceEvent(JobDetail jobDetail) {
    	MaintenanceEvent event = new MaintenanceEvent();
        JobDataMap jdMap = jobDetail.getJobDataMap();
        
        event.setGroupId(jdMap.getIntegerFromString(MaintenanceEventJob.GROUP_ID));
        event.setStartTime(jdMap.getLongValue(MaintenanceEventJob.START_TIME));
        event.setEndTime(jdMap.getLongValue(MaintenanceEventJob.END_TIME));
        event.setModifiedTime(jdMap.getLongValue(MaintenanceEventJob.MODIFIED_TIME));
    	
    	return event;
    }
    
    /**
     * Create a JobDetail object from a MaintenanceEvent
     */
    private JobDetail buildJobDetail(MaintenanceEvent event) {
    	long currentTime = System.currentTimeMillis();
    	event.setModifiedTime(currentTime);
    	
    	if (event.getEndTime() <= event.getStartTime()) {
        	throw new IllegalArgumentException("End time cannot be before the start time.");
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

    /**
     * Perform permission and group check
     */
    private void checkPermission(AuthzSubject subject, MaintenanceEvent event) 
    	throws PermissionException
    {
    	// Check to see if user has access to the group
        ResourceGroup group = ResourceGroupManagerEJBImpl.getOne()
        							.findResourceGroupById(subject, event.getGroupId());
        
        if (group == null) {
        	throw new IllegalArgumentException("Resource group cannot be found.");
        }
    	
    }
    
    /**
     * Create trigger to deactivate and/or activate alerts
     */
    private Trigger buildTrigger(MaintenanceEvent event, boolean deactivateAndActivate) {
        SimpleTrigger trigger = new SimpleTrigger(
										getTriggerName(event.getGroupId()),
										TRIGGER_GROUP);
        
        if (deactivateAndActivate) {
        	// First trigger is to deactivate the alerts
        	// Second trigger is to re-activate the alerts
        	trigger.setStartTime(new Date(event.getStartTime()));
        	trigger.setRepeatCount(1);
        	trigger.setRepeatInterval(event.getEndTime() - event.getStartTime());
        } else {
        	trigger.setStartTime(new Date(event.getEndTime()));
        }
    	
		return trigger;
    }
    
    /**
     * Create new job name with the groupId
     */
    private String getJobName(Integer groupId) {
    	return JOB_NAME_PREFIX + "." + groupId + ".Job";
    }
    
    /**
     * Create new trigger name with the groupId
     */
    private String getTriggerName(Integer groupId) {
    	return JOB_NAME_PREFIX + "." + groupId + ".Trigger";
    } 

    /**
     * Get local home object
     */
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

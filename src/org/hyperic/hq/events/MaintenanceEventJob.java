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

package org.hyperic.hq.events;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.MaintenanceEventManagerEJBImpl;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.util.pager.PageControl;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;


/**
 * A job to disable and enable alerts for the member resources
 * of a group during a maintenance window.
 */
public class MaintenanceEventJob implements Job {
    protected Log log = LogFactory.getLog(MaintenanceEventJob.class);

    public static final String GROUP_ID = "groupId";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String MODIFIED_TIME = "modifiedTime";
        
    public MaintenanceEventJob() {
    }

    public void execute(JobExecutionContext context)
        throws JobExecutionException
    {
        Trigger trigger = context.getTrigger();
        MaintenanceEvent event = MaintenanceEventManagerEJBImpl.getOne()
        								.buildMaintenanceEvent(context.getJobDetail());
                
        try {
        	// De-activate the alerts if this is the first job trigger
        	// Re-activate the alerts if this is the last job trigger
        	updateAlertDefinitionsStatus(
        			AuthzSubjectManagerEJBImpl.getOne().findSubjectByName("hqadmin"),
        			event.getGroupId(),
        			(trigger.getNextFireTime() == null));
        } catch (PermissionException pe) {
        	throw new JobExecutionException(pe);
        }
    }
    
    private void updateAlertDefinitionsStatus(AuthzSubject admin, int groupId, boolean activate) 
    	throws PermissionException
    {   	
    	log.info((activate ? "ACTIVATING" : "DEACTIVATING") + " ALERTS FOR GROUP " + groupId);
    	
    	GalertManagerLocal gam = GalertManagerEJBImpl.getOne();
    	AlertDefinitionManagerLocal adm = AlertDefinitionManagerEJBImpl.getOne();
    	ResourceGroupManagerLocal rgm = ResourceGroupManagerEJBImpl.getOne();   	    	
    	ResourceGroup group = rgm.findResourceGroupById(Integer.valueOf(groupId));
    	
    	Collection resources = rgm.getMembers(group);
    	Resource resource = null;
    	GalertDef galertDef = null;
    	AlertDefinition alertDef = null;
    	Collection alertDefinitions = null;
    	Iterator adIter = null;
    	
    	// Get the alerts for the group
        alertDefinitions = gam.findAlertDefs(group, PageControl.PAGE_ALL);

        for (adIter = alertDefinitions.iterator(); adIter.hasNext(); ) {
        	galertDef = (GalertDef) adIter.next();
        	
        	log.info("Group Alert ----> " + galertDef);
        	
        	gam.enable(galertDef, activate);
        }
    	    	
    	// Get the alerts for the resources of the group
    	for (Iterator rIter = resources.iterator(); rIter.hasNext(); ) {
    		resource = (Resource) rIter.next();
    		alertDefinitions = adm.findRelatedAlertDefinitions(admin, resource);
    		
    		log.info("Resource Name --> " + resource.getName());
    		
    		for (adIter = alertDefinitions.iterator(); adIter.hasNext(); ) {
    			alertDef = (AlertDefinition) adIter.next();
    			
    			log.info("Resource Alert ---->" + alertDef);  			
    			
    			adm.updateAlertDefinitionInternalEnable(
    						admin,
    						alertDef, 
    						activate);
    		}
    	}    	
    }
    
}

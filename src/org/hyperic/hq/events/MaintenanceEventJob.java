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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.events.shared.MaintenanceEventManagerInterface;
import org.quartz.Job;
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
        MaintenanceEventManagerInterface maintMgr =
            PermissionManagerFactory.getInstance().getMaintenanceEventManager();
        MaintenanceEvent event =
            maintMgr.buildMaintenanceEvent(context.getJobDetail());
                
        try {
        	// Disable the monitors if this is the first job trigger
        	// Enable the monitors if this is the last job trigger
        	maintMgr.manageMonitors(
        				AuthzSubjectManagerEJBImpl.getOne().findSubjectByName("hqadmin"),
        				event,
        				(trigger.getNextFireTime() == null));
        } catch (PermissionException pe) {
        	throw new JobExecutionException(pe);
        }
    }    
}

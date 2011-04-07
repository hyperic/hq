/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.events.shared;

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.quartz.SchedulerException;

public interface MaintenanceEventManager {
    /**
     * @deprecated Get maintenance event by AppdefEntityID instead
     * 
     * Get the maintenance event for the group
     */
    public MaintenanceEvent getMaintenanceEvent(AuthzSubject subject,
    											Integer groupId)
        throws PermissionException, SchedulerException;

    /**
     * Get the maintenance event for the resource
     */
    public MaintenanceEvent getMaintenanceEvent(AuthzSubject subject,
    											AppdefEntityID adeId)
        throws PermissionException, SchedulerException;
    
    /**
     * Get current maintenance events
     */
    public List<MaintenanceEvent> getMaintenanceEvents(AuthzSubject subject, String state)
        throws SchedulerException;

    /**
     * Schedule or reschedule a maintenance event
     */
    public MaintenanceEvent schedule(AuthzSubject subject,
                                     MaintenanceEvent event)
        throws PermissionException, SchedulerException;

    /**
     * Unschedule a maintenance event
     */
    public void unschedule(AuthzSubject subject, MaintenanceEvent event)
        throws PermissionException, SchedulerException;

    /**
     * Check to see if user is authorized to schedule a maintenance event
     */
    public boolean canSchedule(AuthzSubject subject, MaintenanceEvent event);

}

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

package org.hyperic.hq.events.server.session;

import java.util.Collections;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The MaintenanceEventManager provides APIs to manage maintenance events.
 * 
 * 
 */
@Service("MaintenanceEventManager")
@Transactional
public class MaintenanceEventManagerImpl implements MaintenanceEventManager {
    /**
     * @deprecated Get maintenance event by AppdefEntityID instead
     * 
     * Get the maintenance event for the group
     */
    public MaintenanceEvent getMaintenanceEvent(AuthzSubject subject, Integer groupId)
        throws PermissionException, SchedulerException {
        return null;
    }

    /**
     * Get the maintenance event for the resource
     */
    public MaintenanceEvent getMaintenanceEvent(AuthzSubject subject, AppdefEntityID adeId)
        throws PermissionException, SchedulerException {
        return null;
    }

    /**
     * Get current maintenance events
     */
    public List<MaintenanceEvent> getMaintenanceEvents(AuthzSubject subject, String state)
        throws SchedulerException {
        return Collections.EMPTY_LIST;
    }
    
    /**
     * Unschedule a maintenance event
     * 
     * 
     */
    public void unschedule(AuthzSubject subject, MaintenanceEvent event)
        throws PermissionException, SchedulerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Schedule or reschedule a maintenance event
     * 
     * 
     */
    public MaintenanceEvent schedule(AuthzSubject subject, MaintenanceEvent event)
        throws PermissionException, SchedulerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Perform group permission check
     * 
     * 
     */
    public boolean canSchedule(AuthzSubject subject, MaintenanceEvent event) {
        throw new UnsupportedOperationException();
    }
}

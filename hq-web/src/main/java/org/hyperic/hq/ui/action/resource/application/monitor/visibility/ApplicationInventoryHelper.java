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

package org.hyperic.hq.ui.action.resource.application.monitor.visibility;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;

/**
 * A class that provides application-specific implementations of utility methods
 * for common monitoring tasks.
 */
public class ApplicationInventoryHelper
    extends InventoryHelper {

    private AppdefBoss appdefBoss;

    public ApplicationInventoryHelper(AppdefEntityID entityId, AppdefBoss appdefBoss) {
        super(entityId);
        this.appdefBoss = appdefBoss;
    }

    /**
     * Get the set of service types representing an application's services.
     * 
     * @param request the http request
     * @param ctx the servlet context
     * @param resource the application
     */
    public List<ServiceTypeValue> getChildResourceTypes(HttpServletRequest request, ServletContext ctx,
                                                        AppdefResourceValue resource) throws PermissionException,
        AppdefEntityNotFoundException, RemoteException, SessionNotFoundException, SessionException, ServletException {
        AppdefEntityID entityId = resource.getEntityId();
        int sessionId = RequestUtils.getSessionId(request).intValue();

        log.trace("finding services for resource [" + entityId + "]");
        List<AppdefResourceValue> services = appdefBoss.findServiceInventoryByApplication(sessionId, entityId.getId(),
            PageControl.PAGE_ALL);
        return MonitorUtilsNG.findServiceTypes(services, null);
    }

    /**
     * Get a service type from the Bizapp.
     * 
     * @param request the http request
     * @param ctx the servlet context
     * @param id the id of the service type
     */
    public AppdefResourceType getChildResourceType(HttpServletRequest request, ServletContext ctx, AppdefEntityTypeID id)
        throws PermissionException, AppdefEntityNotFoundException, RemoteException, SessionNotFoundException,
        SessionTimeoutException, ServletException {
        int sessionId = RequestUtils.getSessionId(request).intValue();

        log.trace("finding service type [" + id + "]");
        return appdefBoss.findServiceTypeById(sessionId, id.getId());
    }

    /**
     * Get from the Bizapp the numbers of children of the given resource.
     * Returns a <code>Map</code> of counts keyed by child resource type.
     * 
     * @param request the http request
     * @param resource the appdef resource whose children we are counting
     */
    public Map<String, Integer> getChildCounts(HttpServletRequest request, ServletContext ctx,
                                               AppdefResourceValue resource) throws PermissionException,
        AppdefEntityNotFoundException, RemoteException, SessionException, ServletException {

        int sessionId = RequestUtils.getSessionId(request).intValue();

        log.trace("finding service counts for application [" + resource.getEntityId() + "]");
        List<AppdefResourceValue> services = appdefBoss.findServiceInventoryByApplication(sessionId, resource.getId(),
            PageControl.PAGE_ALL);
        return AppdefResourceValue.getServiceTypeCountMap(services);
    }

    /**
     * Return a boolean indicating that the default subtab should not be
     * selected, since the <em>Entry Points</em> subtab will be the default
     * selection.
     */
    public boolean selectDefaultSubtab() {
        return false;
    }

}

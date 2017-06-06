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

package org.hyperic.hq.ui.action.resource.application.monitor;

import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceVisibilityPortalActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This action prepares the portal for viewing the application monitoring pages.
 * The subtabs presented to navigate to for an application may be dynamic. If
 * the application has services assoicated with it differing types that must be
 * rendered as their own subtabs, the subtabs will have to be identify
 * themselves when clicked on.
 */
@Component("applicationMonitorPortalActionNG")
public class VisibilityPortalActionNG
    extends ResourceVisibilityPortalActionNG {

    private final Log log = LogFactory.getLog(VisibilityPortalActionNG.class.getName());
    // XXX duplicated from the ServiceVisibilityPortalAction... refactoring
    // needed
    private static final String ERR_SERVER_PERMISSION = "resource.service.monitor.visibility.error.ServerPermission";
    @Autowired
    private MeasurementBoss measurementBoss;

    
    

    /*
     * (non-Javadoc)
     * 
     * @see org.hyperic.hq.ui.action.BaseDispatchAction#getKeyMethodMap()
     */
    public Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_MON_URL, "urlDetail");
        return map;
    }

    public String currentHealth() throws Exception {

    	request = getServletRequest();
        setResource();
        setMiniTabs(request);
        setServersCurrentHealth(request);

        super.currentHealth();

        Portal portal = Portal.createPortal("resource.application.monitor.visibility.CurrentHealthTitle",
            ".resource.application.monitor.visibility.CurrentHealth");
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorApplicationCurrentHealth";
    }

    /*
     * Dispatch the content to the performance action, if a particular type of
     * service is specified, it will be picked up as a request parameter inside
     * the action
     */
    public String performance() throws Exception {

    	request = getServletRequest();
        super.performance();

        
        setResource();
        setMiniTabs(request);
        Portal portal = Portal.createPortal("resource.application.monitor.visibility.PerformanceTitle",
            ".resource.application.monitor.visibility.Performance");
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "performance";
    }

    /*
     * Dispatch the content to the url detail action.
     */
    public String urlDetail() throws Exception {
        setResource();
        Portal portal = Portal.createPortal("resource.application.monitor.visibility.URLDetailTitle",
            ".resource.application.monitor.visibility.UrlDetail");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
        return "urlDetail";
    }

    protected void setMiniTabs(HttpServletRequest request) throws Exception {
        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            // setResource already set a request error
            return;
        }
    }

    private void setServersCurrentHealth(HttpServletRequest request) throws Exception {
        /* throws Exception throws RemoteException { */
        Exception thrown = null;
        AppdefEntityID entityId = null;
        try {
            AppdefResourceValue app = RequestUtils.getResource(request);
            if (app == null) {
                // setResource already set a request error
                return;
            }
            entityId = RequestUtils.getEntityId(request);
            // get the server current health metrics for the servers that
            // support the application's services -- the list is for the current
            // health of the current servers, period, not the servers correlated
            // against services in a variable timeframe -- so have the
            // MonitorUtil's give us a timeframe for the default retrospective
            // window

            if (log.isTraceEnabled()) {
                log.trace("finding servers current health for resource [" + app.getEntityId() + "]");
            }

            List<ResourceDisplaySummary> servers = measurementBoss.findServersCurrentHealth(RequestUtils.getSessionId(
                request).intValue(), app.getEntityId(), PageControl.PAGE_ALL);

            request.setAttribute(Constants.HOST_HEALTH_SUMMARIES_ATTR, servers);
        } catch (PermissionException e) {
            thrown = e;
            request.setAttribute(Constants.ERR_SERVER_HEALTH_ATTR, ERR_SERVER_PERMISSION);
        } catch (AppdefEntityNotFoundException e) {
            thrown = e;
            addActionError(getText(Constants.ERR_RESOURCE_NOT_FOUND));
        } finally {
            if (thrown != null && log.isDebugEnabled())
                log.debug("resource [" + entityId + "] access error", thrown);
        }
    }
}

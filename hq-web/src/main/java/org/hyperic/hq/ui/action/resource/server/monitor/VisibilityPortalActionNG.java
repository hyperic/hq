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

package org.hyperic.hq.ui.action.resource.server.monitor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceVisibilityPortalActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A <code>BaseDispatchAction</code> that sets up server monitor portals.
 */
@Component("serverMonitorPortalActionNG")
@Scope("prototype")
public class VisibilityPortalActionNG
    extends ResourceVisibilityPortalActionNG {

    private static final String TITLE_CURRENT_HEALTH = "resource.server.monitor.visibility.CurrentHealthTitle";

    private static final String PORTLET_CURRENT_HEALTH = ".resource.server.monitor.visibility.CurrentHealth";

    private static final String TITLE_SERVER_METRICS = "resource.server.monitor.visibility.ServerMetricsTitle";

    private static final String PORTLET_SERVER_METRICS = ".resource.server.monitor.visibility.ServerMetrics";

    private static final String TITLE_PERFORMANCE = "resource.server.monitor.visibility.PerformanceTitle";

    private static final String PORTLET_PERFORMANCE = ".resource.server.monitor.visibility.Performance";

    private static final String ERR_PLATFORM_PERMISSION = "resource.server.monitor.visibility.error.PlatformPermission";

    private final Log log = LogFactory.getLog(VisibilityPortalActionNG.class.getName());

    @Autowired
    private MeasurementBoss measurementBoss;

    
    

    public String currentHealth() throws Exception {

        setResource();
        findHostHealths(getServletRequest());

        super.currentHealth();

        Portal portal = Portal.createPortal(TITLE_CURRENT_HEALTH, PORTLET_CURRENT_HEALTH);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return "monitorServerCurrentHealth";
    }

    public String resourceMetrics() throws Exception {

        setResource();
        findHostHealths(getServletRequest());

        super.resourceMetrics();

        Portal portal = Portal.createPortal(TITLE_SERVER_METRICS, PORTLET_SERVER_METRICS);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorServerReourceMetrics";
    }

    public String performance() throws Exception {

        super.performance();

        setResource();

        Portal portal = Portal.createPortal(TITLE_PERFORMANCE, PORTLET_PERFORMANCE);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorServerPerformance";
    }

    private void findHostHealths(HttpServletRequest request) throws Exception {
        Exception thrown = null;
        AppdefEntityID entityId = null;
        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();
            entityId = RequestUtils.getEntityId(request);

            // no need to page host platform health summaries, as
            // there can only be one
            PageControl pc = PageControl.PAGE_ALL;

            if (log.isDebugEnabled()) {
                log.debug("getting host platform health for resource [" + entityId + "]");
            }
            List<ResourceDisplaySummary> healths = measurementBoss.findPlatformsCurrentHealth(sessionId, entityId, pc);
            request.setAttribute(Constants.HOST_HEALTH_SUMMARIES_ATTR, healths);
        } catch (PermissionException e) {
            thrown = e;
            request.setAttribute(Constants.ERR_PLATFORM_HEALTH_ATTR, ERR_PLATFORM_PERMISSION);
        } catch (AppdefEntityNotFoundException e) {
            thrown = e;
            addActionError(Constants.ERR_RESOURCE_NOT_FOUND);
        } finally {
            if (thrown != null && log.isDebugEnabled()) {
                log.debug("resource [" + entityId + "] access error", thrown);
            }
        }
    }
}

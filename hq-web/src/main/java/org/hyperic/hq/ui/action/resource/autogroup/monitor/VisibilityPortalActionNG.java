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

package org.hyperic.hq.ui.action.resource.autogroup.monitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceVisibilityPortalActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A <code>BaseDispatchAction</code> that sets up autogroup monitor portals.
 */
@Component("autogroupMonitorPortalActionNG")
@Scope("prototype")
public class VisibilityPortalActionNG
    extends ResourceVisibilityPortalActionNG {

    private static final String TITLE_CURRENT_HEALTH = "resource.autogroup.monitor.visibility.CurrentHealthTitle";

    private static final String PORTLET_CURRENT_HEALTH = ".resource.autogroup.monitor.visibility.CurrentHealth";

    private static final String TITLE_AUTOGROUP_METRICS = "resource.autogroup.monitor.visibility.AutoGroupMetricsTitle";

    private static final String PORTLET_AUTOGROUP_METRICS = ".resource.autogroup.monitor.visibility.AutoGroupMetrics";

    public static final String PORTLET_PERFORMANCE = ".resource.autogroup.monitor.visibility.Performance";

    public static final String TITLE_PERFORMANCE = "resource.autogroup.monitor.visibility.PerformanceTitle";

    private final Log log = LogFactory.getLog(VisibilityPortalActionNG.class.getName());

    @Autowired
    private MeasurementBoss measurementBoss;

    
    
    public String currentHealth() throws Exception {

        // XXX: what if we have multiple parents?
        setResource();
        findServersHealths(getServletRequest());

        super.currentHealth();

        Portal portal = Portal.createPortal(TITLE_CURRENT_HEALTH, PORTLET_CURRENT_HEALTH);
        portal.setWorkflowParams(getWorkflowParams(getServletRequest()));
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorAutogroupCurrentHealth";
    }

    public String resourceMetrics() throws Exception {

        // XXX: what if we have multiple parents?
        setResource();
        findServersHealths(getServletRequest());

        super.resourceMetrics();

        Portal portal = Portal.createPortal(TITLE_AUTOGROUP_METRICS, PORTLET_AUTOGROUP_METRICS);
        portal.setWorkflowParams(getWorkflowParams(getServletRequest()));
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorAutogroupReourceMetrics";
    }

    public String performance() throws Exception {
        setResource();

        super.performance();

        Portal portal = Portal.createPortal(TITLE_PERFORMANCE, PORTLET_PERFORMANCE);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return "monitorAutogroupPerformance";
    }

    private void findServersHealths(HttpServletRequest request) throws Exception {

        AppdefEntityID entityId = null;
        Exception thrown = null;

        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();
            entityId = RequestUtils.getEntityId(request);

            // get resource health
            List<ResourceDisplaySummary> healths = measurementBoss.findResourcesCurrentHealth(sessionId,
                new AppdefEntityID[] { entityId });

            request.setAttribute(Constants.HOST_HEALTH_SUMMARIES_ATTR, healths);
        } catch (AppdefEntityNotFoundException e) {
            thrown = e;
            addActionError( Constants.ERR_RESOURCE_NOT_FOUND);
        } catch (PermissionException e) {
            thrown = e;
            request.setAttribute(Constants.ERR_SERVER_HEALTH_ATTR,
                "resource.service.monitor.visibility.error.ServerPermission");
        } finally {
            if (thrown != null && log.isDebugEnabled())
                log.debug("resource [" + entityId + "] access error", thrown);
        }
    }

    protected AppdefEntityID getAppdefEntityID(HttpServletRequest request) throws ParameterNotFoundException {

        // when we've fully migrated CAM to eid, this method can go away

        try {
            AppdefEntityID[] eids = RequestUtils.getEntityIds(request);

            // take this opportunity to cache the entity ids in the request
            request.setAttribute(Constants.ENTITY_IDS_ATTR, BizappUtilsNG.stringifyEntityIds(eids));

            return eids[0];
        } catch (ParameterNotFoundException e) {
            log.trace("No entity ids -- must be auto-group of platforms.");
            return null;
        }
    }

    protected Map<String, Object> getWorkflowParams(HttpServletRequest request) throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();

        try {
            // will not be found if auto-group of platforms -- still okay
            AppdefEntityID[] eids = RequestUtils.getEntityIds(request);
            params.put(Constants.ENTITY_ID_PARAM, BizappUtilsNG.stringifyEntityIds(eids));

            // will not be found if not auto-group -- still okay
            AppdefEntityTypeID ctype = RequestUtils.getChildResourceTypeId(request);
            params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
        } catch (ParameterNotFoundException e) {
            log.trace("param not found: " + e.getMessage());
            // keep on movin'
        }

        String mode = request.getParameter(Constants.MODE_PARAM);
        params.put(Constants.MODE_PARAM, mode);

        return params;
    }
}

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

package org.hyperic.hq.ui.action.resource.group.monitor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceVisibilityPortalAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.ListPageFetcher;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>BaseDispatchAction</code> that sets up compatible group monitor
 * portals.
 */
public class VisibilityPortalAction
    extends ResourceVisibilityPortalAction {

    private static final String TITLE_CURRENT_HEALTH = "resource.group.monitor.visibility.CurrentHealthTitle";

    private static final String PORTLET_CURRENT_HEALTH = ".resource.group.monitor.visibility.CurrentHealth";

    private static final String TITLE_GROUP_METRICS = "resource.group.monitor.visibility.GroupMetricsTitle";

    private static final String PORTLET_GROUP_METRICS = ".resource.group.monitor.visibility.GroupMetrics";

    private static final String TITLE_PERFORMANCE = "resource.group.monitor.visibility.PerformanceTitle";

    private static final String PORTLET_PERFORMANCE = ".resource.group.monitor.visibility.Performance";

    private final Log log = LogFactory.getLog(VisibilityPortalAction.class.getName());

    private static final String HOST_TYPE = "hostType";

    private MeasurementBoss measurementBoss;

    @Autowired
    public VisibilityPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss,
                                  MeasurementBoss measurementBoss) {
        super(appdefBoss, authzBoss, controlBoss);
        this.measurementBoss = measurementBoss;
    }

    public ActionForward currentHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        setResource(request, response);
        List<ResourceDisplaySummary> healths = findResourceHealths(request);

        if (healths != null) {
            request.setAttribute(Constants.GROUP_MEMBER_HEALTH_SUMMARIES_ATTR, healths);
            request.setAttribute(Constants.CTX_SUMMARIES, healths);
        }

        super.currentHealth(mapping, form, request, response);

        request.setAttribute(Constants.DEPL_CHILD_MODE_ATTR, "resourceMetrics");
        Portal portal = Portal.createPortal(TITLE_CURRENT_HEALTH, PORTLET_CURRENT_HEALTH);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward resourceMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        setResource(request, response);
        List<ResourceDisplaySummary> healths = findResourceHealths(request);

        if (healths != null) {
            request.setAttribute(Constants.GROUP_MEMBER_HEALTH_SUMMARIES_ATTR, healths);
            request.setAttribute(Constants.CTX_SUMMARIES, healths);

            PageControl pc = RequestUtils.getPageControl(request);
            // Let's not try to resort
            pc.setSortorder(PageControl.SORT_UNSORTED);
            ListPageFetcher lpf = new ListPageFetcher(healths);
            request.setAttribute("pagedMembers", lpf.getPage(pc));
        }

        super.resourceMetrics(mapping, form, request, response);

        Portal portal = Portal.createPortal(TITLE_GROUP_METRICS, PORTLET_GROUP_METRICS);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward performance(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        setResource(request, response);

        super.performance(mapping, form, request, response);

        boolean autogroup = RequestUtils.parameterExists(request, Constants.CHILD_RESOURCE_TYPE_ID_PARAM);

        Portal portal = Portal.createPortal(
            autogroup ? org.hyperic.hq.ui.action.resource.autogroup.monitor.VisibilityPortalAction.TITLE_PERFORMANCE
                     : TITLE_PERFORMANCE,
            autogroup ? org.hyperic.hq.ui.action.resource.autogroup.monitor.VisibilityPortalAction.PORTLET_PERFORMANCE
                     : PORTLET_PERFORMANCE);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    private List<ResourceDisplaySummary> findResourceHealths(HttpServletRequest request) throws Exception {
        AppdefEntityID entityId = null;

        int sessionId = RequestUtils.getSessionId(request).intValue();
        entityId = RequestUtils.getEntityId(request);

        // for a "clustered services" there'd be many... does
        // that get handled here or the group monitoring?
        AppdefGroupValue group = (AppdefGroupValue) RequestUtils.getResource(request);
        if (group == null) {
            // setResource already set a request error
            return null;
        }

        StopWatch watch = new StopWatch();
        watch.markTimeBegin("findGroupCurrentHealth");
        List<ResourceDisplaySummary> healths = measurementBoss.findGroupCurrentHealth(sessionId, entityId.getId());
        watch.markTimeEnd("findGroupCurrentHealth");

        if (log.isDebugEnabled()) {
            log.debug("got " + healths.size() + " ResourceTypeDisplays getting group member's health");
            log.debug("findResourceHealths: " + watch);
        }

        setHostsCurrentHealth(request, group);
        return healths;
    }

    private void setHostsCurrentHealth(HttpServletRequest request, AppdefGroupValue group) throws Exception {
        AppdefEntityID entityId = null;

        try {
            entityId = group.getEntityId();

            // get the server current health metrics for the servers that
            // support the application's services -- the list is for the current
            // health of the current servers, period, not the servers correlated
            // against services in a variable timeframe -- so have the
            // MonitorUtil's give us a timeframe for the default retrospective
            // window

            if (log.isTraceEnabled()) {
                log.trace("finding servers current health for resource [" + entityId + "]");
            }

            int sessionId = RequestUtils.getSessionId(request).intValue();

            List<ResourceDisplaySummary> hosts = measurementBoss.findHostsCurrentHealth(sessionId, group.getEntityId(),
                PageControl.PAGE_ALL);
            if (hosts.size() > 0) {
                ResourceDisplaySummary rds = (ResourceDisplaySummary) hosts.get(0);

                switch (rds.getEntityId().getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                        request.setAttribute(HOST_TYPE, "Platform");
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        request.setAttribute(HOST_TYPE, "Server");
                        break;
                }
                request.setAttribute(Constants.HOST_HEALTH_SUMMARIES_ATTR, hosts);
            }
        } catch (PermissionException e) {
            if (log.isDebugEnabled()) {
                log.debug("resource [" + entityId + "] access error", e);
            }

        } catch (AppdefEntityNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("resource [" + entityId + "] access error", e);
            }
        }
    }

}

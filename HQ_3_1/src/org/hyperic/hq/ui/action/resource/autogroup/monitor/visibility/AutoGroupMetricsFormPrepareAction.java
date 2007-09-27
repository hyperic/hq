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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.visibility;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.MetricsDisplayForm;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.MetricsDisplayFormPrepareAction;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * A <code>MetricsDisplayFormPrepareAction</code> that retrieves data
 * from the Bizapp to be displayed on an <code>AutoGroup
 * Metrics</code> page.
 */
public class AutoGroupMetricsFormPrepareAction
    extends MetricsDisplayFormPrepareAction {

    protected static Log log = LogFactory
            .getLog(AutoGroupMetricsFormPrepareAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display an autogroup's metrics page
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        MetricsDisplayForm displayForm = (MetricsDisplayForm) form;
        displayForm.setShowNumberCollecting(Boolean.TRUE);

        Long begin = null;
        Long end = null;

        // There are two possibilities for an auto-group.  Either it
        // is an auto-group of platforms, in which case there will be
        // no parent entity ids, or it is an auto-group of servers or
        // services.
        InventoryHelper helper = null;
        AppdefEntityID[] entityIds = null;
        AppdefEntityID typeHolder = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            // if we get this far, we are dealing with an auto-group
            // of servers or services

            // find the resource type of the autogrouped resources
            typeHolder = entityIds[0];
            helper = InventoryHelper.getHelper(typeHolder);
        } catch (ParameterNotFoundException e) {
            // if we get here, we are dealing with an auto-group of
            // platforms
            helper = new RootInventoryHelper();
        }

        ServletContext ctx = getServlet().getServletContext();

        // find the resource type of the autogrouped resources

        AppdefEntityTypeID childTypeID;
        try {
            childTypeID = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e1) {
            // must be an autogroup resource
            // childTypeID = RequestUtils.getAutogroupResourceTypeId(request);
            // REMOVE ME?
            throw e1;
        }
        
        AppdefResourceTypeValue selectedType =
            helper.getChildResourceType(request, ctx, childTypeID);
        request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR,
                             selectedType);

        // get the "metric range" user pref
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Map range = user.getMetricRangePreference();
        begin = (Long) range.get(MonitorUtils.BEGIN);
        end = (Long) range.get(MonitorUtils.END);

        // prepare form
        // XXX: needs to go away when the rest of monitoring supports eids
        if (null != typeHolder) {
            displayForm.setRid( typeHolder.getId() );
            displayForm.setType( new Integer( typeHolder.getType() ) );
        }
        displayForm.setCtype(selectedType.getAppdefTypeKey());

        return super.execute(mapping, form, request, response, begin, end);
    }

    // ---------------------------------------------------- Protected Methods

    protected Boolean getShowNumberCollecting() {
        return Boolean.TRUE;
    }

    /**
     * Get from the Bizapp the set of metric summaries for the
     * specified entities that will be displayed on the page. Returns a
     * <code>Map</code> keyed by metric category.
     *
     * @param request the http request
     * @param entityId the entity id of the currently viewed resource
     * @param begin the time (in milliseconds since the epoch) that
     *  begins the timeframe for which the metrics are summarized
     * @param end the time (in milliseconds since the epoch) that
     *  ends the timeframe for which the metrics are summarized
     * @return Map keyed on the category (String), values are List's of 
     * MetricDisplaySummary beans
     */
    protected Map getMetrics(HttpServletRequest request,
                             AppdefEntityID entityId,
                             long filters, String keyword,
                             Long begin, Long end, boolean showAll)
        throws Exception {
        // XXX: this can go away when we finish the conversion to eids
        return null;
    }

    /**
     * Get from the Bizapp the set of metric summaries for the
     * specified entities that will be displayed on the page. Returns a
     * <code>Map</code> keyed by metric category.
     *
     * @param request the http request
     * @param entityId the entity id of the currently viewed resource
     * @param begin the time (in milliseconds since the epoch) that
     *  begins the timeframe for which the metrics are summarized
     * @param end the time (in milliseconds since the epoch) that
     *  ends the timeframe for which the metrics are summarized
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @throws RemoteException
     * @throws InvalidAppdefTypeException
     * @throws AppdefCompatException
     */
    protected Map getMetrics(HttpServletRequest request,
                             AppdefEntityID[] entityIds,
                             long filters, String keyword,
                             Long begin, Long end, boolean showAll)
        throws ServletException, SessionTimeoutException,
               SessionNotFoundException, AppdefEntityNotFoundException,
               PermissionException, AppdefCompatException,
               InvalidAppdefTypeException, RemoteException
    {
        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);
        AppdefResourceTypeValue childType = (AppdefResourceTypeValue)
            request.getAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR);
        Integer selectedId = childType.getId();

        AppdefEntityTypeID atid = new AppdefEntityTypeID(
                childType.getAppdefTypeKey());

        if (null == entityIds) {
            // auto-group of platforms
            log.trace("finding metric summaries for autogrouped platforms " +
                      "of type " + selectedId + " for range " + begin +
                      ":" + end);
            return boss.findAGPlatformMetricsByType(sessionId, atid,
                                                    begin.longValue(),
                                                    end.longValue(),
                                                    showAll);
        } else {
            if (log.isTraceEnabled())
                log.trace("finding metric summaries for autogrouped servers " +
                        "or services of type " + childType.getAppdefTypeKey() +
                        " for resources " + Arrays.asList(entityIds) +
                        " for range " + begin + ":" + end);
            return boss.findAGMetricsByType(sessionId, entityIds, atid,
                                            filters, keyword,
                                            begin.longValue(), end.longValue(),
                                            showAll);
        }
    }
}

// EOF

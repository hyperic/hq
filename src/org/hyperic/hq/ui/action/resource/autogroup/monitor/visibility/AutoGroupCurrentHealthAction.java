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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.CurrentHealthAction;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.IndicatorViewsForm;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.pager.PageControl;

/**
 * A <code>TilesAction</code> that retrieves data from the Bizapp to be
 * displayed on an <code>AutoGroup Current Health</code> page.  Ths is the
 * only resource type that needs its own CurrentHealthAction due to the
 * different APIs it calls.
 */
public class AutoGroupCurrentHealthAction extends CurrentHealthAction {

    private static Log log =
        LogFactory.getLog(AutoGroupCurrentHealthAction.class.getName());

    private PageControl pc = new PageControl(0, Constants.DEFAULT_CHART_POINTS);

    /**
     * Retrieve data needed to display an autogroup's current health
     * summary.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);

        // There are two possibilities for an auto-group.  Either it
        // is an auto-group of platforms, in which case there will be
        // no parent entity ids, or it is an auto-group of servers or
        // services.
        InventoryHelper helper = null;
        AppdefEntityID[] entityIds = null;
        AppdefEntityID typeHolder = null;
        String parentKey;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            // if we get this far, we are dealing with an auto-group
            // of servers or services

            // find the resource type of the autogrouped resources
            typeHolder = entityIds[0];
            helper = InventoryHelper.getHelper(typeHolder);
            
            parentKey = typeHolder.getAppdefKey();
        } catch (ParameterNotFoundException e) {
            // if we get here, we are dealing with an auto-group of
            // platforms
            helper = new RootInventoryHelper();
            parentKey = "autogroup";
        }

        AppdefEntityTypeID childTypeId;
        try {
            childTypeId = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e1) {
            // must be an autogroup resource type
            // childTypeId = RequestUtils.getAutogroupResourceTypeId(request);
            // REMOVE ME?
            throw e1;
        }
        
        AppdefResourceTypeValue selectedType =
            helper.getChildResourceType(request, ctx, childTypeId);
        request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR, selectedType);
        
        // Set the views
        setupViews(request, (IndicatorViewsForm) form,
                   parentKey + "." + childTypeId.getAppdefKey());

        // Get the resource availability
        MeasurementBoss boss =
            ContextUtils.getMeasurementBoss(getServlet().getServletContext());
        WebUser user = SessionUtils.getWebUser(request.getSession());

        try {
            MeasurementTemplate mt =
                boss.getAvailabilityMetricTemplate(sessionId.intValue(),
                                                   entityIds[0], childTypeId);
            
            Map pref = user.getMetricRangePreference(true);
            long begin = ((Long) pref.get(MonitorUtils.BEGIN)).longValue();
            long end = ((Long) pref.get(MonitorUtils.END)).longValue();
            long interval = TimeUtil.getInterval(begin, end,
                    Constants.DEFAULT_CHART_POINTS);

            List data =
                boss.findAGMeasurementData(sessionId.intValue(), entityIds,
                                           mt, childTypeId, begin, end,
                                           interval, true, pc);
            
            // Seems like sometimes Postgres does not average cleanly, and
            // the value ends up being like 0.9999999999.  We don't want the
            // insignificant amount to mess up our display.
            for (Iterator it = data.iterator(); it.hasNext(); ) {
                MetricValue val = (MetricValue) it.next();
                if (val.toString().equals("1"))
                    val.setValue(1);
            }

            request.setAttribute(Constants.CAT_AVAILABILITY_METRICS_ATTR, data);
            request.setAttribute(Constants.AVAIL_METRICS_ATTR,
                                 getFormattedAvailability(data));
        } catch (MeasurementNotFoundException e) {
            // No utilization metric
            log.debug(MeasurementConstants.CAT_AVAILABILITY +
                      " not found for autogroup" + childTypeId);
        }
        
        return null;
    }
}

// EOF

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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * A <code>BaseAction</code> that handles metrics display form
 * submissions.
 */
public class MetricsDisplayAction extends MetricsControlAction {

    protected static Log log =  LogFactory
        .getLog(MetricsDisplayAction.class.getName());

    // ---------------------------------------------------- Public
    // ---------------------------------------------------- Methods

    /**
     * Modify the metrics summary display as specified in the given
     * <code>MetricsDisplayForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        MetricsDisplayForm displayForm = (MetricsDisplayForm) form;

        AppdefEntityID entityId = displayForm.getEntityId();
        Map forwardParams = displayForm.getForwardParams();

        WebUser user = SessionUtils.getWebUser(request.getSession());
        Integer sessionId = user.getSessionId();
        ServletContext ctx = getServlet().getServletContext();

        if (displayForm.isCompareClicked()) {
            return returnCompare(request, mapping, forwardParams);
        }
        else if (displayForm.isChartClicked()) {
            forwardParams.put( Constants.METRIC_PARAM, displayForm.getM() );
            return returnChart(request, mapping, forwardParams);
        }
        else if (displayForm.isThresholdClicked()) {
            Integer threshold = displayForm.getT();
            user.setPreference(WebUser.PREF_METRIC_THRESHOLD,
                               threshold);
            log.trace("saving threshold pref [" + threshold + "]");
            LogFactory.getLog("user.preferences").trace("Invoking setUserPrefs"+
                " in MetricsDisplayAction " +
                " for " + user.getId() + " at "+System.currentTimeMillis() +
                " user.prefs = " + user.getPreferences());
            AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
            boss.setUserPrefs(sessionId, user.getId(), user.getPreferences());
            
            return returnSuccess(request, mapping);
        }
        else if (displayForm.isOkClicked()) {
            Integer[] m = displayForm.getM();
            long interval = displayForm.getIntervalTime();

            // Don't make any back-end call if user has not selected any metrics
            if (m != null && m.length > 0) {
                MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
                if (displayForm.getCtype() == null || entityId.getType() ==
                        AppdefEntityConstants.APPDEF_TYPE_GROUP)
                    mBoss.updateMeasurements(sessionId.intValue(), entityId, m,
                                             interval);
                else {
                    AppdefEntityTypeID ctid =
                        new AppdefEntityTypeID(displayForm.getCtype());
                
                    mBoss.updateAGMeasurements(sessionId.intValue(), entityId,
                                               ctid, m, interval);
                }

            }
            RequestUtils.setConfirmation(request,
                "resource.common.monitor.visibility.config.ConfigMetrics." +
                "Confirmation");
            
            return returnSuccess(request, mapping);
        }
        else if (displayForm.isRemoveClicked()) {
            Integer[] m = displayForm.getM();
            // Don't make any back-end call if user has not selected any metrics
            if (m != null && m.length > 0) {
                MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);

                if (displayForm.getCtype() == null || entityId.getType() ==
                        AppdefEntityConstants.APPDEF_TYPE_GROUP)
                    mBoss.disableMeasurements(sessionId.intValue(), entityId,m);
                else {
                    AppdefEntityTypeID ctid =
                        new AppdefEntityTypeID(displayForm.getCtype());
                    mBoss.disableAGMeasurements(sessionId.intValue(), entityId,
                                                ctid, m);
                }

                RequestUtils.setConfirmation(request, 
                    "resource.common.monitor.visibility.config.RemoveMetrics." +
                    "Confirmation");
            }
            
            return returnSuccess(request, mapping);
        }

        return super.execute(mapping, form, request, response);
    }

    // ---------------------------------------------------- Private Methods

    private ActionForward returnCompare(HttpServletRequest request,
                                        ActionMapping mapping,
                                        Map params)
    throws Exception {
        // set return path
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return constructForward(request, mapping, Constants.COMPARE_URL,
                                params, NO_RETURN_PATH);
    }

    private ActionForward returnChart(HttpServletRequest request,
                                      ActionMapping mapping,
                                      Map params)
    throws Exception {
        // set return path
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return constructForward(request, mapping, Constants.CHART_URL,
                                params, NO_RETURN_PATH);
    }
}

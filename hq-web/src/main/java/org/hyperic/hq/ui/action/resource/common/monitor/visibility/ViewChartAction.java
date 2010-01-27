/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * View a chart for a metric.
 */
public class ViewChartAction
    extends MetricDisplayRangeAction {
    protected final Log log = LogFactory.getLog(ViewChartAction.class.getName());

    private DashboardManager dashboardManager;
    
    @Autowired
    public ViewChartAction(AuthzBoss authzBoss, DashboardManager dashboardManager) {
        super(authzBoss);
        this.dashboardManager = dashboardManager;
    }

    /**
     * Modify the metric chart as specified in the given <code>@{link
     * ViewActionForm}</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        ViewChartForm chartForm = (ViewChartForm) form;
        AppdefEntityID adeId = new AppdefEntityID(chartForm.getType().intValue(), chartForm.getRid());
        Map<String, Object> forwardParams = new HashMap<String, Object>(3);

        forwardParams.put(Constants.RESOURCE_PARAM, chartForm.getRid());
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, chartForm.getType());

        // The autogroup metrics pages pass the ctype to us, and we
        // need to pass it back. If this happens, we don't need the
        // extra "mode" parameter. See bug #7501. (2003/06/24 -- JW)
        if (null != chartForm.getCtype() && !chartForm.getCtype().equals(ViewChartForm.NO_CHILD_TYPE)) {
            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, chartForm.getCtype());
        } else {
            forwardParams.put(Constants.MODE_PARAM, chartForm.getMode());
        }

        if (chartForm.getSaveChart()) {
            // isEE == false, bc this is the .org version of this action
            return saveChartToDashboard(mapping, request, forwardParams, chartForm, adeId, false);
        } else if (chartForm.isPrevPageClicked()) {
            return returnSuccess(request, mapping, forwardParams);
        } else {
            // If prev or next buttons were clicked, the dates
            // caused by those clicks will override what's
            // actually in the form, so we must update the form as
            // appropriate.
            if (chartForm.isNextRangeClicked() || chartForm.isPrevRangeClicked()) {
                MetricRange range = new MetricRange();
                if (chartForm.isNextRangeClicked()) {
                    long newBegin = chartForm.getEndDate().getTime();
                    long diff = newBegin - chartForm.getStartDate().getTime();
                    long newEnd = newBegin + diff;

                    range.setBegin(new Long(newBegin));
                    range.setEnd(new Long(newEnd));
                } else if (chartForm.isPrevRangeClicked()) {
                    long newEnd = chartForm.getStartDate().getTime();
                    long diff = chartForm.getEndDate().getTime() - newEnd;
                    long newBegin = newEnd - diff;

                    range.setBegin(new Long(newBegin));
                    range.setEnd(new Long(newEnd));
                }
                chartForm.setA(MetricDisplayRangeForm.ACTION_DATE_RANGE);
                chartForm.populateStartDate(new Date(range.getBegin().longValue()), request.getLocale());
                chartForm.populateEndDate(new Date(range.getEnd().longValue()), request.getLocale());
                range.shiftNow();
                request.setAttribute(Constants.METRIC_RANGE, range);
            }

            // update metric display range
            ActionForward retVal = super.execute(mapping, form, request, response);
            if (retVal.getName().equals(Constants.SUCCESS_URL)) {
                return returnRedraw(request, mapping, forwardParams);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("returning " + retVal.getName());
                }
                return retVal;
            }
        }

    }

    protected ActionForward returnRedraw(HttpServletRequest request, ActionMapping mapping, Map<String, Object> params)
        throws Exception {
        return constructForward(request, mapping, Constants.REDRAW_URL, params, false);
    }

    protected ActionForward saveChartToDashboard(ActionMapping mapping, HttpServletRequest request,
                                                 Map<String, Object> forwardParams, ViewChartForm chartForm,
                                                 AppdefEntityID adeId, boolean isEE) throws Exception {
        ActionForward success = returnRedraw(request, mapping, forwardParams);

        ResultCode result = SaveChartToDashboardUtil.saveChartToDashboard(getServlet().getServletContext(), request,
            success, chartForm, adeId, chartForm.getChartName(), isEE, dashboardManager);

        switch (result) {
            case DUPLICATE:
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.error.ChartDuplicated");
                break;

            case ERROR:
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.error.ChartNotSaved");
                break;

            case SUCCESS:
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.ChartSaved");
        }

        return success;
    }
}

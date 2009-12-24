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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Forward to chart page for a designated metric.
 */
public class ViewDesignatedChartAction
    extends MetricDisplayRangeAction {

    private MeasurementBoss measurementBoss;

    @Autowired
    public ViewDesignatedChartAction(AuthzBoss authzBoss, MeasurementBoss measurementBoss) {
        super(authzBoss);
        this.measurementBoss = measurementBoss;
    }

    /**
     * Modify the metric chart as specified in the given <code>@{link
     * ViewActionForm}</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(4);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        int sessionId = RequestUtils.getSessionId(request).intValue();
        MeasurementTemplate mt;
        try {
            AppdefEntityTypeID ctype = RequestUtils.getChildResourceTypeId(request);

            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype.getAppdefKey());
            forwardParams.put(Constants.MODE_PARAM, Constants.MODE_MON_CHART_SMMR);

            // Now we have to look up the designated metric template ID
            mt = measurementBoss.getAvailabilityMetricTemplate(sessionId, aeid, ctype);
        } catch (ParameterNotFoundException e) {
            forwardParams.put(Constants.MODE_PARAM, aeid.isGroup() ? Constants.MODE_MON_CHART_SMMR
                                                                  : Constants.MODE_MON_CHART_SMSR);
            // Now we have to look up the designated metric template ID
            mt = measurementBoss.getAvailabilityMetricTemplate(sessionId, aeid);
        }

        forwardParams.put(Constants.METRIC_PARAM, mt.getId());

        return constructForward(request, mapping, Constants.REDRAW_URL, forwardParams, false);
    }
}

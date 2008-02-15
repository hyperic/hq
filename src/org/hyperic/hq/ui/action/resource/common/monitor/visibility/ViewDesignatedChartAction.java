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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Forward to chart page for a designated metric.
 */
public class ViewDesignatedChartAction extends MetricDisplayRangeAction {
    protected static Log log =
        LogFactory.getLog( ViewDesignatedChartAction.class.getName() );

    /**
     * Modify the metric chart as specified in the given <code>@{link
     * ViewActionForm}</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
    
        HashMap forwardParams = new HashMap(4);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        
        MeasurementBoss boss =
            ContextUtils.getMeasurementBoss(getServlet().getServletContext());
        int sessionId  = RequestUtils.getSessionId(request).intValue();
        MeasurementTemplateValue mtv;
        try {
            AppdefEntityTypeID ctype =
                RequestUtils.getChildResourceTypeId(request);
            
            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM,
                              ctype.getAppdefKey());
            forwardParams.put(Constants.MODE_PARAM,
                              Constants.MODE_MON_CHART_SMMR);

            // Now we have to look up the designated metric template ID
            mtv = boss.getAvailabilityMetricTemplate(sessionId, aeid, ctype);
        } catch (ParameterNotFoundException e) {
            forwardParams.put(Constants.MODE_PARAM,
                              aeid.isGroup() ? Constants.MODE_MON_CHART_SMMR :
                                               Constants.MODE_MON_CHART_SMSR);
            // Now we have to look up the designated metric template ID
            mtv = boss.getAvailabilityMetricTemplate(sessionId, aeid);
        }
        
        forwardParams.put(Constants.METRIC_PARAM, mtv.getId());
        
        return constructForward(request, mapping, Constants.REDRAW_URL, 
                                forwardParams, false);
    }
}

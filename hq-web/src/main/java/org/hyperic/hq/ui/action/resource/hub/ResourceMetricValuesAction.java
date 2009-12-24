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

package org.hyperic.hq.ui.action.resource.hub;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.units.FormattedNumber;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Fetch the designated metrics for a resource
 */
public class ResourceMetricValuesAction
    extends TilesAction {

    private MeasurementBoss measurementBoss;

    @Autowired
    public ResourceMetricValuesAction(MeasurementBoss measurementBoss) {
        super();
        this.measurementBoss = measurementBoss;
    }

    @SuppressWarnings("unchecked")
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        AppdefEntityID entityId = (AppdefEntityID) context.getAttribute(Constants.ENTITY_ID_PARAM);

        List<MeasurementTemplate> templates = (List<MeasurementTemplate>) context.getAttribute("Indicators");

        int sessionId = RequestUtils.getSessionId(request);

        Map<Integer, MetricValue> vals = measurementBoss.getLastIndicatorValues(sessionId, entityId);

        // Format the values
        String[] metrics = new String[templates.size()];
        if (vals.size() == 0) {
            Arrays.fill(metrics, RequestUtils.message(request, "common.value.notavail"));
        } else {
            int i = 0;
            for (Iterator<MeasurementTemplate> it = templates.iterator(); it.hasNext(); i++) {
                MeasurementTemplate mt = (MeasurementTemplate) it.next();
                if (vals.containsKey(mt.getId())) {
                    MetricValue mv = (MetricValue) vals.get(mt.getId());
                    FormattedNumber fn = UnitsConvert.convert(mv.getValue(), mt.getUnits());
                    metrics[i] = fn.toString();
                } else {
                    metrics[i] = RequestUtils.message(request, "common.value.notavail");
                }
            }
        }

        request.setAttribute("metrics", metrics);

        return null;
    }
}

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

import javax.servlet.ServletContext;
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
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.units.FormattedNumber;

/**
 *
 * Fetch the designated metrics for a resource
 */
public class ResourceMetricValuesAction extends TilesAction {
    
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        AppdefEntityID entityId =
            (AppdefEntityID) context.getAttribute(Constants.ENTITY_ID_PARAM);

        List templates = (List) context.getAttribute("Indicators");
        
        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);

        Integer[] tids = new Integer[templates.size()];
        int i = 0;
        for (Iterator it = templates.iterator(); it.hasNext(); i++) {
            MeasurementTemplate mtv = (MeasurementTemplate) it.next();
            tids[i] = mtv.getId();
        }
        MetricValue[] vals = boss.getLastMetricValue(sessionId, entityId, tids);

        // Format the values
        String[] metrics = new String[vals.length];
        
        if (vals == null) {
            Arrays.fill(metrics,
                        RequestUtils.message(request, "common.value.notavail"));
        }
        else {
            i = 0;
            for (Iterator it = templates.iterator(); it.hasNext(); i++) {
                MeasurementTemplateValue mtv =
                    (MeasurementTemplateValue) it.next();
                if (vals[i] == null)
                    metrics[i] =
                        RequestUtils.message(request, "common.value.notavail");
                else {                
                    FormattedNumber fn =
                        UnitsConvert.convert(vals[i].getValue(), mtv.getUnits());
                    metrics[i] = fn.toString();
                }
            }
        }

        request.setAttribute("metrics", metrics);
        
        return null;
    }
}

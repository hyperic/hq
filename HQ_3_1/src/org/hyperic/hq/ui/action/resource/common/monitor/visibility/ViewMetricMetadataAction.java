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

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

public class ViewMetricMetadataAction extends TilesAction {

    public ActionForward execute(ComponentContext cc, ActionMapping mapping,
             ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
        MetricMetadataForm cform = (MetricMetadataForm)form;
        ServletContext ctx = getServlet().getServletContext();
        int sessionId = RequestUtils.getSessionId(request).intValue();
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);
        AppdefEntityID aid = new AppdefEntityID(cform.getEid());
        
        AppdefEntityTypeID atid = null;
        if (cform.getCtype() != null) {
            atid = new AppdefEntityTypeID(cform.getCtype());
        }
        
        List mdss = mb.findMetricMetadata(sessionId, aid, atid, cform.getM());
        request.setAttribute(Constants.METRIC_SUMMARIES_ATTR, mdss);
        return null; 
   }
}

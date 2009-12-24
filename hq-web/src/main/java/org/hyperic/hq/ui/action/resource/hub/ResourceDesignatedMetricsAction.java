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

import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Fetch the designated metrics for a resource
 */
public class ResourceDesignatedMetricsAction
    extends TilesAction {

    private final HashSet<String> categories = new HashSet<String>();
    private MeasurementBoss measurementBoss;

    @Autowired
    public ResourceDesignatedMetricsAction(MeasurementBoss measurementBoss) {
        super();
        this.measurementBoss = measurementBoss;
        categories.add(MeasurementConstants.CAT_AVAILABILITY);
        categories.add(MeasurementConstants.CAT_UTILIZATION);
        categories.add(MeasurementConstants.CAT_THROUGHPUT);
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        String appdefKey = (String) context.getAttribute(Constants.ENTITY_ID_PARAM);
        AppdefEntityID entityId = new AppdefEntityID(appdefKey);

        int sessionId = RequestUtils.getSessionId(request).intValue();

        List<MeasurementTemplate> designates = measurementBoss.getDesignatedTemplates(sessionId, entityId, categories);
        context.putAttribute(Constants.CTX_SUMMARIES, designates);

        return null;
    }
}

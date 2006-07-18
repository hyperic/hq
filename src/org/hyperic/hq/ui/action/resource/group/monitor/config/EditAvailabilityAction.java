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

/*
 * EditAvailabilityAction.java
 *
 */

package org.hyperic.hq.ui.action.resource.group.monitor.config;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An Action that edits a compatible group's availability thresholds.
 * XXX waiting on PR: 6617 for actual backend implementation
 */
public class EditAvailabilityAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Add metrics to the resource specified in the given
     * <code>MonitoringAddMetricsForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(EditAvailabilityAction.class.getName());    
        HttpSession session = request.getSession();

        GroupMonitoringConfigForm addForm = (GroupMonitoringConfigForm)form;
        Integer resourceId = addForm.getRid();
        Integer resourceType = addForm.getType();
      
        HashMap parms = new HashMap();
        
        int id = resourceId.intValue();
        int type = resourceType.intValue();      
        AppdefEntityID appdefId = new AppdefEntityID(type, id);

        parms.put(Constants.RESOURCE_PARAM, new Integer(id));
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(type));

        ActionForward forward = checkSubmit(request, mapping, form, parms);
        if (forward != null) {
            return forward;
        }

        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("Editing compatible group's availability thresholds.");
        // XXX    mBoss.createMeasurements(sessionId.intValue(), 
        //preapre the form here    appdefId, pendingMetricsIds);

        RequestUtils.setConfirmation(request,
            "resource.group.monitor.visibility.config.Availability.Confirmation");

        return returnSuccess(request, mapping, parms);


    }
}

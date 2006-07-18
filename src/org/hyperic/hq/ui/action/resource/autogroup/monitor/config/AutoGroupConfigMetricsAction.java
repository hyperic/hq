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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.config;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.config.ConfigMetricsAction;
import org.hyperic.hq.ui.action.resource.common.monitor.config.MonitoringConfigForm;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * modifies the metrics data.
 */
public class AutoGroupConfigMetricsAction extends ConfigMetricsAction {
    
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        
        Log log = LogFactory.getLog(ConfigMetricsAction.class.getName());            
        log.trace("modifying metrics action");                    

        HashMap parms = new HashMap(2);
        
        int sessionId = RequestUtils.getSessionId(request).intValue();
        MonitoringConfigForm mForm = (MonitoringConfigForm) form;
        AppdefEntityID appdefId = RequestUtils.getEntityId(request);

        parms.put(Constants.RESOURCE_PARAM, appdefId.getId());
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
                  new Integer(appdefId.getType()));

        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);

        Integer[] tmplsToUpdate = mForm.getMids();
        // We have to transform the templates to actual measurement IDs 
        InventoryHelper helper = null;
        AppdefEntityID[] entityIds = null;
        AppdefEntityID typeHolder = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            // if we get this far, we are dealing with an auto-group
            // of servers or services

            // find the resource type of the autogrouped resources
            typeHolder = entityIds[0];
            helper = InventoryHelper.getHelper(typeHolder);
        } catch (ParameterNotFoundException e) {
            // if we get here, we are dealing with an auto-group of
            // platforms
            helper = new RootInventoryHelper();
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
        
        ActionForward forward = checkSubmit(request, mapping, form, parms);

        if (forward != null) {
            if (mForm.isRemoveClicked()) {

                // don't make any back-end call if user has not selected any
                // metrics.
                if (tmplsToUpdate.length == 0)
                    return forward;

                mBoss.disableAGMeasurements(sessionId, appdefId, childTypeId,
                                            tmplsToUpdate);
                RequestUtils.setConfirmation(request,
                    "resource.common.monitor.visibility.config.RemoveMetrics.Confirmation");
            }
            return forward;
        }        

        // take the list of pending metric ids (mids),
        // and update them.);
        long interval = mForm.getIntervalTime();
        
        // don't make any back-end call if user has not selected any metrics.                      
        if (tmplsToUpdate.length == 0)
            return returnSuccess(request, mapping, parms);
            
        mBoss.updateAGMeasurements(sessionId, appdefId, childTypeId,
                                   tmplsToUpdate, interval);
        
        RequestUtils.setConfirmation(request, 
            "resource.common.monitor.visibility.config.ConfigMetrics.Confirmation");
        
        return returnSuccess(request, mapping, parms);
    }    
}

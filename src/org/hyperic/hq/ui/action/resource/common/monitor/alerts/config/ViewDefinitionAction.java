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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * View an alert definition.
 *
 */
public class ViewDefinitionAction extends TilesAction {

    private Log log = LogFactory.getLog(ViewDefinitionAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {            
        ServletContext ctx = getServlet().getServletContext();
        
        try {
            AppdefEntityID aeid = RequestUtils.getEntityId(request);
            
            // If group entity, do nothing
            if (aeid.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                return null;
            }
        } catch (ParameterNotFoundException e) {
            // Global alert definition
        }
        
        Integer alertDefId =
            RequestUtils.getIntParameter(request,
                                         Constants.ALERT_DEFINITION_PARAM);
        log.trace("alertDefId=" + alertDefId);

        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);

        // properties
        AlertDefinitionValue adv = eb.getAlertDefinition(sessionID, alertDefId);
        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, adv);

        // conditions
        //
        // if any of the conditions are EventConstants.TYPE_CHANGE, we
        // cannot edit them
        AlertConditionValue[] acvList = adv.getConditions();
        boolean canEditConditions = true;
        for (int i = 0; i < acvList.length; ++i) {
            switch (acvList[i].getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
            case EventConstants.TYPE_CHANGE:
            case EventConstants.TYPE_CONTROL:
            case EventConstants.TYPE_ALERT:
            case EventConstants.TYPE_CUST_PROP:
            case EventConstants.TYPE_LOG:
            case EventConstants.TYPE_CFG_CHG:
                break;
            default:
                canEditConditions = false;
                break;
            }
            
            if (!canEditConditions)
                break;
        }
        request.setAttribute("canEditConditions",
                             new Boolean(canEditConditions));
        List alertDefConditions = AlertDefUtil.getAlertConditionBeanList(
                sessionID, request, mb, acvList,
                EventConstants.TYPE_ALERT_DEF_ID.equals(adv.getParentId()));
        request.setAttribute("alertDefConditions", alertDefConditions);
        request.setAttribute("openNMSEnabled", OpenNMSAction.isLoaded());

        // enablement
        AlertDefUtil.setEnablementRequestAttributes(request, adv);
        
        return null;
    }
}

// EOF

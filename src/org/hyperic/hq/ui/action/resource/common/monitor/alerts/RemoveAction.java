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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.action.BaseAction;

/**
 * An Action that removes an alert
 */
public class RemoveAction extends BaseAction {

    /** 
     * removes alerts 
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(RemoveAction.class.getName());
                
        RemoveForm nwForm = (RemoveForm) form;
        log.debug("entering removeAlertsAction");
        Integer rid = nwForm.getRid();
        Integer type = nwForm.getType();
        Map params = new HashMap();
        params.put(Constants.RESOURCE_TYPE_ID_PARAM, type);
        params.put(Constants.RESOURCE_PARAM, rid);
        
        ActionForward forward = checkSubmit(request, mapping, form, params);
        // if the remove button was clicked, we are coming from
        // the alerts list page and just want to continue
        // processing ...
        if ( forward != null && !forward.getName().equals(Constants.REMOVE_URL) ) {
            log.trace("returning " + forward);
            // if there is no resource type, there is probably no
            // resource -- go to dashboard on cancel
            if ( forward.getName().equals(Constants.CANCEL_URL) &&
                 type.intValue() == 0 ) {
                return returnNoResource(request, mapping);
            }
            return forward;
        }

        Integer[] alertIds = nwForm.getAlerts();
        if ( log.isDebugEnabled() ) {
            log.debug("removing: " + Arrays.asList(alertIds) );
        }

        if (alertIds == null || alertIds.length == 0){
            return returnSuccess(request, mapping, params);
        }

        Integer sessionId =  RequestUtils.getSessionId(request);

        ServletContext ctx = getServlet().getServletContext();
        EventsBoss boss = ContextUtils.getEventsBoss(ctx);

        boss.deleteAlerts(sessionId.intValue(), alertIds);

        log.debug("!!!!!!!!!!!!!!!! removing alerts!!!!!!!!!!!!");

        if (type == null || type.intValue() == 0) {
            return returnNoResource(request, mapping);
        } else {
            return returnSuccess(request, mapping, params);
        }

    }

    protected ActionForward returnNoResource(HttpServletRequest request, ActionMapping mapping)
        throws Exception {
        return constructForward(request, mapping, "noresource");
    }
}

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

package org.hyperic.hq.ui.action.resource.common.control;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * Perform a quick control action on a resource.
 */
public class QuickControlAction extends BaseAction {

    private static final Log log
        = LogFactory.getLog(QuickControlAction.class.getName());    

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        
        QuickControlForm qcForm = (QuickControlForm)form;
        log.trace("performing resouce quick control action: " + qcForm.getResourceAction());
 
        HashMap fwdParms = new HashMap(2);
            
        try {    
            ServletContext ctx = getServlet().getServletContext();            
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            int sessionId =  RequestUtils.getSessionIdInt(request);
            
            // create the new action to schedule
            Integer id = qcForm.getResourceId();
            Integer type = qcForm.getResourceType();
            AppdefEntityID appdefId = new AppdefEntityID(type.intValue(), id);
            fwdParms.put(Constants.RESOURCE_PARAM, id);
            fwdParms.put(Constants.RESOURCE_TYPE_ID_PARAM, type);
            
            String action = qcForm.getResourceAction();
            String args = qcForm.getArguments();

            if ( AppdefEntityConstants.APPDEF_TYPE_GROUP == type ) {
                cBoss.doGroupAction(sessionId, appdefId, action, args, null);
            } else {
                cBoss.doAction(sessionId, appdefId, action, args);
            }
            ActionForward fwd 
                = this.returnSuccess(request, mapping, fwdParms); 
            
            // set confirmation message
            String ctrlStr = qcForm.getResourceAction();
            SessionUtils.setConfirmation(request.getSession(false /* dont create */), 
                "resource.server.QuickControl.Confirmation", ctrlStr);
            
            qcForm.reset(mapping, request);
            return fwd;
        } 
        catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            SessionUtils.setError(request.getSession(false),
                "resource.common.error.ControlNotEnabled");
            return returnFailure(request, mapping, fwdParms);                 
        }
        catch (PermissionException pe) {
            SessionUtils.setError(request.getSession(false),
                "resource.common.control.error.NewPermission");
            return returnFailure(request, mapping, fwdParms);
        }
    }
}

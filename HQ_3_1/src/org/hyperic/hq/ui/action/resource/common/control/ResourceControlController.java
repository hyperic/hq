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

package org.hyperic.hq.ui.action.resource.common.control;

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/*
 * An abstract subclass of <code>ResourceControllerAction</code> that
 * provides common methods for resource control controller actions.
 */
public abstract class ResourceControlController extends ResourceController {

    protected static final Log log =
        LogFactory.getLog(ResourceControlController.class.getName());
    
    /**
     * Checks to see if control is enabled for this resource. Sets
     * Constants.CONTROL_ENABLED_ATTR in request scope.
     */
    protected void checkControlEnabled(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        // check to see if control is enabled                                    
        try {
            ServletContext ctx = getServlet().getServletContext();
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            int type = appdefId.getType();

            boolean isEnabled = false;
            switch (type) {
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS:
                    isEnabled = cBoss.isGroupControlEnabled(sessionId, appdefId);
                    break;
                default:
                    isEnabled = cBoss.isControlEnabled(sessionId, appdefId);
            }
            request.setAttribute( Constants.CONTROL_ENABLED_ATTR, 
                                  new Boolean(isEnabled) );
            if (isEnabled) {
                List actions;
                try {
                    actions = cBoss.getActions(sessionId, appdefId);
                    Boolean hasControls
                        = (actions.size() > 0) ? Boolean.TRUE : Boolean.FALSE;
                    request.setAttribute("hasControlActions", hasControls);
                } catch (PluginNotFoundException e) {
                    log.warn("Error loading plugin for " + appdefId + ": " + e);
                    request.setAttribute("hasControlActions", Boolean.FALSE);
                }
            } else {
                request.setAttribute("hasControlActions", Boolean.FALSE);
            }
        } catch (RemoteException e) {
            // couldn't get servlet context. oh well.   
            log.error("Unexpected exception: " + e, e);
        } catch (ServletException e) {
            // couldn't get servlet context. oh well.   
            log.error("Unexpected exception: " + e, e);
        } catch (ApplicationException e) {
            log.error("Unexpected exception: " + e, e);
        }
    }
    public ActionForward currentControlStatus(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {
        super.setNavMapLocation(request,mapping,
                                Constants.CONTROL_LOC); 
        return null;
    }
    
    public ActionForward controlStatusHistory(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {
        super.setNavMapLocation(request,mapping,
                                Constants.CONTROL_LOC); 
        return null;
    }
}

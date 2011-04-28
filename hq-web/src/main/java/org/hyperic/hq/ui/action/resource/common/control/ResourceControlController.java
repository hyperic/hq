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

import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/*
 * An abstract subclass of <code>ResourceControllerAction</code> that
 * provides common methods for resource control controller actions.
 */
public abstract class ResourceControlController
    extends ResourceController {
    private final Properties keyMethodMap = new Properties();

    protected final Log log = LogFactory.getLog(ResourceControlController.class.getName());

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ResourceControlController(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
        initKeyMethodMap();
    }

    private void initKeyMethodMap() {
        keyMethodMap.setProperty(Constants.MODE_LIST, "currentControlStatus");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "currentControlStatus");
        keyMethodMap.setProperty(Constants.MODE_HST, "controlStatusHistory");
        keyMethodMap.setProperty(Constants.MODE_HST_DETAIL, "controlStatusHistory");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newScheduledControlAction");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editScheduledControlAction");
    }

    /**
     * Checks to see if control is enabled for this resource. Sets
     * Constants.CONTROL_ENABLED_ATTR in request scope.
     */
    protected void checkControlEnabled(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) {
        // check to see if control is enabled
        try {

            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            int type = appdefId.getType();

            boolean isEnabled = false;
            switch (type) {
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS:
                    isEnabled = controlBoss.isGroupControlEnabled(sessionId, appdefId);
                    break;
                default:
                    isEnabled = controlBoss.isControlEnabled(sessionId, appdefId);
            }
            request.setAttribute(Constants.CONTROL_ENABLED_ATTR, new Boolean(isEnabled));
            if (isEnabled) {
                try {
                    List<String> actions = controlBoss.getActions(sessionId, appdefId);
                    Boolean hasControls = (actions.size() > 0) ? Boolean.TRUE : Boolean.FALSE;
                    request.setAttribute("hasControlActions", hasControls);
                } catch (PluginNotFoundException e) {
                    log.warn("Error loading plugin for " + appdefId + ": " + e);
                    request.setAttribute("hasControlActions", Boolean.FALSE);
                }
            } else {
                request.setAttribute("hasControlActions", Boolean.FALSE);
            }
        } catch (ServletException e) {
            // couldn't get servlet context. oh well.
            log.error("Unexpected exception: " + e, e);
        } catch (ApplicationException e) {
            log.error("Unexpected exception: " + e, e);
        }
    }

    private void storePortalDataInRequest(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                          HttpServletResponse response, Portal portal, boolean checkControlEnabled,
                                          boolean moveMessages, boolean setNavMapLocation) throws Exception {
        request.setAttribute(Constants.PORTAL_KEY, portal);

        if (moveMessages) {
            // move messages and errors from session to request scope
            SessionUtils.moveAttribute(request, Globals.MESSAGE_KEY);
            SessionUtils.moveAttribute(request, Globals.ERROR_KEY);
        }

        setResource(request, response);

        if (checkControlEnabled) {
            checkControlEnabled(mapping, form, request, response);
        }

        if (setNavMapLocation) {
            super.setNavMapLocation(request, mapping, Constants.CONTROL_LOC);
        }
    }

    public ActionForward currentControlStatus(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response, Portal portal) throws Exception {
        storePortalDataInRequest(mapping, form, request, response, portal, true, true, true);

        return null;
    }

    public ActionForward controlStatusHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response, Portal portal) throws Exception {
        storePortalDataInRequest(mapping, form, request, response, portal, true, false, true);

        return null;
    }

    public ActionForward controlStatusHistoryDetail(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response, Portal portal) throws Exception {
        storePortalDataInRequest(mapping, form, request, response, portal, true, false, false);

        return null;
    }

    public ActionForward newScheduledControlAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                   HttpServletResponse response, Portal portal) throws Exception {
        storePortalDataInRequest(mapping, form, request, response, portal, false, false, false);

        return null;
    }

    public ActionForward editScheduledControlAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response, Portal portal) throws Exception {
        storePortalDataInRequest(mapping, form, request, response, portal, false, false, false);

        return null;
    }
}

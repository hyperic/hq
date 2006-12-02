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

package org.hyperic.hq.ui.action.admin.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseDispatchAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.json.JSONArray;

/**
 * Populate the list of types to configure metric defaults
 */
public class EditDefaultsAction extends BaseDispatchAction {

    public ActionForward monitor(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse resp)
        throws Exception {
        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        if (!BizappUtils.canAdminHQ(sessionId, ContextUtils.getAuthzBoss(ctx)))
            throw new PermissionException(
                    "User not authorized to configure monitor defaults");
  
        AppdefBoss apBoss = ContextUtils.getAppdefBoss(ctx);
        // get platform types
        int session = sessionId.intValue();
        List platTypes = apBoss.findAllPlatformTypes(session,
                                                     PageControl.PAGE_ALL);
        request.setAttribute(Constants.ALL_PLATFORM_TYPES_ATTR, platTypes);
        // get server types
        List serverTypes = apBoss.findAllServerTypes(session,
                                                     PageControl.PAGE_ALL);
        // get the special service types sans windows special case
        List platServices = new ArrayList();
        List winServices = new ArrayList();
        for (int i = 0; i < serverTypes.size(); i++) {
            ServerTypeValue stv = (ServerTypeValue) serverTypes.get(i);
            if (stv.getVirtual()) {
                if (stv.getName().startsWith("Win")) {
                    winServices.addAll(
                        Arrays.asList(stv.getServiceTypeValues()));
                } else {
                    platServices.addAll(
                        Arrays.asList(stv.getServiceTypeValues()));
                }
            }
        }
        request.setAttribute(Constants.ALL_SERVER_TYPES_ATTR, serverTypes);
        request.setAttribute(Constants.ALL_PLATFORM_SERVICE_TYPES_ATTR,
                             platServices);
        request.setAttribute(Constants.ALL_WINDOWS_SERVICE_TYPES_ATTR,
                             winServices);
        
        // Create the portal
        Portal p = Portal.createPortal("admin.home.ResourceTemplates",
                                       ".admin.config.EditMonitorConfig");
        request.setAttribute(Constants.PORTAL_KEY, p);
        
        return null;
    }
    
    public ActionForward escalate(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse resp)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        JSONArray arr = eb.listAllEscalationName(sessionID);
        
        // Create the portal
        Portal p = Portal.createPortal("admin.home.EscalationSchemes",
                                       ".admin.config.EditEscalationConfig");
        request.setAttribute(Constants.PORTAL_KEY, p);
        
        return null;        
    }
    
	protected Properties getKeyMethodMap() {
		return new Properties();
	}
}

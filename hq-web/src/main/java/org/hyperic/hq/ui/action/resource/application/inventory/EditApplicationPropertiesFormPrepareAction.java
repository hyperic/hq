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

package org.hyperic.hq.ui.action.resource.application.inventory;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.application.ApplicationForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * This class handles preparing the data for edit operations performed 
 * on Application Properties (screen 2.1.6.2)
 */
public class EditApplicationPropertiesFormPrepareAction extends TilesAction {

    private static Log log = LogFactory.
        getLog(EditApplicationPropertiesFormPrepareAction.class.getName());

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        ApplicationValue appVal =
            (ApplicationValue) RequestUtils.getResource(request); 
         if (appVal == null) {
             RequestUtils.setError(request,
                           "resource.application.inventory.error.ApplicationNotFound");
             return null;
         }
        ApplicationForm appForm = (ApplicationForm) form;
        appForm.loadResourceValue(appVal);
        
        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        log.trace("getting all application types");
        List applicationTypes = boss.findAllApplicationTypes(sessionId.intValue());
        appForm.setResourceTypes(applicationTypes);
        appForm.setResourceType(appVal.getApplicationType().getId());
        //request.setAttribute("resourceTypes", applicationTypes);
        request.setAttribute(Constants.NUM_CHILD_RESOURCES_ATTR,
                             new Integer(1));
        return null;                

    }
}

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

import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.ObjectNotFoundException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.application.ApplicationForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;

/**
 * This class handles saving the submission from the application
 * creation screen (2.1.1)
 */
public class NewApplicationAction extends BaseAction {

    private static Log log = LogFactory.getLog(NewApplicationAction.class.getName());

    /**
     * Create the server with the attributes specified in the given
     * <code>ServerForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        ApplicationForm newForm = (ApplicationForm) form;
        HashMap forwardParams = new HashMap(2);
    
        try {
            ActionForward forward = checkSubmit(request, mapping, form);            
            if (forward != null) {
                return forward;
            }         
            ServletContext ctx = getServlet().getServletContext();
            Integer sessionId = RequestUtils.getSessionId(request);
            Integer applicationTypeId = newForm.getResourceType();
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            ApplicationValue app = new ApplicationValue();
            app.setName(newForm.getName());
            app.setDescription(newForm.getDescription());
            app.setEngContact(newForm.getEngContact());
            app.setBusinessContact(newForm.getBusContact()) ;
            app.setOpsContact(newForm.getOpsContact()) ;
            app.setLocation(newForm.getLocation());
            log.trace("finding application type [" + applicationTypeId + "]");
            ApplicationTypeValue applicationType =
                boss.findApplicationTypeById(sessionId.intValue(),
                                             applicationTypeId);
            app.setApplicationType(applicationType);
            log.trace("creating application [" + app.getName() +
                      "] with attributes " + newForm);
            // XXX ConfigResponse is a dummy arg, must be nuked when the boss
            // interface fixed
            app = boss.createApplication(sessionId.intValue(), app,
                                         new ArrayList(), new ConfigResponse());
            AppdefEntityID appId = app.getEntityId();
            log.trace("created application [" + app.getName() +
                      "] with attributes " + app.toString() +
                      " and has appdef ID " + appId);
            RequestUtils.setConfirmation(request,
                                         "resource.application.inventory." +
                                         "confirm.CreateApplication",
                                         app.getName());
            forwardParams.put(Constants.ENTITY_ID_PARAM, appId);
            return returnNew(request, mapping, forwardParams);
        }
        catch (ObjectNotFoundException oe) {
            RequestUtils
                .setError(request,
                          "resource.application.inventory.error." +
                          "ApplicationTypeNotFound",
                          "resourceType");
            return returnFailure(request, mapping, forwardParams);
        }        
        catch (AppdefDuplicateNameException e1) {
            RequestUtils
                .setError(request,
                          Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }
    }
}

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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles saving the general properties editing performed on screen
 * 2.1.6.1
 */
public class EditGeneralPropertiesAction
    extends BaseAction {
    private final Log log = LogFactory.getLog(EditGeneralPropertiesAction.class.getName());

    private AppdefBoss appdefBoss;

    @Autowired
    public EditGeneralPropertiesAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        ResourceForm editForm = (ResourceForm) form;
        Integer appId = editForm.getRid();
        Integer entityType = editForm.getType();

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.RESOURCE_PARAM, appId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

        try {
            ActionForward forward = checkSubmit(request, mapping, form, forwardParams, BaseAction.YES_RETURN_PATH);
            if (forward != null) {
                return forward;
            }

            Integer sessionId = RequestUtils.getSessionId(request);

            // now set up the application
            ApplicationValue app = appdefBoss.findApplicationById(sessionId.intValue(), appId);
            log.trace("in preparation to update it, retrieved app " + app);
            if (app == null) {
                RequestUtils.setError(request, "resource.application.error.ApplicationNotFound");
                return returnFailure(request, mapping, forwardParams);
            }

            editForm.updateResourceValue(app);

            log.trace("editing general properties of application [" + app.getName() + "]" + " with attributes " +
                      editForm);

            appdefBoss.updateApplication(sessionId.intValue(), app);

            RequestUtils.setConfirmation(request, "resource.application.inventory.confirm.EditGeneralProperties", app
                .getName());
            return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
        } catch (AppdefDuplicateNameException e1) {
            RequestUtils.setError(request, Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }
    }
}

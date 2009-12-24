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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Edit the General Properties of a Service with the attributes specified in the
 * given <code>ServiceForm</code>.
 * 
 */

public class EditGeneralAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(EditGeneralAction.class.getName());

    private AppdefBoss appdefBoss;

    @Autowired
    public EditGeneralAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        try {
            ServiceForm rForm = (ServiceForm) form;

            Integer rid = rForm.getRid();
            Integer entityType = rForm.getType();

            Map<String, Object> forwardParams = new HashMap<String, Object>(2);
            forwardParams.put(Constants.RESOURCE_PARAM, rid);
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
            ActionForward forward = checkSubmit(request, mapping, form, forwardParams, YES_RETURN_PATH);

            if (forward != null) {
                return forward;
            }

            Integer sessionId = RequestUtils.getSessionId(request);

            Integer serviceId = RequestUtils.getResourceId(request);

            ServiceValue sValue = appdefBoss.findServiceById(sessionId.intValue(), serviceId);

            rForm.updateServiceValue(sValue);

            ServiceValue updatedServer = appdefBoss.updateService(sessionId.intValue(), sValue, null);

            // XXX: enable when we have a confirmed functioning API
            log.trace("saving service [" + sValue.getName() + "]" + " with attributes " + rForm);

            RequestUtils.setConfirmation(request, "resource.service.inventory.confirm.EditGeneralProperties",
                updatedServer.getName());

            return returnSuccess(request, mapping, forwardParams, YES_RETURN_PATH);
        } catch (AppdefDuplicateNameException e1) {
            RequestUtils.setError(request, Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }

    }
}

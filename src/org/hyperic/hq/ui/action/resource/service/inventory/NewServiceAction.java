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
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

    /**
    * Create the service with the attributes specified in the given
    * <code>ServiceForm</code>.
    *
    */
public class NewServiceAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(NewServiceAction.class.getName());
        Map forwardParams = new HashMap(2);
        try {
            ServiceForm newForm = (ServiceForm) form;

            AppdefEntityID aeid = RequestUtils.getEntityId(request);

            forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
            
            switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                forwardParams.put(Constants.ACCORDION_PARAM, "3");
                break;
            }

            ActionForward forward =
                checkSubmit(request, mapping, form, forwardParams,
                            YES_RETURN_PATH);
            if (forward != null) {
                return forward;
            }         

            ServletContext ctx = getServlet().getServletContext();
            Integer sessionId = RequestUtils.getSessionId(request);
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);

            ServiceValue service = new ServiceValue();
            
            service.setName(newForm.getName());
            service.setDescription(newForm.getDescription());

            Integer stPk = newForm.getResourceType();
            ServiceValue newService =
                boss.createService(sessionId.intValue(), service, stPk, aeid);
              
            log.trace("creating service [" + service.getName() +
                      "] with attributes " + newForm);

            Integer serviceId = newService.getId();
            newForm.setRid(serviceId);
            
            RequestUtils.setConfirmation(request,
                                         "resource.service.inventory.confirm.CreateService",
                                         service.getName());

            forwardParams.put(Constants.ENTITY_ID_PARAM,
                              newService.getEntityId().getAppdefKey());
            forwardParams.put(Constants.ACCORDION_PARAM, "0");

            return returnNew(request, mapping, forwardParams);
        } catch (AppdefDuplicateNameException e) {
            RequestUtils.setError(request, Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }
    }
}

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
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When the user selects one of the checkboxes in the services list on the View
 * Application page (2.1.6), this class handles removing the
 * {@link org.hyperic.hq.appdef.shared.AppService}.
 */
public class RemoveServicesAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(RemoveServicesAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public RemoveServicesAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        RemoveResourceForm cform = (RemoveResourceForm) form;
        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, cform.getEid());
        forwardParams.put(Constants.ACCORDION_PARAM, "3");

        Integer[] appSvcIds = cform.getResources();
        if (appSvcIds != null && appSvcIds.length > 0) {

            Integer sessionId = RequestUtils.getSessionId(request);

            for (int i = 0; i < appSvcIds.length; i++) {
                Integer appSvcId = appSvcIds[i];
                log.debug("Removing appSvc = " + appSvcId + "  from application " + cform.getRid());
                appdefBoss.removeAppService(sessionId.intValue(), cform.getRid(), appSvcId);
            }

            RequestUtils.setConfirmation(request, "resource.application.inventory.confirm.RemoveServices");

        }
        return returnSuccess(request, mapping, forwardParams);
    }
}

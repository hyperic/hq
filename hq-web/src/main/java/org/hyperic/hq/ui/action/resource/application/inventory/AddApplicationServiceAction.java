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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When the list of pending services on the Add Services page (2.1.6.4) is
 * grown, shrunk or committed (by selecting from the checkbox lists and clicking
 * add, remove or ok) this class manages the pending list and commitment.
 */
public class AddApplicationServiceAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(AddApplicationServiceAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public AddApplicationServiceAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        AddApplicationServicesForm addForm = (AddApplicationServicesForm) form;
        AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(), addForm.getRid());

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        forwardParams.put(Constants.ACCORDION_PARAM, "3");

        ActionForward forward = checkSubmit(request, mapping, form, forwardParams);
        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                log.trace("removing pending service list");
                SessionUtils.removeList(session, Constants.PENDING_APPSVCS_SES_ATTR);
            } else if (spiderForm.isAddClicked()) {
                log.trace("adding to pending service list " + Arrays.asList(addForm.getAvailableServices()));
                SessionUtils.addToList(session, Constants.PENDING_APPSVCS_SES_ATTR, addForm.getAvailableServices());
            } else if (spiderForm.isRemoveClicked()) {
                log.trace("removing from pending service list");
                SessionUtils.removeFromList(session, Constants.PENDING_APPSVCS_SES_ATTR, addForm.getPendingServices());
            }
            return forward;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("getting pending service list");
        List<String> uiPendings = SessionUtils.getListAsListStr(session, Constants.PENDING_APPSVCS_SES_ATTR);
        List<AppdefEntityID> svcList = new ArrayList<AppdefEntityID>();

        for (int pRcs = 0; pRcs < uiPendings.size(); pRcs++) {
            log.debug("uiPendings = " + uiPendings.get(pRcs));
            StringTokenizer tok = new StringTokenizer(uiPendings.get(pRcs), " ");
            svcList.add(new AppdefEntityID(tok.nextToken()));
        }
        // when we call boss.setApplicationServices(...) our map must
        // be populated with all of the existing services (and whether
        // or not they're entry points) as well as our new ones

        // first, get the existing ones
        PageControl nullPc = new PageControl(-1, -1);
        List<AppdefResourceValue> existingServices = appdefBoss.findServiceInventoryByApplication(sessionId.intValue(),
            aeid.getId(), nullPc);
        DependencyTree tree = appdefBoss.getAppDependencyTree(sessionId.intValue(), aeid.getId());
        for (AppdefResourceValue service : existingServices) {

            log.debug("service =" + service.getClass().getName());

            tree.findAppService(service);
            svcList.add(service.getEntityId());
        }

        // second, get the new ones

        // set all added services to be entry points, if they're not to be
        // entry points and are instead part of a dependency chain,
        // setting up the dependencies is a separate activity

        log.trace("adding servicess " + svcList + " for application [" + aeid.getID() + "]");
        appdefBoss.setApplicationServices(sessionId.intValue(), aeid.getId(), svcList);
        log.trace("removing pending service list");
        SessionUtils.removeList(session, Constants.PENDING_APPSVCS_SES_ATTR);

        RequestUtils.setConfirmation(request, "resource.application.inventory.confirm.AddedServices");
        return returnSuccess(request, mapping, forwardParams);

    }
}

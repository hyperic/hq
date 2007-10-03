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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;


/**
 * When the list of pending service dependencies on the Add 
 * Dependencies page (2.1.6.5) is grown, shrunk or committed 
 * (by selecting from the checkbox lists and clicking add, 
 * remove or ok) this class manages the pending list and  
 * commitment.
 */
public class AddServiceDependenciesAction extends BaseAction {

    private static Log log =
        LogFactory.getLog(AddServiceDependenciesAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        HttpSession session = request.getSession();

        AddApplicationServicesForm addForm = (AddApplicationServicesForm) form;
        Integer resourceId = addForm.getRid();
        Integer entityType = addForm.getType();
        Integer appSvcId = addForm.getAppSvcId();

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
        forwardParams.put("appSvcId",appSvcId);
        
        ActionForward forward =
            checkSubmit(request, mapping, form, forwardParams);
        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() ||
                spiderForm.isResetClicked()) {
                log.trace("removing pending service list");
                SessionUtils.removeList(session,
                                        Constants.PENDING_SVCDEPS_SES_ATTR);
            }
            else if (spiderForm.isAddClicked()) {
                log.trace("adding to pending service list " +
                          Arrays.asList(addForm.getAvailableServices()));
                SessionUtils.addToList(session,
                                       Constants.PENDING_SVCDEPS_SES_ATTR,
                                       addForm.getAvailableServices());
            }
            else if (spiderForm.isRemoveClicked()) {
                log.trace("removing from pending service list");
                SessionUtils.removeFromList(session,
                                            Constants.PENDING_SVCDEPS_SES_ATTR,
                                            addForm.getPendingServices());
            }
            return forward;
        }

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("getting pending service list");
        List uiPendings =
            SessionUtils.getListAsListStr(session,
                                          Constants.PENDING_SVCDEPS_SES_ATTR);
        List pendingServiceIdList = new ArrayList();

        for(int i = 0;i< uiPendings.size(); i++) {
            StringTokenizer tok =
                new StringTokenizer((String) uiPendings.get(i), " ");
            if (tok.countTokens() > 1) {
                pendingServiceIdList.add(
                    new AppdefEntityID(
                        AppdefEntityConstants.stringToType(tok.nextToken()),
                        Integer.parseInt(tok.nextToken())));
            }
            else {
                pendingServiceIdList.add(new AppdefEntityID(tok.nextToken()));
            }
        }

        DependencyTree tree =
            boss.getAppDependencyTree(sessionId.intValue(), resourceId);

        Map depNodeChildren = new HashMap();
        DependencyNode depNode =
            DependencyTree.findAppServiceById(tree, appSvcId);
        for (Iterator iter = depNode.getChildren().iterator(); iter.hasNext();){
            AppServiceValue anAppSvc = (AppServiceValue) iter.next();
            if(anAppSvc.getIsCluster())
                depNodeChildren.put(anAppSvc.getServiceCluster().getGroupId(),
                                    anAppSvc);
            else
                depNodeChildren.put(anAppSvc.getService().getId(),anAppSvc);
        }

        if (log.isTraceEnabled())
            log.trace("adding servicess " + uiPendings.toString() +
                  " for application [" + resourceId + "]");            

        // look through the tree's DependencyNodes to find the ones
        // we have pending (identified by their service ids)
        for (Iterator iter = tree.getNodes().iterator(); iter.hasNext();) {
            DependencyNode node = (DependencyNode) iter.next();
            AppdefEntityID lookFor;
            
            if (node.isCluster())
                lookFor =
                    new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                       node.getAppService()
                                           .getServiceCluster().getGroupId());
            else
                lookFor = node.getAppService().getService().getEntityId();
            if (pendingServiceIdList.contains(lookFor) && 
                !depNodeChildren.containsKey(lookFor)) {
                depNode.addChild(node.getAppService());
            }                                
        }
        log.trace("Saving tree: " + tree);
        boss.setAppDependencyTree(sessionId.intValue(), tree);
        // XXX remember to kill this, this is just to demonstrate for Javier
        DependencyTree savedTree =
            boss.getAppDependencyTree(sessionId.intValue(),
                                      tree.getApplication().getId());
        log.trace("Saved tree: " + savedTree);

        log.trace("removing pending service list");
        SessionUtils.removeList(session,
                                Constants.PENDING_SVCDEPS_SES_ATTR);

        RequestUtils.setConfirmation(request,
                                     "resource.application.inventory.confirm." +
                                     "AddedServices");
        return returnSuccess(request, mapping, forwardParams);        

    }
}

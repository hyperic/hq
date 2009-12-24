/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * On screen 2.1.6.4, a user can select one or more checkboxes (
 * <code>resources</code>), for removal. Handling that action requires rewriting
 * the {@link org.hyperic.hq.appdef.shared.DependencyTree} and saving it.
 */
public class RemoveServiceDependenciesAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(RemoveServiceDependenciesAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public RemoveServiceDependenciesAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        RemoveResourceForm cform = (RemoveResourceForm) form;
        Integer resourceId = cform.getRid();
        Integer entityType = cform.getType();
        Integer[] resources = cform.getResources();
        Integer appSvcId = RequestUtils.getIntParameter(request, "appSvcId");
        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
        forwardParams.put("appSvcId", appSvcId);

        Integer sessionId = RequestUtils.getSessionId(request);

        try {
            DependencyTree tree = appdefBoss.getAppDependencyTree(sessionId.intValue(), resourceId);
            log.debug("got tree " + tree);
            // walk through the nodes to find the ones that are
            // to be removed as dependees

            DependencyNode depNode = DependencyTree.findAppServiceById(tree, appSvcId);
            log.debug("will remove selected children from node " + depNode);
            List<AppService> children = depNode.getChildren();
            List<AppService> toRemove = new ArrayList<AppService>();
            for (int i = 0; i < resources.length; i++) {
                for (AppService asv : children) {

                    if (resources[i].equals(asv.getId())) {
                        // remove this one
                        toRemove.add(asv);
                    }
                }
            }
            for (AppService asv : toRemove) {

                depNode.removeChild(asv);
            }

            log.debug("saving tree " + tree);
            appdefBoss.setAppDependencyTree(sessionId.intValue(), tree);
            DependencyTree savedTree = appdefBoss.getAppDependencyTree(sessionId.intValue(), tree.getApplication()
                .getId());
            log.debug("retrieving saved tree " + savedTree);
        } catch (PermissionException e) {
            log.debug("removing services from application failed:", e);
            throw new ServletException("can't remove services from application", e);
        } catch (ApplicationException e) {
            log.debug("removing services from application failed:", e);
            throw new ServletException("can't remove services from application", e);
        }
        return returnSuccess(request, mapping, forwardParams);
    }
}

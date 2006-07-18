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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * On screen 2.1.6.4, a user can select one or more checkboxes 
 * (<code>resources</code>), for removal.  Handling that action
 * requires rewriting the {@link org.hyperic.hq.appdef.shared.DependencyTree} 
 * and saving it.
 */
public class RemoveServiceDependenciesAction extends BaseAction {

    private static Log log = LogFactory.getLog(RemoveServiceDependenciesAction.class.getName());
    
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
                
        RemoveResourceForm cform = (RemoveResourceForm) form;
        Integer resourceId = cform.getRid();
        Integer entityType = cform.getType();
        Integer[] resources = cform.getResources();
        Integer appSvcId = RequestUtils.getIntParameter(request, "appSvcId");
        List resourceIds = Arrays.asList(resources);
        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
        forwardParams.put("appSvcId", appSvcId);

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        // note: these are _Service_ ids not _AppService_ ids
        List appSvcIdList = Arrays.asList(resources);
        try {
            DependencyTree tree = boss.getAppDependencyTree(sessionId.intValue(),resourceId);
            log.debug("got tree " + tree);
            List nodes = tree.getNodes();
            // walk through the nodes to find the ones that are
            // to be removed as dependees

            DependencyNode depNode = DependencyTree.findAppServiceById(tree, appSvcId);
            log.debug("will remove selected children from node " + depNode);
            List children = depNode.getChildren();
            List toRemove = new ArrayList();
            for(int i =0;i< resources.length; i++) {
                for(Iterator it = children.iterator(); it.hasNext();) {
                    AppServiceValue asv = (AppServiceValue)it.next();
                    if(resources[i].equals(asv.getId())) {
                        // remove this one
                        toRemove.add(asv);
                    }
                }
            }
            for(Iterator it = toRemove.iterator(); it.hasNext();) {
                AppServiceValue asv = (AppServiceValue)it.next();
                depNode.removeChild(asv);
            }
/*            for (Iterator i = children.iterator(); i.hasNext();) {
                AppServiceValue child = (AppServiceValue) i.next();
                Integer lookFor = child.getId();
                log.debug("searching [" + appSvcIdList + "] for " + lookFor);
                if (appSvcIdList.contains(lookFor)) {
                    log.debug("found " + lookFor);
                    // if it's in the list of things to ditch, say adios
                    depNode.removeChild(child);
                }                                                
            }*/
            log.debug("saving tree " + tree);            
            boss.setAppDependencyTree(sessionId.intValue(), tree);   
            DependencyTree savedTree = boss.getAppDependencyTree(sessionId.intValue(), tree.getApplication().getId());
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

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * An Action that adds Resources to a Group in the BizApp. This is first
 * created with AddGroupResourcesFormPrepareAction, which creates the list
 * of pending Resources to add to the group.
 *
 * Heavily based on:
 * @see org.hyperic.hq.ui.action.resource.group.inventory.AddGroupResourcesFormPrepareAction
 */
public class AddGroupResourcesAction extends BaseAction {
    private static Log log = LogFactory.getLog(AddGroupResourcesAction.class.getName());    
    
    /**
     * Add roles to the user specified in the given
     * <code>AddGroupResourcesForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        HttpSession session = request.getSession();
        AddGroupResourcesForm addForm = (AddGroupResourcesForm) form;
        AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(), addForm.getRid());
        Map<String, String> forwardParams = new HashMap<String, String>(2);
        
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        forwardParams.put(Constants.ACCORDION_PARAM, "1");
        forwardParams.put(Constants.RESOURCE_PARAM, addForm.getRid().toString());
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, addForm.getType().toString());
        
        try {
            ActionForward forward = checkSubmit(request, mapping, form, forwardParams);
            
            if (forward != null) {
                return forward;
            }

            ServletContext ctx = getServlet().getServletContext();
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            Integer sessionId = RequestUtils.getSessionId(request);

            log.trace("getting pending resource list");
            
            List<String> pendingResourceIds = SessionUtils.getListAsListStr(request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR);
            
            if (pendingResourceIds.size() == 0) {
                return returnSuccess(request, mapping, forwardParams);
            }
            
            log.trace("getting group [" + aeid.getID() + "]");
            
            AppdefGroupValue agroup = boss.findGroup(sessionId.intValue(), aeid.getId());
            ResourceGroup group = boss.findGroupById(sessionId.intValue(), agroup.getId());
            List<AppdefEntityID> newIds = new ArrayList<AppdefEntityID>();
            ResourceGroupManagerLocal groupMan = ResourceGroupManagerEJBImpl.getOne();
            ResourceManagerLocal resourceMan = ResourceManagerEJBImpl.getOne();
            
            for (Iterator<String> i = pendingResourceIds.iterator(); i.hasNext(); ) {
                AppdefEntityID entity = new AppdefEntityID(i.next());
                Resource r = resourceMan.findResource(entity);
                
                if (!groupMan.isMember(group, r)) {
                    newIds.add(entity);
                }            
            }

            // XXX:  We have the list of resources above.  Should use this
            //       instead of passing in IDs.. waste of effort.
            boss.addResourcesToGroup(sessionId.intValue(), group, newIds);

            log.trace("removing pending user list");
            
            SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.AddResources");
                                         
            return returnSuccess(request, mapping, forwardParams);
        } catch (AppSvcClustDuplicateAssignException e1) {
            log.debug("group update failed:", e1);
         
            RequestUtils.setError(request,Constants.ERR_DUP_CLUSTER_ASSIGNMENT);
            
            return returnFailure(request, mapping);
        } catch (AppdefGroupNotFoundException e) {
            RequestUtils.setError(request, "resource.common.inventory.error.ResourceNotFound");
                     
            return returnFailure(request, mapping, forwardParams);
        } catch (VetoException ve) {
            RequestUtils.setErrorObject(request, 
                    "resource.group.inventory.error.UpdateResourceListVetoed",
                    ve.getMessage());
            
            return returnFailure(request, mapping);            
        }
    }
    
    @Override
    protected ActionForward checkSubmit(HttpServletRequest request, 
                                        ActionMapping mapping, 
                                        ActionForm form,
                                        Map params, 
                                        boolean doReturnPath)
    throws Exception {
        HttpSession session = request.getSession();
        BaseValidatorForm spiderForm = (BaseValidatorForm) form;

        if (spiderForm.isCancelClicked()) {
            log.trace("removing pending/removed resources list");
            SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            
            return returnCancelled(request, mapping, params, doReturnPath);
        }

        if (spiderForm.isResetClicked()) {
            log.trace("removing pending/removed resources list");
            SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            spiderForm.reset(mapping, request);
            
            return returnReset(request, mapping, params);
        }

        if (spiderForm.isCreateClicked()) {
            return returnNew(request, mapping, params);
        }

        if (spiderForm.isAddClicked()) {
            log.trace("adding to pending resources list");
            SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, ((AddGroupResourcesForm) form).getAvailableResources());
            
            return returnAdd(request, mapping, params);
        }

        if (spiderForm.isRemoveClicked()) {
            log.trace("removing from pending resources list");
            SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, ((AddGroupResourcesForm) form).getPendingResources());
            
            return returnRemove(request, mapping, params);
        }

        return null;
    }
}

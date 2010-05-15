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

package org.hyperic.hq.ui.action.resource.hub;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Removes resources in ResourceHub
 */
public class RemoveResourceAction extends BaseAction {

    protected Log log =
        LogFactory.getLog(RemoveResourceAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception{

        ResourceHubForm hubForm = (ResourceHubForm) form;
        if (hubForm.isGroupClicked()) {
            HttpSession session = request.getSession();
            session.setAttribute(Constants.ENTITY_IDS_ATTR,
                                 hubForm.getResources());
            
            session.setAttribute(Constants.RESOURCE_TYPE_ATTR, hubForm.getFf());
            return mapping.findForward("newgroup");
        }
        else if (hubForm.isDeleteClicked()) {
            removeResources(request, hubForm.getResources());
        } else if (hubForm.getEnableAlerts().isSelected()) {
            activateAlerts(request, hubForm.getResources(), true);
        } else if (hubForm.getDisableAlerts().isSelected()) {
            activateAlerts(request, hubForm.getResources(), false);
        }
        return returnSuccess(request, mapping);
    }

    private void activateAlerts(HttpServletRequest request,
                                String[] resourceItems,
                                boolean enabled)
        throws Exception {
        
        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss ev = ContextUtils.getEventsBoss(ctx);

        List resourceList = new ArrayList();
        CollectionUtils.addAll(resourceList, resourceItems);
        List entities =
            BizappUtils.buildAppdefEntityIds(resourceList);
        
        ev.activateAlertDefinitions(
                 sessionId.intValue(),
                 (AppdefEntityID[]) entities
                         .toArray(new AppdefEntityID[entities.size()]), 
                 enabled);

        RequestUtils.setConfirmation(request,
                        enabled ? "resource.common.confirm.AlertsEnabled"
                                : "resource.common.confirm.AlertsDisabled");
    }
    
    private void removeResources(HttpServletRequest request,
                                 String[] resourceItems)
        throws SessionNotFoundException, ApplicationException, VetoException,
               RemoteException, ServletException {

        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);

        List resourceList = new ArrayList();
        CollectionUtils.addAll(resourceList, resourceItems);
        List entities =
            BizappUtils.buildAppdefEntityIds(resourceList);
        if (resourceItems != null && resourceItems.length > 0) {
            Set deleted = new HashSet();
            String vetoMessage = null;
            // about the exception handling:
            // if someone either deleted the entity out from under our user
            // or the user hit the back button, a derivative of 
            // AppdefEntityNotFoundException gets thrown... we can still 
            // keep going on, trying to delete the other things in our list
            // (which is why the whole shebang isn't in one big
            // try / catch) but we only confirm that something was deleted
            // if something actually, um, was
            for (Iterator i = entities.iterator(); i.hasNext();) {
                AppdefEntityID resourceId = (AppdefEntityID) i.next();
                try {
                    deleted.addAll(Arrays.asList(
                            boss.removeAppdefEntity(sessionId.intValue(), resourceId)));
                } catch (AppdefEntityNotFoundException e) {
                    log.error("Removing resource " + resourceId +
                               "failed.");
                } catch (VetoException v) {
                    vetoMessage = v.getMessage();
                    log.info(vetoMessage);
                }
            }
            
            if (vetoMessage != null) {
                RequestUtils
                    .setErrorObject(request, 
                                    "resource.common.inventory.groups.error.RemoveVetoed",
                                    vetoMessage);
            } else if (deleted.size() > 0) {
                RequestUtils
                    .setConfirmation(request,
                                    "resource.common.confirm.ResourcesRemoved");
            }        
        }
    }
    
    /**
     * HHQ-2854: Remove deleted resources from dashboard preferences
     */
    private void removeResourcesFromDashboard(HttpServletRequest request,
                                              Set resourcesToRemove) {
        try {
            ServletContext ctx = getServlet().getServletContext();
            AuthzBoss aBoss = ContextUtils.getAuthzBoss(ctx);
            HttpSession session = request.getSession();
            WebUser user = RequestUtils.getWebUser(session);                                                
            DashboardConfig dashConfig = DashboardUtils.findDashboard(
            		(Integer)session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
            		user, aBoss);
            ConfigResponse dashPrefs = dashConfig.getConfig();
            
            for (Iterator it=dashPrefs.getKeys().iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                if (key.indexOf(Constants.USERPREF_KEY_FAVORITE_RESOURCES) > -1
                        || key.indexOf(Constants.USERPREF_KEY_AVAILABITY_RESOURCES) > -1
                        || key.indexOf(Constants.USERPREF_KEY_CRITICAL_ALERTS_RESOURCES) > -1) {
                    
                    List prefResources = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
                    prefResources.retainAll(resourcesToRemove);
                    
                    if (!prefResources.isEmpty()) {
                        String[] appdefKeyToRemove = new String[prefResources.size()]; 
                        for (int i=0; i<prefResources.size(); i++) {
                            appdefKeyToRemove[i] = ((AppdefEntityID)prefResources.get(i)).getAppdefKey();
                        }
                        DashboardUtils.removeResources(appdefKeyToRemove, key, dashPrefs);
                    }
                }
            }
            ConfigurationProxy.getInstance().setDashboardPreferences(
                                                    session, user, aBoss, dashPrefs);
        } catch (Exception e) {
            log.info("Unable to remove deleted resources from dashboard preferences: " 
                        + e.getMessage());
        }        
    }
}

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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.util.MessageResources;

public class NewGroupFormPrepareAction 
    extends WorkflowPrepareAction {

    public ActionForward workflow(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        GroupForm newForm = (GroupForm) form;
        MessageResources res = getResources(request);
            
        List groupTypes = BizappUtils.buildGroupTypes(request);
        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        HttpSession session = request.getSession();
        
        List platformTypes, serverTypes, serviceTypes, applicationTypes;
        String[] eids =
            (String[]) session.getAttribute(Constants.ENTITY_IDS_ATTR);

        if (eids != null) {
            newForm.setEntityIds(eids);
            
            AppdefResourceTypeValue art = null;
            Integer ff = (Integer)
                session.getAttribute(Constants.RESOURCE_TYPE_ATTR);

            if (ff != null) {
                // Only check if resource type is platform, server, or service
                switch (ff.intValue()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    // See if they have a common resource type
                    art = boss.findCommonResourceType(sessionId.intValue(),
                                                      eids);
                    break;
                default:
                    break;
                }
            }
            
            if (art != null) {
                newForm.setGroupType(
                        new Integer(Constants.APPDEF_TYPE_GROUP_COMPAT));
                newForm.setTypeAndResourceTypeId(art.getAppdefTypeKey());
                newForm.setTypeName(art.getName());
                return null;
            }
            else {
                newForm.setGroupType(
                        new Integer(Constants.APPDEF_TYPE_GROUP_ADHOC));

                String mixRes;
                if (ff != null) {
                    switch (ff.intValue()) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        newForm.setTypeAndResourceTypeId("" +
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS +
                            ":-1");
                        mixRes = "dash.home.DisplayCategory.group." +
                                 "plat.server.service";
                        break;
                    default:
                        newForm.setTypeAndResourceTypeId(ff.toString() +":-1");
                        mixRes =
                            ff.intValue() ==
                                AppdefEntityConstants.APPDEF_TYPE_GROUP ?
                            "dash.home.DisplayCategory.group.groups" :
                            "dash.home.DisplayCategory.group.application";
                        break;
                    }
                    
                    newForm.setTypeName(res.getMessage(mixRes));
                    return null;
                }
            }
        }
        
        platformTypes = boss.findViewablePlatformTypes(sessionId.intValue(),
                                                       PageControl.PAGE_ALL);

        serverTypes = boss.findViewableServerTypes(sessionId.intValue(),
                                                   PageControl.PAGE_ALL);

        serviceTypes = boss.findViewableServiceTypes(sessionId.intValue(),
                                                     PageControl.PAGE_ALL);

        applicationTypes = boss.findAllApplicationTypes(sessionId.intValue());
        
        newForm.setPlatformTypes(platformTypes);
        newForm.setServerTypes(serverTypes);
        newForm.setServiceTypes(serviceTypes);
        newForm.setApplicationTypes(applicationTypes);
        newForm.setGroupTypes(groupTypes);

        return null;
    }
}

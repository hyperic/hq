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

/*
 * Created on Feb 14, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class RemoveAction
    extends BaseAction {

    private ResourceManager resourceManager;
    private AppdefBoss appdefBoss;

    @Autowired
    public RemoveAction(ResourceManager resourceManager, AppdefBoss appdefBoss) {
        super();
        this.resourceManager = resourceManager;
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        RemoveGroupResourcesForm nwForm = (RemoveGroupResourcesForm) form;
        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, nwForm.getEid());
        forwardParams.put(Constants.ACCORDION_PARAM, "1");

        try {
            String[] rsrcIds = nwForm.getResources();

            if (rsrcIds == null || rsrcIds.length == 0) {
                return returnSuccess(request, mapping, forwardParams);
            }

            Integer groupId = RequestUtils.getResourceId(request);
            Integer sessionId = RequestUtils.getSessionId(request);

            ResourceGroup group = appdefBoss.findGroupById(sessionId.intValue(), groupId);

            List<Resource> resources = new ArrayList<Resource>(rsrcIds.length);
            for (int i = 0; i < rsrcIds.length; i++) {
                AppdefEntityID entity = new AppdefEntityID(rsrcIds[i]);

                resources.add(resourceManager.findResource(entity));
            }

            appdefBoss.removeResourcesFromGroup(sessionId.intValue(), group, resources);
            RequestUtils.setConfirmation(request,"resource.group.inventory.confirm.RemoveResources");

            return returnSuccess(request, mapping, forwardParams);
        } catch (ParameterNotFoundException e2) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_ID_FOUND);
            return returnFailure(request, mapping, forwardParams);
        } catch (AppdefGroupNotFoundException e) {
            RequestUtils.setError(request, "resource.common.inventory.error.ResourceNotFound");

            return returnFailure(request, mapping, forwardParams);
        } catch (VetoException ve) {
            RequestUtils.setErrorObject(request,"resource.group.inventory.error.UpdateResourceListVetoed",
                ve.getMessage());
            return returnFailure(request, mapping, forwardParams);
        }
    }
}

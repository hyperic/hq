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

package org.hyperic.hq.ui.action.resource.server.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

public class NewServerFormPrepareAction
    extends WorkflowPrepareAction {

    private AppdefBoss appdefBoss;

    @Autowired
    public NewServerFormPrepareAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServerForm newForm = (ServerForm) form;
        Integer platformId = newForm.getRid();
        Integer resourceType = newForm.getType();

        try {
            Integer sessionId = RequestUtils.getSessionId(request);

            if (platformId == null) {
                platformId = RequestUtils.getResourceId(request);
            }
            if (resourceType == null) {
                resourceType = RequestUtils.getResourceTypeId(request);
            }

            PlatformValue pValue = appdefBoss.findPlatformById(sessionId.intValue(), platformId);

            List<ServerTypeValue> stValues = appdefBoss.findServerTypesByPlatformType(sessionId.intValue(), pValue
                .getPlatformType().getId(), PageControl.PAGE_ALL);

            TreeMap<String, ServerTypeValue> returnMap = new TreeMap<String, ServerTypeValue>();
            for (ServerTypeValue stv : stValues) {

                if (!stv.getVirtual()) {
                    returnMap.put(stv.getSortName(), stv);
                }
            }
            newForm.setResourceTypes(new ArrayList<ServerTypeValue>(returnMap.values()));
            request.setAttribute(Constants.PARENT_RESOURCE_ATTR, pValue);
            newForm.setRid(platformId);
            newForm.setType(resourceType);

            return null;
        } catch (Throwable t) {
            throw new ServletException("Can't prepare new server form", t);
        }
    }
}

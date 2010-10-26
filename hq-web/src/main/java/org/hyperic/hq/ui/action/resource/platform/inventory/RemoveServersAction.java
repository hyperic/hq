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

package org.hyperic.hq.ui.action.resource.platform.inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>BaseAction</code> that removes servers from the inventory.
 */
public class RemoveServersAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(RemoveServersAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public RemoveServersAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    /**
     * Removes the servers identified in the <code>RemoveServersForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        RemoveServersForm rmForm = (RemoveServersForm) form;
        Integer platformId = rmForm.getRid();
        Integer resourceType = rmForm.getType();

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.RESOURCE_PARAM, platformId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

        Integer sessionId = RequestUtils.getSessionId(request);

        Integer[] resources = rmForm.getResources();
        if (resources != null && resources.length > 0) {
            List<Integer> servers = Arrays.asList(resources);
            log.trace("removing servers " + servers + " for platform [" + platformId + "]");

            for (Integer serverId : servers) {
                appdefBoss.removeAppdefEntity(sessionId.intValue(), AppdefEntityID.newServerID(serverId), false);
            }

            RequestUtils.setConfirmation(request, "resource.platform.inventory.confirm.RemoveServers");
        }

        return returnSuccess(request, mapping, forwardParams);

    }
}

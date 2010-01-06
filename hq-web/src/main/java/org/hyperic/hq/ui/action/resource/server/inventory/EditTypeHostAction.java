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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class EditTypeHostAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(EditTypeHostAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public EditTypeHostAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    /**
     * Create the server with the attributes specified in the given
     * <code>ServerForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

       
            ServerForm serverForm = (ServerForm) form;
            AppdefEntityID aeid = new AppdefEntityID(serverForm.getType().intValue(), serverForm.getRid());

            HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
            forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

            ActionForward forward = checkSubmit(request, mapping, form, forwardParams);

            if (forward != null) {
                return forward;
            }

            Integer sessionId = RequestUtils.getSessionId(request);

            ServerValue server = appdefBoss.findServerById(sessionId.intValue(), serverForm.getRid());

            serverForm.updateServerValue(server);

            appdefBoss.updateServer(sessionId.intValue(), server);

            // XXX: enable when we have a confirmed functioning API
            log.trace("saving server [" + server.getName() + "]" + " with attributes " + serverForm);

            Integer serverId = new Integer(-1);
            serverForm.setRid(serverId);

            RequestUtils.setConfirmation(request, "resource.server.inventory.confirm.SaveServer", server.getName());

            return returnSuccess(request, mapping, forwardParams);
        
    }
}

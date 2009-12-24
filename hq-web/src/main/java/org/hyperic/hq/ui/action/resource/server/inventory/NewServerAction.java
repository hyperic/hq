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
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create the server with the attributes specified in the given
 * <code>ServerForm</code>.
 */
public class NewServerAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(NewServerAction.class.getName());

    private AppdefBoss appdefBoss;

    @Autowired
    public NewServerAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        Map<String, Object> forwardParams = new HashMap<String, Object>(2);
        try {
            ServerForm newForm = (ServerForm) form;
            AppdefEntityID aeid = new AppdefEntityID(newForm.getType().intValue(), newForm.getRid());

            forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
            forwardParams.put(Constants.ACCORDION_PARAM, "2");

            ActionForward forward = checkSubmit(request, mapping, form, forwardParams, YES_RETURN_PATH);

            if (forward != null) {
                return forward;
            }

            Integer sessionId = RequestUtils.getSessionId(request);

            ServerValue server = new ServerValue();

            server.setName(newForm.getName());
            server.setDescription(newForm.getDescription());
            server.setInstallPath(newForm.getInstallPath());
            // NOTE: DON'T SET THE AI IDENTIFIER -- ONLY SERVERS CREATED VIA
            // AUTOINVENTORY SHOULD EVER SET THIS VALUE.
            // FOR OTHER SERVERS, IT WILL BE SET AUTOMAGICALLY TO A UNIQUE VALUE
            // server.setAutoinventoryIdentifier(newForm.getInstallPath());

            Integer platformId = RequestUtils.getResourceId(request);
            Integer ppk = platformId;
            Integer stPk = newForm.getResourceType();

            log.trace("creating server [" + server.getName() + "]" + " with attributes " + newForm);

            ServerValue newServer = appdefBoss.createServer(sessionId.intValue(), server, ppk, stPk, null);
            Integer serverId = newServer.getId();
            AppdefEntityID entityId = newServer.getEntityId();

            newForm.setRid(serverId);

            RequestUtils.setConfirmation(request, "resource.server.inventory.confirm.CreateServer", server.getName());

            forwardParams.put(Constants.ENTITY_ID_PARAM, entityId.getAppdefKey());
            forwardParams.put(Constants.ACCORDION_PARAM, "0");

            return returnNew(request, mapping, forwardParams);
        } catch (AppdefDuplicateNameException e1) {
            RequestUtils.setError(request, Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }
    }
}

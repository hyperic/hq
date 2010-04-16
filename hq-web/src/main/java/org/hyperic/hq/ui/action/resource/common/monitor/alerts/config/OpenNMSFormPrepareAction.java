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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * View an alert definition -- OpenNMS action.
 * 
 */
public class OpenNMSFormPrepareAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(OpenNMSFormPrepareAction.class);
    private EventsBoss eventsBoss;
    private AuthzBoss authzBoss;
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public OpenNMSFormPrepareAction(EventsBoss eventsBoss, AuthzBoss authzBoss, AlertPermissionManager alertPermissionManager) {
        super();
        this.eventsBoss = eventsBoss;
        this.authzBoss = authzBoss;
        this.alertPermissionManager = alertPermissionManager;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        int sessionID = RequestUtils.getSessionId(request).intValue();

        AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition(request, sessionID, eventsBoss);
        OpenNMSForm oForm = (OpenNMSForm) form;
        try {
            oForm.importAction(adv);
        } catch (InvalidActionDataException e) {
            log.error("Invalid OpenNMSAction", e);
        } catch (EncodingException e) {
            log.error("Invalid OpenNMSAction", e);
        }
        
        // ...check to see if user has the ability to modify...
        try {
            AuthzSubject subject = authzBoss.getCurrentSubject(sessionID);
            alertPermissionManager.canModifyAlertDefinition(subject, new AppdefEntityID(adv.getAppdefType(), adv.getAppdefId()));
            oForm.setCanModify(true);
        } catch(PermissionException e) {
            // We can view it, but can't modify it
            oForm.setCanModify(false);
        }

        return null;
    }
}

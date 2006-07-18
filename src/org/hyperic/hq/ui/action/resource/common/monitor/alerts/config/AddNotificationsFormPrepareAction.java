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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;

import javax.ejb.FinderException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.tiles.actions.TilesAction;


/**
 * An abstract base class for setting up the add notifications
 * form(s).
 *
 */
public abstract class AddNotificationsFormPrepareAction extends TilesAction {
    private Log log = LogFactory.getLog( AddNotificationsFormPrepareAction.class.getName() );

    // ---------------------------------------------------- Public Methods

    /**
     * <p>Return the array of Integer ids for the given
     * <code>notificationType</code> contained in the alert definition
     * notification actions.</p>
     *
     * <p>This method also sets the alert definition object on the
     * form and places it in the request.  It also puts the resource
     * id and resource type in the request.</p>
     * @param addForm the form being prepared
     * @param notificationType the type of notification
     *
     * @return the array of ids for already existing notifications
     * based on the notificationType, or a zero-length array if there
     * are not yet any notifications of this type
     * @throws ServletException
     * @throws FinderException
     * @throws RemoteException
     * @throws PermissionException
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws EncodingException
     * @throws InvalidActionDataException
     */
    public Integer[] getNotificationIds(HttpServletRequest request,
                                        AddNotificationsForm addForm,
                                        AppdefEntityID aeid,
                                        int notificationType)
        throws ServletException, SessionNotFoundException,
               SessionTimeoutException, PermissionException, RemoteException,
               FinderException, EncodingException, InvalidActionDataException {
        Integer[] ids = new Integer[0];
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss eventBoss = ContextUtils.getEventsBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        
        Integer alertDefId = addForm.getAd();
        log.debug("(1) alertDefId=" + alertDefId);
        if (alertDefId == null)
            throw new ParameterNotFoundException("alert definition id not found");

        log.debug("(2) alertDefId=" + alertDefId);
        AlertDefinitionValue alertDef = (AlertDefinitionValue)
             request.getAttribute(Constants.ALERT_DEFS_ATTR);
        if (alertDef == null) {
            alertDef = eventBoss.getAlertDefinition(sessionId.intValue(),
                                                    alertDefId);
        }
        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, alertDef);
        
        ActionValue[] actions = alertDef.getActions();
        for (int i = 0; i < actions.length; ++i) {
            if ( actions[i].classnameHasBeenSet() &&
                 !(actions[i].getClassname().equals(null) ||
               actions[i].getClassname().equals("") ) ) {
                EmailActionConfig emailCfg = new EmailActionConfig();
                ConfigResponse configResponse =
                    ConfigResponse.decode( actions[i].getConfig() );
                
                try {
                    emailCfg.init(configResponse);
                } catch (InvalidActionDataException e) {
                    // Not an EmailAction
                    log.debug("Action is " + actions[i].getClassname());
                    continue;
                }

                if (emailCfg.getType() == notificationType) {
                    ids = new Integer[emailCfg.getUsers().size()];
                    ids = (Integer[])emailCfg.getUsers().toArray(ids);
                    break;
                }
            }
        }
        
        if (aeid instanceof AppdefEntityTypeID) {
            addForm.setAetid(aeid.getAppdefKey());
            request.setAttribute(Constants.APPDEF_RES_TYPE_ID,
                                 aeid.getAppdefKey());
        }
        else {
            addForm.setRid(aeid.getId());
            addForm.setType(new Integer(aeid.getType()));
            request.setAttribute(Constants.ENTITY_ID_PARAM,
                                 aeid.getAppdefKey());
        }
        
        return ids;
    }
}

// EOF

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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An Action that removes notifications for an alert definition.
 *
 */
public abstract class RemoveNotificationsAction
    extends BaseAction
    implements NotificationsAction
{
    private Log log = LogFactory.getLog(RemoveNotificationsAction.class.getName());

    /** 
     * removes alert definitions 
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        RemoveNotificationsForm rnForm = (RemoveNotificationsForm)form;
        Map params = new HashMap();
        
        if (rnForm.getAetid() != null)
            params.put(Constants.APPDEF_RES_TYPE_ID, rnForm.getAetid());
        else {
            AppdefEntityID aeid =
                new AppdefEntityID(rnForm.getType().intValue(),rnForm.getRid());
            params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        }
        params.put( "ad", rnForm.getAd() );
        
        Integer sessionID =  RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        AlertDefinitionValue adv = eb.getAlertDefinition( sessionID.intValue(),
                                                          rnForm.getAd() );
        ActionValue[] actions = adv.getActions();
        for (int i=0; i<actions.length; i++) {
            if ( actions[i].classnameHasBeenSet() &&
                 !( actions[i].getClassname().equals(null) ||
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

                if ( emailCfg.getType() == getNotificationType() ) {
                    return handleRemove(mapping, request, params, sessionID,
                                        actions[i], emailCfg, eb,  rnForm);
                }
            }
        }

        return returnFailure(request, mapping, params);

    }

    protected abstract ActionForward handleRemove(ActionMapping mapping,
                                                  HttpServletRequest request,
                                                  Map params,
                                                  Integer sessionID,
                                                  ActionValue action,
                                                  EmailActionConfig ea,
                                                  EventsBoss eb,
                                                  RemoveNotificationsForm rnForm)
        throws Exception;
}

// EOF

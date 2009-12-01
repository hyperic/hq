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

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.FinderException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
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
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;


/**
 * View an alert definition -- notified roles.
 *
 */
public abstract class ViewDefinitionNotificationsAction
    extends TilesAction
    implements NotificationsAction
{
    private Log log = LogFactory.getLog(ViewDefinitionNotificationsAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        AuthzBoss ab = ContextUtils.getAuthzBoss(ctx);

        PageControl pc = RequestUtils.getPageControl(request);

        ActionValue[] actions;
        String a = request.getParameter("a");
        if (null != a) {
            log.debug("Viewing notifications for an alert ...");
            Integer aid = new Integer(a);
            List actionList = eb.getActionsForAlert(sessionID, aid);
            actions = (ActionValue[])
                actionList.toArray(new ActionValue[actionList.size()]);
        } else {
            log.debug("Viewing notifications for an alert definition ...");
            AlertDefinitionValue adv =
                AlertDefUtil.getAlertDefinition(request, sessionID, eb);
            actions = adv.getActions();
        }

        boolean listNotSet = true;
        PageList notifyList = new PageList();
        for (int i = 0; i < actions.length; ++i) {
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
                    try {
                        PageList pl = getPageList(sessionID, ab, emailCfg, pc);
                        notifyList.setTotalSize( pl.size() );
                        notifyList.addAll( getSubList(pl, pc) );
                        listNotSet = false;
                    } catch (PermissionException e) {
                        // No permission to view
                        return null;
                    }
                }
            }
        }
        request.setAttribute("notifyList", notifyList);
        request.setAttribute( Constants.LIST_SIZE_ATTR,
                              new Integer( notifyList.getTotalSize() ) );
        if (listNotSet) {
            request.setAttribute("notifyList", new PageList());
            request.setAttribute( Constants.LIST_SIZE_ATTR, new Integer(0) );
            log.debug("No notifications ...");
        }

        return null;
    }

    protected abstract PageList getPageList(int sessionID, AuthzBoss ab,
                                            EmailActionConfig ea, PageControl pc)
        throws FinderException,
               SessionTimeoutException,
               SessionNotFoundException,
               PermissionException,
               RemoteException;

    //-----------------------------------------------------------------
    //-- private helpers
    //-----------------------------------------------------------------
    private List getSubList(PageList pl, PageControl pc) {
        if (pc.getPagesize() == PageControl.SIZE_UNLIMITED) {
            return pl;
        } else {
            if ( pc.getPagesize() >= pl.size() ) {
                return pl;
            } else {
                int startIdx = pc.getPagenum() * pc.getPagesize();
                int endIdx = startIdx + pc.getPagesize();
                if ( endIdx > pl.size() ) {
                    endIdx = pl.size();
                }
                log.debug("startIdx=" + startIdx + ", endIdx=" + endIdx);
                return pl.subList(startIdx, endIdx);
            }
        }
    }
}

// EOF

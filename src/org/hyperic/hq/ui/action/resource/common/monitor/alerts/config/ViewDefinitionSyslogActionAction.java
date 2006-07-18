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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * View an alert definition -- syslog action.
 *
 */
public class ViewDefinitionSyslogActionAction extends TilesAction {

    private Log log = LogFactory.getLog(ViewDefinitionSyslogActionAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {            
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        ConfigBoss configBoss = ContextUtils.getConfigBoss(ctx);
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        String enabledStr = configBoss.getConfig().getProperty
            (HQConstants.SyslogActionsEnabled);
        Boolean syslogActionsEnabled = Boolean.valueOf(enabledStr);
        request.setAttribute(HQConstants.SyslogActionsEnabled,
                             syslogActionsEnabled);

        if ( syslogActionsEnabled.booleanValue() ) {
            AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition
                (request, sessionID, eb);
            SyslogActionForm saForm = new SyslogActionForm();
            AlertDefUtil.prepareSyslogActionForm(adv, saForm);
            request.setAttribute("syslogActionForm", saForm);
        }

        return null;
    }
}

// EOF

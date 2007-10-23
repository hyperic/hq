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

package org.hyperic.hq.ui.action.admin.config;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class EditConfigAction extends BaseAction {

    Log log = LogFactory.getLog(EditConfigAction.class.getName());

    /**
     * Create the cam config with the attributes specified in the given
     * <code>ConfigForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception 
    {
        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        int sessionId = RequestUtils.getSessionIdInt(request);
        SystemConfigForm cForm = (SystemConfigForm)form;    
        ServletContext ctx     = getServlet().getServletContext();
        ConfigBoss boss = ContextUtils.getConfigBoss(ctx);
        
        if (cForm.isOkClicked()) {
            if (log.isTraceEnabled())
                log.trace("Getting config");
            Properties props = cForm.saveConfigProperties(boss.getConfig());

            if (log.isTraceEnabled())
                log.trace("Setting config");
            boss.setConfig(sessionId, props);

            if (log.isTraceEnabled())
                log.trace("Restarting config service");
            boss.restartConfig();

            // Set the update mode
            UpdateBoss uboss = ContextUtils.getUpdateBoss(ctx);
            uboss.setUpdateMode(sessionId,
                UpdateStatusMode.findByCode(cForm.getUpdateMode()));
            
            // Get the alert enablement status
            EventsBoss eboss = ContextUtils.getEventsBoss(ctx);
            eboss.setAlertsAllowed(sessionId, cForm.isAlertsAllowed());
        }

        RequestUtils.setConfirmation(request,
                                     "admin.config.confirm.saveSettings");
        return returnSuccess(request, mapping);
    }
}

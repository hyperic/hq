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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;

/**
 * Update the syslog action for an alert definition.
 *
 */
public class EditDefinitionSyslogActionAction extends BaseAction {

    private Log log = LogFactory.getLog(EditDefinitionSyslogActionAction.class.getName());

    // ---------------------------------------------------- Public Methods

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        SyslogActionForm saForm = (SyslogActionForm)form;

        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        
        Map params = new HashMap(2);
        
        if (saForm.getAetid() != null) {
            params.put(Constants.APPDEF_RES_TYPE_ID,
                       new AppdefEntityTypeID(saForm.getAetid()));
        }
        else {
            params.put(Constants.ENTITY_ID_PARAM,
                       new AppdefEntityID(saForm.getEid()));
        }
        params.put("ad", saForm.getAd());
        
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);


        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        AlertDefinitionValue adv =
            eb.getAlertDefinition( sessionID, saForm.getAd() );
        ActionValue actionValue = AlertDefUtil.getSyslogActionValue(adv);
        if ( saForm.getShouldBeRemoved() ) {
            if (null != actionValue) {
                adv.removeAction(actionValue);
                eb.updateAlertDefinition(sessionID, adv);
            }
        } else {
            SyslogActionConfig sa = new SyslogActionConfig();
            sa.setMeta( saForm.getMetaProject() );
            sa.setProduct( saForm.getProject() );
            sa.setVersion( saForm.getVersion() );
            ConfigResponse configResponse = sa.getConfigResponse();
            if (null == actionValue) {
                eb.createAction( sessionID, saForm.getAd(),
                                 sa.getImplementor(), configResponse );
            } else {
                actionValue.setConfig( configResponse.encode() );
                eb.updateAction(sessionID, actionValue);
            }
        }

        return returnSuccess(request, mapping, params);
    }
}

// EOF

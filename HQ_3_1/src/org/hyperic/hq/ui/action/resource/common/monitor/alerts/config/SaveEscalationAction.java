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
 * An Action that saves escalation scheme for an alert definition.
 *
 */
public class SaveEscalationAction extends BaseAction {
    private Log log = LogFactory.getLog(SaveEscalationAction.class.getName());

    /** 
     * removes alert definitions 
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        EscalationSchemeForm rnForm = (EscalationSchemeForm) form;
        Map params = new HashMap();
        
        if (rnForm.getAetid() != null)
            params.put(Constants.APPDEF_RES_TYPE_ID, rnForm.getAetid());
        else {
            AppdefEntityID aeid =
                new AppdefEntityID(rnForm.getType().intValue(),rnForm.getRid());
            params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        }
        params.put( "ad", new Integer(rnForm.getAd()) );
        
        Integer sessionID =  RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        // XXX Save stuff from the form

        return returnSuccess(request, mapping, params);

    }
}

// EOF

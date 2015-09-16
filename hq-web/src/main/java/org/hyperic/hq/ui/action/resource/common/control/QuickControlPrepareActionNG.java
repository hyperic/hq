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

package org.hyperic.hq.ui.action.resource.common.control;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This populates the server control list. "Start," "Stop," etc.
 */

@Component ("quickControlPrepareActionNG")
public class QuickControlPrepareActionNG
extends BaseActionNG implements
    ViewPreparer{

    private final Log log = LogFactory.getLog(QuickControlPrepareActionNG.class.getName());
    @Resource
    private ControlBoss controlBoss;

  

    public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

        log.trace("Preparing quick control options.");
        request=getServletRequest();

        QuickControlFormNG qForm = new QuickControlFormNG();

        try {
            int sessionId = RequestUtils.getSessionIdInt(request);

            AppdefEntityID appdefId = RequestUtils.getEntityId(request);

            List<String> actions = controlBoss.getActions(sessionId, appdefId);
            Map<String, String> options =new LinkedHashMap<String, String>();
            for (String action : actions) {
            	String option = action.substring(0, 1).toUpperCase() + action.substring(1);
            	options.put(action,option);
			}
            qForm.setControlActions(options);
            qForm.setNumControlActions(new Integer(options.size()));
            request.setAttribute("qForm", qForm);
           
        } catch (PluginNotFoundException cpe) {
            log.trace("No control plugin available");
            qForm.setNumControlActions(new Integer(0));
            RequestUtils.setError(request, "resource.common.control.error.NoPlugin");

            
        } catch (Exception e) {
        	log.error(e);
		
    }
    }



}

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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * This populates the server control list. "Start," "Stop," etc.
 */
public class QuickControlPrepareAction extends TilesAction {

    private static final Log log
        = LogFactory.getLog(QuickControlPrepareAction.class.getName());    

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        log.trace("Preparing quick control options.");
 
        QuickControlForm qForm = (QuickControlForm)form;
        
        try {
            int sessionId = RequestUtils.getSessionIdInt(request);           
            ControlBoss cBoss =
                ContextUtils.getControlBoss(getServlet().getServletContext());
            
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            
            List actions = cBoss.getActions(sessionId, appdefId);
            actions = OptionItem.createOptionsList(actions);

            qForm.setControlActions(actions);
            qForm.setNumControlActions(new Integer(actions.size()));

            return null;
        } catch (PluginNotFoundException cpe) {
            log.trace("No control plugin available");
            qForm.setNumControlActions(new Integer(0));
            RequestUtils.setError(request, "resource.common.control.error.NoPlugin");
            
            return null;
        }
    }

}

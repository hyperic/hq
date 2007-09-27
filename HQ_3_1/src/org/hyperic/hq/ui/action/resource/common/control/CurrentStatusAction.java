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

import javax.servlet.ServletContext;
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
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;


/**
 * An Action that the current status of actions on a resource.
 */
public class CurrentStatusAction extends TilesAction {

    // ---------------------------------------------------- Public Methods

    /** 
     * Displays state of current actions of a resource. 
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        Log log = LogFactory.getLog(CurrentStatusAction.class.getName());
                
        log.trace("determining current status.");              
        int sessionId = RequestUtils.getSessionId(request).intValue(); 
        ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
        AppdefEntityID appId = RequestUtils.getEntityId(request);

        Integer batchId = null;
        try {
            batchId = RequestUtils.getIntParameter(request,
                        Constants.CONTROL_BATCH_ID_PARAM);
        } 
        /* failed to get that param, that's ok, use current */
        catch (NullPointerException npe) {}
        catch (ParameterNotFoundException pnfe) {}
        catch (NumberFormatException nfe) {}

        ControlHistory cValue = null;
        if (null == batchId) {
            cValue = cBoss.getCurrentJob(sessionId, appId);
        } else {
            cValue = cBoss.getJobByJobId(sessionId, batchId);
        }
        
        if (cValue == null /* no current job */) {
            cValue = cBoss.getLastJob(sessionId, appId);
        }

        if (cValue == null /* no last job */) {
            request.setAttribute(Constants.CONTROL_STATUS_PARAM,
                                 Constants.CONTROL_STATUS_NONE);
        } else {
            // the default should end up being CONTROL_STATUS_INPROGRESS
            request.setAttribute(Constants.CONTROL_STATUS_PARAM,
                                 cValue.getStatus());
            batchId = cValue.getId();
        }

        if (batchId != null) {
            request.setAttribute(Constants.CONTROL_BATCH_ID_PARAM, batchId);
        }

        request.setAttribute(Constants.CONTROL_CURRENT_STATUS_ATTR, cValue);
        
        return null;
    }
}

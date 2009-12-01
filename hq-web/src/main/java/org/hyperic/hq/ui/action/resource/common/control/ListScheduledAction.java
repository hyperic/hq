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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * An Action that retrieves all <em>ControlActionSchedule</em>'s
 * for a resource.
 */
public class ListScheduledAction extends TilesAction {
    
    // ---------------------------------------------------- Public Methods
    
    /**
     * Retrieve a <code>List</code> of all
     * <code>ControlActionSchedule</code> objects and save it into the
     * request attribute <code>Constants.CONTROL_ACTIONS_SERVER_ATTR</code>.
     */
     public ActionForward execute(ComponentContext context,
                                  ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
    throws Exception {
        
        Log log = LogFactory.getLog(ListScheduledAction.class.getName());
        
        try {
            log.trace("Getting all scheduled control actions for resource.");

            ServletContext ctx = getServlet().getServletContext();
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);

            Integer sessionId = RequestUtils.getSessionId(request);
            PageControl pc = RequestUtils.getPageControl(request);
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);

            PageList jobs =
                cBoss.findScheduledJobs(sessionId.intValue(), appdefId, pc);           
            
            request.setAttribute( Constants.CONTROL_ACTIONS_SERVER_ATTR, jobs );
            
            // have set page size by hand b/c of redirects
            BaseValidatorForm sForm = (BaseValidatorForm)form;
            try {
                sForm.setPs(Constants.PAGESIZE_DEFAULT);
                sForm.setPs(RequestUtils.getIntParameter(request, 
                    Constants.PAGESIZE_PARAM));
            } 
            catch (NullPointerException npe) {}
            catch (ParameterNotFoundException pnfe) {}
            catch (NumberFormatException nfe) {}

            log.trace("Successfulling obtained all"
                + " scheduled control actions for resource.");
            
            return null;
        } catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            RequestUtils.setError(request,
                "resource.common.error.ControlNotEnabled");
            return null;                 
        } catch (ApplicationException t) {
            throw new ServletException(ListHistoryAction.class.getName()
                + "Can't get resource control history list.", t);
        }
    }
}

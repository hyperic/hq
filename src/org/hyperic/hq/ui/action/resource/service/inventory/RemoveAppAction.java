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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * removes an application
 * 
 *
 */
public class RemoveAppAction extends BaseAction {

    /** Removes a application identified by the
     * value of the request parameter <code>Constants.RESOURCE_PARAM</code>
     * from the BizApp.
     * @return
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(RemoveAppAction.class.getName());
                
        RemoveResourceForm nwForm = (RemoveResourceForm) form;

        Integer[] resources = nwForm.getResources();

        if (resources == null || resources.length == 0){
            return buildSuccessForwardMapping(request, mapping);
        }

        Integer sessionId =  RequestUtils.getSessionId(request);

        //get the spiderSubjectValue of the user to be deleated.
        ServletContext ctx = getServlet().getServletContext();            
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);            

        log.trace("removing resource");                                                      
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);

        for (int i = 0; i < resources.length; i++){
            //boss.removeService(sessionId.intValue(), resources[i], false);
        }

        return buildSuccessForwardMapping(request, mapping);

    }
    
    /**
     * returns the server action for this action
     * 
     * @param request
     * @param mapping
     * @return ActionForward
     * @throws ParameterNotFoundException
     * @throws Exception
     */
    private ActionForward buildSuccessForwardMapping(HttpServletRequest request,
                                            ActionMapping mapping)
    throws ParameterNotFoundException, Exception{
            
        Integer serviceId = RequestUtils.getResourceId(request);
            
        return returnSuccess(request, mapping, Constants.RESOURCE_PARAM,
                             serviceId);
    }
}

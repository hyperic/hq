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

/*
 * Created on Apr 18, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 *
 */
public class RemoveScheduleAction extends BaseAction {

    /** Removes a server identified by the
     * value of the request parameter <code>Constants.SERVER_PARAM</code>
     * from the BizApp.
     * @return
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(RemoveScheduleAction.class.getName());
                
        RemoveResourceForm nwForm = (RemoveResourceForm) form;
        
        Integer[] resources = nwForm.getResources();
        Integer rid = nwForm.getRid();
        Integer type = nwForm.getType();
        Map params = new HashMap();
        params.put(Constants.RESOURCE_PARAM, rid);
        params.put(Constants.RESOURCE_TYPE_ID_PARAM, type);
        
        if (resources == null || resources.length == 0){
            returnSuccess(request, mapping, params); 
        }
        
        Integer sessionId =  RequestUtils.getSessionId(request);
        
        //get the spiderSubjectValue of the user to be deleated.
        ServletContext ctx = getServlet().getServletContext();            
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);            
        
        log.trace("removing resource");                                                      
        AIBoss boss = ContextUtils.getAIBoss(ctx);
        
        boss.deleteAIJob(sessionId.intValue(), resources);

        return returnSuccess(request, mapping, params);
            
    }
}

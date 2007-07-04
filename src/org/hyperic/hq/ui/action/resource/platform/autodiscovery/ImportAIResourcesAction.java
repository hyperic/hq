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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class ImportAIResourcesAction extends BaseAction {

    /**
     * Process the results of an AutoDiscovery scan based on 
     * <code>AutoDiscoveryResultsForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        AutoDiscoveryResultsForm aiForm = (AutoDiscoveryResultsForm) form;
        
        // Set the parameters if available
        Map params = new HashMap();
        if (aiForm.getRid() != null && aiForm.getRid().intValue() > 0) {
            params.put(Constants.ENTITY_ID_PARAM, aiForm.getEid());
        }
        
        try {
            ActionForward forward = checkSubmit(request, mapping, form,
                                                YES_RETURN_PATH);
            
            if (forward != null) { 
                return forward;
            }         

            try {
                importAIResource(request, aiForm.getAiPid());
            } catch (AIQApprovalException e) {
                RequestUtils.setError(request, 
                                      "dash.autoDiscovery.import.Error",
                                      e.getMessage());
                return returnFailure(request, mapping, params);
            }

            return returnSuccess(request, mapping, params, NO_RETURN_PATH);
            
        }
        catch (AIQApprovalException ae) {
            if (ae.getReason() == AIQApprovalException.ERR_PARENT_NOT_APPROVED) {
                RequestUtils.setError(request,
                                      "resource.platform.inventory.autoinventory.error.NoPlatformFound");
            } else if (ae.getReason() == AIQApprovalException.ERR_ADDED_TO_APPDEF) {
                RequestUtils.setError(request,
                                      "resource.platform.inventory.autoinventory.error.PlatformFound");
            } else {
                // don't care about any other error
                throw ae;
            }
            return returnFailure(request, mapping, params);
        }
        catch (PlatformNotFoundException ae) {
            RequestUtils.setError(request,
                                  "resource.platform.inventory.autoinventory.error.NoPlatformFound");
            return returnFailure(request, mapping, params);
        }
    }

    /**
     * import the ai resource
     */
    private void importAIResource(HttpServletRequest request,
                                  Integer aiPlatformId)
        throws Exception 
    {
        ServletContext ctx = getServlet().getServletContext();
        AIBoss aiBoss = ContextUtils.getAIBoss(ctx);            
        Integer sessionId = RequestUtils.getSessionId(request);
        int sessionInt = sessionId.intValue();

        String aiPlatform = AppdefEntityConstants.typeToString(AppdefEntityConstants.
                                                               APPDEF_TYPE_AIPLATFORM);
        String aiServer = AppdefEntityConstants.typeToString(AppdefEntityConstants.
                                                             APPDEF_TYPE_AISERVER);
        String aiIp = AppdefEntityConstants.typeToString(AppdefEntityConstants.
                                                         APPDEF_TYPE_AIIP);

        // build the list of platforms to be ignored. This *could* happen from the ui
        // by clicking from the dashboard.
        List aiPlatformIds = buildResources(request,
                                            AIQueueConstants.Q_DECISION_IGNORE,
                                            aiPlatform);

        // if not empty process this request.
        if(!aiPlatformIds.isEmpty()) 
            aiBoss.processQueue(sessionInt, aiPlatformIds, null, null , 
                                AIQueueConstants.Q_DECISION_IGNORE);
        
        aiPlatformIds = buildResources(request,
                                       AIQueueConstants.Q_DECISION_UNIGNORE,
                                       aiPlatform);
        if(!aiPlatformIds.isEmpty()) 
            aiBoss.processQueue(sessionInt, aiPlatformIds, null, null , 
                                AIQueueConstants.Q_DECISION_UNIGNORE);

        // Similarly build lists of ignored and unignored AIServers and AIIps in the
        // code below.
        List servers = buildResources(request,
                                      AIQueueConstants.Q_DECISION_IGNORE,
                                      aiServer);
        List ips = buildResources(request, AIQueueConstants.Q_DECISION_IGNORE,
                                  aiIp);
        if(!(servers.isEmpty() && ips.isEmpty()))
            aiBoss.processQueue(sessionInt, null, servers, ips , 
                                AIQueueConstants.Q_DECISION_IGNORE);
        
        servers = buildResources(request, AIQueueConstants.Q_DECISION_UNIGNORE, 
                                 aiServer);
        ips = buildResources(request, AIQueueConstants.Q_DECISION_UNIGNORE,aiIp);
        if(!(servers.isEmpty() && ips.isEmpty()))
            aiBoss.processQueue(sessionInt, null, servers, ips, 
                                AIQueueConstants.Q_DECISION_UNIGNORE);
        AIPlatformValue aiVal = 
                aiBoss.findAIPlatformById(sessionInt, aiPlatformId.intValue());

        // we call clear to make sure that we are not holding on to older values.
        aiPlatformIds.clear();

        // add the curremt platform to the list only if it is not ignored.
        if (!aiVal.getIgnored() )
            aiPlatformIds.add(aiPlatformId);

        // build the ai ip ids
        List aiIpIds = BizappUtils.buildAIResourceIds(aiVal.getAIIpValues(),
                                                      false);

        // build the server ids
        List aiServerIds = BizappUtils.buildAIResourceIds(aiVal.getAIServerValues(), 
                                                          false);

        aiBoss.processQueue(sessionInt, aiPlatformIds,
                            aiServerIds, aiIpIds, 
                            AIQueueConstants.Q_DECISION_APPROVE);
    }
    
    /**
    * A private method which builds a list of AIResourceValue ids based on the 
    * AIResourceType and a comparator. The comparator is any of the constants 
    * from the <code> AIQueueConstants</code>. 
    * @param request a HttpServletRequest object
    * @param int an int to compare
    * @param resourceType a String object representing the AIResourceType
    * @return ret a List
    */

    private List buildResources(HttpServletRequest request, int comparator, 
                                String resourceType) 
    {
        List ret = new ArrayList();
        Enumeration values = request.getParameterNames();
        while (values.hasMoreElements()) {
            String name = (String)values.nextElement();
            String value = request.getParameter(name);
            int index = name.indexOf(':');
            if(index > 0 && !(name.indexOf(resourceType) < 0) )   {
               if(Integer.parseInt(value) == comparator) {
                    ret.add(new Integer(name.substring(index+1)));    
                } 
            }
            
        }
        return ret;
    }
}

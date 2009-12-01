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

import java.rmi.RemoteException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringifiedException;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class PlatformAutoDiscoveryAction extends ResourceController {

    private static final String TITLE_PATH =
        "resource.autodiscovery.inventory.";
    
    // @see org.hyperic.hq.ui.action.BaseDispatchAction#getKeyMethodMap
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_NEW,          "newAutoDiscovery");
        map.setProperty(Constants.MODE_EDIT,         "editAutoDiscovery");
        map.setProperty(Constants.MODE_VIEW,         "viewResource");
        map.setProperty(Constants.MODE_VIEW_RESULTS, "viewResults");
        map.setProperty(Constants.MODE_RESULTS,      "results");
        return map;
    }

    public ActionForward newAutoDiscovery(ActionMapping mapping,
                                          ActionForm form,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
        throws Exception {

        ServletContext ctx = getServlet().getServletContext();
        int sessionId = RequestUtils.getSessionIdInt(request);
        AIBoss aiboss = ContextUtils.getAIBoss(ctx);
        
        // If the caller specified an aiPlatformID, then lookup
        // the agent token and other stuff from that.
        ScanStateCore ssc;
        try {
            Integer aiPid =
                RequestUtils.getIntParameter(request,
                                             Constants.AI_PLATFORM_PARAM);
            AIPlatformValue aiPlatform =
                aiboss.findAIPlatformById(sessionId, aiPid.intValue());
            
            try {
                ssc = aiboss.getScanStatusByAgentToken
                    (sessionId, aiPlatform.getAgentToken());
            } catch (AgentConnectionException e) {
                log.warn("AgentConnectException: " + e);
            } catch (AgentNotFoundException e) {
                log.warn("AgentNotFoundException: " + e);
            }
            
            request.setAttribute(Constants.TITLE_PARAM_ATTR, 
                                 aiPlatform.getName());
        } catch (ParameterNotFoundException e) {
            findAndSetResource(request);
            
            PlatformValue pVal =
                (PlatformValue)RequestUtils.getResource(request);
            try {
                ssc = aiboss.getScanStatus(sessionId,
                                           pVal.getId().intValue());
            } catch (AgentConnectionException ace) {
                // redirect to viewResource
                return viewResource(mapping, form, request, response);
            } catch (AgentNotFoundException anfe) {
                // redirect to viewResource
                return viewResource(mapping, form, request, response);
            } 
        }
        
        Portal portal = Portal 
             .createPortal(TITLE_PATH + "NewPlatformAutodiscoveryTitle",
                          ".resource.platform.autodiscovery.NewAutoDiscovery");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward editAutoDiscovery(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {

        findAndSetResource(request);
        findAndSetAISchedule(request);
        
        Portal portal = Portal 
             .createPortal(TITLE_PATH + "EditPlatformAutodiscoveryTitle",
                          ".resource.platform.autodiscovery.EditAutoDiscovery");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewResource(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {

        findAndSetResource(request);

        Portal portal = Portal 
            .createPortal("resource.platform.inventory.ViewPlatformTitle",
                          ".resource.platform.inventory.ViewPlatform");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }


    public ActionForward viewResults(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {

        findAndSetResource(request);

        Portal portal = Portal 
             .createPortal(TITLE_PATH + "ViewAutodiscoveryResultsTitle",
                          ".resource.platform.autodiscovery.ViewResults");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        // Set the workflow
        if (mapping instanceof BaseActionMapping) {
            BaseActionMapping smap = (BaseActionMapping) mapping;
            String workflow = smap.getWorkflow();
            if (workflow != null) {
                SessionUtils.pushWorkflow(request.getSession(), mapping,
                                          workflow);
            }
        }

        return null;
    }

    public ActionForward results(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        loadAIResource(request);

        return viewResults(mapping, form, request, response);
    }

    private void findAndSetAISchedule(HttpServletRequest request)
        throws Exception {

        ServletContext ctx = getServlet().getServletContext();
        AIBoss aiBoss = ContextUtils.getAIBoss(ctx);
        int sessionId = RequestUtils.getSessionIdInt(request);

        Integer scheduleId = RequestUtils.getScheduleId(request);
        AIScheduleValue sValue = aiBoss.findScheduledJobById(sessionId, scheduleId);

        request.setAttribute(Constants.AISCHEDULE_ATTR, sValue);
    }

    private void findAndSetResource(HttpServletRequest request)
        throws Exception {

        setResource(request);
        
        PlatformValue pVal = (PlatformValue) RequestUtils.getResource(request);
        if (pVal != null)
            loadScanState(request, pVal);
    }

    /**
     * Loads the scan state using a platform value
     */
    private void loadScanState(HttpServletRequest request, PlatformValue pVal)
        throws ServletException, SessionTimeoutException,
               SessionNotFoundException, PermissionException,
               AgentRemoteException, AutoinventoryException, RemoteException {
        ServletContext ctx = getServlet().getServletContext();
        int sessionId = RequestUtils.getSessionIdInt(request);
        AIBoss aiboss = ContextUtils.getAIBoss(ctx);
        
        AIPlatformValue aip;
        ScanState ss;
        ScanStateCore ssc;
        try {
            ssc = aiboss.getScanStatus(sessionId, pVal.getId().intValue());
            ss = new ScanState(ssc);
            StringifiedException lastException = BizappUtils.findLastError(ssc);
            
            request.setAttribute(Constants.LAST_AI_ERROR_ATTR, lastException);
        
        } 
        catch (AgentConnectionException e) {
            RequestUtils.setError(request,
                "resource.platform.inventory.configProps.NoAgentConnection");
            return;                          
        } 
        catch (AgentNotFoundException e) {
            RequestUtils.setError(request,
                "resource.platform.inventory.configProps.NoAgentConnection");
            return;                          
        }

        try {
            aip = aiboss.findAIPlatformByPlatformID(sessionId, 
                                                    pVal.getId().intValue());
        } catch (PlatformNotFoundException e) {
            aip = null;
        }

        // load the scanstate object            
        request.setAttribute(Constants.SCAN_STATE_ATTR, ss);
        request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
    }
    
    public void loadAIResource(HttpServletRequest request)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        AIBoss aiboss = ContextUtils.getAIBoss(ctx);
        int sessionId = RequestUtils.getSessionIdInt(request);

        Integer id =
            RequestUtils.getIntParameter(request, Constants.AI_PLATFORM_PARAM);
        AIPlatformValue aip =
            aiboss.findAIPlatformById(sessionId, id.intValue());
                                               
        request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, aip.getName());        
    }
}

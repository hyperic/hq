/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.autoDisc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

public class ViewAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ViewAction.class.getName());
    private AuthzBoss authzBoss;
    private AIBoss aiBoss;
    private AppdefBoss appdefBoss;
    private DashboardManager dashboardManager;

    @Autowired
    public ViewAction(AuthzBoss authzBoss, AIBoss aiBoss, AppdefBoss appdefBoss, DashboardManager dashboardManager) {
        super();
        this.authzBoss = authzBoss;
        this.aiBoss = aiBoss;
        this.appdefBoss = appdefBoss;
        this.dashboardManager = dashboardManager;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        WebUser user = RequestUtils.getWebUser(request);
        int sessionId = user.getSessionId().intValue();
        AIQueueForm queueForm = (AIQueueForm) form;

        PageControl page = new PageControl();

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        page.setPagesize(Integer.parseInt(dashPrefs.getValue(".dashContent.autoDiscovery.range")));

        
        StopWatch watch = new StopWatch();
        if(log.isDebugEnabled()) {
            watch.start("getQueue");
        }
        // always show ignored platforms and already-processed platforms
        PageList<AIPlatformValue> aiQueue = aiBoss.getQueue(sessionId, true, false, true, page);

        if(log.isDebugEnabled()) {
            watch.stop();
            log.debug(watch.prettyPrint());
        }
        List<AIPlatformWithStatus> queueWithStatus = getStatuses(sessionId, aiQueue);
        context.putAttribute("resources", queueWithStatus);

        // If the queue is empty, check to see if there are ANY agents
        // defined in HQ inventory.
        if (aiQueue.size() == 0) {
            int agentCnt = appdefBoss.getAgentCount(sessionId);
            request.setAttribute("hasNoAgents", new Boolean(agentCnt == 0));
        }

        // check every box for queue
        Integer[] platformsToProcess = new Integer[aiQueue.size()];
        List<Integer> serversToProcess = new ArrayList<Integer>();
        AIPlatformValue aiPlatform;
        AIServerValue[] aiServers;
        for (int i = 0; i < platformsToProcess.length; i++) {
            aiPlatform = aiQueue.get(i);
            platformsToProcess[i] = aiPlatform.getId();

            // Add all non-virtual servers on this platform
            aiServers = aiPlatform.getAIServerValues();
            for (int j = 0; j < aiServers.length; j++) {
                if (!BizappUtils.isAutoApprovedServer(sessionId, appdefBoss, aiServers[j])) {
                    serversToProcess.add(aiServers[j].getId());
                }
            }
        }
        queueForm.setPlatformsToProcess(platformsToProcess);
        queueForm.setServersToProcess(serversToProcess);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        // Check for previous error
        // First, check for ignore error.
        Object ignoreErr = request.getSession().getAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR);
        if (ignoreErr != null) {
            ActionMessage err = new ActionMessage("dash.autoDiscovery.import.ignore.Error");
            RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);
            // Only show the error once
            request.getSession().setAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR, null);
        }

        // Check for import exception
        Exception exc = (Exception) request.getSession().getAttribute(Constants.IMPORT_ERROR_ATTR);
        if (exc != null) {
            request.getSession().removeAttribute(Constants.IMPORT_ERROR_ATTR);
            log.error("Failed to approve AI report", exc);
            ActionMessage err = new ActionMessage("dash.autoDiscovery.import.Error", exc);
            RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);
        }
        return null;
    }

    private List<AIPlatformWithStatus> getStatuses(int sessionId, PageList<AIPlatformValue> aiQueue) 
        throws Exception {
        
        ScanStateCore ssc = null;
        List<AIPlatformWithStatus> results = new ArrayList<AIPlatformWithStatus>();

        for (Iterator<AIPlatformValue> it = aiQueue.iterator(); it.hasNext();) {
            AIPlatformValue aiPlatform = it.next();
            
            // FIXME: HHQ-4242: This needs to be done at the server-side / manager layer,
            // not at the UI layer. Re-sync the queue, ensuring the status is up-to-date
            aiPlatform = aiBoss.findAIPlatformById(sessionId, aiPlatform.getId().intValue());
            
            if (aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_PLACEHOLDER) {
                it.remove();
            } else {
                results.add(new AIPlatformWithStatus(aiPlatform, ssc));
            }
        }

        return results;
    }
}

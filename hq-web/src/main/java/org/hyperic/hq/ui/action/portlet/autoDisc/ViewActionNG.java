package org.hyperic.hq.ui.action.portlet.autoDisc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import com.opensymphony.xwork2.ModelDriven;


@Component("autoDiscViewActionNG")
@Scope("prototype")
public class ViewActionNG extends BaseActionNG implements ViewPreparer, ModelDriven<AIQueueFormNG> {

    private final Log log = LogFactory.getLog(ViewActionNG.class);
    
    @Resource
    private AuthzBoss authzBoss;
    @Resource
    private AIBoss aiBoss;
    @Resource
    private AppdefBoss appdefBoss;
    @Resource
    private DashboardManager dashboardManager;
    
    private AIQueueFormNG queueForm=new AIQueueFormNG();
	
	public void execute(TilesRequestContext requestContext, AttributeContext attrContext) {
		HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        WebUser user = null;
        
        try {
        	user = RequestUtils.getWebUser(request);
        } catch (Exception ex) {
        	log.error(ex);
        	return;
        }
        int sessionId = user.getSessionId().intValue();

        PageControl page = new PageControl();

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        page.setPagesize(Integer.parseInt(dashPrefs.getValue(".ng.dashContent.autoDiscovery.range")));

        
        StopWatch watch = new StopWatch();
        if(log.isDebugEnabled()) {
            watch.start("getQueue");
        }
        try {
	        // always show ignored platforms and already-processed platforms
	        PageList<AIPlatformValue> aiQueue = aiBoss.getQueue(sessionId, true, false, true, page);
	
	        if(log.isDebugEnabled()) {
	            watch.stop();
	            log.debug(watch.prettyPrint());
	        }
	        List<AIPlatformWithStatus> queueWithStatus = getStatuses(sessionId, aiQueue);
	        attrContext.putAttribute("resources", new Attribute( queueWithStatus ));
	
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
       
        } catch (Exception ex) {
        	log.error(ex);
        }


        // clean out the return path
        // SessionUtils.resetReturnPath(request.getSession());

        // Check for previous error
        // First, check for ignore error.
        Object ignoreErr = request.getSession().getAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR);
        if (ignoreErr != null) {
            // ActionMessage err = new ActionMessage("dash.autoDiscovery.import.ignore.Error");
            // RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);
            // Only show the error once
            // request.getSession().setAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR, null);
        	this.addCustomActionErrorMessages("dash.autoDiscovery.import.ignore.Error");
        }

        // Check for import exception
        Exception exc = (Exception) request.getSession().getAttribute(Constants.IMPORT_ERROR_ATTR);
        if (exc != null) {
            request.getSession().removeAttribute(Constants.IMPORT_ERROR_ATTR);
            log.error("Failed to approve AI report", exc);
            // ActionMessage err = new ActionMessage("dash.autoDiscovery.import.Error", exc);
            // RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);
            this.addCustomActionErrorMessages(exc.getMessage());
        }
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

	public AIQueueFormNG getQueueForm() {
		return queueForm;
	}

	public void setQueueForm(AIQueueFormNG queueForm) {
		this.queueForm = queueForm;
	}

	public AIQueueFormNG getModel() {
		
		return queueForm;
	}

}

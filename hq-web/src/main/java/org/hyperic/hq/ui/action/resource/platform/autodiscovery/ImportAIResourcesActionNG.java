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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.action.resource.platform.inventory.EditPlatformTypeNetworkPropertiesActionNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


@Component("ImportAIResourcesActionNG")
@Scope(value = "prototype")
public class ImportAIResourcesActionNG extends BaseActionNG implements ModelDriven<AutoDiscoveryResultsFormNG> {

	private final Log log = LogFactory.getLog(ImportAIResourcesActionNG.class);
	@Resource
    private AIBoss aiBoss;
	
	AutoDiscoveryResultsFormNG aiForm = new AutoDiscoveryResultsFormNG();
	String internalEid;
	
	public String save() throws Exception {
        // Set the parameters if available
       
        if (aiForm.getRid() != null && aiForm.getRid().intValue() > 0) {
            request.setAttribute(Constants.ENTITY_ID_PARAM, aiForm.getEid());
            internalEid = aiForm.getEid();
        }

		String forward = checkSubmit(aiForm);
		if (forward != null) {
			return forward;
		}

        try {
            importAIResource(request, aiForm.getAiPid());
        } catch (AIQApprovalException e) {
            addActionError(getText( "dash.autoDiscovery.import.Error", new String [] {e.getMessage()} ) );
            return INPUT;
        }
		return SUCCESS;
	}
	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		return "cancel";
	}

	public AutoDiscoveryResultsFormNG getModel() {
		return aiForm;
	}
	
	public AutoDiscoveryResultsFormNG getAiForm() {
		return aiForm;
	}

	public void setAiForm(AutoDiscoveryResultsFormNG aiForm) {
		this.aiForm = aiForm;
	}
	
	   /**
     * import the ai resource
     */
    private void importAIResource(HttpServletRequest request, Integer aiPlatformId) throws Exception {

        Integer sessionId = RequestUtils.getSessionId(request);
        int sessionInt = sessionId.intValue();

        String aiPlatform = AppdefEntityConstants.typeToString(AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM);
        String aiServer = AppdefEntityConstants.typeToString(AppdefEntityConstants.APPDEF_TYPE_AISERVER);
        String aiIp = AppdefEntityConstants.typeToString(AppdefEntityConstants.APPDEF_TYPE_AIIP);

        // build the list of platforms to be ignored. This *could* happen from
        // the ui
        // by clicking from the dashboard.
        List<Integer> aiPlatformIds = buildResources(request, AIQueueConstants.Q_DECISION_IGNORE, aiPlatform);

        // if not empty process this request.
        if (!aiPlatformIds.isEmpty()) {
            aiBoss.processQueue(sessionInt, aiPlatformIds, null, null, AIQueueConstants.Q_DECISION_IGNORE);
        }

        aiPlatformIds = buildResources(request, AIQueueConstants.Q_DECISION_UNIGNORE, aiPlatform);
        if (!aiPlatformIds.isEmpty()) {
            aiBoss.processQueue(sessionInt, aiPlatformIds, null, null, AIQueueConstants.Q_DECISION_UNIGNORE);
        }
        // Similarly build lists of ignored and unignored AIServers and AIIps in
        // the
        // code below.
        List<Integer> servers = buildResources(request, AIQueueConstants.Q_DECISION_IGNORE, aiServer);
        List<Integer> ips = buildResources(request, AIQueueConstants.Q_DECISION_IGNORE, aiIp);
        if (!(servers.isEmpty() && ips.isEmpty())) {
            aiBoss.processQueue(sessionInt, null, servers, ips, AIQueueConstants.Q_DECISION_IGNORE);
        }

        servers = buildResources(request, AIQueueConstants.Q_DECISION_UNIGNORE, aiServer);
        ips = buildResources(request, AIQueueConstants.Q_DECISION_UNIGNORE, aiIp);
        if (!(servers.isEmpty() && ips.isEmpty())) {
            aiBoss.processQueue(sessionInt, null, servers, ips, AIQueueConstants.Q_DECISION_UNIGNORE);
        }
        AIPlatformValue aiVal = aiBoss.findAIPlatformById(sessionInt, aiPlatformId.intValue());

        // we call clear to make sure that we are not holding on to older
        // values.
        aiPlatformIds.clear();

        // add the curremt platform to the list only if it is not ignored.
        if (!aiVal.getIgnored()) {
            aiPlatformIds.add(aiPlatformId);
        }

        // build the ai ip ids
        List<Integer> aiIpIds = BizappUtils.buildAIResourceIds(aiVal.getAIIpValues(), false);

        // build the server ids
        List<Integer> aiServerIds = BizappUtils.buildAIResourceIds(aiVal.getAIServerValues(), false);

        aiBoss.processQueue(sessionInt, aiPlatformIds, aiServerIds, aiIpIds, AIQueueConstants.Q_DECISION_APPROVE);
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

    private List<Integer> buildResources(HttpServletRequest request, int comparator, String resourceType) {
        List<Integer> ret = new ArrayList<Integer>();
        Enumeration values = request.getParameterNames();
        while (values.hasMoreElements()) {
            String name = (String) values.nextElement();
            String value = request.getParameter(name);
            int index = name.indexOf(':');
            if (index > 0 && !(name.indexOf(resourceType) < 0)) {
                if (Integer.parseInt(value) == comparator) {
                    ret.add(new Integer(name.substring(index + 1)));
                }
            }

        }
        return ret;
    }

}

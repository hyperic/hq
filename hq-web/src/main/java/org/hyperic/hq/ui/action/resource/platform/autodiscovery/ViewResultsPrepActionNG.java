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
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.AIAppdefResourceValue;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;


@Component("viewResultsPrepActionNG")
public class ViewResultsPrepActionNG extends BaseActionNG implements ViewPreparer {
	
	private final Log log = LogFactory.getLog(ViewResultsPrepActionNG.class);

	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private AIBoss aiBoss;
	@Resource
	private AIQueueManager aiQueueManager;
	@Resource
	private SessionManager sessionManager;
	
	public void execute(TilesRequestContext tilesContext, AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	        AutoDiscoveryResultsFormNG aForm = new AutoDiscoveryResultsFormNG();
	        
	        ServletContext ctx = ServletActionContext.getServletContext();
	        Integer sessionId = RequestUtils.getSessionId(request);

	        PlatformValue pValue = (PlatformValue) RequestUtils.getResource(request);
	        if (pValue != null) {
	            aForm.setEid(pValue.getEntityId().getAppdefKey());
	        }
	        
	       
	        AIPlatformValue aiVal = (AIPlatformValue) request.getAttribute(Constants.AIPLATFORM_ATTR);
	        
	        String inpStdStatusFilter = request.getParameter("stdStatusFilter");
	        if (inpStdStatusFilter != null && !inpStdStatusFilter.equals("")) {
	        	aForm.setStdStatusFilter(Integer.valueOf(inpStdStatusFilter));
	        }
	        
	        String inpServerTypeFilter = request.getParameter("serverTypeFilter");
	        if (inpServerTypeFilter != null && !inpServerTypeFilter.equals("")) {
	        	aForm.setServerTypeFilter(Integer.valueOf(inpServerTypeFilter));
	        }
	        
	        String inpIpsStatusFilter = request.getParameter("ipsStatusFilter");
	        if (inpIpsStatusFilter != null && !inpIpsStatusFilter.equals("")) {
	        	aForm.setIpsStatusFilter(Integer.valueOf(inpIpsStatusFilter));
	        }

	        AppdefResourceTypeValue[] supportedSTypeFilter;
	        supportedSTypeFilter = BizappUtilsNG.buildSupportedAIServerTypes(ctx, request, aiVal.getPlatformTypeName(), appdefBoss, aiBoss);

	        AppdefResourceTypeValue[] serverTypeFilter = BizappUtilsNG.buildfilteredAIServerTypes(supportedSTypeFilter, aiVal
	            .getAIServerValues());

	        aForm.setServerTypeFilterList(serverTypeFilter);

	        aForm.setAiRid(aiVal.getId());

	        aForm.buildActionOptions(request);

	        List<AIServerValue> newModifiedServers = new PageList<AIServerValue>();
	        AIServerValue[] aiServerVals = aiVal.getAIServerValues();
	        CollectionUtils.addAll(newModifiedServers, aiServerVals);

	        List<AIAppdefResourceValue> filteredNewServers = BizappUtilsNG.filterAIResourcesByStatus(newModifiedServers,
	            aForm.getStdStatusFilter());

	        String name = "";

	        if (aForm.getServerTypeFilter() != null && aForm.getServerTypeFilter().intValue() != -1) {
	            ServerType sTypeVal = appdefBoss.findServerTypeById(sessionId.intValue(), aForm.getServerTypeFilter());
	            name = sTypeVal.getName();
	        }

	        List<AIServerValue> filteredServers2 = BizappUtilsNG.filterAIResourcesByServerType(filteredNewServers, name);

	        List<AIIpValue> newIps = new ArrayList<AIIpValue>();

	        AIIpValue[] aiIpVals = aiVal.getAIIpValues();
	        CollectionUtils.addAll(newIps, aiIpVals);

	        List<AIAppdefResourceValue> filteredIps = BizappUtilsNG.filterAIResourcesByStatus(newIps, aForm
	            .getIpsStatusFilter());

	        List sortedFilteredIps = BizappUtilsNG.sortAIResource(filteredIps);
	        request.setAttribute(Constants.AI_IPS, sortedFilteredIps);
	        request.setAttribute(Constants.AI_SERVERS, filteredServers2);
	        request.setAttribute("aForm", aForm);
			
			
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

}

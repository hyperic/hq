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
 * Created on Mar 3, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.AIAppdefResourceValue;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

public class ViewResultsPrepAction
    extends WorkflowPrepareAction {

    private AppdefBoss appdefBoss;
    private AIBoss aiBoss;

    @Autowired
    public ViewResultsPrepAction(AppdefBoss appdefBoss, AIBoss aiBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.aiBoss = aiBoss;
    }

    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        AutoDiscoveryResultsForm aForm = (AutoDiscoveryResultsForm) form;

        AIPlatformValue aiVal = (AIPlatformValue) request.getAttribute(Constants.AIPLATFORM_ATTR);

        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);

        AppdefResourceTypeValue[] supportedSTypeFilter;
        supportedSTypeFilter = BizappUtils.buildSupportedAIServerTypes(ctx, request, aiVal.getPlatformTypeName(), appdefBoss, aiBoss);

        AppdefResourceTypeValue[] serverTypeFilter = BizappUtils.buildfilteredAIServerTypes(supportedSTypeFilter, aiVal
            .getAIServerValues());

        aForm.setServerTypeFilterList(serverTypeFilter);

        PlatformValue pValue = (PlatformValue) RequestUtils.getResource(request);
        if (pValue != null) {
            aForm.setEid(pValue.getEntityId().getAppdefKey());
        }

        aForm.setAiRid(aiVal.getId());

        aForm.buildActionOptions(request);

        List<AIServerValue> newModifiedServers = new PageList<AIServerValue>();
        AIServerValue[] aiServerVals = aiVal.getAIServerValues();
        CollectionUtils.addAll(newModifiedServers, aiServerVals);

        List<AIAppdefResourceValue> filteredNewServers = BizappUtils.filterAIResourcesByStatus(newModifiedServers,
            aForm.getStdStatusFilter());

        String name = "";

        if (aForm.getServerTypeFilter() != null && aForm.getServerTypeFilter().intValue() != -1) {
            ServerType sTypeVal = appdefBoss.findServerTypeById(sessionId.intValue(), aForm.getServerTypeFilter());
            name = sTypeVal.getName();
        }

        List<AIServerValue> filteredServers2 = BizappUtils.filterAIResourcesByServerType(filteredNewServers, name);

        List<AIIpValue> newIps = new ArrayList<AIIpValue>();

        AIIpValue[] aiIpVals = aiVal.getAIIpValues();
        CollectionUtils.addAll(newIps, aiIpVals);

        List<AIAppdefResourceValue> filteredIps = BizappUtils.filterAIResourcesByStatus(newIps, aForm
            .getIpsStatusFilter());

        List sortedFilteredIps = BizappUtils.sortAIResource(filteredIps);
        request.setAttribute(Constants.AI_IPS, sortedFilteredIps);
        request.setAttribute(Constants.AI_SERVERS, filteredServers2);

        return null;
    }
}

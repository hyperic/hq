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

package org.hyperic.hq.ui.action.portlet.availsummary;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("availSummaryPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {
	private final Log log = LogFactory.getLog(PrepareActionNG.class);

	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	        HttpSession session = request.getSession();
	        WebUser user = RequestUtils.getWebUser(session);
	        Integer sessionId = user.getSessionId();
	        
	        PageList<AppdefResourceValue> resources = new PageList<AppdefResourceValue>();

	        String token = (String) this.request.getAttribute("portletIdentityToken");
	        
	        String resKey = PropertiesFormNG.RESOURCES;
	        String numKey = PropertiesFormNG.NUM_TO_SHOW;
	        String titleKey = PropertiesFormNG.TITLE;

	        if (token != null) {
	            resKey += token;
	            numKey += token;
	            titleKey += token;
	        } else {
	        	token="";
	        }

	        // Clean up session attributes
	        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

	        // We set defaults here rather than in DefaultUserPreferences.properites

	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        ConfigResponse dashPrefs = dashConfig.getConfig();
	        Integer numberToShow = new Integer(dashPrefs.getValue(numKey, "10"));
	        // pForm.setNumberToShow(numberToShow);

	        // pForm.setTitle(dashPrefs.getValue(titleKey, ""));

	        request.setAttribute("numberToShow", numberToShow);
	        request.setAttribute("titleDescription", dashPrefs.getValue(titleKey, ""));
	        request.setAttribute("portletIdentityToken", token);
	        
	        
	        List<AppdefEntityID> resourceList = DashboardUtils.preferencesAsEntityIds(resKey, dashPrefs);
	        AppdefEntityID[] aeids = resourceList.toArray(new AppdefEntityID[resourceList.size()]);

	        PageControl pc = RequestUtils.getPageControl(request);
	        resources = appdefBoss.findByIds(sessionId.intValue(), aeids, pc);
	        request.setAttribute("availSummaryList", resources);
	        setPendingResources(user,dashPrefs,resKey);
	        
	        setValueInSession("currentPortletKey",resKey);
	        setValueInSession("currentPortletToken",token);
	        resetSessionFilter();
	        
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			log.error(ex.getMessage());
		}
	}
	
	private void setPendingResources(WebUser user, ConfigResponse dashPrefs, String favResourcesKey){
		HttpSession session = request.getSession();
		List pendingResourcesIds = (List) session.getAttribute(Constants.PENDING_RESOURCES_SES_ATTR);
        if (pendingResourcesIds == null) {
            log.debug("get avalable resources from user preferences");
            try {   
                pendingResourcesIds = dashPrefs.getPreferenceAsList(favResourcesKey,
                    StringConstants.DASHBOARD_DELIMITER);
            } catch (InvalidOptionException e) {
                // Then we don't have any pending resources
                pendingResourcesIds = new ArrayList(0);
            }
            log.debug("put entire list of pending resources in session");
            session.setAttribute(Constants.PENDING_RESOURCES_SES_ATTR, pendingResourcesIds);
        }
	}
	
	private void resetSessionFilter(){
    	this.removeValueInSession("latestNameFilter");
    	this.removeValueInSession("latestFt");
    	this.removeValueInSession("latestFf");
	}
	

}

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

package org.hyperic.hq.ui.action.portlet.metricviewer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionRedirect;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;


@Component(value = "metricViewerViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewActionNG.class);
	
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private MeasurementBoss measurementBoss;
	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private DashboardManager dashboardManager;
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        
        long ts = System.currentTimeMillis();
        String token;
        try {
            token = RequestUtils.getStringParameter(request, "token");
        } catch (ParameterNotFoundException e) {
            token = null;
        }
        
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        String titleKey = PropertiesFormNG.TITLE;
		// request.setAttribute("titleDescription", dashPrefs.getValue(titleKey, ""));

	}

}

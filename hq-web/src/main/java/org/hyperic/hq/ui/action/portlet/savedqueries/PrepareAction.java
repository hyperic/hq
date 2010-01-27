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

package org.hyperic.hq.ui.action.portlet.savedqueries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *prepairs the list and form for the saved queries properties page.
 */

public class PrepareAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(PrepareAction.class.getName());
    private AuthzBoss authzBoss;
    private DashboardManager dashboardManager;

    @Autowired
    public PrepareAction(AuthzBoss authzBoss, DashboardManager dashboardManager) {
        super();
        this.authzBoss = authzBoss;
        this.dashboardManager = dashboardManager;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.trace("getting saved charts associated with user ");

        WebUser user = RequestUtils.getWebUser(request);

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) request.getSession().getAttribute(
            Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        List<String> chartList = StringUtil.explode(dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS),
            StringConstants.DASHBOARD_DELIMITER);

        ArrayList<KeyValuePair> charts = new ArrayList<KeyValuePair>();

        for (Iterator<String> i = chartList.iterator(); i.hasNext();) {
            StringTokenizer st = new StringTokenizer(i.next(), ",");
            if (st.countTokens() >= 2)
                charts.add(new KeyValuePair(st.nextToken(), st.nextToken()));
        }
        request.setAttribute("charts", charts);
        request.setAttribute("chartsize", String.valueOf(charts.size()));

        return null;
    }
}

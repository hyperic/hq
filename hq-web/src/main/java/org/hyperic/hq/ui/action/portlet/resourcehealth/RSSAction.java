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

package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.BaseRSSAction;
import org.hyperic.hq.ui.action.portlet.RSSFeed;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class RSSAction
    extends BaseRSSAction {

    private MeasurementBoss measurementBoss;

    @Autowired
    public RSSAction(DashboardManager dashboardManager, ConfigBoss configBoss, MeasurementBoss measurementBoss) {
        super(dashboardManager, configBoss);
        this.measurementBoss = measurementBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        RSSFeed feed = getNewRSSFeed(request);

        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.ResourceHealth"));

        // Get the resources health

        String user = getUsername(request);
        List<ResourceDisplaySummary> list = null;
        try {
            // Set the managingEditor
            setManagingEditor(request);

            // Get user preferences
            ConfigResponse preferences = getUserPreferences(request, user);

            String favIds = preferences.getValue(Constants.USERPREF_KEY_FAVORITE_RESOURCES);

            if (favIds != null) {
                List<AppdefEntityID> ids = DashboardUtils.listAsEntityIds(StringUtil.explode(favIds,
                    Constants.DASHBOARD_DELIMITER));
                AppdefEntityID[] arrayIds = new AppdefEntityID[ids.size()];
                arrayIds = ids.toArray(arrayIds);

                list = measurementBoss.findResourcesCurrentHealth(user, arrayIds);
            }
        } catch (Exception e) {
            throw new ServletException("Error finding resource health", e);
        }

        if (list != null) {
            int i = 0;
            for (Iterator<ResourceDisplaySummary> it = list.iterator(); it.hasNext(); i++) {
                ResourceDisplaySummary summary = it.next();
                AppdefEntityID aeid = summary.getEntityId();

                String link = feed.getBaseUrl() + "/Resource.do?eid=" + aeid.getAppdefKey();

                long current = System.currentTimeMillis();

                StringBuffer desc = new StringBuffer("<table><tr><td align=center>");
                if (Boolean.FALSE.equals(summary.getMonitorable()) || summary.getAvailability() == null) {
                    desc.append(res.getMessage("common.value.notavail"));
                } else {
                    UnitNumber avail = new UnitNumber(summary.getAvailability().doubleValue(),
                        UnitsConstants.UNIT_PERCENTAGE);
                    desc.append(
                        res.getMessage("dash.home.ResourceHealth.rss.item.availability", UnitsFormat.format(avail)
                            .toString())).append("</td></tr><tr><td>").append("<img src=\"").append(feed.getBaseUrl())
                        .append("/resource/AvailHealthChart?eid=").append(aeid.getAppdefKey()).append("&tid=").append(
                            summary.getAvailTempl()).append("&user=").append(user).append("&").append(current).append(
                            "\">");
                }

                desc.append("</td></tr></table>");

                feed.addItem(summary.getResourceName(), link, desc.toString(), current);
            }
        }
        request.setAttribute("rssFeed", feed);

        return mapping.findForward(Constants.RSS_URL);
    }
}

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

package org.hyperic.hq.ui.action.portlet.criticalalerts;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.uibeans.DashboardAlertBean;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.BaseRSSAction;
import org.hyperic.hq.ui.action.portlet.RSSFeed;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.hyperic.util.units.DateFormatter.DateSpecifics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class RSSAction extends BaseRSSAction {
    private static final Log log = LogFactory.getLog(RSSAction.class.getName());
    
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        RSSFeed feed = getNewRSSFeed(request);
        
        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.CriticalAlerts"));

        // Get the recent alerts
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss boss = ContextUtils.getEventsBoss(ctx);

        String user = getUsername(request);
        List list;
        try {
            // Set the managingEditor
            setManagingEditor(request);
            
            // Get user preferences
            ConfigResponse preferences = getUserPreferences(request, user);

            int count = Integer.parseInt(preferences
                .getValue(".dashContent.criticalalerts.numberOfAlerts").trim());
            int priority = Integer.parseInt(preferences
                .getValue(".dashContent.criticalalerts.priority").trim());
            long timeRange = Long.parseLong(preferences
                .getValue(".dashContent.criticalalerts.past").trim());

            list = boss.findAlerts(user, count, priority, timeRange, null,
                                   new PageControl());
        } catch (Exception e) {
            throw new ServletException("Error finding recent alerts", e);
        }

        for (Iterator it = list.iterator(); it.hasNext(); ) {
            DashboardAlertBean alert = (DashboardAlertBean) it.next();
            AppdefEntityID aeid = alert.getResource().getEntityId();
            
            String link = feed.getBaseUrl() +
                          "/alerts/Alerts.do?mode=viewAlert&eid=" +
                          aeid.getAppdefKey() + "&a=" + alert.getAlertId();

            DateSpecifics specs = new DateSpecifics();
            specs.setDateFormat(new SimpleDateFormat(res.getMessage(
                Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis")));
            
            FormattedNumber fmtd = UnitsFormat.format(
                    new UnitNumber(alert.getCtime(), UnitsConstants.UNIT_DATE,
                                   UnitsConstants.SCALE_MILLI), 
                                   request.getLocale(), specs);

            String desc;
            if (alert.isFixed()) {
                desc = fmtd.toString() + " " +
                    res.getMessage("parenthesis", res.getMessage(
                                   "resource.common.alert.action.fixed.label"));
            }
            else {
                desc = "<table cellspacing=4><tr>" +
                          "<td>" + fmtd.toString() + "</td>" +
                          "<td><a href='" + feed.getBaseUrl() +
                          "/alerts/Alerts.do?mode=FIXED&a=" +
                          alert.getAlertId() + "'>" +
                          res.getMessage(
                              "resource.common.alert.action.fixed.label") +
                          "</a></td><td><a href='" + feed.getBaseUrl() +
                          "/alerts/Alerts.do?mode=ACKNOWLEDGE&a=" +
                          alert.getAlertId() + "'>" +
                          res.getMessage(
                              "resource.common.alert.action.acknowledge.label")+
                          "</a></td></tr></table>";
            }
            feed.addItem(alert.getResource().getName() + " " +
                         alert.getAlertDefName(), link, desc, alert.getCtime());
        }
        request.setAttribute("rssFeed", feed);
        
        return mapping.findForward(Constants.RSS_URL);
    }
}

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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.BaseRSSAction;
import org.hyperic.hq.ui.action.portlet.RSSFeed;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.hyperic.util.units.DateFormatter.DateSpecifics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriTemplate;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class RSSAction
    extends BaseRSSAction {
    
    private final Log log = LogFactory.getLog(RSSAction.class.getName());

    private EventsBoss eventsBoss;

    private AuthzBoss authzBoss;

    @Autowired
    public RSSAction(DashboardManager dashboardManager, ConfigBoss configBoss, EventsBoss eventsBoss,
                     AuthzBoss authzBoss) {
        super(dashboardManager, configBoss);
        this.eventsBoss = eventsBoss;
        this.authzBoss = authzBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        RSSFeed feed = getNewRSSFeed(request);

        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.CriticalAlerts"));

        String user = getUsername(request);
        List<Escalatable> list;
        try {
            // Set the managingEditor
            setManagingEditor(request);

            // Get user preferences
            ConfigResponse preferences = getUserPreferences(request, user);

            int count = Integer.parseInt(preferences.getValue(".dashContent.criticalalerts.numberOfAlerts").trim());
            int priority = Integer.parseInt(preferences.getValue(".dashContent.criticalalerts.priority").trim());
            long timeRange = Long.parseLong(preferences.getValue(".dashContent.criticalalerts.past").trim());

            list = eventsBoss.findRecentAlerts(user, count, priority, timeRange, null);
        } catch (Exception e) {
            log.warn("Error finding recent alerts", e);
            list = new ArrayList<Escalatable>();
        }

        for (Escalatable alert : list) {
            AlertDefinitionInterface defInfo = alert.getDefinition().getDefinitionInfo();
            AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(defInfo.getResource());

            DateSpecifics specs = new DateSpecifics();
            specs
                .setDateFormat(new SimpleDateFormat(res.getMessage(Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis")));

            FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(alert.getAlertInfo().getTimestamp(),
                UnitsConstants.UNIT_DATE, UnitsConstants.SCALE_MILLI), request.getLocale(), specs);
            UriTemplate uriTemplate = new UriTemplate(feed.getBaseUrl() + "/alerts/Alerts.do?mode={mode}&a={alertId}&eid={entityId}");
            String desc;
            if (alert.getAlertInfo().isFixed()) {
                desc = fmtd.toString() + " " +
                       res.getMessage("parenthesis", res.getMessage("resource.common.alert.action.fixed.label"));
            } else {
                desc = "<table cellspacing=4><tr>" + "<td>" + fmtd.toString() + "</td>" + "<td><a href='" +
                       response.encodeURL(uriTemplate.expand("FIXED", alert.getId(), aeid.getAppdefKey()).toASCIIString()) + 
                       "'>" + res.getMessage("resource.common.alert.action.fixed.label") +
                       "</a></td>";

                if (alert.isAcknowledgeable()) {
                	desc += "<td><a href='" + 
                    	response.encodeURL(uriTemplate.expand("ACKNOWLEDGE", alert.getId(), aeid.getAppdefKey()).toASCIIString()) + "'>" +
                        res.getMessage("resource.common.alert.action.acknowledge.label") + "</a></td></tr></table>";

                }
            }

            AuthzSubject subject = authzBoss.getCurrentSubject(user);
            AppdefEntityValue resource = new AppdefEntityValue(aeid, subject);

            String link = response.encodeURL(uriTemplate.expand("viewAlert", alert.getId(), aeid.getAppdefKey()).toASCIIString());
            
            feed.addItem(resource.getName() + " " + defInfo.getName(), link, desc, alert.getAlertInfo().getTimestamp());
        }
        request.setAttribute("rssFeed", feed);

        return mapping.findForward(Constants.RSS_URL);
    }
}

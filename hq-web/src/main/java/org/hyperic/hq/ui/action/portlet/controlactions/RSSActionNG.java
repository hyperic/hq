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

package org.hyperic.hq.ui.action.portlet.controlactions;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.BaseRSSActionNG;
import org.hyperic.hq.ui.action.portlet.RSSFeed;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.DateFormatter.DateSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */

@Component("controlActionsRSSActionNG")
public class RSSActionNG
    extends BaseRSSActionNG {

	@Resource
    private ControlBoss controlBoss;


    public String execute() throws Exception {
        RSSFeed feed = getNewRSSFeed(this.request);

        // Set title
        feed.setTitle(getText("dash.home.Subhead.Recent"));

        // Get the resources health

        String user = getUsername(request);
        List<ControlHistory> list = null;
        try {
            // Set the managingEditor
            setManagingEditor(request);

            // Get user preferences
            ConfigResponse preferences = getUserPreferences(request, user);

            String rowsStr = preferences.getValue(".ng.dashContent.controlActions.lastCompleted");

            int rows = 10;
            if (rowsStr != null) {
                rows = Integer.parseInt(rowsStr);
            }

            String pastStr = preferences.getValue(".ng.dashContent.controlActions.past");
            long past = 604800000;
            if (pastStr != null) {
                past = Long.parseLong(pastStr);
            }
            list = controlBoss.getRecentControlActions(user, rows, past);
        } catch (Exception e) {
            throw new ServletException("Error finding recent control actions", e);
        }

        if (list != null) {
            int i = 0;
            for (Iterator<ControlHistory> it = list.iterator(); it.hasNext(); i++) {
                ControlHistory hist = it.next();
                AppdefEntityID aeid = new AppdefEntityID(hist.getEntityType().intValue(), hist.getEntityId());

                String link = feed.getBaseUrl() + "/ResourceControlHistory.do?eid=" + aeid.getAppdefKey();

                long current = System.currentTimeMillis();

                DateSpecifics specs = new DateSpecifics();
                specs.setDateFormat(new SimpleDateFormat(getText(Constants.UNIT_FORMAT_PREFIX_KEY +
                                                                        "epoch-millis")));

                FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(hist.getStartTime(), UnitsConstants.UNIT_DATE,
                    UnitsConstants.SCALE_MILLI), request.getLocale(), specs);

                StringBuffer desc = new StringBuffer(fmtd.toString());
                desc.append(" ").append(getText("common.label.Dash")).append(" ").append(hist.getAction());

                feed.addItem(hist.getEntityName(), link, desc.toString(), current);
            }
        }
        request.setAttribute("rssFeed", feed);

        return SUCCESS;
    }
}

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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.BaseRSSAction;
import org.hyperic.hq.ui.action.portlet.RSSFeed;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.hyperic.util.units.DateFormatter.DateSpecifics;

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
        feed.setTitle(res.getMessage("dash.home.Subhead.Recent"));

        // Get the resources health
        ServletContext ctx = getServlet().getServletContext();
        ControlBoss boss = ContextUtils.getControlBoss(ctx);

        String user = getUsername(request);
        List list = null;
        try {
            // Set the managingEditor
            setManagingEditor(request);
            
            // Get user preferences
            ConfigResponse preferences = getUserPreferences(request, user);

            String rowsStr =
                preferences.getValue(".dashContent.controlActions.lastCompleted");
            
            int rows = 10;
            if (rowsStr != null) {
                rows = Integer.parseInt(rowsStr);
            }
            
            String pastStr =
                preferences.getValue(".dashContent.controlActions.past");
            long past = 604800000;
            if (pastStr != null) {
                past = Long.parseLong(pastStr);
            }
            list = boss.getRecentControlActions(user, rows, past);
        } catch (Exception e) {
            throw new ServletException("Error finding recent control actions",
                                       e);
        }

        if (list != null) {
            int i = 0;
            for (Iterator it = list.iterator(); it.hasNext(); i++) {
                ControlHistory hist = (ControlHistory) it.next();
                AppdefEntityID aeid =
                    new AppdefEntityID(hist.getEntityType().intValue(),
                                       hist.getEntityId());
                
                String link = feed.getBaseUrl() +
                              "/ResourceControlHistory.do?eid=" +
                              aeid.getAppdefKey();

                long current = System.currentTimeMillis();

                DateSpecifics specs = new DateSpecifics();
                specs.setDateFormat(new SimpleDateFormat(res.getMessage(
                    Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis")));
                
                FormattedNumber fmtd = UnitsFormat.format(
                        new UnitNumber(hist.getStartTime(),
                                       UnitsConstants.UNIT_DATE,
                                       UnitsConstants.SCALE_MILLI), 
                                       request.getLocale(), specs);

                StringBuffer desc = new StringBuffer(fmtd.toString());
                desc.append(" ")
                    .append(res.getMessage("common.label.Dash"))
                    .append(" ")
                    .append(hist.getAction());
                
                feed.addItem(hist.getEntityName(), link, desc.toString(),
                             current);
            }
        }
        request.setAttribute("rssFeed", feed);

        return mapping.findForward(Constants.RSS_URL);
    }
}

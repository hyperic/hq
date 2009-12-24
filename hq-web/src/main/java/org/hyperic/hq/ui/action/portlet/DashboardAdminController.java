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

package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseDispatchAction;

/**
 * A <code>Controller</code> that sets up dashboard portlets
 */
public class DashboardAdminController
    extends BaseDispatchAction {

    protected final Log log = LogFactory.getLog(DashboardAdminController.class.getName());

    private ActionForward setPortal(HttpServletRequest request, String title, String content) {
        Portal portal = Portal.createPortal(title, content);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward savedQueries(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.SQ", ".dashContent.admin.savedQueries");
    }

    public ActionForward resourceHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.RH", ".dashContent.admin.resourceHealth");
    }

    public ActionForward recentlyApproved(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.RA", ".dashContent.admin.recentlyApproved");
    }

    public ActionForward criticalAlerts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.A", ".dashContent.admin.criticalAlerts");
    }

    public ActionForward summaryCounts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.SC", ".dashContent.admin.summaryCounts");
    }

    public ActionForward autoDiscovery(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.AD", ".dashContent.admin.autoDiscovery");
    }

    public ActionForward resourceHealthAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.RH.addResources",
            ".dashContent.admin.resourcehealth.addResources");
    }

    public ActionForward criticalAlertsAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.A.addResources",
            ".dashContent.admin.criticalAlerts.addResources");
    }

    public ActionForward changeLayout(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.PL", ".dashContent.admin.changeLayout");
    }

    public ActionForward controlActions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.CA", ".dashContent.admin.controlActions");
    }

    public ActionForward availSummary(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.AS", ".dashContent.admin.availSummary");
    }

    public ActionForward availSummaryAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.AS.addResources",
            ".dashContent.admin.availSummary.addResources");
    }

    public ActionForward metricViewer(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.MV", ".dashContent.admin.metricViewer");
    }

    public ActionForward metricViewerAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        return setPortal(request, "dash.settings.PageTitle.MV.addResources",
            ".dashContent.admin.metricViewer.addResources");
    }
}
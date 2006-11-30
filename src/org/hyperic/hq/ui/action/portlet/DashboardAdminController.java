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

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseDispatchAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * A <code>Controller</code> that sets up dashboard portlets
 */
public class DashboardAdminController extends BaseDispatchAction {

    protected static Log log = LogFactory.
        getLog(DashboardAdminController.class.getName());
    
    private static Properties keyMethodMap = new Properties();
    static {
        keyMethodMap.setProperty("savedQueries",     "savedQueries");
        keyMethodMap.setProperty("resourceHealth",   "resourceHealth");
        keyMethodMap.setProperty("recentlyApproved", "recentlyApproved");
        keyMethodMap.setProperty("criticalAlerts",   "criticalAlerts");
        keyMethodMap.setProperty("summaryCounts",    "summaryCounts");
        keyMethodMap.setProperty("autoDiscovery",    "autoDiscovery");
        keyMethodMap.setProperty("changeLayout",     "changeLayout");
        keyMethodMap.setProperty("controlActions",   "controlActions");
        keyMethodMap.setProperty("rsrcHealthAddResources",
                                 "rsrcHealthAddResources");
        keyMethodMap.setProperty("crtAlertsAddResources",
                                 "crtAlertsAddResources");
        keyMethodMap.setProperty("availSummary", "availSummary");
        keyMethodMap.setProperty("availSummaryAddResources",
                                 "availSummaryAddResources");
        keyMethodMap.setProperty("metricViewer", "metricViewer");
        keyMethodMap.setProperty("metricViewerAddResources",
                                 "metricViewerAddResources");
    }
    
    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }
             
    public ActionForward savedQueries(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.SQ",
                                ".dashContent.admin.savedQueries");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }

    public ActionForward resourceHealth(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.RH",
                                ".dashContent.admin.resourceHealth");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }

    public ActionForward recentlyApproved(ActionMapping mapping,
                                          ActionForm form,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.RA",
                                ".dashContent.admin.recentlyApproved");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }

    public ActionForward criticalAlerts(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.A",
                                ".dashContent.admin.criticalAlerts");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
    public ActionForward summaryCounts(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.SC",
                                ".dashContent.admin.summaryCounts");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
    
    public ActionForward autoDiscovery(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.AD",
                                ".dashContent.admin.autoDiscovery");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
    
    public ActionForward rsrcHealthAddResources(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.RH.addResources",
                                ".dashContent.admin.resourcehealth.addResources");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
        
    public ActionForward crtAlertsAddResources(ActionMapping mapping,
                                               ActionForm form,
                                               HttpServletRequest request,
                                               HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.A.addResources",
                                ".dashContent.admin.criticalAlerts.addResources");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
    
    public ActionForward changeLayout(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {
        
        Portal portal = 
            Portal.createPortal("dash.settings.PageTitle.PL",
                                ".dashContent.admin.changeLayout");
        
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }

    public ActionForward controlActions(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception
    {
        Portal portal =
            Portal.createPortal("dash.settings.PageTitle.CA",
                               ".dashContent.admin.controlActions");
        portal.setDialog(true);
            
        request.setAttribute(Constants.PORTAL_KEY, portal);
            
        return null;
    }

    public ActionForward availSummary(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception
    {
        Portal portal =
            Portal.createPortal("dash.settings.PageTitle.AS",
                                ".dashContent.admin.availSummary");
        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward availSummaryAddResources(ActionMapping mapping,
                                                  ActionForm form,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response)
        throws Exception {

        Portal portal =
            Portal.createPortal("dash.settings.PageTitle.A.addResources",
                                ".dashContent.admin.availSummary.addResources");
        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward metricViewer(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception
    {
        Portal portal =
            Portal.createPortal("dash.settings.PageTitle.MV",
                                ".dashContent.admin.metricViewer");
        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;

    }

    public ActionForward metricViewerAddResources(ActionMapping mapping,
                                                  ActionForm form,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response)
        throws Exception
    {
        Portal portal =
            Portal.createPortal("dash.settings.PageTitle.A.addResources",
                                ".dashContent.admin.metricViewer.addResources");
        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }
}
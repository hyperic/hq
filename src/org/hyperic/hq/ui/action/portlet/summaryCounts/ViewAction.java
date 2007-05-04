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

package org.hyperic.hq.ui.action.portlet.summaryCounts;

import java.io.IOException;
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
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.hyperic.util.StringUtil;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;

import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.util.timer.StopWatch;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class ViewAction extends TilesAction {
    // --------------------------------------------------------- Public Methods
    
    
   public ActionForward execute(ComponentContext context,
                            ActionMapping mapping,
                            ActionForm form,
                            HttpServletRequest request,
                            HttpServletResponse response)
   throws Exception {
        StopWatch timer = new StopWatch();
        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);            
        WebUser user = (WebUser) request.getSession().getAttribute( Constants.WEBUSER_SES_ATTR );
        AppdefInventorySummary summary = appdefBoss.getInventorySummary( 
                                            user.getSessionId().intValue(), true );

        context.putAttribute("summary", summary);            

        //get all the displayed subtypes
        context.putAttribute("application", new Boolean( user.getPreference(".dashContent.summaryCounts.application") ) );
        context.putAttribute("applicationTypes", StringUtil.explode( user.getPreference(".dashContent.summaryCounts.applicationTypes"), "," ) );

        context.putAttribute("platform", new Boolean( user.getPreference(".dashContent.summaryCounts.platform") ) ); 
        context.putAttribute("platformTypes", StringUtil.explode( user.getPreference(".dashContent.summaryCounts.platformTypes"), "," ) );                        

        context.putAttribute("server", new Boolean( user.getPreference(".dashContent.summaryCounts.server") ) );
        context.putAttribute("serverTypes", StringUtil.explode( user.getPreference(".dashContent.summaryCounts.serverTypes"), "," ) );

        context.putAttribute("service", new Boolean( user.getPreference(".dashContent.summaryCounts.service") ) );
        context.putAttribute("serviceTypes", StringUtil.explode( user.getPreference(".dashContent.summaryCounts.serviceTypes"), "," ) );            

        context.putAttribute("cluster", new Boolean( user.getPreference(".dashContent.summaryCounts.group.cluster" ) ) );
        context.putAttribute("clusterTypes", StringUtil.explode( user.getPreference(".dashContent.summaryCounts.group.clusterTypes"), "," ) );

        context.putAttribute("groupMixed", new Boolean( user.getPreference(".dashContent.summaryCounts.group.mixed") ) );
        context.putAttribute("groupGroups", new Boolean( user.getPreference(".dashContent.summaryCounts.group.groups") ) );
        context.putAttribute("groupPlatServerService", new Boolean( user.getPreference(".dashContent.summaryCounts.group.plat.server.service") ) );
        context.putAttribute("groupApplication", new Boolean( user.getPreference(".dashContent.summaryCounts.group.application") ) );

        timingLog.trace("SummaryCounts - timing ["+timer.toString()+"]");
        return null;
    }
    
}

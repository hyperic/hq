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

package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniResourceTree;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniPlatformNode;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.WebUser;

import org.hyperic.util.timer.StopWatch;
import org.hyperic.util.pager.PageList;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class ViewAction extends TilesAction {

    private static String RANGE = ".dashContent.recentlyApproved.range";
    private static String EXPANDED_PLATFORMS 
        = ".dashContent.recentlyApproved.expandedPlatforms";

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        
        Log log = LogFactory.getLog(ViewAction.class.getName());
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        
        WebUser user = (WebUser) request.getSession().
            getAttribute(Constants.WEBUSER_SES_ATTR);
        int sessionId = user.getSessionId().intValue();

        try {
            // Based on the user preference, generate a timestamp of the 
            // oldest resouce to display.
            long range = Long.parseLong(user.getPreference(RANGE));
            long ts = System.currentTimeMillis() - range * 3600 * 1000;

            StopWatch watch = new StopWatch();
            MiniResourceTree tree;

            // Generate the resource tree based on the user's configured
            // time range.
            watch.markTimeBegin("ResourceTreeGeneration");
            tree = appdefBoss.getMiniResourceTreeByCTime(sessionId, ts);
            watch.markTimeEnd("ResourceTreeGeneration");

            // Set the show servers flag on all expanded platforms.
            // Find the list of expanded platforms for this user and make
            // it available to the jsp.
            List expandedPlatforms =
                user.getPreferenceAsList(EXPANDED_PLATFORMS);

            for (Iterator i = tree.getPlatformIterator(); i.hasNext();) {
                MiniPlatformNode pNode = (MiniPlatformNode)i.next();
                if (expandedPlatforms.contains(pNode.getId().toString())) {
                    pNode.setShowServers(true);
                }
            }

            // Make the list available to the jsp.
            context.putAttribute("recentlyApproved", tree.getPlatforms());

            log.debug(watch);
        } catch (Exception e) {
            // Most likely a permissions error.  Return an empty list
            List emptyList = new ArrayList();
            context.putAttribute("recentlyApproved", emptyList);
            log.error("Error generating tree: " + e.getMessage());
        }
       
        return null;
    }
}

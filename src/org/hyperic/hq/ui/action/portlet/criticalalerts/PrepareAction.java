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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * prepairs the list and form for the saved queries properties page.
 */
public class PrepareAction extends TilesAction {

    // --------------------------------------------------------- Public Methods
    

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(PrepareAction.class.getName());
        PropertiesForm pForm = (PropertiesForm) form;
        
        log.trace("getting resource helath info for user ");

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        PageControl pc = RequestUtils.getPageControl(request);

        HttpSession session = request.getSession();
        Integer sessionId = RequestUtils.getSessionId(request);
        WebUser user =
            (WebUser) session.getAttribute( Constants.WEBUSER_SES_ATTR );            
        PageList resources = new PageList();
        String key = ".dashContent.criticalalerts.resources";

        // This quarantees that the session dosen't contain any resources it
        // shouldn't
        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        // set all the form properties
        pForm.setDisplayOnDash(true);

        Integer numberOfAlerts =
            new Integer(user.getPreference(PropertiesForm.ALERT_NUMBER));
        long past = Long.parseLong(user.getPreference(PropertiesForm.PAST));
        String prioritity = user.getPreference(PropertiesForm.PRIORITY);
        String selectedOrAll =
            user.getPreference(PropertiesForm.SELECTED_OR_ALL);

        DashboardUtils.verifyResources(key, ctx, user);

        pForm.setNumberOfAlerts(numberOfAlerts);
        pForm.setPast(past);
        pForm.setPriority(prioritity);
        pForm.setSelectedOrAll(selectedOrAll);

        List resourceList =
            user.getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);

        Iterator i = resourceList.iterator();

        while( i.hasNext() ){

            ArrayList resourceIds =
                (ArrayList) StringUtil.explode((String) i.next(), ":");

            Iterator j = resourceIds.iterator();
            int type = Integer.parseInt( (String) j.next() );
            int id = Integer.parseInt( (String) j.next() );

            AppdefEntityID entityID = new AppdefEntityID(type, id);               
            AppdefResourceValue resource = appdefBoss.findById(sessionId.intValue(), entityID);
            resources.add(resource);  
        }

        resources.setTotalSize(resources.size());
        request.setAttribute("criticalAlertsList", resources);
        request.setAttribute("criticalAlertsTotalSize",
                             new Integer(resources.getTotalSize()));                                 

        
        return null;
    }
}

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

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

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
       EventsBoss eventBoss = ContextUtils.getEventsBoss(ctx);
       WebUser user = (WebUser) request.getSession().getAttribute(
                    Constants.WEBUSER_SES_ATTR);
       String key = ".dashContent.criticalalerts.resources";
        
       List entityIds =  DashboardUtils.preferencesAsEntityIds(key, user);                                    
        
       AppdefEntityID[] arrayIds =  new AppdefEntityID[entityIds.size()];
        
       int h = 0;
       for (Iterator i = entityIds.iterator(); i.hasNext(); h++) {
           arrayIds[h] = (AppdefEntityID) i.next();
       }
        
       int count = new Integer(user.getPreference(
                PropertiesForm.ALERT_NUMBER)).intValue();
       int priority =
           new Integer(user.getPreference(PropertiesForm.PRIORITY)).intValue();
       long timeRange =
           new Long(user.getPreference(PropertiesForm.PAST)).longValue();
       boolean all =
           "all".equals(user.getPreference(PropertiesForm.SELECTED_OR_ALL)); 
        
       int sessionID = user.getSessionId().intValue();
       PageControl pc = new PageControl();
       pc.setPagesize(10);
        
       if(all)
           arrayIds = null;
        
       // need to test if findAlerts is working correclty.
       // this may acutally need to call findUserAlerts
       PageList criticalAlerts =
           eventBoss.findAlerts(sessionID, count, priority, timeRange, arrayIds,
                                pc);
        
       context.putAttribute("criticalAlerts", criticalAlerts);   
       timingLog.trace("ViewCriticalAlerts - timing ["+timer.toString()+"]");
        
       return null;
   }
}

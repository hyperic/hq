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

import java.util.Iterator;

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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.shared.ControlScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.beans.DashboardControlBean;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class ViewAction extends TilesAction {
    // --------------------------------------------------------- Public Methods
    
    
   public ActionForward execute(ComponentContext context, ActionMapping mapping,
                                ActionForm form, HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        StopWatch timer = new StopWatch();
        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");
        ServletContext ctx = getServlet().getServletContext();
        ControlBoss boss = ContextUtils.getControlBoss(ctx);        
        WebUser user = (WebUser) request.getSession().getAttribute( 
                                                   Constants.WEBUSER_SES_ATTR );
        int sessionId = user.getSessionId().intValue();

        Boolean lastCompleted = Boolean.valueOf(user
                .getPreference(".dashContent.controlActions.useLastCompleted",
                               Boolean.TRUE.toString()));
        context.putAttribute("displayLastCompleted", lastCompleted);

        Boolean mostFrequent = new Boolean(user
                .getPreference(".dashContent.controlActions.useMostFrequent",
                               Boolean.FALSE.toString()));
        context.putAttribute("displayMostFrequent", mostFrequent);

        Boolean nextScheduled = new Boolean(user
                .getPreference(".dashContent.controlActions.useNextScheduled",
                               Boolean.TRUE.toString()));
        context.putAttribute("displayNextScheduled", nextScheduled);

        if (lastCompleted.booleanValue()) {
            int rows = Integer.parseInt(user
                    .getPreference(".dashContent.controlActions.lastCompleted",
                                   "5"));
            long past = Long.parseLong(user
                    .getPreference(".dashContent.controlActions.past",
                                   "604800000"));
            PageList pageList = boss.getRecentControlActions(sessionId, rows,
                                                             past);
            context.putAttribute("lastCompleted", pageList);
        }
        
        if (nextScheduled.booleanValue()) {
            int rows = Integer.parseInt(user
                    .getPreference(".dashContent.controlActions.nextScheduled",
                                   "5"));                                 
            PageList pageList = boss.getPendingControlActions(sessionId, rows);                
            AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);  

            PageList pendingList = new PageList();
            pendingList.setTotalSize(pageList.getTotalSize());

            for(Iterator i = pageList.iterator(); i.hasNext();){
                ControlScheduleValue control = (ControlScheduleValue) i.next();
                DashboardControlBean bean = new DashboardControlBean();     
                try{
                    AppdefEntityID entity =
                        new AppdefEntityID(control.getEntityType().intValue(),
                                           control.getEntityId().intValue() );                    
                    bean.setResource( appdefBoss.findById(sessionId,  entity) );
                    bean.setControl(control);                    
                    pendingList.add(bean);
                }catch(NullPointerException e){
                  //ignore the error don't add it to the page this is 
                  //added as a result of bug #7596
                }
            }

            context.putAttribute("nextScheduled", pendingList);                
        }
        
        if (mostFrequent.booleanValue()) {  
            int size = Integer.parseInt( 
                user.getPreference(".dashContent.controlActions.mostFrequent"));
            PageList pageList = boss.getOnDemandControlFrequency(sessionId, 
                                                                 size);                
            context.putAttribute("mostFrequent", pageList);                
        }                        
        timingLog.trace("ViewControl- timing ["+timer.toString()+"]");
        return null;
    }
    
}

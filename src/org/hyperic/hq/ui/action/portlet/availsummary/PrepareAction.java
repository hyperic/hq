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

package org.hyperic.hq.ui.action.portlet.availsummary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.StringUtil;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class PrepareAction extends TilesAction {

    private Log _log = LogFactory.getLog(PrepareAction.class);

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
         HttpSession session = request.getSession();
        Integer sessionId = RequestUtils.getSessionId(request);
        WebUser user =
            (WebUser)session.getAttribute(Constants.WEBUSER_SES_ATTR);
        String key = ".dashContent.availsummary.resources";
        PropertiesForm pForm = (PropertiesForm) form;
        PageList resources = new PageList();

        pForm.setDisplayOnDash(true);

        Integer numberToShow =
            new Integer(user.getPreference(PropertiesForm.NUM_TO_SHOW));

        pForm.setNumberToShow(numberToShow);
        
        List resourceList =
            user.getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);

        Iterator i = resourceList.iterator();

        while(i.hasNext()) {

            ArrayList resourceIds =
                (ArrayList) StringUtil.explode((String) i.next(), ":");

            Iterator j = resourceIds.iterator();
            int type = Integer.parseInt( (String) j.next() );
            int id = Integer.parseInt( (String) j.next() );

            AppdefEntityID entityID = new AppdefEntityID(type, id);
            AppdefResourceValue resource =
                appdefBoss.findById(sessionId.intValue(), entityID);
            resources.add(resource);
        }

        _log.info("Loaded " + resources.size() + " items.");

        resources.setTotalSize(resources.size());
        request.setAttribute("availSummaryList", resources);
        request.setAttribute("availSummaryTotalSize",
                             new Integer(resources.getTotalSize()));

        return null;
    }
}

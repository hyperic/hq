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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.MessageResources;

public class ViewGroupAction extends TilesAction {

    Log log = LogFactory.getLog(ViewGroupAction.class.getName());

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        int sessionId = RequestUtils.getSessionIdInt(request);
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        PageControl pc = RequestUtils.getPageControl(request,"ps",
                                                     "pn","so","sc");
        AppdefGroupValue group =
            (AppdefGroupValue) RequestUtils.getResource(request);

        if (group == null) {
            RequestUtils.setError(request,
                "resource.group.inventory.error.GroupNotFound");
            return null;
        }

        List appdefValues =
            BizappUtils.buildGroupResources(boss, sessionId, group, pc);

        if (group.getGroupType() ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP ||
            group.getGroupType() ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS)
        {
            Map typeMap =
                AppdefResourceValue.getResourceTypeCountMap(appdefValues);
            request.setAttribute(Constants.RESOURCE_TYPE_MAP_ATTR, typeMap);
        }

        request.setAttribute(Constants.APPDEF_ENTRIES_ATTR, appdefValues);

        Locale locale = getLocale(request);
        MessageResources res = getResources(request);

        RemoveGroupResourcesForm rmGroupForm = new RemoveGroupResourcesForm();
        int ps = RequestUtils.getPageSize(request, "ps");
        rmGroupForm.setPs(new Integer(ps));

        request.setAttribute(Constants.GROUP_REMOVE_MEMBERS_FORM_ATTR,
                             rmGroupForm);
        
        // set the group type label
        List groupLabels = BizappUtils.buildGroupTypes(request);

        String groupType = getGroupTypeLabel(group, groupLabels, res, locale);
        request.setAttribute(Constants.GROUP_TYPE_LABEL, groupType);
            
        return null;
    }
    
    /**
     * @return a group type label from the list of group labels
     */
    public static String getGroupTypeLabel(AppdefGroupValue group, 
                                           List groupLabels,
                                           MessageResources res,
                                           Locale locale)
    {
        Iterator gIterator = groupLabels.iterator();
        
        while (gIterator.hasNext()) {
            Map item = (Map)gIterator.next();
            Integer groupType = (Integer)item.get("value");
            if (groupType.intValue() == group.getGroupType())
                return (String)item.get("label");
        }
        
        return "";
    }
}

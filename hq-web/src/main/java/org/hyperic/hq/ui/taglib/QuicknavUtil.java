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

package org.hyperic.hq.ui.taglib;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.UIUtils;

public class QuicknavUtil {

    public final static String ICON_BORDER = "0";
    public final static String ICON_HEIGHT = "11";
    public final static String ICON_WIDTH = "11";
    public final static String ICON_SRC_LOCKED = "/images/icon_hub_locked.gif";
    public final static String ICON_HREF_A = "/alerts/Config.do?mode=list";
    public final static String ICON_SRC_A = "/images/icon_hub_a.gif";
    public final static String ICON_HREF_C = "/Control.do?mode=view";
    public final static String ICON_SRC_C = "/images/icon_hub_c.gif";
    public final static String ICON_HREF_I = "/Inventory.do?mode=view";
    public final static String ICON_SRC_I = "/images/icon_hub_i.gif";
    public final static String ICON_HREF_M = "/monitor/Visibility.do?mode=currentHealth";
    public final static String ICON_SRC_M = "/images/icon_hub_m.gif";

    public static String getNA() {
        return "";
    }

    public static boolean isMonitorable(AppdefResourceValue rv) {
        if (rv.getEntityId().isGroup()) {
            AppdefGroupValue grp = (AppdefGroupValue) rv;
            if (grp.isGroupAdhoc() || grp.isDynamicGroup()) {
                return false;
            }
        }
    
        return true;
    }

    public static boolean isAlertable(AppdefResourceValue rv, PageContext ctx) {
    	UIUtils uiUtils = (UIUtils) ProductProperties.getPropertyInstance("hyperic.hq.ui.utils");
    	
    	if (uiUtils == null) {
    		return false;
    	}
    	
    	return uiUtils.isResourceAlertable(rv);
    }

    public static boolean canControl(AppdefResourceValue rv,
                                     PageContext context) {
        List perms = (List) context.getRequest()
            .getAttribute(Constants.ALL_RESOURCES_CONTROLLABLE);
    
        // if no perms, assume nothing is controllable
        if (perms == null) {
            return false;
        }
    
        return perms.contains(rv.getEntityId());
    }

    public static void parameterizeUrl(AppdefResourceValue rv,
                                       StringBuffer buf) {
        buf.append(Constants.ENTITY_ID_PARAM)
           .append("=")
           .append(rv.getEntityId().getType())
           .append(":")
           .append(rv.getId());
    }

    public static void makeLinkedIconWithRef(AppdefResourceValue rv,
                                       StringBuffer buf,
                                       String href,
                                       String src, PageContext context) {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
    
        buf.append("<a href=\"")
           .append(req.getContextPath())
           .append(href)
           .append("&");
        QuicknavUtil.parameterizeUrl(rv, buf);
        buf.append("\">");
        
        buf.append("<img src=\"")
           .append(req.getContextPath())
           .append(src);
        buf.append("\" width=\"")
           .append(QuicknavUtil.ICON_WIDTH)
           .append("\" height=\"")
           .append(QuicknavUtil.ICON_HEIGHT)
           .append("\" alt=\"\" border=\"")
           .append(QuicknavUtil.ICON_BORDER)
           .append("\">");
        
        buf.append("</a>\n");
    }

    public static void makeLinkedIcon(AppdefResourceValue rv,
                                StringBuffer buf,
                                String href,
                                String src, PageContext context) {
        String full = "/resource/" + rv.getEntityId().getTypeName() + href;
        makeLinkedIconWithRef(rv, buf, full, src, context);
    }

    public static void makeLockedIcon(StringBuffer buf, PageContext context) {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
    
        buf.append("<img src=\"")
           .append(req.getContextPath())
           .append(QuicknavUtil.ICON_SRC_LOCKED)
           .append("\" width=\"")
           .append(QuicknavUtil.ICON_WIDTH)
           .append("\" height=\"")
           .append(QuicknavUtil.ICON_HEIGHT)
           .append("\" alt=\"\" border=\"")
           .append(QuicknavUtil.ICON_BORDER)
           .append("\">\n");
    }

    public static String getOutput(AppdefResourceValue rv, PageContext context)
        throws Exception {
        StringBuffer buf = new StringBuffer();
    
        if (QuicknavUtil.isMonitorable(rv)) {
            QuicknavUtil.makeLinkedIcon(rv, buf, QuicknavUtil.ICON_HREF_M,
                           QuicknavUtil.ICON_SRC_M, context);
        }
    
        QuicknavUtil.makeLinkedIcon(rv, buf, QuicknavUtil.ICON_HREF_I,
                       QuicknavUtil.ICON_SRC_I, context);
    
        if (QuicknavUtil.isAlertable(rv, context)) {
            QuicknavUtil.makeLinkedIconWithRef(rv, buf,
                                               QuicknavUtil.ICON_HREF_A,
                                               QuicknavUtil.ICON_SRC_A,
                                               context);
        }

        /*
        if (QuicknavUtil.isControllable(rv, context)) {
            boolean skip = false;
    
            // Skip if group has no members
            if (rv instanceof AppdefGroupValue) {
                skip = ((AppdefGroupValue) rv).getTotalSize() == 0;
            }
            
            if (!skip) {
                if (!QuicknavUtil.canControl(rv, context)) {
                    QuicknavUtil.makeLockedIcon(buf, context);
                } else {
                    QuicknavUtil.makeLinkedIcon(rv, buf,
                                                QuicknavUtil.ICON_HREF_C,
                                                QuicknavUtil.ICON_SRC_C,
                                                context);
                }
            }
        }
        */
    
        return buf.toString();
    }

}

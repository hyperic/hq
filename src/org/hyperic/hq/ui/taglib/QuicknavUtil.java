package org.hyperic.hq.ui.taglib;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

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
        if (rv.getEntityId().getType() ==
            AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            AppdefGroupValue grp = (AppdefGroupValue) rv;
            if (grp.isGroupAdhoc()) {
                return false;
            }
        }
    
        return true;
    }

    public static boolean isAlertable(AppdefResourceValue rv) {
        switch (rv.getEntityId().getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isControllable(AppdefResourceValue rv, PageContext context)
        throws Exception {
        HttpServletRequest request =
            (HttpServletRequest)context.getRequest();
        ServletContext ctx = context.getServletContext();
        ControlBoss boss = ContextUtils.getControlBoss(ctx);
        int sessionId = RequestUtils.getSessionId(request).intValue();
        
        return boss.isControlSupported(sessionId, rv);
    }

    public static boolean canControl(AppdefResourceValue rv, PageContext context) {
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
           .append(src)
           .append("?");
        QuicknavUtil.parameterizeUrl(rv, buf);
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
    
        if (QuicknavUtil.isControllable(rv, context)) {
            boolean skip = false;
    
            // Skip if group has no members
            if (rv instanceof AppdefGroupValue) {
                skip = ((AppdefGroupValue) rv).getSize() == 0;
            }
            
            if (!skip) {
                if (!QuicknavUtil.canControl(rv, context)) {
                    QuicknavUtil.makeLockedIcon(buf, context);
                } else {
                    QuicknavUtil.makeLinkedIcon(rv, buf, QuicknavUtil.ICON_HREF_C,
                                   QuicknavUtil.ICON_SRC_C, context);
                }
            }
        }
    
        if (QuicknavUtil.isAlertable(rv)) {
            QuicknavUtil.makeLinkedIconWithRef(rv, buf, QuicknavUtil.ICON_HREF_A,
                                  QuicknavUtil.ICON_SRC_A, context);
        }
    
        return buf.toString();
    }

}

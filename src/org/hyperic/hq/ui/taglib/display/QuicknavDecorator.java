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

package org.hyperic.hq.ui.taglib.display;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.Constants;


/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag
 * that that creates a row of quicknav icons for the resource hub.
 */
public class QuicknavDecorator extends ColumnDecorator implements Tag {

    //----------------------------------------------------static variables

    private final static String ICON_SRC_M      = "/images/icon_hub_m.gif";
    private final static String ICON_HREF_M     =
        "/monitor/Visibility.do?mode=currentHealth";
    private final static String ICON_SRC_I      = "/images/icon_hub_i.gif";
    private final static String ICON_HREF_I     = "/Inventory.do?mode=view";
    private final static String ICON_SRC_A      = "/images/icon_hub_a.gif";
    private final static String ICON_HREF_A     = "/alerts/Config.do?mode=list";
    private final static String ICON_WIDTH      = "11";
    private final static String ICON_HEIGHT     = "11";
    private final static String ICON_BORDER     = "0";

    protected static Log log =
        LogFactory.getLog(QuicknavDecorator.class.getName());

    //----------------------------------------------------instance variables

    private String resource;
    private PageContext context;
    private Tag parent;

    //----------------------------------------------------constructors

    public QuicknavDecorator() {
        super();
    }

    //----------------------------------------------------public methods

    public String getResource() {
        return resource;
    }

    public void setResource(String s) {
        resource = s;
    }

    public String decorate(Object obj) throws Exception{
        AppdefResourceValue rv = null;
        try {
            rv = (AppdefResourceValue) evalAttr("resource", getResource(),
                                                AppdefResourceValue.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + getResource() + " not found");
            return getNA();
        } catch (JspException je) {
            log.debug("can't evaluate resource type [" + getResource() + "]",
                      je);
            return getNA();
        }

        if (rv.getEntityId() == null) {
            return getNA();
        }

        return getOutput(rv);
    }

    private String getOutput(AppdefResourceValue rv) {
        StringBuffer buf = new StringBuffer();

        if (isMonitorable(rv)) {
            makeLinkedIcon(rv, buf, ICON_HREF_M, ICON_SRC_M);
        }

        makeLinkedIcon(rv, buf, ICON_HREF_I, ICON_SRC_I);

        if (isAlertable(rv)) {
            makeLinkedIconWithRef(rv, buf, ICON_HREF_A, ICON_SRC_A);
        }

        return buf.toString();
    }

    private String getNA() {
        return "";
    }

    private void makeLinkedIcon(AppdefResourceValue rv,
                                StringBuffer buf,
                                String href,
                                String src) {
        String full = "/resource/" + rv.getEntityId().getTypeName() + href;
        makeLinkedIconWithRef(rv, buf, full, src);
    }
    
    private void makeLinkedIconWithRef(AppdefResourceValue rv,
                                       StringBuffer buf,
                                       String href,
                                       String src) {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();

        buf.append("<a href=\"")
           .append(req.getContextPath())
           .append(href)
           .append("&");
        parameterizeUrl(rv, buf);
        buf.append("\">");
        
        buf.append("<img src=\"")
           .append(req.getContextPath())
           .append(src)
           .append("?");
        parameterizeUrl(rv, buf);
        buf.append("\" width=\"")
           .append(ICON_WIDTH)
           .append("\" height=\"")
           .append(ICON_HEIGHT)
           .append("\" alt=\"\" border=\"")
           .append(ICON_BORDER)
           .append("\">");
        
        buf.append("</a>\n");
    }

    private void parameterizeUrl(AppdefResourceValue rv,
                                 StringBuffer buf) {
        buf.append(Constants.ENTITY_ID_PARAM)
           .append("=")
           .append(rv.getEntityId().getType())
           .append(":")
           .append(rv.getId());
    }

    private boolean isMonitorable(AppdefResourceValue rv) {
        if (rv.getEntityId().getType() ==
            AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            AppdefGroupValue grp = (AppdefGroupValue) rv;
            if (grp.isGroupAdhoc()) {
                return false;
            }
        }

        return true;
    }

    private boolean isAlertable(AppdefResourceValue rv) {
        switch (rv.getEntityId().getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return true;
            default:
                return false;
        }
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag =
            (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException(
                    "An QuicknavDecorator must be used within a ColumnTag.");
        }
        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    public Tag getParent() {
        return parent;
    }
    public void setParent(Tag t) {
        parent = t;
    }

    public void setPageContext(PageContext pc) {
        context = pc;
    }

    public void release() {
        parent = null;
        context = null;
        resource = null;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("quicknavdecorator", name, value,
                                          type, this, context);
    }
}

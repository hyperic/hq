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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.util.StringUtil;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * a tag to show the inventory hierarchy links from the current resource
 * Such as:
 * Platform > Linux > mazinger.hyperic.net
 */
public class InventoryHierarchyTag extends TagSupport {

    private AppdefEntityID resourceId;
    private AppdefEntityTypeID childTypeId;
    private String resource;
    private String ctype;
    private static final String SEPARATOR = "  >  ";
    private static final String RESHUB_ANCHOR = 
        "<a href=\"@@WAR@@/ResourceHub.do?ff=@@FF@@&ft=@@FT@@\">@@NAME@@</a>";
    private static final String RES_ANCHOR = 
        "<a href=\"@@WAR@@/Resource.do?rid=@@RID@@&type=@@TYPE@@\">@@NAME@@</a>";
    
    public String getResource() {
        return this.resource;
    }
    
    public void setResource(String r){
        this.resource = r;
    }

    public String getCtype() {
        return ctype;
    }
    public void setCtype(String ctype) {
        this.ctype = ctype;
    }
    
    public final int doStartTag() throws JspException {
        
        try {
            String res = (String) ExpressionUtil.evalNotNull(
                "inventoryHierarchy", "resource", resource,
                     String.class, this, pageContext);

            String childType = (String) ExpressionUtil.evalNotNull(
                "inventoryHierarchy", "childType", ctype,
                String.class, this, pageContext);
            
            resourceId = new AppdefEntityID(res);
            if (childType != null) {
                childTypeId = new AppdefEntityTypeID(childType);
                
                // Fix for previously badly saved charts, because group type
                // is not valid for autogroups
                if (childTypeId.getType() ==
                    AppdefEntityConstants.APPDEF_TYPE_GROUP)
                    childTypeId = null;
            }
            
            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();
            ServletContext ctx = pageContext.getServletContext();
            String webapp = request.getContextPath();
            AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
            AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefResourceValue arv = appdefBoss.findById(sessionId, resourceId);
            AppdefResourceTypeValue artv = arv.getAppdefResourceTypeValue();
            AppdefGroupValue group = null;
            if(resourceId.isGroup()) {
                group = (AppdefGroupValue)arv;
            }
            StringBuffer sb = new StringBuffer();
            if(childTypeId == null) {
                sb.append(StringUtil.replace(
                        StringUtil.replace(
                                StringUtil.replace(getResHubAnchor(webapp) , "@@FF@@", String.valueOf(resourceId.getType()))  
                            , "@@FT@@", ((group != null) ? (group.isGroupCompat() ? "&g=1" : "&g=2") : "")) 
                        , "@@NAME@@", 
                        org.hyperic.hq.ui.taglib.display.StringUtil.toUpperCaseAt(resourceId.getTypeName(), 0) + "s"));
                sb.append(SEPARATOR);
                if(!resourceId.isApplication()) {
                    if(resourceId.isGroup()) {
                        if(group.isGroupCompat()) {
                            sb.append(StringUtil.replace(
                                StringUtil.replace(
                                    StringUtil.replace(getResHubAnchor(webapp),
                                        "@@FF@@", String.valueOf(resourceId.getType())),
                                        "@@FT@@", artv.getAppdefTypeKey() + "&g=1"),
                                        "@@NAME@@", artv.getName()));
                        } else {
                            sb.append(StringUtil.replace(
                                StringUtil.replace(
                                    StringUtil.replace(getResHubAnchor(webapp),
                                        "@@FF@@", String.valueOf(resourceId.getType())),
                                        "@@FT@@", "&g=2"),
                                        "@@NAME@@", artv.getName()));
                        }
                    } else {
                        sb.append(StringUtil.replace(
                                StringUtil.replace(
                                        StringUtil.replace(getResHubAnchor(webapp), "@@FF@@", String.valueOf(resourceId.getType()))
                                    , "@@FT@@", artv.getAppdefTypeKey())
                                , "@@NAME@@", artv.getName()));
                    }
                    sb.append(SEPARATOR);
                }
                sb.append(StringUtil.replace(StringUtil.replace(
                            StringUtil.replace(
                                    getResourceAnchor(webapp), "@@RID@@", String.valueOf(resourceId.getID()))
                            , "@@TYPE@@", String.valueOf(resourceId.getType())), "@@NAME@@", arv.getName()));
                        
            } else {
                // autogroup
                artv = appdefBoss.findResourceTypeById(sessionId, childTypeId);
                sb.append("Auto-Groups ");
                sb.append(SEPARATOR);
                sb.append(StringUtil.replace(
                        StringUtil.replace(
                                StringUtil.replace(getResHubAnchor(webapp), "@@FF@@", String.valueOf(childTypeId.getType()))
                            , "@@FT@@", childTypeId.getAppdefKey())
                        , "@@NAME@@", artv.getName()));;
                sb.append(SEPARATOR);
                sb.append(StringUtil.replace(StringUtil.replace(
                        StringUtil.replace(
                                getResourceAnchor(webapp), "@@RID@@", String.valueOf(resourceId.getID()))
                        , "@@TYPE@@", String.valueOf(resourceId.getType())), "@@NAME@@", arv.getName()));
                
            }
            pageContext.getOut().write(sb.toString());
            
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
        
    }
    
    private String getResHubAnchor(String webapp) {
        return StringUtil.replace(RESHUB_ANCHOR, "@@WAR@@", webapp);
    }
    
    private String getResourceAnchor(String webapp) {
        return StringUtil.replace(RES_ANCHOR, "@@WAR@@", webapp);
    }
    
}

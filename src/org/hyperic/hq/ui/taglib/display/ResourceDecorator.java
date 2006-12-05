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

import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag
 * that returns property values of resources and their dependent
 * objects, handling null values and the like.
 */
public class ResourceDecorator extends ColumnDecorator implements Tag {

    private static Log log =
        LogFactory.getLog(ResourceDecorator.class.getName());

    private String baseKey;
    private String bundle = org.apache.struts.Globals.MESSAGES_KEY;
    private String locale = org.apache.struts.Globals.LOCALE_KEY;
    private String function;
    private String resource;
    private String type;
    private PageContext context;
    private Tag parent;
    private boolean resourceTypeIdIsSet = false;
    private boolean resourceIdIsSet = false;
    private String resourceId = new String();
    private String resourceTypeId = new String();

    private String resourceName;

    public ResourceDecorator() {
        super();
    }

    public String getBaseKey() {
        return baseKey != null ? baseKey : "";
    }

    public void setBaseKey(String s) {
        baseKey = s;
    }
    
    public String getBundle() {
        return bundle;
    }

    public void setBundle(String s) {
        bundle = s;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String s) {
        locale = s;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String s) {
        function = s;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String s) {
        resource = s;
    }

    public String getType() {
        return type;
    }

    public void setType(String s) {
        type = s;
    }

    public String decorate(Object obj) throws JspException{
        
        if (resourceTypeIdIsSet && resourceIdIsSet){
            
            Integer id =  (Integer) evalAttr("resourceId", 
                                             this.resourceId,
                                             Integer.class);
            
            Integer typeId = (Integer) evalAttr("resourceType", 
                                                this.resourceTypeId,
                                                Integer.class);
            String resourceName = (String) evalAttr("resourceName", 
                                                     this.resourceName,
                                                     String.class);
            
            String resourceTypeName = AppdefEntityConstants.typeToString(typeId.intValue()).toLowerCase();
            HttpServletRequest req = (HttpServletRequest) context.getRequest();
            
            //resource/server/Inventory.do?mode=view&rid=${resource.resourceId}&type=${resource.resourceTypeId}
            return "<a href=\"" + req.getContextPath() + "/resource/" + resourceTypeName + "/Inventory.do?mode=view&rid=" + id + "&type=" + typeId +" \" />" + resourceName + "</a>";
        }
                    
        AppdefResourceValue resource = null;
        try {
            resource = (AppdefResourceValue)
                evalAttr("resource", this.resource, AppdefResourceValue.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.resource + "  not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate resource [" + this.resource +
                               "]: ", je);
            return "";
        }

        if (this.function != null) {
            try {
                Boolean doFunction =
                    (Boolean) evalAttr("function", this.function, Boolean.class);
                if (doFunction.booleanValue()) {
                    return doFunction(resource);
                }
            }
            catch (NullAttributeException ne) {
                log.debug("bean " + this.function + " not found");
                return "";
            }
            catch (JspException je) {
                log.debug("can't evaluate function [" + this.function +
                                   "]: ", je);
                return "";
            }
        }

        if (this.type != null) {
            try {
                Boolean doType =
                    (Boolean) evalAttr("type", this.type, Boolean.class);
                if (doType.booleanValue()) {
                    return doType(resource);
                }
            }
            catch (NullAttributeException ne) {
                log.debug("bean " + this.type + " not found");
                return "";
            }
            catch (JspException je) {
                log.debug("can't evaluate type [" + this.type + "]: ", je);
                return "";
            }
        }

        return "";
    }

    private String doFunction(AppdefResourceValue resource) {
        // XXX: groups currently don't have entity ids. when they get
        // them, we'll be able to return a function for them.
        AppdefEntityID entityId = resource.getEntityId();
        if (entityId == null) {
            return "";
        }

        String typeName = entityId.getTypeName();
        String key = getBaseKey() + '.' + typeName;
        try {
            String msg = RequestUtils.message(context, bundle, locale, key);
            return msg != null ? msg : typeName;
        }
        catch (JspException je) {
            log.debug("can't look up message [" + key + "]: ", je);
            return typeName;
        }
    }

    private String doType(AppdefResourceValue resource) {
        try {
            // XXX: groups currently don't have resource types. when they
            // get them, we'll be able to return a type for them.
            AppdefResourceTypeValue resourceType =
                resource.getAppdefResourceTypeValue();
            return resourceType != null ? resourceType.getName() : "";
        }
        catch (InvalidAppdefTypeException e) {
            log.debug("can't get resource type for resource [" +
                      resource.getName() + "]: ", e);
            return "";
        }
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A ResourceDecorator must be used within a ColumnTag.");
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
        this.parent = t;
    }

    public void setPageContext(PageContext pc) {
        this.context = pc;
    }

    public void release() {
        parent = null;
        context = null;
        bundle = org.apache.struts.Globals.MESSAGES_KEY;
        locale = org.apache.struts.Globals.LOCALE_KEY;
        function = null;
        resource = null;
        type = null;        
        baseKey = null;
        parent = null;
    }
    
    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("resourcedecorator", name, value,
                                          type, this, context);
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public void setResourceId(String resourceId) {
        resourceIdIsSet = true;
        this.resourceId = resourceId;
    }

    public String getResourceTypeId() {        
        return this.resourceTypeId;
    }

    public void setResourceTypeId(String resourceTypeId) {
        resourceTypeIdIsSet = true;
        this.resourceTypeId = resourceTypeId;
    }    

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}

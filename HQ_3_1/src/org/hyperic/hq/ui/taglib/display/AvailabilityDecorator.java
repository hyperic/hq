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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;


/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag
 * that that creates a column of availability icons.
 * 
 * One of these days, when the whole DependencyNode thing is cleaned
 * up, a lot of this stuff should just move to it's own decorator just for 
 * DependencyNodes
 */
public class AvailabilityDecorator extends ColumnDecorator implements Tag {

    //----------------------------------------------------static variables

    private final static String ICON_SRC    = "/resource/Availability";
    private final static String ICON_WIDTH  = "12";
    private final static String ICON_HEIGHT = "12";
    private final static String ICON_BORDER = "0";
    private final static String AVAILABILITY_TIMEOUT = "30000"; /* 30 seconds */
    
    private final static String ICON_PRE    = "/images/icon_available_";
    private final static String ICON_UP     = ICON_PRE + "green.gif";
    private final static String ICON_DOWN   = ICON_PRE + "red.gif";
    private final static String ICON_WARN   = ICON_PRE + "yellow.gif";
    private final static String ICON_PAUSED = ICON_PRE + "orange.gif";
    private final static String ICON_ERR    = ICON_PRE + "error.gif";
    
    private static Log log =
        LogFactory.getLog(AvailabilityDecorator.class.getName());

    //----------------------------------------------------instance variables

    // tag attrs
    private String resource;
    private String resourceId;
    private String resourceTypeId;
    
    private String monitorable; // optional attribute
    private String value;       // optional attribute

    // flags
    private boolean resourceIsSet = false;
    private boolean resourceIdIsSet = false;
    private boolean resourceTypeIdIsSet = false;
    private boolean monitorableIsSet = false;
    private boolean valueIsSet = false;
    
    private PageContext context;
    private Tag parent;

    //----------------------------------------------------constructors

    public AvailabilityDecorator() {
        super();
    }

    //----------------------------------------------------public methods

    public String getResource() {
        return resource;
    }

    public void setResource(String s) {
        resourceIsSet = true;
        resource = s;
    }

    /**
     * Returns the resourceId.
     * @return String
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Returns the resourceTypeId.
     * @return String
     */
    public String getResourceTypeId() {
        return resourceTypeId;
    }

    /**
     * Sets the resourceId.
     * @param resourceId The resourceId to set
     */
    public void setResourceId(String resourceId) {
        resourceIdIsSet = true;
        this.resourceId = resourceId;
    }
    
    /**
     * @return String
     */
    public String getMonitorable() {
        return monitorable;
    }

    /**
     * Sets the compatibleGroup.
     * @param compatibleGroup The compatibleGroup to set
     */
    public void setMonitorable(String monitorable) {
        monitorableIsSet = true;
        this.monitorable = monitorable;
    }

    /**
     * Sets the resourceTypeId.
     * @param resourceTypeId The resourceTypeId to set
     */
    public void setResourceTypeId(String resourceTypeId) {
        resourceTypeIdIsSet = true;
        this.resourceTypeId = resourceTypeId;
    }

    /**
     * @return String
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     * @param value The value to set
     */
    public void setValue(String value) {
        valueIsSet = true;
        this.value = value;
    }
    
    public String decorate(Object obj) throws Exception{
        if (valueIsSet) {
            return getOutputByValue();
        }
        
        if (resourceIdIsSet && resourceTypeIdIsSet) {
            try {
                return getOutputById();
            } catch (java.lang.NumberFormatException e) {
                log.debug("bogus params: resourceId=" +
                    resourceId + " resourceTypeId=" + resourceTypeId);
                return "";
            }
        }
        return getOutputByResource();
    }

    private String getOutputById() throws JspException {
        Integer id = null;
        Integer typeId = null;
        Boolean monitorableFlag = null;
        try {
            id =  (Integer) evalAttr("resourceId", this.resourceId,
                                     Integer.class);
            typeId = (Integer) evalAttr("resourceType", this.resourceTypeId,
                                        Integer.class);
        } catch (NullAttributeException ne) {
            log.debug("resourceId [" + this.resourceId +
                      "] and resourceTypeId [" + this.resourceTypeId +
                      "] not parameterized");
            return getNA();
        } catch (JspException je) {
            log.debug("resourceId [" + this.resourceId +
                      "] and resourceTypeId [" + this.resourceTypeId +
                      "] could not be evaluated");
            return getNA();
        }

        if (typeId.intValue() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            if (monitorableIsSet) {
                monitorableFlag = (Boolean)
                    evalAttr("monitorable", this.monitorable, Boolean.class);
                
                if (!monitorableFlag.booleanValue())
                    return getNA();
            }
            else {
                log.debug("don't know if group is compatible, returning n/a");
                return getNA();
            }
        }
        
        return getOutput(new AppdefEntityID(typeId.intValue(), id.intValue()));
    }

    private String getOutputByResource() throws JspException {
        AppdefResourceValue resource = null;
        try {
            resource =
                (AppdefResourceValue) evalAttr("resource", this.resource,
                                               AppdefResourceValue.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + this.resource + " not found");
            return getNA();
        } catch (JspException je) {
            log.debug("can't evaluate resource type [" + this.resource + "]",
                      je);
            return getNA();
        }

        if (resource.getEntityId() == null)
            return getNA();
        else
            return getOutput(resource.getEntityId());
    }

    private String getOutputByValue() throws JspException {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        StringBuffer src = new StringBuffer(req.getContextPath());

        if (monitorableIsSet) {
            Boolean monitorableFlag = (Boolean)
                evalAttr("monitorable", this.monitorable, Boolean.class);
            
            if (!monitorableFlag.booleanValue()) {
                if (resourceIdIsSet && resourceTypeIdIsSet) {
                    Integer id = null;
                    Integer typeId = null;
                    try {
                        id =  (Integer) evalAttr("resourceId", this.resourceId,
                                                 Integer.class);
                        typeId = (Integer) evalAttr("resourceType",
                                                    this.resourceTypeId,
                                                    Integer.class);
                        return this.getOutput(
                            new AppdefEntityID(typeId.intValue(),
                                               id.intValue()));
                    } catch (NullAttributeException ne) {
                        log.debug("resourceId [" + this.resourceId +
                                  "] and resourceTypeId [" +
                                  this.resourceTypeId +
                                  "] not parameterized");
                    } catch (JspException je) {
                        log.debug("resourceId [" + this.resourceId +
                                  "] and resourceTypeId [" +
                                  this.resourceTypeId +
                                  "] could not be evaluated");
                    }
                }
                
                // Just return not available
                return getNA();
            }
        }

        String availStr = (String) evalAttr("value", this.value, String.class);
        double availVal;
        
        if (availStr.length() > 0)
            availVal = Double.parseDouble(availStr);
        else
            availVal = Double.NaN;

        if (availVal == MeasurementConstants.AVAIL_DOWN) {
            src.append(ICON_DOWN);
        } else if (availVal == MeasurementConstants.AVAIL_UP) {
            src.append(ICON_UP);
        } else if (availVal == MeasurementConstants.AVAIL_PAUSED) {
            src.append(ICON_PAUSED);
        } else if (availVal < MeasurementConstants.AVAIL_UP &&
                   availVal > MeasurementConstants.AVAIL_DOWN) {
            src.append(ICON_WARN);
        } else {
            src.append(ICON_ERR);
        }

        StringBuffer buf = new StringBuffer()
            .append("<img src=\"")
            .append(src)
            .append("\" width=\"")
            .append(ICON_WIDTH)
            .append("\" height=\"")
            .append(ICON_HEIGHT)
            .append("\" alt=\"\" border=\"")
            .append(ICON_BORDER)
            .append("\">");
        return buf.toString();
    }

    private String getOutput(AppdefEntityID aeid) {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        StringBuffer src = new StringBuffer(req.getContextPath());
        src.append(ICON_SRC)
           .append("?")
           .append(Constants.ENTITY_ID_PARAM)
           .append("=")
           .append(aeid.getAppdefKey());

        StringBuffer buf = new StringBuffer();
        buf.append("<img name=\"avail")
           .append(aeid.getID())
           .append("\" src=\"")
           .append(src)
           .append("\" width=\"")
           .append(ICON_WIDTH)
           .append("\" height=\"")
           .append(ICON_HEIGHT)
           .append("\" alt=\"\" border=\"")
           .append(ICON_BORDER)
           .append("\">");
        return buf.toString();
    }

    private String getNA() throws JspException {
        return RequestUtils.message(this.getPageContext(), null, null, 
                                    "common.value.notavail");
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("An AvailabilityDecorator must be used within a ColumnTag.");
        }
        // the rules are a little more complicated than what can be expressed in the tld...
        if (resourceIsSet) {
            if (resourceIdIsSet || resourceTypeIdIsSet) {
                throw new JspTagException("An AvailabilityDecorator must either specify a 'resource' " +
                    " attribute or both 'resourceId' and 'resourceTypeId' attributes");
            }
        } else if (!valueIsSet) {
            if (! resourceIdIsSet && ! resourceTypeIdIsSet) {
                throw new JspTagException("An AvailabilityDecorator must either specify a 'resource' " +
                    " attribute or both 'resourceId' and 'resourceTypeId' attributes");
            }
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
        resource = null;
        resourceId = null;
        resourceTypeId = null;
        resourceIsSet = false;
        resourceIdIsSet = false;
        resourceTypeIdIsSet = false;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("availabilitydecorator", name, value,
                                          type, this, context);
    }

}

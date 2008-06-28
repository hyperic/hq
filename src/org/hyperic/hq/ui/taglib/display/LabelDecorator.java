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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a two in one decorator/tag for use within the display:table
 * tag, it is a ColumnDecorator tag that that creates a column of form element labels.
 */
public class LabelDecorator extends ColumnDecorator implements Tag
{

    //----------------------------------------------------static variables

    private static Log log =
        LogFactory.getLog(LabelDecorator.class.getName());

    //----------------------------------------------------instance variables

    /** The class property of the Label.
     */
    private String styleClass = null;

    /** The for="foo" property of the Label.
     */
    private String forElement = null;

    /** The onClick attribute of the Label.
     */
    private String onclick = null;

    // ctors
    public LabelDecorator() {
        styleClass = "listMemberCheckbox";
        forElement = "";
        onclick = "";
    }

    // accessors 
    public String getStyleClass() {
        return this.styleClass;
    }
    
    public void setStyleClass(String c) {
        this.styleClass = c;
    }
    
    public String getForElement() {
        return this.forElement;
    }
    
    public void setForElement(String n) {
        this.forElement = n;
    }
    
    public String getOnclick() {
        return this.onclick;
    }
    
    public void setOnclick(String o) {
        this.onclick = o;
    }
    
    public String decorate(Object obj) {
        String name = null, id = null;
        String value = null, label = null;
        String click = "";

        try {
            forElement = (String) evalAttr("forElement", this.forElement, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.forElement + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate forElement [" + this.forElement + "]: ", je);
            return "";
        }

        try {
            styleClass = (String) evalAttr("styleClass", this.styleClass, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.styleClass + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate styleClass [" + this.styleClass + "]: ", je);
            return "";
        }

        try {
            value = (String) evalAttr("value", this.value, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.value + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate value [" + this.value + "]: ", je);
            return "";
        }

        if (value == null)
            value = obj.toString();

        try {
            click = (String) evalAttr("onclick", this.getOnclick(), String.class);
        } catch (NullAttributeException e) {
            // Onclick is empty
        } catch (JspException e) {
            // Onclick is empty
        }

        StringBuffer buf = new StringBuffer();
        
        buf.append("<label for=\"");
        buf.append(forElement);
        buf.append("\" onclick=\"");
        buf.append(click);
        buf.append("\" class=\"");
        buf.append(styleClass);
        buf.append("\">");
        buf.append(value);
        buf.append("</label>");
        
        return buf.toString();
    }

    public int doStartTag() throws javax.servlet.jsp.JspException {
        ColumnTag ancestorTag = (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A LabelDecorator must be used within a ColumnTag.");
        }
        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }
    
    public int doEndTag() {
        return EVAL_PAGE;
    }

    // the JSP tag interface for this decorator
    Tag parent;
    PageContext context;

    /** Holds value of property value. */
    private String value;
    
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
        styleClass = null;
        forElement = null;
        onclick = null;
        parent = null;
        context = null;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("Labeldecorator", name, value,
                                          type, this, context);
    }
    
    /** Getter for property value.
     * @return Value of property value.
     *
     */
    public String getValue() {
        return this.value;
    }
    
    /** Setter for property value.
     * @param value New value of property value.
     *
     */
    public void setValue(String value) {
        this.value = value;
    }
    
}

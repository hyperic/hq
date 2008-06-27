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
 * tag, it is a ColumnDecorator tag that that creates a column of checkboxes.
 */
public class CheckBoxDecorator extends ColumnDecorator implements Tag
{

    //----------------------------------------------------static variables

    private static Log log =
        LogFactory.getLog(CheckBoxDecorator.class.getName());

    //----------------------------------------------------instance variables

    /** The class property of the checkbox.
     */
    private String styleClass = null;

    /** The name="foo" property of the checkbox.
     */
    private String name = null;

    /** The onClick attribute of the checkbox.
     */
    private String onclick = null;

    /** A flag indicating whether or not to suppress the checkbox
     */
    private String suppress = null;

    /** A string label to display after checkbox
     */
    private String label = null;

    /** A string ID for the checkbox
     */
    private String id = null;

    // ctors
    public CheckBoxDecorator() {
        styleClass = "listMember";
        name = "";
        onclick = "";
    }

    // accessors 
    public String getStyleClass() {
        return this.styleClass;
    }
    
    public void setStyleClass(String c) {
        this.styleClass = c;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String n) {
        this.name = n;
    }
    
    public String getOnclick() {
        return this.onclick;
    }
    
    public void setOnclick(String o) {
        this.onclick = o;
    }
    
    public String getSuppress() {
        return this.suppress;
    }
    
    public void setSuppress(String o) {
        this.suppress = o;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String decorate(Object obj) {
        String name = null, id = null;
        String value = null, label = null;
        String click = "";

        try {
            name = (String) evalAttr("name", this.name, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.name + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate name [" + this.name + "]: ", je);
            return "";
        }

        try {
            id = (String) evalAttr("id", this.id, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.id + " not found");
            id = null;
        }
        catch (JspException je) {
            log.debug("can't evaluate id [" + this.id + "]: ", je);
            id = null;
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
            label = (String) evalAttr("label", this.label, String.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.label + " not found");
            label = null;
        }
        catch (JspException je) {
            log.debug("can't evaluate label [" + this.label + "]: ", je);
            label = null;
        }

        try {
            String suppress = (String) evalAttr("suppress", getSuppress(),
                                                String.class);
            if (value.equals(suppress))
                return "";
        }
        catch (NullAttributeException ne) {
            // No suppression
        }
        catch (JspException je) {
            log.debug("can't evaluate suppress [" + this.suppress + "]: ", je);
            return "";
        }

        try {
            click = (String) evalAttr("onclick", this.getOnclick(), String.class);
        } catch (NullAttributeException e) {
            // Onclick is empty
        } catch (JspException e) {
            // Onclick is empty
        }

        StringBuffer buf = new StringBuffer();
        
        if (label != null) {
            buf.append("<label>");
        }
        
        buf.append("<input type=\"checkbox\" onclick=\"");
        buf.append(click);
        buf.append("\" class=\"");
        buf.append(getStyleClass());
        buf.append("\" id=\"");
        buf.append(id);
        buf.append("\" name=\"");
        buf.append(name);
        buf.append("\" value=\"");
        buf.append(value);
        buf.append("\"");
        buf.append(">");
        
        if (label != null) {
            buf.append(label)
               .append("</label>");
        }
        
        return buf.toString();
    }

    public int doStartTag() throws javax.servlet.jsp.JspException {
        ColumnTag ancestorTag = (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A CheckboxDecorator must be used within a ColumnTag.");
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
        name = null;
        onclick = null;
        suppress = null;
        parent = null;
        context = null;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("checkboxdecorator", name, value,
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

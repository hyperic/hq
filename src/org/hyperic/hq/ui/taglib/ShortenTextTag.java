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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

public class ShortenTextTag extends TagSupport {

    private int maxlength;
    private String value = null;
    private String property = null;
    private String styleClass = "listcellpopup4";
    private boolean shorten = false;
    
    public ShortenTextTag () { super(); }

    public int doStartTag() throws JspException {

        try {
            value = (String) ExpressionUtil.evalNotNull("spider", 
                                                        "value", 
                                                        getValue(), 
                                                        String.class, 
                                                        this, 
                                                        pageContext );
        } catch (NullAttributeException ne) {
            throw new JspTagException("typeId not found: " + ne);
        } catch (JspException je) {
            throw new JspTagException( je.toString() );
        }

        shorten = value.length() > maxlength;
        
        if (property != null)
            pageContext.setAttribute(property, value);
                
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        try {
            if (property == null) {
                if (shorten) {
                    StringBuffer text =
                        new StringBuffer("<a href=\"#\" class=\"")
                        .append(styleClass)
                        .append("\">")
                        .append(value.substring(0, maxlength))
                        .append("...")
                        .append("<span>")
                        .append(value)
                        .append("</span></a>");
                    pageContext.getOut().println(text.toString());
                }
                else
                    pageContext.getOut().println(value);
            }
        } catch (java.io.IOException e) {
            throw new JspException(e);
        }
        
        return super.doEndTag();
    }

    public String getValue() {
        return this.value;
    }    
    public void setValue(String value) {
        this.value = value;
    }

    public String getProperty() {
        return this.property;
    }    
    public void setProperty(String property) {
        this.property = property;
    }

    public int getMaxlength() {
        return this.maxlength;
    }    
    public void setMaxlength(int preChars) {
        this.maxlength = preChars;
    }
    public String getStyleClass() {
        return styleClass;
    }
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }
}

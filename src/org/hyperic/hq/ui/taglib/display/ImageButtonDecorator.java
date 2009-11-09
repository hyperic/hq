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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag
 * that that creates a column of image buttons.
 */
public class ImageButtonDecorator extends ColumnDecorator implements Tag {

    //----------------------------------------------------static variables

    private static Log log =
        LogFactory.getLog(ImageButtonDecorator.class.getName());

    //----------------------------------------------------instance variables

    private PageContext context;
    private Tag parent;
    private String form = null;
    private String input = null;
    private String page = null;

    //----------------------------------------------------constructors

    public ImageButtonDecorator() {
        super();
    }

    //----------------------------------------------------public methods

    public String getForm() {
        return this.form;
    }

    public void setForm(String n) {
        this.form = n;
    }

    public String getInput() {
        return this.input;
    }

    public void setInput(String n) {
        this.input = n;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String decorate(Object obj) {
    	try {
	        HttpServletRequest req = (HttpServletRequest) context.getRequest();
	        String src = req.getContextPath() + page;
	    	String formName = getForm();
	    	StringBuffer buf = new StringBuffer();

	        buf.append("<input type=\"image\" ");
	        buf.append("src=\"");
	        buf.append(src);
	        buf.append("\" ");
	        buf.append("border=\"0\" onClick=\"clickSelect('");
	        buf.append(formName);
	        buf.append("', '");
	        buf.append(getInput());
	        buf.append("', '");
	        buf.append(obj.toString());
	        buf.append("');\">");

	        return buf.toString();
    	} catch(NullPointerException npe) {
    		log.debug(npe.toString());
    	}
    	
    	return "";
    }

    public int doStartTag() throws JspTagException {
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
        form = null;
        input = null;
        page = null;
        parent = null;
        context = null;
    }
}
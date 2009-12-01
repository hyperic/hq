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

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is a two in one decorator/tag for use within the display:table
 * tag, it is a ColumnDecorator tag that that creates a column of form element
 * labels.
 */
public class LabelDecorator extends ColumnDecorator implements Tag {
	private static Log log = LogFactory.getLog(LabelDecorator.class.getName());

	/**
	 * The class property of the Label.
	 */
	private String styleClass = null;

	/**
	 * The for="foo" property of the Label.
	 */
	private String forElement = null;

	/**
	 * The onClick attribute of the Label.
	 */
	private String onclick = null;

	public LabelDecorator() {
		styleClass = "listMemberCheckbox";
		forElement = "";
		onclick = "";
	}

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
		try {
			String forElement = getForElement();
			String style = getStyleClass();
			String value = getValue();
			String click = getOnclick();
	
			if (value == null) {
				value = obj.toString();
			}
			
			StringBuffer buf = new StringBuffer();
	
			buf.append("<label for=\"");
			buf.append(forElement);
			buf.append("\" onclick=\"");
			buf.append(click);
			buf.append("\" class=\"");
			buf.append(style);
			buf.append("\">");
			buf.append(value);
			buf.append("</label>");
	
			return buf.toString();
		} catch(NullPointerException npe) {
			log.debug(npe.toString());
		}
		
		return "";
	}

	public int doStartTag() throws javax.servlet.jsp.JspException {
		ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
		
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

	/**
	 * Getter for property value.
	 * 
	 * @return Value of property value.
	 * 
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Setter for property value.
	 * 
	 * @param value
	 *            New value of property value.
	 * 
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
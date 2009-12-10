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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.util.StringUtil;

public class RemovePrefixTag extends TagSupport {
	private static final long serialVersionUID = 1L;

	private String prefix = null;
	private String value = null;
	private String property = null;

	public int doStartTag() throws JspException {
		try {
			String realPrefix = getPrefix();
			String realValue = getValue();
	
			value = StringUtil.removePrefix(realValue, realPrefix);
		} catch(NullPointerException npe) {
			throw new JspTagException("prefix or value attribute value is null", npe);
		}
		
		return SKIP_BODY;
	}

	public int doEndTag() throws JspException {
		try {
			if (property == null) {
				pageContext.getOut().println(value);
			} else {
				pageContext.setAttribute(property, value);
			}
		} catch (IOException e) {
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

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
}

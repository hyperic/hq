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

import org.hyperic.hq.ui.util.TaglibUtils;

public class ShortenPathTag extends TagSupport {
	private static final long serialVersionUID = 1L;

	private int preChars;
	private int postChars;
	private String value = null;
	private String realValue = null;
	private String newValue = null;
	private String property = null;
	private boolean strict = false;
	private String styleClass = null;
	private boolean shorten = false;

	public int doStartTag() throws JspException {
		try {
			realValue = getValue();
			newValue = TaglibUtils.shortenPath(realValue, preChars, postChars, strict);
			shorten = !newValue.equals(realValue);
		} catch(NullPointerException npe) {
			throw new JspTagException(npe);
		}
		
		return SKIP_BODY;
	}

	public int doEndTag() throws JspException {
		try {
			if (shorten && styleClass != null) {
				StringBuffer text = new StringBuffer("<a href=\"#\" class=\"")
						.append(styleClass).append("\">").append(newValue)
						.append("<span>").append(realValue).append(
								"</span></a>");

				if (property == null) {
					pageContext.getOut().println(text.toString());
				} else {
					pageContext.setAttribute(property, text.toString());
				}
			} else {
				if (property == null) {
					pageContext.getOut().println(newValue);
				} else {
					pageContext.setAttribute(property, newValue);
				}
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

	public int getPreChars() {
		return this.preChars;
	}

	public void setPreChars(int preChars) {
		this.preChars = preChars;
	}

	public int getPostChars() {
		return this.postChars;
	}

	public void setPostChars(int postChars) {
		this.postChars = postChars;
	}

	public boolean getStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public String getStyleClass() {
		return styleClass;
	}

	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
	}
}

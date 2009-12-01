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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The ImageButtonDecorator is nice for when the images are submitting a form
 * from within a table but when the table is populated with links that are
 * orthogonal to the form's purpose and link to another set of functionality,
 * this decorator is the ticket.
 */
public class ImageLinkDecorator extends BaseDecorator {
	private static Log log = LogFactory.getLog(ImageLinkDecorator.class.getName());
	
	private static final String DEFAULT_BORDER = "0";
	
	// tag attrs
	private String href_el;
	private String src_el;
	private String border_el;
	private String id_el;

	// attrs are optional
	private boolean borderIsSet = false;
	private boolean idIsSet = false;
	private boolean hrefIsSet = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hyperic.hq.ui.taglib.display.ColumnDecorator#decorate(java.lang.Object
	 * )
	 */
	public String decorate(Object columnValue) {
		if (!borderIsSet) {
			setBorder(DEFAULT_BORDER);
		}

		return generateOutput();
	}

	public void release() {
		super.release();
		href_el = null;
		src_el = null;
		border_el = null;
		id_el = null;
		borderIsSet = false;
		idIsSet = false;
		hrefIsSet = false;
	}

	private String generateOutput() {
		StringBuffer sb = new StringBuffer();
		
		if (hrefIsSet) {
			sb.append("<a ");
		
			if (idIsSet) {
				sb.append("id=\"");
				sb.append(getId());
				sb.append("\" ");
			}
			
			sb.append("href=\"").append(getHref()).append("\">");
		}
		
		sb.append("<img src=\"");
		sb.append(getSrc()).append("\" border=\"");
		sb.append(getBorder()).append("\">");
		
		if (hrefIsSet) {
			sb.append("</a>");
		}
		
		return sb.toString();
	}

	/**
	 * Returns the border_el.
	 * 
	 * @return String
	 */
	public String getBorder() {
		return border_el;
	}

	/**
	 * Returns the href_el.
	 * 
	 * @return String
	 */
	public String getHref() {
		return href_el;
	}

	/**
	 * Returns the src_el.
	 * 
	 * @return String
	 */
	public String getSrc() {
		return src_el;
	}

	/**
	 * Sets the border_el.
	 * 
	 * @param border_el
	 *            The border_el to set
	 */
	public void setBorder(String border_el) {
		borderIsSet = true;
		this.border_el = border_el;
	}

	/**
	 * Sets the href_el.
	 * 
	 * @param href_el
	 *            The href_el to set
	 */
	public void setHref(String href_el) {
		hrefIsSet = true;
		this.href_el = href_el;
	}

	/**
	 * Sets the src_el.
	 * 
	 * @param src_el
	 *            The src_el to set
	 */
	public void setSrc(String src_el) {
		this.src_el = src_el;
	}

	/**
	 * Returns the id_el.
	 * 
	 * @return String
	 */
	public String getId() {
		return id_el;
	}

	/**
	 * Sets the id_el.
	 * 
	 * @param id_el
	 *            The id_el to set
	 */
	public void setId(String id_el) {
		idIsSet = true;
		this.id_el = id_el;
	}
}

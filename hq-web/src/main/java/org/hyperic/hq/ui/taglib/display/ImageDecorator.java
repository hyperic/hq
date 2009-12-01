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
 * The ImageDecorator is nice for when the images are within a table and may
 * have attributes that would need to reflect the changing values in iteration
 * loop
 */
public class ImageDecorator extends BaseDecorator {
	private static Log log = LogFactory.getLog(ImageDecorator.class.getName());

	private static final String DEFAULT_BORDER = "0";
	
	// tag attrs
	private String onmouseover_el;
	private String onmouseout_el;
	private String src_el;
	private String border_el;
	private String title_el;

	// attrs are optional
	private boolean borderIsSet = false;
	private boolean onmouseoverIsSet = false;
	private boolean onmouseoutIsSet = false;
	private boolean titleIsSet = false;

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
		
		onmouseover_el = null;
		onmouseout_el = null;
		src_el = null;
		border_el = null;
		title_el = null;
		borderIsSet = false;
		onmouseoverIsSet = false;
		onmouseoutIsSet = false;
		titleIsSet = false;
	}

	private String generateOutput() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<img src=\"");
		sb.append(getSrc()).append("\"");

		if (borderIsSet) {
			sb.append(" border=\"").append(getBorder()).append("\"");
		}

		if (onmouseoverIsSet) {
			sb.append(" onmouseover=\"").append(getOnmouseover()).append("\"");
		}

		if (onmouseoutIsSet) {
			sb.append(" onmouseout=\"").append(getOnmouseout()).append("\"");
		}

		if (titleIsSet) {
			sb.append(" title=\"").append(getTitle()).append("\"");
		}

		sb.append("\">");
	
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
	 * Returns the onmouseover_el.
	 * 
	 * @return String
	 */
	public String getOnmouseover() {
		return onmouseover_el;
	}

	/**
	 * Returns the onmouseout_el.
	 * 
	 * @return String
	 */
	public String getOnmouseout() {
		return onmouseout_el;
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
	 * Returns the title_el
	 * 
	 * @return String
	 */
	public String getTitle() {
		return title_el;
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
	 * Sets the onmouseover_el.
	 * 
	 * @param onmouseover_el
	 *            The onmouseover_el to set
	 */
	public void setOnmouseover(String onmouseover_el) {
		onmouseoverIsSet = true;
		this.onmouseover_el = onmouseover_el;
	}

	/**
	 * Sets the onmouseout_el.
	 * 
	 * @param onmouseout_el
	 *            The onmouseout_el to set
	 */
	public void setOnmouseout(String onmouseout_el) {
		onmouseoutIsSet = true;
		this.onmouseout_el = onmouseout_el;
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
	 * Sets the title_el
	 * 
	 * @param title_el
	 *            The title_el to set
	 */
	public void setTitle(String title_el) {
		titleIsSet = true;
		this.title_el = title_el;
	}
}

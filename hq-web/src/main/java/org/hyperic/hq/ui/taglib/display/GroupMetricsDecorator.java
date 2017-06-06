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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.views.jsp.TagUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * This class acts as a decorator for tables, displaying "YES" "NO" or "SOME",
 * depending if total is 0, equal to, or less than active.
 */
public class GroupMetricsDecorator extends BaseDecorator {

	private static Log log = LogFactory.getLog(GroupMetricsDecorator.class.getName());

	/** Holds value of property active. */
	private String active;

	/** Holds value of property total. */
	private String total;

	/**
	 * Compares active and total, and outputs "YES" "NO" or "SOME"
	 * 
	 * @param obj
	 *            Does not use this value
	 * @return formatted date
	 */
	public String decorate(Object obj) {
		String tmpActive = getActive();
		String tmpTotal = getTotal();
		int tmpIntActive = 0;
		int tmpIntTotal = 0;

		if (tmpActive != null && tmpTotal != null) {
			try {
				tmpIntActive = Integer.parseInt(tmpActive);
				tmpIntTotal = Integer.parseInt(tmpTotal);
			} catch (NumberFormatException nfe) {
				log.debug("invalid property");
				
				return "";
			}
		}

		
		if (tmpIntActive == 0) {
			return RequestUtils.message("resource.common.monitor.visibility.config.NO");
		} else if (tmpIntActive < tmpIntTotal) {
			return RequestUtils.message("resource.common.monitor.visibility.config.SOME");
		} else {
			return RequestUtils.message("resource.common.monitor.visibility.config.YES");
		}
	}

	public void release() {
		super.release();
		active = null;
		total = null;
	}

	/**
	 * Getter for property active.
	 * 
	 * @return Value of property active.
	 * 
	 */
	public String getActive() {
		return this.active;
	}

	/**
	 * Setter for property active.
	 * 
	 * @param active
	 *            New value of property active.
	 * 
	 */
	public void setActive(String active) {
		this.active = active;
	}

	/**
	 * Getter for property total.
	 * 
	 * @return Value of property total.
	 * 
	 */
	public String getTotal() {
		return this.total;
	}

	/**
	 * Setter for property total.
	 * 
	 * @param total
	 *            New value of property total.
	 * 
	 */
	public void setTotal(String total) {
		this.total = total;
	}
}
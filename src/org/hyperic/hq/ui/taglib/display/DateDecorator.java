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

import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.units.DateFormatter;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

/**
 * This class decorates longs representing dates to dates.
 */
public class DateDecorator extends BaseDecorator {
	public static final String defaultKey = "common.value.notavail";

	private static Log log = LogFactory.getLog(DateDecorator.class.getName());

	/** Holds value of property isElapsedTime. */
	private Boolean isElapsedTime;

	/** Holds value of property isGroup. */
	private Boolean isGroup;

	/** Holds value of property active. */
	private String active;
	private PageContext context;

	protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;

	/**
	 * Decorates a date represented as a long.
	 * 
	 * @param obj
	 *            a long representing the time as a long
	 * @return formatted date
	 */
	public String decorate(Object obj) {
		String tempVal = getName();
		Long newDate = null;

		if (tempVal != null) {
			try {
				newDate = Long.valueOf(tempVal);
			} catch (NumberFormatException nfe) {
				log.debug("number format exception parsing long for: " + tempVal);
				
				return "";
			}
		} else {
			newDate = (Long) obj;
		}

		tempVal = getActive();
		
		if (tempVal != null) {
			try {
				int tmpIntActive = Integer.parseInt(tempVal);
					
				if (tmpIntActive == 0) {
						return "";
				}
			} catch (NumberFormatException nfe) {
				log.debug("invalid property");
			}
		}

		HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();

		if (newDate != null && newDate.equals(new Long(0))) {
			String resString;
			
			if (this.getIsGroup() != null && this.getIsGroup().booleanValue()) {
				resString = RequestUtils.message(request, "resource.common.monitor.visibility.config.DIFFERENT");
			} else {
				resString = RequestUtils.message(request, "resource.common.monitor.visibility.config.NONE");
			}
			
			return resString;
		}

		StringBuffer buf = new StringBuffer(512);

		if (obj == null) {
			// there may be cases where we have no date set when rendering a
			// table, so just show n/a (see PR 8443)
			buf.append(RequestUtils.message(request, bundle, request.getLocale().toString(), DateDecorator.defaultKey));
			
			return buf.toString();
		}

		Boolean b = getIsElapsedTime();
		
		if (null == b) {
			b = Boolean.FALSE;
		}

		int unit = b.booleanValue() ? UnitsConstants.UNIT_DURATION
				                    : UnitsConstants.UNIT_DATE;
		String formatString = RequestUtils.message(
				(HttpServletRequest) getPageContext().getRequest(),
				Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis");
		DateFormatter.DateSpecifics dateSpecs;

		dateSpecs = new DateFormatter.DateSpecifics();
		dateSpecs.setDateFormat(new SimpleDateFormat(formatString));
		
		FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(newDate.doubleValue(), 
				                                                 unit, 
				                                                 UnitsConstants.SCALE_MILLI),
				                                  getPageContext().getRequest().getLocale(), 
				                                  dateSpecs);
		
		buf.append(fmtd.toString());
		
		return buf.toString();
	}

	/**
	 * Getter for property isElapsedTime.
	 * 
	 * @return Value of property isElapsedTime.
	 * 
	 */
	public Boolean getIsElapsedTime() {
		return this.isElapsedTime;
	}

	/**
	 * Setter for property isElapsedTime.
	 * 
	 * @param isElapsedTime
	 *            New value of property isElapsedTime.
	 * 
	 */
	public void setIsElapsedTime(Boolean isElapsedTime) {
		this.isElapsedTime = isElapsedTime;
	}

	/**
	 * If this is a group, display "DIFFERENT" if the metric interval value is
	 * "0".
	 * 
	 * @return Value of property isGroup.
	 * 
	 */
	public Boolean getIsGroup() {
		return this.isGroup;
	}

	/**
	 * Setter for property isGroup.
	 * 
	 * @param isGroup
	 *            New value of property isGroup.
	 * 
	 */
	public void setIsGroup(Boolean isGroup) {
		this.isGroup = isGroup;
	}

	public PageContext getContext() {
		return context;
	}

	public void setContext(PageContext context) {
		this.context = context;
	}

	/**
	 * @return Returns the active.
	 */
	public String getActive() {
		return active;
	}

	/**
	 * @param active
	 *            The active to set.
	 */
	public void setActive(String active) {
		this.active = active;
	}
}
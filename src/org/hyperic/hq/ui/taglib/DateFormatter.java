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
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.units.DateFormatter.DateSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

/**
 * Tag that will take a value that is a long, or a runtime expression, and
 * output a date representation of that long.
 */
public class DateFormatter extends VarSetterBaseTag {
	private static final long serialVersionUID = 1L;

	/**
	 * A string which contains the long, or the expression, we hope to convert
	 * into a Long, and format as a date.
	 */
	private Long value;
	private Boolean time = Boolean.FALSE;
	private Boolean showDate = Boolean.TRUE;
	private Boolean showTime = Boolean.TRUE;
	private Boolean approx = Boolean.FALSE;

	public Long getValue() {
		return value;
	}

	public void setValue(Long v) {
		value = v;
	}

	/**
	 * Utility method for formatting dates.
	 * 
	 * XXX Might want to pass in a dateFmt'r if need more than 1 format. Right
	 * now, using simple "time" flag in tag to decide if should format as a
	 * time.
	 * 
	 * @param date
	 *            The long to convert to a date.
	 */
	private String formatDate(Long date) {
		int unit;

		if (time) {
			if (approx) {
				unit = UnitsConstants.UNIT_APPROX_DUR;
			} else {
				unit = UnitsConstants.UNIT_DURATION;
			}
		} else {
			unit = UnitsConstants.UNIT_DATE;
		}

		String key = Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis";

		if (!showTime) {
			key += ".dateonly";
		}
		
		if (!showDate) {
			key += ".timeonly";
		}
		
		String formatString = RequestUtils.message((HttpServletRequest) pageContext.getRequest(), key);
		DateSpecifics specs = new DateSpecifics();
		
		specs.setDateFormat(new SimpleDateFormat(formatString));
		
		FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(date.doubleValue(), 
				                                                 unit, 
				                                                 UnitsConstants.SCALE_MILLI), 
				                                  pageContext.getRequest().getLocale(), 
				                                  specs);
		
		return fmtd.toString();
	}

	/**
	 * This evaluates <em>value</em> as a struts expression, then outputs the
	 * resulting string to the <em>pageContext</em>'s out.
	 */
	public int doStartTag() throws JspException {
		Long newDate = getValue();

		if (newDate == null) {
			newDate = new Long(System.currentTimeMillis());
		}

		String d = formatDate(newDate);

		if (getVar() != null) {
			setScopedVariable(d);
		} else {
			try {
				pageContext.getOut().write(d);
			} catch (IOException ioe) {
				throw new JspException(getClass().getName() + " Could not output date.");
			}
		}

		return SKIP_BODY;
	}

	public void release() {
		value = null;
	}

	public Boolean getTime() {
		return time;
	}

	public void setTime(Boolean time) {
		this.time = time;
	}

	public Boolean getApprox() {
		return approx;
	}

	public void setApprox(Boolean approx) {
		this.approx = approx;
	}

	public Boolean getShowDate() {
		return showDate;
	}

	public void setShowDate(Boolean showDate) {
		this.showDate = showDate;
	}

	public Boolean getShowTime() {
		return showTime;
	}

	public void setShowTime(Boolean showTime) {
		this.showTime = showTime;
	}
}
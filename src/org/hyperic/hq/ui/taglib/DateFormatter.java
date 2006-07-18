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

import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.units.DateFormatter.DateSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

/**
 *  Tag that will take a value that is a long, or a runtime expression,
 *  and output a date representation of that long.
 */
public class DateFormatter extends VarSetterBaseTag {

    /** 
     * A string which contains the long, or the expression,
     * we hope to convert into a Long, and format as a date.
     */
    private String value = null;
    
    /** Holds value of property time. */
    private Boolean time = Boolean.FALSE;
    
    /** Holds value of property showTime. */
    private Boolean showTime = Boolean.TRUE;
    
    public DateFormatter() {
        super();
    }
    
    public String getValue() {
        return this.value;
    }
    
    public void setValue(String v) {
        this.value = v;
    }
    
    /**
     * Utility method for formatting dates.
     *
     * XXX Might want to pass in a dateFmt'r if need more than 1 
     * format. Right now, using simple "time" flag in tag to
     * decide if should format as a time.
     *
     * @param date The long to convert to a date.
     * @param asTime Whether to format this date as a time.
     */
    private String formatDate(Long date) {
        int unit = time.booleanValue() ? UnitsConstants.UNIT_DURATION :
                                         UnitsConstants.UNIT_DATE;
        String key = Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis";
        
        if (!showTime.booleanValue())
            key += ".dateonly";
        
        String formatString =
            RequestUtils.message((HttpServletRequest) pageContext.getRequest(), 
                                 key);
        DateSpecifics specs = new DateSpecifics();
        specs.setDateFormat(new SimpleDateFormat(formatString));
        FormattedNumber fmtd = UnitsFormat.format(
            new UnitNumber(date.doubleValue(), unit, UnitsConstants.SCALE_MILLI), 
            pageContext.getRequest().getLocale(), specs);
        return fmtd.toString();
    }
    
    /**
     * This evaluates <em>value</em> as a struts expression, then
     * outputs the resulting string to the <em>pageContext</em>'s out.
     */
    public int doStartTag() throws JspException {
        Long newDate;
        
        try {
            newDate = (Long) ExpressionUtil.evalNotNull("dateFormatter",
                    "value", value, Long.class, this, pageContext);
        } catch (NullAttributeException ne) {
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

    /** Reset the values of the tag.
     */
    public void release() {
        value = null;
    }
    
    /** Getter for property time.
     * @return Value of property time.
     *
     */
    public Boolean getTime() {
        return this.time;
    }
    
    /** Setter for property time.
     * @param time New value of property time.
     *
     */
    public void setTime(Boolean time) {
        this.time = time;
    }
    
    /** Getter for property showTime.
     * @return Value of property showTime.
     *
     */
    public Boolean getShowTime() {
        return this.showTime;
    }
    
    /** Setter for property showTime.
     * @param time New value of property showTime.
     *
     */
    public void setShowTime(Boolean showTime) {
        this.showTime = showTime;
    }
    
}

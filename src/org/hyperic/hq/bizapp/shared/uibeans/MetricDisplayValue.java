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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.units.FormattedNumber;

public class MetricDisplayValue implements Serializable {

    private static Log log = LogFactory.getLog(MetricDisplayValue.class.getName());
    private FormattedNumber valueFmt;
    private Double value;
    /**
     * flags whether or not the value meets the criteria for
     * comparison against a user specified threshold
     */
    private Boolean highlighted;
    
    public MetricDisplayValue(double aValue) {
        this.value = new Double(aValue);
    }
        
    public MetricDisplayValue(Double aValue) {
        this.value = aValue;
    }
    
    public Double getValue() {
        return value;
    }

    public FormattedNumber getValueFmt() {
        return valueFmt;
    }

    public void setValue(Double aValue) {
        this.value = aValue;
    }

    public void setValueFmt(FormattedNumber aValueFmt) {
        valueFmt = aValueFmt;
    }

    public Boolean getHighlighted() {
        return highlighted;
    }

    public void setHighlighted(Boolean flag) {
        highlighted = flag;
    }

    public String toString() {
        if (valueFmt != null)
            return valueFmt.toString();
        log.trace("toString() returning unformatted value");
        return value.toString();
    }
}

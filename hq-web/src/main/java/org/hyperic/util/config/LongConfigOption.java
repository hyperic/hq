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

package org.hyperic.util.config;

import java.io.Serializable;

public class LongConfigOption extends ConfigOption implements Serializable {
    private long minValue;  /* Minimum value for the option  */
    private long maxValue;  /* Maximum value for the option  */

    public LongConfigOption(String optName, String optDesc, Long defValue)
    {
        super(optName, optDesc, defValue == null ? null : defValue.toString());

        this.minValue = Long.MIN_VALUE;
        this.maxValue = Long.MAX_VALUE;
    }

    public void checkOptionIsValid(String value) 
        throws InvalidOptionValueException {

        long val;

        try {
            val = Long.parseLong(value);
        } catch(NumberFormatException exc){
            throw invalidOption("must be a long");
        }

        if (val < getMinValue()) {
            throw invalidOption("must be >= " + getMinValue());
        }

        if (val > getMaxValue()) {
            throw invalidOption("must be <= " + getMaxValue());
        }
    } 

    /**********************
     * Option properties
     **********************/

    public void setMinValue(long len){
        this.minValue = len;
    }

    public long getMinValue(){
        return this.minValue;
    }

    public void setMaxValue(long len){
        this.maxValue = len;
    }

    public long getMaxValue(){
        return this.maxValue;
    }
}

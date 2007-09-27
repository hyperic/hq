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

package org.hyperic.util.units;

import java.math.BigDecimal;

public class UnitNumber {
    private double value;
    private int    units;
    private int    scale;

    public UnitNumber(double value, int units) {
        this(value, units, UnitsConstants.SCALE_NONE);
    }
    
    public UnitNumber(double value, int units, int scale){
        this.value = value;
        this.units = units;
        this.scale = scale;

        UnitsUtil.checkValidUnits(units);
        UnitsUtil.checkValidScale(scale);
        UnitsUtil.checkValidScaleForUnits(units, scale);
    }

    public double getValue(){
        return this.value;
    }

    public int getUnits(){
        return this.units;
    }

    public int getScale(){
        return this.scale;
    }

    public BigDecimal getBaseValue(){
        return UnitsFormat.getBaseValue(this.value, this.units, this.scale);
    }

    public BigDecimal getScaledValue(int targScale){
        return UnitsFormat.getScaledValue(this.getBaseValue(), this.units, 
                                          targScale);
    }

    public String toString(){
        return Double.toString(value);
    }
}

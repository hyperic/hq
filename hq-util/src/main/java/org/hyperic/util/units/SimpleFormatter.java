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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public abstract class SimpleFormatter 
    implements Formatter
{
    protected abstract int getUnitType();
    protected abstract int getUnitScale();

    public FormattedNumber format(UnitNumber val, Locale locale, 
                                  FormatSpecifics specifics)
    {
        NumberFormat fmt;

        fmt = this.getNumberFormat(locale);
        return this.formatNumber(val.getValue(), fmt);
    }

    public FormattedNumber[] formatSame(double[] vals, int unitType,
                                        int scale, Locale locale, 
                                        FormatSpecifics specifics)
    {
        FormattedNumber[] res;
        NumberFormat fmt;

        if(unitType != this.getUnitType()){
            throw new IllegalArgumentException("Invalid unit specified");
        }

        if(scale != this.getUnitScale()){
            throw new IllegalArgumentException("Invalid scale specified");
        }

        fmt = UnitsUtil.getNumberFormat(vals, locale);
        res = new FormattedNumber[vals.length];

        for(int i=0; i<vals.length; i++){
            res[i] = this.formatNumber(vals[i], fmt);
        }
        return res;
    }

    protected abstract FormattedNumber 
        formatNumber(double rawValue, NumberFormat fmt);

    protected NumberFormat getNumberFormat(Locale locale){
        NumberFormat res = NumberFormat.getInstance(locale);

        res.setMinimumFractionDigits(1);
        res.setMaximumFractionDigits(1);
        return res;
    }

    public BigDecimal getBaseValue(double value, int scale){
        if(scale != this.getUnitScale()){
            throw new IllegalArgumentException("Invalid scale specified");
        }

        return new BigDecimal(value);
    }

    public BigDecimal getScaledValue(BigDecimal value, int targScale){
        if(targScale != this.getUnitScale()){
            throw new IllegalArgumentException("Invalid scale specified");
        }

        return value;
    }

    public UnitNumber parse(String val, Locale locale, 
                            ParseSpecifics specifics)
        throws ParseException
    {
        NumberFormat fmt;

        fmt = NumberFormat.getInstance(locale);
        return new UnitNumber(fmt.parse(val).doubleValue(),
                              this.getUnitType(), this.getUnitScale());
    }
}

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

import org.hyperic.util.TimeUtil;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class DateFormatter 
    implements Formatter
{
    public static class DateSpecifics
        extends FormatSpecifics 
    {
        private DateFormat df;

        public DateSpecifics(){}
        
        public void setDateFormat(DateFormat df){
            this.df = df;
        }
        
        public DateFormat getDateFormat(){
            return this.df;
        }
    }

    public FormattedNumber format(UnitNumber val, Locale locale){
        DateSpecifics specifics = new DateSpecifics();

        specifics.setDateFormat(this.getDefaultFormat(locale));
        return this.format(val, locale, specifics);
    }

    public FormattedNumber format(UnitNumber val, Locale locale, 
                                  FormatSpecifics specifics)
    {
        BigDecimal dec;
        DateFormat df;

        // We need a value in the milliseconds range
        dec = val.getBaseValue().divide(UnitsUtil.FACT_MILLIS,
                                        BigDecimal.ROUND_HALF_EVEN);
        if(specifics == null){
            df = this.getDefaultFormat(locale);
        } else {
            df = ((DateSpecifics)specifics).getDateFormat();
        }
        return new FormattedNumber(df.format(new Date(dec.longValue())), "");
    }

    public FormattedNumber[] formatSame(double[] val, int unitType, int scale,
                                        Locale locale)
    {
        FormattedNumber[] res;

        res = new FormattedNumber[val.length];

        for(int i=0; i<val.length; i++){
            res[i] = this.format(new UnitNumber(val[i], unitType, scale),
                                 locale);
        }
        
        return res;
    }

    public FormattedNumber[] formatSame(double[] val, int unitType, int scale,
                                        Locale locale, 
                                        FormatSpecifics specifics)
    {
        FormattedNumber[] res;

        res = new FormattedNumber[val.length];

        for(int i=0; i<val.length; i++){
            res[i] = this.format(new UnitNumber(val[i], unitType, scale),
                                 locale, specifics);
        }
        
        return res;
    }

    private DateFormat getDefaultFormat(Locale locale){
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                              DateFormat.MEDIUM, locale);
    }

    public BigDecimal getBaseValue(double value, int scale){
        return DateFormatter.getBaseTime(value, scale);
    }

    public BigDecimal getScaledValue(BigDecimal value, int targScale){
        return DateFormatter.getScaledTime(value, targScale);               
    }

    public static BigDecimal getScaledTime(BigDecimal value, int targScale){
        return value.divide(getScaleCoeff(targScale),
                            BigDecimal.ROUND_HALF_EVEN);
    }

    public static BigDecimal getBaseTime(double value, int scale){
        BigDecimal res;

        res = new BigDecimal(value);
        return res.multiply(getScaleCoeff(scale));
    }

    private static BigDecimal getScaleCoeff(int scale){
        switch(scale){
        case UnitsConstants.SCALE_NONE:
            return UnitsUtil.FACT_NONE;
        case UnitsConstants.SCALE_NANO:
            return UnitsUtil.FACT_NANOS;
        case UnitsConstants.SCALE_MICRO:
            return UnitsUtil.FACT_MICROS;
        case UnitsConstants.SCALE_MILLI:
            return UnitsUtil.FACT_MILLIS;
        case UnitsConstants.SCALE_JIFFY:
            return UnitsUtil.FACT_JIFFYS;
        case UnitsConstants.SCALE_SEC:
            return UnitsUtil.FACT_SECS;
        case UnitsConstants.SCALE_MIN:
            return UnitsUtil.FACT_MINS;
        case UnitsConstants.SCALE_HOUR:
            return UnitsUtil.FACT_HOURS;
        case UnitsConstants.SCALE_DAY:
            return UnitsUtil.FACT_DAYS;
        case UnitsConstants.SCALE_WEEK:
            return UnitsUtil.FACT_WEEKS;
        case UnitsConstants.SCALE_YEAR:
            return UnitsUtil.FACT_YEARS;
        }
        
        throw new IllegalArgumentException("Value did not have time " +
                                           "based scale");
    }

    public UnitNumber parse(String val, Locale locale, 
                            ParseSpecifics specifics)
        throws ParseException
    {
        long curTime = System.currentTimeMillis();

        return new UnitNumber(TimeUtil.parseComplexTime(val, curTime, false),
                              UnitsConstants.UNIT_DATE,
                              UnitsConstants.SCALE_MILLI);
    }
}

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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UnitsFormat {
    private static final HashMap formatters;

    static {
        formatters = new HashMap();

        formatters.put(new Integer(UnitsConstants.UNIT_NONE),
                       new NoFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_CURRENCY),
                       new CurrencyFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_BYTES),
                       new BytesFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_BITS),
                       new BitRateFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_BYTES2BITS),
                       new BytesToBitsFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_DURATION),
                       new DurationFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_DATE),
                       new DateFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_PERCENTAGE),
                       new PercentageFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_PERCENT),
                       new PercentFormatter());
        formatters.put(new Integer(UnitsConstants.UNIT_APPROX_DUR),
                       new ApproxDurationFormatter());
    }

    private static Log log = LogFactory.getLog(UnitsFormat.class);

    public static FormattedNumber format(UnitNumber val){
        return format(val, Locale.getDefault());
    }
    
    public static FormattedNumber format(UnitNumber val, Locale locale){
        return format(val, locale, null);
    }
    
    private static Formatter getFormatter(int unitType){
        Formatter res;

        res = (Formatter)formatters.get(new Integer(unitType));
        if(res == null){
            throw new IllegalStateException("Unhandled unit type: " + 
                                            unitType);
        }
        return res;
    }


    public static FormattedNumber format(UnitNumber val, Locale locale, 
                                         FormatSpecifics specifics)
    {
        FormattedNumber res;
        Formatter formatter;

        formatter = getFormatter(val.getUnits());

        res = formatter.format(val, locale, specifics);
        if(log.isDebugEnabled()){
            log.debug("format(" + val.getValue() + ") -> " + res);
        }
        return res;
    }

    public static FormattedNumber[] formatSame(double[] values, int unitType, 
                                               int scale)
    {
        return formatSame(values, unitType, scale, Locale.getDefault());
    }

    public static FormattedNumber[] formatSame(double[] values, int unitType, 
                                               int scale, Locale locale)
    {
        return(formatSame(values, unitType, scale, locale, null));
    }

    public static FormattedNumber[] formatSame(double[] values, int unitType, 
                                               int scale, Locale locale, 
                                               FormatSpecifics specifics)
    {
        FormattedNumber[] res;
        Formatter formatter;

        formatter = getFormatter(unitType);

        res = formatter.formatSame(values, unitType, scale, locale, 
                                   specifics);

        if(log.isDebugEnabled()){
            StringBuffer buf = new StringBuffer();
            
            buf.append("format({");
            for(int i=0; i<values.length; i++){
                buf.append(values[i]);
                if(i != values.length)
                    buf.append(", ");
            }
            buf.append("}) -> {");

            for(int i=0; i<res.length; i++){
                buf.append(res[i].toString());
                if(i != values.length)
                    buf.append(", ");
            }
            buf.append("}");

            log.debug(buf.toString());
        }
        return res;
    }

    static BigDecimal getBaseValue(double value, int unitType, int scale){
        return getFormatter(unitType).getBaseValue(value, scale);
    }

    static BigDecimal getScaledValue(BigDecimal baseValue, int unitType, 
                                     int scale)
    {
        return getFormatter(unitType).getScaledValue(baseValue, scale);
    }

    public static UnitNumber parse(String value, int unitType)
        throws ParseException 
    {
        return parse(value, unitType, null);
    }

    public static UnitNumber parse(String value, int unitType, 
                                   ParseSpecifics specifics)
        throws ParseException 
    {
        return parse(value, unitType, Locale.getDefault(), specifics);
    }

    public static UnitNumber parse(String value, int unitType, Locale locale,
                                   ParseSpecifics specifics)
        throws ParseException
    {
        Formatter formatter;

        formatter = getFormatter(unitType);
        return formatter.parse(value, locale, specifics);
    }

    public static void main(String[] args) throws Exception {
        for(int i=0; i<args.length; i++){
            UnitNumber num = UnitsFormat.parse(args[i], 
                                               UnitsConstants.UNIT_BYTES);

            System.out.println("Raw = " + num.getBaseValue());
            System.out.println("Fmt = " + 
                               UnitsFormat.format(num, Locale.getDefault()));

            System.out.println("Scaled to bytes = " +
                               num.getScaledValue(UnitsConstants.SCALE_NONE));
            System.out.println("Scaled to kilo = " +
                               num.getScaledValue(UnitsConstants.SCALE_KILO));
            System.out.println("Scaled to giga = " +
                               num.getScaledValue(UnitsConstants.SCALE_GIGA));
        }
    }
}

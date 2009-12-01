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

package org.hyperic.hq.measurement;

import java.util.HashMap;
import java.util.Locale;

import org.hyperic.util.units.FormatSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;


public class UnitsConvert {
    private static HashMap      unitsToUnit;
    private static HashMap      unitsToScale;
    
    static {
        initUnitsToUnit();
        initUnitsToScale();
    }

    private UnitsConvert(){}

    private static void initUnitsToUnit(){
        unitsToUnit  = new HashMap();
        
        unitsToUnit.put(MeasurementConstants.UNITS_NONE,
                        new Integer(UnitsConstants.UNIT_NONE));

        unitsToUnit.put(MeasurementConstants.UNITS_PERCENTAGE,
                        new Integer(UnitsConstants.UNIT_PERCENTAGE));
        unitsToUnit.put(MeasurementConstants.UNITS_PERCENT,
                        new Integer(UnitsConstants.UNIT_PERCENT));

        unitsToUnit.put(MeasurementConstants.UNITS_BYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        unitsToUnit.put(MeasurementConstants.UNITS_KBYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        unitsToUnit.put(MeasurementConstants.UNITS_MBYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        unitsToUnit.put(MeasurementConstants.UNITS_GBYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        unitsToUnit.put(MeasurementConstants.UNITS_TBYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        unitsToUnit.put(MeasurementConstants.UNITS_PBYTES,
                        new Integer(UnitsConstants.UNIT_BYTES));
        
        unitsToUnit.put(MeasurementConstants.UNITS_BITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_BYTES_TO_BITS,
                        new Integer(UnitsConstants.UNIT_BYTES2BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_KBITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_MBITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_GBITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_TBITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        unitsToUnit.put(MeasurementConstants.UNITS_PBITS,
                        new Integer(UnitsConstants.UNIT_BITS));
        
        unitsToUnit.put(MeasurementConstants.UNITS_EPOCH_MILLIS,
                        new Integer(UnitsConstants.UNIT_DATE));
        unitsToUnit.put(MeasurementConstants.UNITS_EPOCH_SECONDS,
                        new Integer(UnitsConstants.UNIT_DATE));
        
        unitsToUnit.put(MeasurementConstants.UNITS_NANOS,
                        new Integer(UnitsConstants.UNIT_DURATION));
        unitsToUnit.put(MeasurementConstants.UNITS_MICROS,
                        new Integer(UnitsConstants.UNIT_DURATION));
        unitsToUnit.put(MeasurementConstants.UNITS_MILLIS,
                        new Integer(UnitsConstants.UNIT_DURATION));
        unitsToUnit.put(MeasurementConstants.UNITS_JIFFYS,
                        new Integer(UnitsConstants.UNIT_DURATION));
        unitsToUnit.put(MeasurementConstants.UNITS_SECONDS,
                        new Integer(UnitsConstants.UNIT_DURATION));

        unitsToUnit.put(MeasurementConstants.UNITS_CENTS,
                        new Integer(UnitsConstants.UNIT_CURRENCY));
    }

    private static void initUnitsToScale(){
        unitsToScale = new HashMap();
        
        unitsToScale.put(MeasurementConstants.UNITS_NONE,
                         new Integer(UnitsConstants.SCALE_NONE));

        unitsToScale.put(MeasurementConstants.UNITS_PERCENTAGE,
                         new Integer(UnitsConstants.SCALE_NONE));
        unitsToScale.put(MeasurementConstants.UNITS_PERCENT,
                         new Integer(UnitsConstants.SCALE_NONE));

        unitsToScale.put(MeasurementConstants.UNITS_BYTES,
                         new Integer(UnitsConstants.SCALE_NONE));
        unitsToScale.put(MeasurementConstants.UNITS_KBYTES,
                         new Integer(UnitsConstants.SCALE_KILO));
        unitsToScale.put(MeasurementConstants.UNITS_MBYTES,
                         new Integer(UnitsConstants.SCALE_MEGA));
        unitsToScale.put(MeasurementConstants.UNITS_GBYTES,
                         new Integer(UnitsConstants.SCALE_GIGA));
        unitsToScale.put(MeasurementConstants.UNITS_TBYTES,
                         new Integer(UnitsConstants.SCALE_TERA));
        unitsToScale.put(MeasurementConstants.UNITS_PBYTES,
                         new Integer(UnitsConstants.SCALE_PETA));
        
        unitsToScale.put(MeasurementConstants.UNITS_BITS,
                         new Integer(UnitsConstants.SCALE_NONE));
        unitsToScale.put(MeasurementConstants.UNITS_BYTES_TO_BITS,
                         new Integer(UnitsConstants.SCALE_NONE));
        unitsToScale.put(MeasurementConstants.UNITS_KBITS,
                         new Integer(UnitsConstants.SCALE_KILO));
        unitsToScale.put(MeasurementConstants.UNITS_MBITS,
                         new Integer(UnitsConstants.SCALE_MEGA));
        unitsToScale.put(MeasurementConstants.UNITS_GBITS,
                         new Integer(UnitsConstants.SCALE_GIGA));
        unitsToScale.put(MeasurementConstants.UNITS_TBITS,
                         new Integer(UnitsConstants.SCALE_TERA));
        unitsToScale.put(MeasurementConstants.UNITS_PBITS,
                         new Integer(UnitsConstants.SCALE_PETA));
        
        unitsToScale.put(MeasurementConstants.UNITS_EPOCH_MILLIS,
                         new Integer(UnitsConstants.SCALE_MILLI));
        unitsToScale.put(MeasurementConstants.UNITS_EPOCH_SECONDS,
                         new Integer(UnitsConstants.SCALE_SEC));
        
        unitsToScale.put(MeasurementConstants.UNITS_NANOS,
                         new Integer(UnitsConstants.SCALE_NANO));
        unitsToScale.put(MeasurementConstants.UNITS_MICROS,
                         new Integer(UnitsConstants.SCALE_MICRO));
        unitsToScale.put(MeasurementConstants.UNITS_MILLIS,
                         new Integer(UnitsConstants.SCALE_MILLI));
        unitsToScale.put(MeasurementConstants.UNITS_JIFFYS,
                         new Integer(UnitsConstants.SCALE_JIFFY));
        unitsToScale.put(MeasurementConstants.UNITS_SECONDS,
                         new Integer(UnitsConstants.SCALE_SEC));
    }

    /**
     * Return the corresponding unit constant from
     * <code>UnitsConstants</code> for the given unit.
     *
     * @param unit the unit
     * @return the corresponding <code>UnitsConstants</code> unit
     * @see org.hyperic.util.units.UnitsConstants
     */
    public static int getUnitForUnit(String unit){
        Integer res;

        if((res = (Integer)unitsToUnit.get(unit)) == null)
            return UnitsConstants.UNIT_NONE;

        return res.intValue();
    }

    /**
     * Return the corresponding scale constant from
     * <code>UnitsConstants</code> for the given unit.
     *
     * @param unit the unit
     * @return the corresponding <code>UnitsConstants</code> scale
     * @see org.hyperic.util.units.UnitsConstants
     */
    public static int getScaleForUnit(String unit){
        Integer res;

        if((res = (Integer)unitsToScale.get(unit)) == null)
            return UnitsConstants.SCALE_NONE;

        return res.intValue();
    }

    public static FormattedNumber convert(double val, String units){
        return convert(val, units, Locale.getDefault());
    }

    /**
     * Convert the value into a string based on the specified units.
     * 
     * @param val    Value to render
     * @param units  One of MeasurementConstants.UNITS_*
     * @param locale The locale to use when rendering the result
     * 
     * @return a formattedNumber representing the approximated value
     */
    public static FormattedNumber convert(double val, String units, 
                                          Locale locale)
    {
        return convert(val, units, locale, null);
    }

    /**
     * Convert the value into a string based on the specified units.
     * 
     * @param val    Value to render
     * @param units  One of MeasurementConstants.UNITS_*
     * @param specifics config object for formatting specifics
     * 
     * @return a formattedNumber representing the approximated value
     */
    public static FormattedNumber convert(double val, String units, 
                                          FormatSpecifics specifics)
    {
        return convert(val, units, Locale.getDefault(), specifics);
    }

    /**
     * Convert the value into a string based on the specified units.
     * 
     * @param val       Value to render
     * @param units     One of MeasurementConstants.UNITS_*
     * @param locale    The locale to use when rendering the result
     * @param specifics config object for formatting specifics
     * 
     * @return a formattedNumber representing the approximated value
     */
    public static FormattedNumber convert(double val, String units, 
                                          Locale locale,
                                          FormatSpecifics specifics)
    {
        int unit, scale;

        unit  = getUnitForUnit(units);
        scale = getScaleForUnit(units);
        return UnitsFormat.format(new UnitNumber(val, unit, scale),
                                  locale, specifics);
    }

    /**
     * Format multiple values into approximated values, all of which
     * use the same unit.  
     *
     * Example:  If 1, 100, 1000, 10000  are the values passed and
     *           the units == UNITS_BYTES, the following is returned
     *
     *           1 B, 100B, 1000B, 10000B
     *
     *           Notice that the bytes on 1000) did not change to KB
     *
     * @param vals   Values to render
     * @param units  One of MeasurementConstants.UNITS_*
     * @param locale The locale to use when rendering the result
     * 
     * @return An array of FormattedNumber[] objects which represent
     *         the formatted value for each corresponding value in 'vals'
     */
    public static FormattedNumber[] 
        convertSame(double[] vals, String units, Locale locale)
    {
        FormattedNumber[] res;
        int unit, scale;

        unit  = getUnitForUnit(units);
        scale = getScaleForUnit(units);
        res   = UnitsFormat.formatSame(vals, unit, scale, locale);
        return res;
    }

    public static FormattedNumber[] 
        convertSame(double[] vals, String units, Locale locale, 
                    FormatSpecifics specifics)
    {
        FormattedNumber[] res;
        int unit, scale;

        unit  = getUnitForUnit(units);
        scale = getScaleForUnit(units);
        res   = UnitsFormat.formatSame(vals, unit, scale, locale, specifics);
        return res;
    }
    
    public static void main(String[] args) {
        double val = Double.parseDouble(args[0]);
        String units = args[1];
        FormattedNumber number = convert(val, units);
        System.out.println(number.toString());
    }
}

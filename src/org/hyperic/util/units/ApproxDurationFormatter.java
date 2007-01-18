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

import org.hyperic.util.StringUtil;

/**
 * Format a value into a duration.  
 */
public class ApproxDurationFormatter extends DurationFormatter
    implements Formatter
{
    protected FormattedNumber format(BigDecimal baseTime, int granularity,
                                     int milliDigits, Locale locale)
    {
        TimeBreakDown tbd = breakDownTime(baseTime);
        String res;

        // We work in a few different time ranges depending on the
        // magnitude of the duration.  Quite hardcoded
        if(granularity == GRANULAR_YEARS){
            res = tbd.nYears + (tbd.nYears == 1 ? " year " : " years ");
        } else if(granularity == GRANULAR_DAYS){
            long nDays = tbd.nYears * 365 + tbd.nDays;
            res   = nDays + (nDays == 1 ? " day " : " days ");
        } else if(granularity == GRANULAR_HOURS) {
            long hours = tbd.nYears * 365 * 24 + tbd.nDays * 24 + tbd.nHours;
            res   = hours + (hours == 1 ? " hour " : " hours ");
        } else if (granularity == GRANULAR_MINS) {
            long minutes = tbd.nYears * 365 * 24 * 60 + tbd.nDays * 24 * 60 +
                           tbd.nHours * 60 + tbd.nMins;
            res   = minutes + (minutes == 1 ? " minute " : " minutes ");
        } else if (granularity == GRANULAR_SECS ||
                   granularity == GRANULAR_MILLIS) {
            res = " 0 minute ";
        } else {
            throw new IllegalStateException("Unexpected granularity");
        }

        return new FormattedNumber(res.trim(), "");
    }

    /**
     * Parse a string which is of the same format that we output, 
     * when outputting durations.
     */
    private UnitNumber parseRegular(String val, Locale locale,
                                    ParseSpecifics specifics)
        throws ParseException
    {
        final int unitType = UnitsConstants.UNIT_APPROX_DUR;
        String[] vals;

        vals = (String[])StringUtil.explode(val, " ").toArray(new String[0]);
        
        if(vals.length == 2 &&
           vals[0].charAt(vals[0].length() - 1) == 'y' &&
           vals[1].charAt(vals[1].length() - 1) == 'd')
        {
            try {
                String yStr = vals[0].substring(0, vals[0].length() - 1);
                String dStr = vals[1].substring(0, vals[1].length() - 1);

                return new UnitNumber(Integer.parseInt(yStr) * 365 + 
                                      Integer.parseInt(dStr), unitType,
                                      UnitsConstants.SCALE_DAY);
            } catch(NumberFormatException exc){
            }
        } else if(vals.length == 3 && 
                  (vals[1].equals("day") || vals[1].equals("days")))
        {
            try {
                return new UnitNumber(Integer.parseInt(vals[0]) * 24 * 60 * 60+
                                      parseTimeStr(vals[2]),
                                      unitType, UnitsConstants.SCALE_SEC);
            } catch(NumberFormatException exc){
            }
        } else if(vals.length == 1){
            return new UnitNumber(parseTimeStr(vals[0]), unitType,
                                  UnitsConstants.SCALE_SEC);
        }

        throw new ParseException(val, 0);
    }

    public UnitNumber parse(String val, Locale locale, 
                            ParseSpecifics specifics)
        throws ParseException
    {
        NumberFormat fmt = NumberFormat.getInstance(locale);
        double numberPart;
        String tagPart;
        int nonIdx, scale;

        try {
            return parseRegular(val, locale, specifics);
        } catch(ParseException exc){
            // That's fine, try another format
        }

        nonIdx = UnitsUtil.findNonNumberIdx(val, fmt);
        if(nonIdx == -1){
            throw new ParseException("Number had no units with it", 
                                     val.length());
        }

        if(nonIdx == 0){
            throw new ParseException("Invalid number specified", 0);
        }

        numberPart = fmt.parse(val.substring(0, nonIdx)).doubleValue();
        tagPart    = val.substring(nonIdx, val.length()).trim();

        if(tagPart.equalsIgnoreCase("y") ||
           tagPart.equalsIgnoreCase("yr") ||
           tagPart.equalsIgnoreCase("yrs") ||
           tagPart.equalsIgnoreCase("year") ||
           tagPart.equalsIgnoreCase("years"))
        {
            scale = UnitsConstants.SCALE_YEAR;
        } else if(tagPart.equalsIgnoreCase("w") ||
                  tagPart.equalsIgnoreCase("wk") ||
                  tagPart.equalsIgnoreCase("wks") ||
                  tagPart.equalsIgnoreCase("week") ||
                  tagPart.equalsIgnoreCase("weeks"))
        {
            scale = UnitsConstants.SCALE_WEEK;
        } else if(tagPart.equalsIgnoreCase("d") ||
                  tagPart.equalsIgnoreCase("day") ||
                  tagPart.equalsIgnoreCase("days"))
        {
            scale = UnitsConstants.SCALE_DAY;
        } else if(tagPart.equalsIgnoreCase("h") ||
                  tagPart.equalsIgnoreCase("hr") ||
                  tagPart.equalsIgnoreCase("hrs") ||
                  tagPart.equalsIgnoreCase("hour") ||
                  tagPart.equalsIgnoreCase("hours"))
        {
            scale = UnitsConstants.SCALE_HOUR;
        } else if(tagPart.equalsIgnoreCase("m") ||
                  tagPart.equalsIgnoreCase("min") ||
                  tagPart.equalsIgnoreCase("mins") ||
                  tagPart.equalsIgnoreCase("minute") ||
                  tagPart.equalsIgnoreCase("minutes"))
        {
            scale = UnitsConstants.SCALE_MIN;
        } else if(tagPart.equalsIgnoreCase("s") ||
                  tagPart.equalsIgnoreCase("sec") ||
                  tagPart.equalsIgnoreCase("secs") ||
                  tagPart.equalsIgnoreCase("second") ||
                  tagPart.equalsIgnoreCase("seconds"))
        {
            scale = UnitsConstants.SCALE_SEC;
        } else if(tagPart.equalsIgnoreCase("j") ||
                  tagPart.equalsIgnoreCase("jif") ||
                  tagPart.equalsIgnoreCase("jifs") ||
                  tagPart.equalsIgnoreCase("jiffy") ||
                  tagPart.equalsIgnoreCase("jiffys") ||
                  tagPart.equalsIgnoreCase("jifferoonies"))
        {
            scale = UnitsConstants.SCALE_JIFFY;
        } else if(tagPart.equalsIgnoreCase("ms") ||
                  tagPart.equalsIgnoreCase("milli") ||
                  tagPart.equalsIgnoreCase("millis") ||
                  tagPart.equalsIgnoreCase("millisecond") ||
                  tagPart.equalsIgnoreCase("milliseconds"))
        {
            scale = UnitsConstants.SCALE_MILLI;
        } else if(tagPart.equalsIgnoreCase("us") ||
                  tagPart.equalsIgnoreCase("micro") ||
                  tagPart.equalsIgnoreCase("micros") ||
                  tagPart.equalsIgnoreCase("microsecond") ||
                  tagPart.equalsIgnoreCase("microseconds"))
        {
            scale = UnitsConstants.SCALE_MICRO;
        } else if(tagPart.equalsIgnoreCase("ns") ||
                  tagPart.equalsIgnoreCase("nano") ||
                  tagPart.equalsIgnoreCase("nanos") ||
                  tagPart.equalsIgnoreCase("nanosecond") ||
                  tagPart.equalsIgnoreCase("nanoseconds"))
        {
            scale = UnitsConstants.SCALE_NANO;
        } else {
            throw new ParseException("Unknown duration '" + tagPart + "'",
                                     nonIdx);
        }

        return new UnitNumber(numberPart, UnitsConstants.UNIT_APPROX_DUR,
                              scale);
    }
}

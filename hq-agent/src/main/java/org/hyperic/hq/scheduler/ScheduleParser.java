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

package org.hyperic.hq.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduleParser {

    // This is a utility class, not made for instantiation.
    private ScheduleParser() {}

    // Get the cron string based on the schedule value.  If null is
    // returned cron string is not supported.
    public static String getCronString(ScheduleValue schedule)
        throws ScheduleParseException
    {
        if (schedule instanceof ScheduleDailyValue)
            return getCronStringDaily((ScheduleDailyValue)schedule);
        if (schedule instanceof ScheduleWeeklyValue)
            return getCronStringWeekly((ScheduleWeeklyValue)schedule);
        if (schedule instanceof ScheduleMonthlyValue)
            return getCronStringMonthly((ScheduleMonthlyValue)schedule);
        if (schedule instanceof ScheduleSingleValue)
            return getCronStringSingle((ScheduleSingleValue)schedule);

        throw new ScheduleParseException("Unknown schedule value");
    }

    // Private helper methods

    private static String getCronStringDaily(ScheduleDailyValue schedule)
        throws ScheduleParseException
    {
        Calendar cal = new GregorianCalendar();
        cal.setTime(schedule.getStart());

        if (schedule.getEveryWeekDay()) {
            return cal.get(Calendar.SECOND) + " " +
                cal.get(Calendar.MINUTE) + " " +
                cal.get(Calendar.HOUR_OF_DAY) + " " +
                "? * MON-FRI";
        }

        // Else we have daily recurrance
        return cal.get(Calendar.SECOND) + " " +
            cal.get(Calendar.MINUTE) + " " +
            cal.get(Calendar.HOUR_OF_DAY) + " " +
            "*/" + schedule.getInterval() +
            " * ?";
    }

    /**
     * XXX: we ignore the interval, not possible with quartz
     */
    private static String getCronStringWeekly(ScheduleWeeklyValue schedule)
        throws ScheduleParseException
    {
        Calendar cal = new GregorianCalendar();
        cal.setTime(schedule.getStart());
        int interval = schedule.getInterval() * 7;
        
        String buffer = new String();
        String days;

        if (schedule.isDaySet(Calendar.MONDAY))
            buffer = buffer + "MON,";
        if (schedule.isDaySet(Calendar.TUESDAY))
            buffer = buffer + "TUE,";
        if (schedule.isDaySet(Calendar.WEDNESDAY))
            buffer = buffer + "WED,";
        if (schedule.isDaySet(Calendar.THURSDAY))
            buffer = buffer + "THU,";
        if (schedule.isDaySet(Calendar.FRIDAY))
            buffer = buffer + "FRI,";
        if (schedule.isDaySet(Calendar.SATURDAY))
            buffer = buffer + "SAT,";
        if (schedule.isDaySet(Calendar.SUNDAY))
            buffer = buffer + "SUN,";

        if (buffer.length() == 0) {
            throw new ScheduleParseException("No days scheduled");
        }

        days = buffer.substring(0, buffer.length() - 1);

        return cal.get(Calendar.SECOND) + " " +
            cal.get(Calendar.MINUTE) + " " +
            cal.get(Calendar.HOUR_OF_DAY) + " ?" +
            " * " + days + "/" + schedule.getInterval();
    }

    private static String getCronStringMonthly(ScheduleMonthlyValue schedule)
        throws ScheduleParseException
    {
        Calendar cal = new GregorianCalendar();
        cal.setTime(schedule.getStart());

        if (schedule.isOffset()) {
            // every x month(s) on the xth [mon-sun]
            return cal.get(Calendar.SECOND) + " " +
                cal.get(Calendar.MINUTE) + " " +
                cal.get(Calendar.HOUR_OF_DAY) + " ? " +
                "1-12/" + schedule.getInterval() + " " +
                schedule.getDayOfWeek() + "#" + 
                schedule.getWeekOfMonth();
        } else {
            // every x month(s) on the xth day
            return cal.get(Calendar.SECOND) + " " +
                cal.get(Calendar.MINUTE) + " " +
                cal.get(Calendar.HOUR_OF_DAY) + " " +
                schedule.getDay() + " " +
                "1-12/" + schedule.getInterval() + " ?";
        }
    }

    private static String getCronStringSingle(ScheduleSingleValue schedule)
        throws ScheduleParseException
    {
        // Quartz does not allow for cron strings that only occur once.  We
        // work around by using a Simple trigger
        //
        // Calendar cal = new GregorianCalendar();
        // cal.setTime(schedule.getStart());
        //
        // return 
        //    cal.get(Calendar.SECOND) + " " +
        //    cal.get(Calendar.MINUTE) + " " +
        //    cal.get(Calendar.HOUR_OF_DAY) + " " +
        //    cal.get(Calendar.DAY_OF_MONTH) + " " +
        //    cal.get(Calendar.MONTH) + " " +
        //    "?" + " " +
        //    cal.get(Calendar.YEAR);

        return null;
    }
}

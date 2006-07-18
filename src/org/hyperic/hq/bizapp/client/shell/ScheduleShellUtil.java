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

package org.hyperic.hq.bizapp.client.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleDailyValue;
import org.hyperic.hq.scheduler.ScheduleWeeklyValue;
import org.hyperic.hq.scheduler.ScheduleMonthlyValue;
import org.hyperic.hq.scheduler.ScheduleSingleValue;


/**
 * A utility class that encapsulates parsing options for commands
 * that relate to scheduling.  For example, both control and autoinventory
 * have "schedule" sub-commands that accept  <-single -daily -weekly -monthly>
 * and it's nice to have a single chunk of code to handle these cases.
 */
public class ScheduleShellUtil {

    public static final String PARAM_SINGLE  = "-single";
    public static final String PARAM_DAILY   = "-daily";
    public static final String PARAM_WEEKLY  = "-weekly";
    public static final String PARAM_MONTHLY = "-monthly";

    public static final String[] PARAM_VALID_RECURRANCE = {
        PARAM_SINGLE,
        PARAM_DAILY,
        PARAM_WEEKLY,
        PARAM_MONTHLY
    };

    public ScheduleShellUtil () {}

    public boolean isValidRecurrance(String recur)
    {
        for (int i = 0; i < PARAM_VALID_RECURRANCE.length; i++) {
            if (recur.equals(PARAM_VALID_RECURRANCE[i]))
                return true;
        }
        return false;
    }
    
    /**
     * Parse command options to produce a ScheduleValue object.
     *
     * @param args The options to parse to generate the ScheduleValue
     * @param startIndex Which option in the args array should be 
     * considered "first".  Options that appear before this one in the
     * array will be ignored.  This is provided for convenience, so you can
     * use an existing args array that you probably already have hanging
     * around when you call this method.
     * @param df The date format to use when parsing and formatting dates.
     * @param schedName What is being scheduled.  For example, "control action"
     * @param caller The shell command that is calling this method.  We use
     * this object to get the output stream, error stream, and a reference back
     * to the shell so we can use it's getInput method to request user input.
     * @exception ScheduleParseException If something goes wrong parsing the 
     * user's data.
     */
    public ScheduleValue parseOptions (String[] args, 
                                       int startIndex,
                                       DateFormat df,
                                       String schedName,
                                       ShellCommandBase caller)
        throws ScheduleParseException {

        if (!isValidRecurrance(args[startIndex])) {
            throw new ScheduleParseException("Invalid recurrance " +
                                             args[startIndex]);
        }

        String description;
        Date startDate;
        Date endDate = null; // Avoid compiler warnings.
        int interval = 0;
        String errMsg;
        ScheduleValue schedValue;
        PrintStream out = caller.getOutStream();
        PrintStream err = caller.getErrStream();
        ShellBase shell = caller.getShell();

        try {
            out.println("[ Configuring scheduled " + schedName + " ]");
            
            Date now = new Date(System.currentTimeMillis() + 60 * 1000);
            
            description = 
                shell.getInput("Enter scheduled " + schedName
                               + " description " +
                               "[default '']: ", false);
            
            String startStr = shell.getInput("Enter start time " +
                                             "[default: '" +
                                             df.format(now) + "']: ", 
                                             false);
            if (startStr == null || startStr.equals("")) {
                startDate = now;
            } else {
                startDate = df.parse(startStr);
            }
            
            if (!args[startIndex].equals(PARAM_SINGLE)) {
                String endStr = 
                    shell.getInput("Enter end time [default '']: ", 
                                   false);
                if (endStr == null || endStr.equals("")) {
                    endDate = null;
                } else {
                    endDate = df.parse(endStr);
                }
                
                String intervalStr = shell.getInput("Enter interval " +
                                                    "[default '1']: ",
                                                    false);
                if (intervalStr == null || intervalStr.equals("")) {
                    interval = 1;
                } else {
                    interval = Integer.parseInt(intervalStr);
                }
            }
            
        } catch (Exception e) {
            errMsg = "Error parsing input: " + e.getMessage();
            err.println(errMsg);
            throw new ScheduleParseException(errMsg, e);
        }
        
        if (args[startIndex].equals(PARAM_SINGLE)) {
            schedValue = new ScheduleSingleValue(startDate);
            schedValue.setDescription(description);
            
        } else if (args[startIndex].equals(PARAM_DAILY)) {
            schedValue = new ScheduleDailyValue(startDate, endDate, interval);
            
            String everyWeekDay;
            try {
                everyWeekDay =
                    shell.getInput("Recur only on weekdays? " +
                                   "[default 'n']: ", false);
            } catch ( IOException e ) {
                throw new ScheduleParseException("Error reading input", e);
            }
            if (everyWeekDay == null || everyWeekDay.equals("")) {
                ((ScheduleDailyValue) schedValue).setEveryWeekDay(false);
            } else if (everyWeekDay.equals("y") ||
                       everyWeekDay.equals("yes")) {
                ((ScheduleDailyValue) schedValue).setEveryWeekDay(true);
            }
            
            schedValue.setDescription(description);
            
        } else if (args[startIndex].equals(PARAM_WEEKLY)) {
            schedValue = new ScheduleWeeklyValue(startDate, endDate, interval);
            
            // XXX: which days to recur on? mon, tues, etc
            
            schedValue.setDescription(description);
            
        } else if (args[startIndex].equals(PARAM_MONTHLY)) {
            schedValue = new ScheduleMonthlyValue(startDate, endDate, interval);
            
            int day;
            String dayStr;
            try {
                dayStr =
                    shell.getInput("Day of the month to recur on " +
                                   "[default '1']: ", false);
            } catch ( IOException e ) {
                throw new ScheduleParseException("Error reading input", e);
            }
            if (dayStr == null || dayStr.equals("")) {
                day = 1;
            } else {
                try {
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException e) {
                    errMsg = "Error parsing input: " + e.getMessage();
                    err.println(errMsg);
                    throw new ScheduleParseException(errMsg);
                }
            }
            
            ((ScheduleMonthlyValue) schedValue).setDay(day);
            schedValue.setDescription(description);
            
        } else {
            // Shouldn't happen, we validate the recurrance
            throw new ScheduleParseException("Unknown repeat " +
                                             "interval: " + args[startIndex]);
        }

        return schedValue;
    }

    public static final String SCHEDULE_OPTIONS
        = "<" + PARAM_SINGLE
        + " " + PARAM_DAILY
        + " " + PARAM_WEEKLY
        + " " + PARAM_MONTHLY
        + ">";

    public String getScheduleOptions () {
        return SCHEDULE_OPTIONS;
    }
}

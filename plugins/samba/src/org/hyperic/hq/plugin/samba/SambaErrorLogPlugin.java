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

package org.hyperic.hq.plugin.samba;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class SambaErrorLogPlugin extends LogFileTailPlugin
{
    private static final Pattern fatal = Pattern.compile("fatal",
                                                Pattern.CASE_INSENSITIVE),
                                 failed = Pattern.compile("failed",
                                                Pattern.CASE_INSENSITIVE),
                                 error = Pattern.compile("error",
                                                Pattern.CASE_INSENSITIVE),
                                 beginLine =
                                Pattern.compile("\\[\\d{4}\\/\\d{2}\\/\\d{2}");

    private static final String FATAL  = "Fatal",
                                ERROR  = "Error",
                                FAILED = "Failed",
                                INFO   = "Information",
                                DEBUG  = "Debug";

    private String myBegin = "";
    private static final String[] LOG_LEVELS = {
        FATAL, ERROR+","+FAILED, INFO, DEBUG
    };

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line)
    {
        String errorLevel;
System.out.println(line);
        if (beginLine.matcher(line).find())
        {
            myBegin = line;
            return null;
        }

        try
        {
            String msg = myBegin+line;
            myBegin = "";

            String[] toks = msg.split("\\s");

            //time section will look like
            //[2007/07/31 14:34:32, 2]
            long millis = getTimeMillis(toks[0].substring(1, toks[0].length()),
                                    toks[1].substring(0, toks[1].length()-1));

            int logLevel = 0;
            try {
                logLevel = Integer.parseInt(toks[2].substring(0,1));
            } catch (NumberFormatException e) {
            }

            return newTrackEvent(millis, getErrorType(msg, logLevel),
                                 info.getName(), msg);
        }
        catch (NumberFormatException e) {
        }
        return null;
    }

    private String getErrorType(String line, int logLevel)
    {
        if (logLevel > 0)
            return DEBUG;
        if (fatal.matcher(line).find())
            return FATAL;
        else if (failed.matcher(line).find())
            return FAILED;
        else if (error.matcher(line).find())
            return ERROR;
        else
            return INFO;
    }

    private long getTimeMillis(String date, String timeOfDay)
        throws NumberFormatException
    {
        Calendar rtn = Calendar.getInstance();
        rtn.clear();
        String[] dateToks = date.split("\\/"),
                 timeToks = timeOfDay.split(":");

        if (dateToks.length != 3 || timeToks.length != 3)
            return System.currentTimeMillis();

        int month = Integer.parseInt(dateToks[0]),
            day   = Integer.parseInt(dateToks[1]),
            year  = Integer.parseInt(dateToks[2]),
            hour  = Integer.parseInt(timeToks[0]),
            min   = Integer.parseInt(timeToks[1]),
            secs  = Integer.parseInt(timeToks[2]);
        rtn.set(Calendar.MONTH, month-1);
        rtn.set(Calendar.DAY_OF_MONTH, day);
        rtn.set(Calendar.YEAR, year);
        rtn.set(Calendar.HOUR_OF_DAY, hour);
        rtn.set(Calendar.MINUTE, min);
        rtn.set(Calendar.SECOND, secs);
        return rtn.getTimeInMillis();
    }
}

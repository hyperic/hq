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

package org.hyperic.hq.plugin.coldfusion;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

public class ColdfusionErrorLogPlugin extends LogFileTailPlugin
{
    private static final String[] LOG_LEVELS = {
        "Fatal,Error", //Error
        "Warning", //Warning
        "Information", //Info
        "Debug" //Debug
    };

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line)
    {
        String errorLevel;
        if (null == (errorLevel = getErrorLevel(line)))
            return null;

        try
        {
            String[] toks = line.split("\",\"");
            return newTrackEvent(getTimeMillis(toks[2], toks[3]),
                                 errorLevel, info.getName(),
                                 getMessage(toks[1], toks[5]));
        }
        catch (NumberFormatException e) {
        }
        return null;
    }

    private String getErrorLevel(String line)
    {
        for (Iterator it=getLogLevelMap().keySet().iterator(); it.hasNext();)
        {
            String level = (String)it.next();
            if ((line.indexOf(level)) != -1)
                return level;
        }
        return null;
    }

    private String getMessage(String thread, String msg)
        throws NumberFormatException
    {
        return thread+": "+msg;
    }

    private long getTimeMillis(String date, String timeOfDay)
        throws NumberFormatException
    {
        Calendar rtn = Calendar.getInstance();
        rtn.clear();
        String[] dateToks = date.split("\\/"),
                 timeToks = timeOfDay.split("\\/");
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

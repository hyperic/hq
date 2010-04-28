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

package org.hyperic.hq.plugin.sybase;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

import java.util.Calendar;
import java.util.regex.Pattern;

public class SybaseErrorLogPlugin extends LogFileTailPlugin
{
    private static final String SEVERITY   = "Severity:",
                                ERROR_EXPR = SEVERITY;
    private static final Pattern SEVERITY_PATTERN = Pattern.compile(SEVERITY),
                                 ERROR_PATTERN    = SEVERITY_PATTERN;

    public TrackEvent processLine(FileInfo info, String line)
    {
        try
        {
            if (!ERROR_PATTERN.matcher(line).find())
                return null;

            return newTrackEvent(getTimeMillis(line),
                                 getErrorLevel(line),
                                 info.getName(),
                                 getMessage(line));
        }
        catch (NumberFormatException e) {
        }
        catch (SybasePluginException e) {
        }
        return null;
    }

    private String getMessage(String line)
        throws NumberFormatException, SybasePluginException
    {
        return (line.split("\\s+", 3))[2];
    }

    private int getErrorLevel(String line)
        throws NumberFormatException, SybasePluginException
    {
        if (!SEVERITY_PATTERN.matcher(line).find())
            throw new SybasePluginException();

        String[] array = line.split(SEVERITY),
                 array2 = array[1].split("\\s+");
        String level = array2[0].substring(0, array2[0].length()-1);
        return Integer.parseInt(level);
    }

    private long getTimeMillis(String line)
        throws NumberFormatException
    {
        Calendar rtn = Calendar.getInstance();
        rtn.clear();
        //00:00000:00015:2007/04/19 11:35:11.69 server  Error: 2812, Severity: 16, State: 5
        String[] array = line.split(":");
        String date_buf = array[3];
        array = line.split("\\s+");
        String time_buf = array[1];
        array = date_buf.split("\\/");
        String[] array2 = time_buf.split(":");
        rtn.set(getInt(array[0]), getInt(array[1]), getInt(array[2]),
                getInt(array2[0]), getInt(array2[1]));
        return rtn.getTimeInMillis();
    }

    public int getInt(String integer)
        throws NumberFormatException
    {
        return Integer.parseInt(integer);
    }
}

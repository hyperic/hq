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

package org.hyperic.hq.product;

import java.util.Iterator;

import org.hyperic.sigar.FileInfo;

public class Log4JLogTrackPlugin
    extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "FATAL,ERROR", //Error
        "WARN", //Warning
        "INFO", //Info
        "DEBUG" //Debug
    };

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line) {
        Log4JEntry entry = new Log4JEntry();
        if (!entry.parse(line)) {
            return null;
        }

        return newTrackEvent(System.currentTimeMillis(),
                             entry.level,
                             info.getName(),
                             entry.message);
    }

    class Log4JEntry {
        String message;
        String level;
        
        //Since log4j formats can vary, just parse the full line and log level
        public boolean parse(String line) {
            for (Iterator it = getLogLevelMap().keySet().iterator();
                 it.hasNext();)
            {
                String level = (String)it.next();
                int idx;
                if ((idx = line.indexOf(level)) != -1) {
                    this.level = line.substring(idx, idx + level.length());
                    this.message = line;
                    
                    return true;
                }
            }

            return false;
        }
    }
}

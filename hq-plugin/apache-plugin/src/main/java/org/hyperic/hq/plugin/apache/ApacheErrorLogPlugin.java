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

package org.hyperic.hq.plugin.apache;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

public class ApacheErrorLogPlugin
    extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "emerg,alert,crit,error", //Error
        "warn", //Warning
        "info,notice", //Info
        "debug" //Debug
    };

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line) {
        ApacheErrorLogEntry entry = new ApacheErrorLogEntry();
        if (!entry.parse(line)) {
            return null;
        }

        return newTrackEvent(System.currentTimeMillis(), //XXX parse entry.timeStamp
                             entry.level,     
                             info.getName(),
                             entry.message);
    }

    class ApacheErrorLogEntry {
        String message;
        String timeStamp;
        String level;
        
        //XXX need charAt sanity checks
        public boolean parse(String line) {
            //parse "[Wed Jan 21 20:47:35 2004] "
            if (line.charAt(0) != '[') {
                return false;
            }
            int ix = line.indexOf("]");
            if (ix == -1) {
                return false;
            }
            this.timeStamp = line.substring(1, ix);
        
            ix++;
            while (line.charAt(ix) == ' ') {
                ix++;
            }

            //parse "[error] "
            line = line.substring(ix);
            if (line.charAt(0) != '[') {
                return false;
            }
            ix = line.indexOf("]");
            if (ix == -1) {
                return false;
            }
            this.level = line.substring(1, ix);

            //rest is the message
            ix++;
            while (line.charAt(ix) == ' ') {
                ix++;
            }
            this.message = line.substring(ix);

            return true;
        }
    }
}

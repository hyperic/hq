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

package org.hyperic.hq.plugin.mysql;

import java.io.File;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MySQLErrorLogTrackPlugin
    extends LogFileTailPlugin {

    private static Log log =
        LogFactory.getLog(LogFileTailPlugin.class.getName());

    public String getDefaultLogFile(String installPath) {
        File dataDir = new File(installPath, "data");

        if (isWin32()) {
            return new File(dataDir, "mysql.err").getAbsolutePath();
        } else {
            // hostname is used on unix platforms.
            String hostname = getPlatformName();
            return new File(dataDir, hostname + ".err").getAbsolutePath();
        }
    }

    public TrackEvent processLine(FileInfo info, String line) {

        int level;

        if (line.indexOf("ERROR") != -1) {
            level = LOGLEVEL_ERROR;
        } else if (line.indexOf("Note") != -1) {
            level = LOGLEVEL_INFO;
        } else {
            //level = LOGLEVEL_DEBUG;
            return null;
        }

        return newTrackEvent(System.currentTimeMillis(),
                             level,
                             info.getName(),
                             line);
    }
}

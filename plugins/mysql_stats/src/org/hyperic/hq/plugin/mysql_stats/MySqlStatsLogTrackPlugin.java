/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.mysql_stats;

import java.util.Calendar;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileInfo;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.StringConfigOption;

public class MySqlStatsLogTrackPlugin extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = new String[0];
    private final Log _log = LogFactory.getLog(MySqlStatsLogTrackPlugin.class);
    private final Pattern TIME_REGEX = Pattern.compile(" Time: "),
                          HASH_REGEX = Pattern.compile("^#");
    private static final String SLOW_LOG_FILE = "slow_query.file";
    private long _time = -1l;
    private final StringBuffer _lineBuf = new StringBuffer();
    private static final int MAX_LENGTH = 2000;

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    protected boolean supportsLogLevels() {
        return false;
    }

    protected ConfigOption getFilesOption(TypeInfo info, ConfigResponse config) {
        StringConfigOption option = new StringConfigOption(
            SLOW_LOG_FILE, "Slow Query Log File",
            config.getValue("installpath") + "/log/mysql_general.slow");
        option.setOptional(true);
        return option;
    }

    public String[] getFiles(ConfigResponse config) {
        return new String[] {config.getValue(SLOW_LOG_FILE)};
    }

    public TrackEvent processLine(FileInfo info, String line) {
        if (TIME_REGEX.matcher(line).find()) {
            _time = getTime(line);
        }
        if (_time == -1l) {
            _time = System.currentTimeMillis();
        }
        _lineBuf.append(line);
        if (HASH_REGEX.matcher(line).find()) {
            return null;
        }
        String log = (_lineBuf.length() > MAX_LENGTH) ?
            _lineBuf.substring(0, MAX_LENGTH) : _lineBuf.toString();
        _lineBuf.setLength(0);
        return newTrackEvent(
            _time, LogTrackPlugin.LOGLEVEL_ANY, info.getName(), log);
    }

    protected String getDefaultLogFile(TypeInfo info, ConfigResponse config) {
        String file = config.getValue(SLOW_LOG_FILE);
        if (file == null) {
            return "";
        }
        if (info.isWin32Platform()) {
            file = StringUtil.replace(file, "/", "\\");
        }
        else {
            file = StringUtil.replace(file, "\\", "/");
        }
        return file;
    }
    
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        
    }

    private long getTime(String line) {
        // # Time: 081222 11:13:53
        // NOTE: this time format is horrible, hopefully it holds up
        String[] toks = line.split("\\s+");
        String dateBuf = toks[2];
        Calendar cal = Calendar.getInstance();
        cal.clear();
        char[] dateToks = dateBuf.toCharArray();
        int year = Integer.valueOf(dateToks[0] + "" + dateToks[1]).intValue();
        year = year + 2000;
        cal.set(Calendar.YEAR, year);
        int month = Integer.valueOf(dateToks[2] + "" + dateToks[3]).intValue();
        month--;
        cal.set(Calendar.MONTH, month);
        int day = Integer.valueOf(dateToks[4] + "" + dateToks[5]).intValue();
        cal.set(Calendar.DAY_OF_MONTH, day);
        String timeBuf = toks[3];
        String[] timeToks = timeBuf.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeToks[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(timeToks[1]));
        cal.set(Calendar.SECOND, Integer.parseInt(timeToks[2]));
        return cal.getTimeInMillis();
    }

}

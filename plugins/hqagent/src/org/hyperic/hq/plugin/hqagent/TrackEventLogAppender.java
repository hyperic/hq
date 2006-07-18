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

package org.hyperic.hq.plugin.hqagent;

import org.hyperic.hq.product.LogTrackPlugin;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * log4j appender sends error log events directly to
 * the LogTrackPluginManager avoiding log file parsing.
 */
public class TrackEventLogAppender extends AppenderSkeleton {

    private LogTrackPlugin plugin;
    private String source;

    public TrackEventLogAppender(LogTrackPlugin plugin,
                                 String source) {
        this.plugin = plugin;
        this.source = source;
        setName(plugin.getName());
    }

    public static int toLogTrackLevel(Priority level) {
        switch (level.toInt()) {
          case Level.FATAL_INT:
          case Level.ERROR_INT:
            return LogTrackPlugin.LOGLEVEL_ERROR;
          case Level.WARN_INT:
            return LogTrackPlugin.LOGLEVEL_WARN;
          case Level.INFO_INT:
            return LogTrackPlugin.LOGLEVEL_INFO;
          case Level.DEBUG_INT:
          default:
            return LogTrackPlugin.LOGLEVEL_DEBUG;
        }
    }

    protected void append(LoggingEvent event) {
        Priority level = event.level;
        //XXX level field is deprecated in newer log4j
        //Level level = event.getLevel();

        String category = event.categoryName;
        int ix = category.lastIndexOf(".");
        if (ix > -1) {
            category = category.substring(ix+1);
        }

        this.plugin.reportEvent(event.timeStamp,
                                toLogTrackLevel(level),
                                this.source,
                                "[" + category + "]" +
                                " " +
                                event.getMessage().toString());                
    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }
}

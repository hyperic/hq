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

import org.hyperic.sigar.win32.EventLogNotification;
import org.hyperic.sigar.win32.EventLogRecord;
import org.hyperic.sigar.win32.EventLog;

/**
 * Base class for windows event notifications.   This handles parsing the event
 * record and sending it to HQ.  All plugins sould subclass this class if they
 * need to override the standard matches() function in the base class.
 */
public abstract class Win32EventLogNotification 
    implements EventLogNotification
{
    public static final String PROP_EVENT_LOGS =
        "platform.log_track.eventlogs";

    private LogTrackPlugin plugin;

    public Win32EventLogNotification(LogTrackPlugin plugin) {
        super();
        this.plugin = plugin;
    }

    public abstract boolean matches(EventLogRecord record);

    public String getLogName() {
        String name = plugin.getTypeProperty("EVENT_LOG_NAME");

        if (name == null) {
            name = EventLog.APPLICATION;
        }

        return name;
     }

    public void handleNotification(EventLogRecord record) { 

        // Make time in milliseconds
        long generated = record.getTimeGenerated() * 1000;

        // Message will include source name
        String msg = record.getSource() + ": " + record.getMessage();

        TrackEvent event =
            this.plugin.newTrackEvent(generated,
                                      mapLogLevel(record.getEventType()),
                                      record.getLogName(),
                                      msg);
    
        if (event != null) {
            this.plugin.getManager().reportEvent(event);
        }
    }

    /**
     * Maps event log levels to the levels defined within HQ.
     */
    private int mapLogLevel(int level)
    {
        switch (level) {
        case EventLog.EVENTLOG_ERROR_TYPE:
            return LogTrackPlugin.LOGLEVEL_ERROR;
        case EventLog.EVENTLOG_WARNING_TYPE:
            return LogTrackPlugin.LOGLEVEL_WARN;
        case EventLog.EVENTLOG_INFORMATION_TYPE:
        case EventLog.EVENTLOG_AUDIT_SUCCESS:
        case EventLog.EVENTLOG_AUDIT_FAILURE:
        case EventLog.EVENTLOG_SUCCESS:
            // All info and audit logs will be INFO
            return LogTrackPlugin.LOGLEVEL_INFO;
        default:
            throw new IllegalArgumentException("Unknown log level: " +
                                               level);
        }
    }
}

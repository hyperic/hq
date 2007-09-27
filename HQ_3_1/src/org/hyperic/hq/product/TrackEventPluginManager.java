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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.sigar.win32.EventLog;
import org.hyperic.sigar.win32.EventLogThread;

import org.hyperic.sigar.FileWatcher;
import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;

public abstract class TrackEventPluginManager
    extends PluginManager {

    public static final int DEFAULT_INTERVAL = 60 * 1 * 1000;

    public static final String PROP_INTERVAL =
        "track.interval";

    //XXX probably better off in another file of somesort.
    private static final String[][] GENERIC_HELP = {
        {
            ConfigTrackPlugin.PROP_ENABLE,
            "Check to enable config tracking."
        },
        {
            ConfigFileTrackPlugin.PROP_FILES,
            "Comma delimited list of configuration files to track. " +
            "Relative files are resolved to ${installpath}."
        },
        {
            LogTrackPlugin.PROP_ENABLE,
            "Check to enable log tracking."
        },
        {
            LogTrackPlugin.PROP_LEVEL,
            "Only track events of level greater than or equal to this level.  " +
            "Order is: " + Arrays.asList(LogTrackPlugin.LOGLEVEL_LABELS)
        },
        {
            LogTrackPlugin.PROP_INCLUDE,
            "Include messages that match the given regular expression.  " +
            "The given pattern can be a substring to look for in log messages " +
            "or a regular expression.  " +
            "See: " +
            "<a href=\"http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html\">" +
            "java.util.regex.Pattern</a>."
        },
        {
            LogTrackPlugin.PROP_EXCLUDE,
            "Exclude messages that match the given regular expression.  "
        },
        {
            LogFileTrackPlugin.PROP_FILES,
            "Comma delimited list of log files to track. " +
            "Relative files are resolved to ${installpath}."
        },
        {
            Win32EventLogNotification.PROP_EVENT_LOGS,
            "Comma delimited list of Event Log names to track. " +
            "Value of <b>*</b> will track all existing Event Logs."
        }
    };
    
    private long fileWatchInterval = 0;
    private long eventLogInterval  = 0;
    private long runnableTrackInterval = 0;

    private HashMap eventLogs = new HashMap();
    private FileWatcherThread fileWatcherThread = null;
    private RunnableTrackThread runnableTrackThread = null;

    // A list of in-memory events that are waiting to be flushed to
    // the AgentStorageProvider.  The TrackerThread queries this list
    // periodically.  This data may be lost if the agent crashes.. on
    // normal shutdown the list is flushed to disk before the 
    // TrackerThread exits.
    private LinkedList events = new LinkedList();

    public TrackEventPluginManager() {
        super();
    }

    public TrackEventPluginManager(Properties props) {
        super(props);
    }

    public abstract String getName();

    public void reportEvent(TrackEvent event) {
        synchronized(this.events) {
            this.events.add(event);
        }
    }

    public LinkedList getEvents() {

        LinkedList eventsToReport;

        if (this.events.isEmpty())
            return new LinkedList();

        synchronized(this.events) {
            // We have something to report.
            eventsToReport = (LinkedList)this.events.clone();
            this.events.clear();
        }

        return eventsToReport;
    }

    private long getInterval(PluginManager manager, String source) {
        String prop = PROP_INTERVAL + "." + source;
        String interval =
            manager.getProperty(prop,
                                manager.getProperty(PROP_INTERVAL));

        if (interval == null) {
            return DEFAULT_INTERVAL;
        }
        else {
            return Long.parseLong(interval) * 1000;
        }
    }

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        this.fileWatchInterval = getInterval(manager, "files");
        this.eventLogInterval = getInterval(manager, "eventlog");
        this.runnableTrackInterval = getInterval(manager, "runnable");
    }

    public void shutdown()
        throws PluginException {

        super.shutdown();

        if (this.fileWatcherThread != null) {
            this.fileWatcherThread.doStop();
            this.fileWatcherThread = null;
        }

        if (this.runnableTrackThread != null) {
            this.runnableTrackThread.doStop();
            this.runnableTrackThread = null;
        }

        closeEventLogs();
        
        ConfigFileTrackPlugin.cleanup();
        LogFileTailPlugin.cleanup();
    }

    private FileWatcherThread getFileWatcherThread() {
        if (this.fileWatcherThread == null) {
            this.fileWatcherThread = FileWatcherThread.getInstance();
            if (this.fileWatchInterval != 0) {
                this.fileWatcherThread.setInterval(this.fileWatchInterval);
            }
            this.fileWatcherThread.doStart();            
        }

        return this.fileWatcherThread;
    }

    public void addFileWatcher(FileWatcher watcher) {
        getFileWatcherThread().add(watcher);
    }

    public void removeFileWatcher(FileWatcher watcher) {
        getFileWatcherThread().remove(watcher);
    }

    private EventLogThread getEventLogThread(String name) {
        name = name.toUpperCase();
        EventLogThread instance =
            (EventLogThread)eventLogs.get(name);

        if (instance == null) {
            instance = new EventLogThread();
            instance.setLogName(name);
            if (this.eventLogInterval != 0) {
                instance.setInterval(this.eventLogInterval);
            }
            instance.doStart();
            eventLogs.put(name, instance);
            log.debug("Created EventLogThread(" + name + ")");
        }

        return instance;
    }

    public void closeEventLogs() {
        for (Iterator it = eventLogs.values().iterator();
             it.hasNext();)
        {
            EventLogThread eventLogThread =
                (EventLogThread)it.next();
            eventLogThread.doStop();
        }
        eventLogs.clear();
    }

    private String[] getEventLogNames(Win32EventLogNotification notifier)
        throws PluginException {

        String[] eventLogs = EventLog.getLogNames();
        String name = notifier.getLogName();
        if (name.equals("*")) {
            return eventLogs;
        }
        else {
            //Validate the Event Log exists due to:
            //http://msdn2.microsoft.com/en-us/library/aa363672.aspx
            //"If a custom log cannot be found,
            //the event logging service opens the Application log"
            HashMap lcNames = new HashMap();
            for (int i=0; i<eventLogs.length; i++) {
                lcNames.put(eventLogs[i].toLowerCase(),
                            Boolean.TRUE);
            }

            StringTokenizer tok = new StringTokenizer(name, ",");
            int i=0, num = tok.countTokens();
            String[] names = new String[num];

            while (tok.hasMoreTokens()) {
                String logName = tok.nextToken();
                if (lcNames.get(logName.toLowerCase()) != Boolean.TRUE) {
                    String msg =
                        "Event Log '" + logName + "' does not exist";
                    throw new PluginException(msg);
                }
                names[i++] = logName;
            }
            return names;
        }
    }

    public void addEventLogNotification(Win32EventLogNotification notifier)
        throws PluginException {

        String[] names = getEventLogNames(notifier);
        for (int i=0; i<names.length; i++) {
            getEventLogThread(names[i]).add(notifier);
        }
    }

    public void removeEventLogNotification(Win32EventLogNotification notifier)
        throws PluginException {

        String[] names = getEventLogNames(notifier);
        for (int i=0; i<names.length; i++) {
            getEventLogThread(names[i]).remove(notifier);
        }
    }
    
    private RunnableTrackThread getRunnableTrackThread() {
        if (this.runnableTrackThread == null) {
            this.runnableTrackThread = RunnableTrackThread.getInstance();
            if (this.runnableTrackInterval != 0) {
                this.runnableTrackThread.setInterval(this.runnableTrackInterval);
            }
            this.runnableTrackThread.doStart();
        }

        return this.runnableTrackThread;
    }

    public void addRunnableTracker(Runnable tracker) {
        getRunnableTrackThread().add(tracker);
    }

    public void removeRunnableTracker(Runnable tracker) {
        getRunnableTrackThread().remove(tracker);
    }
    
    static String getGenericHelp(LogTrackPluginManager ltpm,
                                 ConfigSchema schema, TypeInfo info) {
        StringBuffer buffer = null;
        boolean isPlatform = info.getType() == TypeInfo.TYPE_PLATFORM;
        String defaultDir =
            info.isWin32Platform() ? "\\" : "/";
        LogTrackPlugin plugin =
            ltpm.getLogTrackPlugin(info.getName());
        
        for (int i=0; i<GENERIC_HELP.length; i++) {
            String name = GENERIC_HELP[i][0];
            String key =
                GenericPlugin.TYPE_LABELS[info.getType()] +
                name;
            String val = GENERIC_HELP[i][1];
            ConfigOption option = schema.getOption(key);
            if (option == null) {
                option = schema.getOption(name);
            }
            if (option == null) {
                continue;
            }
            if (buffer == null) {
                buffer = new StringBuffer();
                buffer.append("<p><h4>General Log and Config Track Properties</h4></p>\n");
                buffer.append("<ul>\n");
            }
            if (isPlatform) {
                //platforms have no installpath attribute
                val = StringUtil.replace(val, "${installpath}", defaultDir);
            }
            buffer.append("<li>").append(option.getDescription()).
                append(" - ").append(val);
            if (key.endsWith(LogTrackPlugin.PROP_LEVEL) && (plugin != null)) {
                String[] aliases = plugin.getLogLevelAliases();
                String[] levels = LogTrackPlugin.LOGLEVEL_LABELS;
                if (aliases != levels) {
                    buffer.append("<br>Mapping:<ul>\n");
                    for (int j=0; j<levels.length; j++) {
                        buffer.append("<li>").append(aliases[j]).
                        append(" -> ").append(levels[j]).append("</li>\n");
                    }
                    buffer.append("</ul>\n");
                }
            }
            buffer.append("</li>\n");
        }

        if (buffer != null) {
            buffer.append("</ul>\n");
            return buffer.toString();
        }
        else {
            return null;
        }
    }
}

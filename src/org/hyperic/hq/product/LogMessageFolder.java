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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.sigar.FileWatcherThread;

public class LogMessageFolder {
    static final int DEFAULT_REPEAT_WINDOW = FileWatcherThread.DEFAULT_INTERVAL;
    static final int DEFAULT_REPEAT_MAX = 2;

    private static Log log =
        LogFactory.getLog(LogMessageFolder.class.getName());

    private Map _messages = new HashMap();
    private LogTrackPlugin _plugin;
    private long _window;
    private long _max;

    public LogMessageFolder(LogTrackPlugin plugin) {
        _plugin = plugin;

        String window = getProperty("window");
        if (window == null) {
            window =
                getManagerProperty(TrackEventPluginManager.PROP_INTERVAL);
        }
        if (window != null) {
            setRepeatWindow(Integer.parseInt(window) * 1000);
        }
        else {
            setRepeatWindow(DEFAULT_REPEAT_WINDOW);
        }

        String max = getProperty("max");
        if (max != null) {
            setRepeatMax(Integer.parseInt(max));
        }
        else {
            setRepeatMax(DEFAULT_REPEAT_MAX);
        }
    }

    private void debug(String message) {
        String name =
            _plugin.getTypeInfo().getName() +
            " (" + _plugin.getName() + ") ";

        log.debug(name + message);
    }

    public long getRepeatMax() {
        return _max;
    }

    public void setRepeatMax(long max) {
        _max = max;
        if (log.isDebugEnabled()) {
            debug("configured repeat max=" + _max);
        }
    }

    public long getRepeatWindow() {
        return _window;
    }

    public void setRepeatWindow(long window) {
        _window = window;
        if (log.isDebugEnabled()) {
            debug("configured repeat window=" +
                  _window + "ms");
        }
    }

    private String getManagerProperty(String key) {
        return _plugin.getManager().getProperty(key);
    }

    private String getProperty(String key) {
        return getManagerProperty("track.repeat." + key);
    }

    private class MessageCounter {
        private String _message;
        private TrackEvent _event;
        private long _number = 0;
        private long _timestamp = System.currentTimeMillis();

        private MessageCounter(String message) {
            _message = message;
        }
    }

    public boolean shouldFold(TrackEvent event, List messages) {
        boolean shouldFold = false;
        for (int i=0; i<messages.size(); i++) {
            String message = (String)messages.get(i);
            if (shouldFold(event, message)) {
                shouldFold = true;
            }
        }

        return shouldFold;
    }

    public boolean shouldFold(TrackEvent event, String message) {
        long num = addMessage(event, message);
        return num > _max;
    }

    private void reportRepeatMessage(MessageCounter counter, long time) {
        TrackEvent repeat = counter._event;
        String message =
            "Message '" + counter._message +
            "' repeated " + counter._number + " times";

        TrackEvent event = 
            new TrackEvent(_plugin.getName(),
                           time,
                           repeat.getLevel(),
                           repeat.getSource(),
                           message);

        _plugin.getManager().reportEvent(event);
    }

    private boolean checkWindow(TrackEvent event,
                                MessageCounter counter) {
        long now = System.currentTimeMillis();

        if (((now - counter._timestamp) > _window) &&
            (counter._number > _max))
        {
            reportRepeatMessage(counter, now);
            counter._number = 0;
            counter._timestamp = now;
            counter._event = event;

            return true;
        }
        else {
            return false;
        }
    }

    public long addMessage(TrackEvent event, String message) {
        MessageCounter counter =
            (MessageCounter)_messages.get(message);

        if (counter == null) {
            counter = new MessageCounter(message);
            counter._event = event;
            _messages.put(message, counter);
            if (log.isDebugEnabled()) {
                log.debug("Creating counter for message=" + message);
            }
        }
        else {
            checkWindow(event, counter);
        }

        return ++counter._number;
    }
}

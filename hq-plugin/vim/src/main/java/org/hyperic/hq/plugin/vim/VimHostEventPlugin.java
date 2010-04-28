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

package org.hyperic.hq.plugin.vim;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.mo.EventHistoryCollector;
import com.vmware.vim25.mo.EventManager;

public class VimHostEventPlugin
    extends LogTrackPlugin
    implements Runnable {

    private static final long INTERVAL = 1000 * 60 * 5;
    private static final Log _log =
        LogFactory.getLog(VimHostEventPlugin.class.getName());
    protected Properties _props;
    private long _lastCheck;
    private VimUtil _vim; 
    private EventHistoryCollector _history;

    private String getEventClass(Event event) {
        String name = event.getClass().getName();
        int ix = name.lastIndexOf('.');
        if (ix != -1) {
            name = name.substring(ix+1);
        }
        return name;
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);
        _props = config.toProperties();
        setup();
        getManager().addRunnableTracker(this);
    }

    private void setup() throws PluginException {
        _lastCheck = System.currentTimeMillis();
        try {
            _vim = VimUtil.getInstance(_props);
        } catch (PluginException e) {
            _props = null;
            throw e;
        }

        EventFilterSpec eventFilter = new EventFilterSpec();

        try {
            EventManager eventManager = _vim.getEventManager();
            _history =
            eventManager.createCollectorForEvents(eventFilter);
        } catch (Exception e) {
            VimUtil.dispose(_vim);
            _vim = null;
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown() throws PluginException {
        if (_vim != null) {
            getManager().removeRunnableTracker(this);
            VimUtil.dispose(_vim);
            _vim = null;
        }
        super.shutdown();
    }

    private void checkForEvents(VimUtil vim) throws PluginException {
        long now = System.currentTimeMillis();
        if ((now - _lastCheck) < INTERVAL) {
            //XXX checkForUpdates() api?
            return;
        }
        _log.debug("Checking for events");

        if (!_vim.isSessionValid()) {
            VimUtil.dispose(_vim);
            setup();
        }
        Event[] events;
        try {
            events = _history.getLatestPage();
        } catch (Exception e) {
            throw new PluginException("getEvents: " + e, e);
        }

        for (int i = 0; i < events.length; i++) { 
            Event event = events[i];
            long created = event.getCreatedTime().getTimeInMillis();
            if (created < _lastCheck) {
                continue;
            }

            reportEvent(created,
                        //XXX how-to map log level?
                        LogTrackPlugin.LOGLEVEL_INFO,
                        "Event ID=" + event.getKey() + ", " +
                        "User=" + event.getUserName(),
                        "[" + getEventClass(event) + "] " +
                        event.getFullFormattedMessage());
        }

        _lastCheck = now;
    }

    public void run() {
        try {
            checkForEvents(_vim);
        } catch (PluginException e) {
            _log.error("checkForEvents: " + e, e);
        }
    }
}

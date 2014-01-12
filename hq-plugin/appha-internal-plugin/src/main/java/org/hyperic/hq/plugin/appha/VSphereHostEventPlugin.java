/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMWare, Inc.
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

package org.hyperic.hq.plugin.appha;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.EventFilterSpecByTime;

public class VSphereHostEventPlugin extends LogTrackPlugin implements Runnable {

    private static final long INTERVAL = 1000 * 60 * 5;
    private static final Log _log = LogFactory.getLog(VSphereHostEventPlugin.class.getName());
    protected Properties _props;
    private long _lastCheck;

    private String getEventClass(Event event) {
        String name = event.getClass().getName();
        int ix = name.lastIndexOf('.');
        if (ix != -1) {
            name = name.substring(ix+1);
        }
        return name;
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        _props = config.toProperties();
        setup();
        getManager().addRunnableTracker(this);
    }

    private void setup() throws PluginException {
        _lastCheck = System.currentTimeMillis();
    }
    
    public void shutdown() throws PluginException {
        super.shutdown();
    }
    
    private EventFilterSpecByTime getTimeFilter(long begin, long end) {
        EventFilterSpecByTime filter = new EventFilterSpecByTime();
        Calendar beginCal = Calendar.getInstance();
        beginCal.setTimeInMillis(begin);
        filter.setBeginTime(beginCal);
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(end);
        filter.setEndTime(endCal);
        return filter;
    }

    private void processEvents(Event[] events) {
        for (int i = 0; i < events.length; i++) { 
            Event event = events[i];
            if (event == null || (event.getHost() == null && event.getVm() == null)) {
                // XXX we observed that events are being returned that are not associated
                // with the host in the criteria.  this needs to be verified and a bug against
                // vijava filed if it is really the case.
                continue;
            }
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
    }

    private Event[] getEvents() throws PluginException {
        VSphereConnection conn = null;
        try {
            conn = VSphereConnection.getPooledInstance(_props);
            String hostname = getConfig("vm");
            boolean isVm = true;
            if (hostname == null) {
                // Could be a physical host (not a vm)
                hostname = getConfig("hostname");
                isVm = false;
            }
            if (hostname == null) {
                return new Event[0];
            }
            _log.debug("querying events for vm=" + hostname);
            EventFilterSpec criteria = new EventFilterSpec();
            criteria.setTime(getTimeFilter(_lastCheck, now()));
            Event[] events = conn.vim.getEventManager().queryEvents(criteria);
            if (events == null) {
                return new Event[0];
            }
            List<Event> rtn = new ArrayList<Event>(events.length);
            for (Event event : events) {
                if (event.getVm() != null || event.getHost() != null) {
                    event.toString();
                }
                if (isVm && event.getVm() != null && event.getVm().getName().equals(hostname)) {
                    rtn.add(event);
                } else if (!isVm && event.getHost() != null && event.getHost().getName().equals(hostname)) {
                    rtn.add(event);
                }
            }
            _log.debug("returning " + rtn.size() + " events");
            return (Event[]) rtn.toArray(new Event[0]);
        } catch (Exception e) {
            throw new PluginException("getEvents: " + e, e);
        } finally {
            if (conn != null) conn.release();
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public void run() {
        try {
            long now = now();
            if ((now - _lastCheck) < INTERVAL) {
                //XXX checkForUpdates() api?
                return;
            }
            setup();
            Event[] events = getEvents();
            processEvents(events);
            _lastCheck = now;
        } catch (PluginException e) {
            _log.error("checkForEvents: " + e, e);
        }
    }
}

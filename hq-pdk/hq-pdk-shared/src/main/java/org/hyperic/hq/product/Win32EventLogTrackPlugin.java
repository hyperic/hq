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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.sigar.win32.EventLogRecord;
import org.hyperic.util.config.ConfigResponse;

public class Win32EventLogTrackPlugin
    extends LogTrackPlugin {

    private static final Log log =
        LogFactory.getLog("Win32EventLogTrackPlugin");

    private Win32EventLogNotification notifier;

    public Win32EventLogNotification getEventLogNotification() {
        SourceFilterNotification notifier =
            new SourceFilterNotification(this);

        String source =
            getTypeProperty("EVENT_LOG_SOURCE_FILTER");
        if (source == null) {
            source =
                getConfig().getValue(Win32ControlPlugin.PROP_SERVICENAME);
        }

        if (source == null) {
            source = ".*";
        }
        notifier.setSource(source);

        if (log.isDebugEnabled()) {
            String msg =
                "Created filter '" + notifier.source +
                "' for type=" + getTypeInfo().getName();
            log.debug(msg);
        }
        
        return notifier;
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        try {
            this.notifier = getEventLogNotification();
        } catch (IllegalArgumentException e) {
            //programmer error
            String msg =
                "getEventLogNotification error: " +
                e.getMessage();
            throw new PluginException(msg);
        }

        getManager().addEventLogNotification(notifier);
    }

    public void shutdown()
        throws PluginException {

        if (this.notifier != null) {
            getManager().removeEventLogNotification(notifier);
        }

        super.shutdown();
    }
    
    private class SourceFilterNotification extends Win32EventLogNotification {
        private StringMatcher source;
        
        private SourceFilterNotification(LogTrackPlugin plugin) {
            super(plugin);
        }

        private void setSource(String source) {
            this.source = new StringMatcher();
            this.source.setIncludes(source);
        }

        public boolean matches(EventLogRecord record) {
            //XXX may want to maintain 1.3 compat
            return this.source.matches(record.getSource());
        }
    }
}

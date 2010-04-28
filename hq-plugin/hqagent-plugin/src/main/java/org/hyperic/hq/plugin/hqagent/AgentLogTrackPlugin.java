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
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.apache.log4j.Logger;

public class AgentLogTrackPlugin
    extends LogTrackPlugin {

    private TrackEventLogAppender appender = null;

    protected boolean shouldDebugLog() {
        //prevent infinite recursion if -Dlog_track.debug=true
        return false;
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        this.appender =
            new TrackEventLogAppender(this,
                                      "logs/agent.log"); //XXX source field

        Logger.getRootLogger().addAppender(this.appender);
    }

    public void shutdown()
        throws PluginException {

        if (this.appender != null) {
            Logger.getRootLogger().removeAppender(this.appender);
            this.appender = null;
        }

        super.shutdown();
    }
}

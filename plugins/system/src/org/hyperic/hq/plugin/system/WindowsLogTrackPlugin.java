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

package org.hyperic.hq.plugin.system;

import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.Win32EventLogNotification;
import org.hyperic.hq.product.Win32EventLogTrackPlugin;
import org.hyperic.sigar.win32.EventLog;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

public class WindowsLogTrackPlugin
    extends Win32EventLogTrackPlugin
{
    private WhoLogTrackPlugin whoTracker = null;
    private boolean enableWhoTracker;

    protected boolean supportsPatternMatching() {
        return false;
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        this.enableWhoTracker =
            manager.isPropertyEnabled("system.whoTracker", true);
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        if (this.enableWhoTracker) {
            this.whoTracker = new WhoLogTrackPlugin(this);
            getManager().addRunnableTracker(this.whoTracker);
        }
        //XXX getManager().setDefaultPlatformPlugin(this)
        final String prop = LogTrackPluginManager.DEFAULT_PLATFORM_PLUGIN;
        getManager().getProperties().setProperty(prop, getName());
    }

    public void shutdown() throws PluginException {
        if (this.whoTracker != null) {
            getManager().removeRunnableTracker(this.whoTracker);
            this.whoTracker.shutdown();
            this.whoTracker = null;
        }
        super.shutdown();
    }

    static void setDefaultConfig(ConfigResponse config) {
        config.setValue(Win32EventLogNotification.PROP_EVENT_LOGS,
                        EventLog.SYSTEM);
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config) {
        ConfigSchema schema =
            super.getConfigSchema(info, config);

        StringConfigOption option =
            new StringConfigOption(Win32EventLogNotification.PROP_EVENT_LOGS,
                                   "Event Log names",
                                   EventLog.SYSTEM);
        schema.addOption(option);

        return schema;
    }

    public Win32EventLogNotification getEventLogNotification() {
        return new WindowsEventLogNotification(this);
    }
}

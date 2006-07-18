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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.StringUtil;

public class RtPluginManager extends PluginManager {

    public RtPluginManager() {
        super();
    }

    public RtPluginManager(Properties props) {
        super(props);
    }

    public String getName() {
        return ProductPlugin.TYPE_RESPONSE_TIME;
    }

    public boolean supportsEndUser(String name)
        throws PluginNotFoundException
    {
        RtPlugin plugin = (RtPlugin)getPlugin(name);

        return plugin.supportsEndUser();
    }

    public boolean supportsAny(String name)
    {
        try {
            getPlugin(name);
            return true;
        }
        catch (PluginNotFoundException e) {
            return false;
        }
    }

    private ArrayList getNoLog(String noLog)
    {
        if (noLog == null) {
            return new ArrayList();
        }
        return new ArrayList(StringUtil.explode(noLog, " "));
    }

    public Collection getTimes(String name, ConfigResponse config,
                               Integer svcID, Properties prop, boolean eu,
                               boolean collectIPs)
        throws PluginNotFoundException, IOException {

        RtPlugin plugin = (RtPlugin)getPlugin(name);
        
        String logdir;
        String logmask;
        String logfmt;
        int svcType;

        if (eu) {
            logdir = config.getValue(RtPlugin.CONFIG_EULOGDIR);
            logmask = config.getValue(RtPlugin.CONFIG_EULOGMASK);
            logfmt = plugin.getEULogFormat(config);
            svcType = RtPlugin.ENDUSER;
        } else {
            logdir = config.getValue(RtPlugin.CONFIG_LOGDIR);
            logmask = config.getValue(RtPlugin.CONFIG_LOGMASK);
            logfmt = plugin.getLogFormat(config);
            svcType = plugin.getSvcType();
        }
        
        return plugin.getTimes(svcID, prop, logdir, logmask, logfmt, svcType,
                               config.getValue(RtPlugin.CONFIG_TRANSFORM),
                               getNoLog(config.getValue(
                                              RtPlugin.CONFIG_DONTLOG)),
                               collectIPs);
    }
}

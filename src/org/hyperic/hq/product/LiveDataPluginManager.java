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

import org.hyperic.hq.livedata.shared.LiveDataTranslator;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONArray;

import java.util.Properties;

public class LiveDataPluginManager extends PluginManager {

    public LiveDataPluginManager(Properties props) {
        super(props);
    }

    public String getName() {
        return ProductPlugin.TYPE_LIVE_DATA;
    }

    private LiveDataPlugin getLiveDataPlugin(String plugin)
        throws PluginException
    {
        ProductPluginManager mgr = (ProductPluginManager)getParent();
        ProductPlugin pp = mgr.getProductPlugin(plugin);

        //XXX: fix me, this only works for platforms.
        LiveDataPlugin p =
            (LiveDataPlugin)pp.getPlugin(ProductPlugin.TYPE_LIVE_DATA,
                                         pp.getTypeInfo());
        if (p == null) {
            throw new PluginException("Live data plugin for " +
                                      pp.getTypeInfo() + " not found.");
        }
        return p;
    }

    public JSONArray getData(String plugin, String command,
                             ConfigResponse config)
        throws PluginException
    {
        LiveDataPlugin p = getLiveDataPlugin(plugin);
        Object o = p.getData(command, config);

        try {
            return LiveDataTranslator.encode(o);
        } catch (Exception e) {
            throw new PluginException(e);
        }
    }

    public String[] getCommands(String plugin)
        throws PluginException
    {
        LiveDataPlugin p = getLiveDataPlugin(plugin);
        return p.getCommands();
    }
}

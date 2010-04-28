/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;

public class VimPoolDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final Log _log =
        LogFactory.getLog(VimPoolDetector.class.getName());

    private ServerResource discoverPool(VimUtil vim, ResourcePool pool)
        throws Exception {

        String name = pool.getName();
        ServerResource server = createServerResource("/");
        server.setIdentifier(name);
        server.setName(name);
        ConfigResponse config = new ConfigResponse();
        config.setValue(VimPoolCollector.PROP_POOL, name);
        server.setProductConfig(config);
        server.setMeasurementConfig();

        return server;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String hostname = platformConfig.getValue(VimUtil.PROP_HOSTNAME);
        if (hostname == null) {
            return null;
        }
        VimUtil vim = null;
        List servers = new ArrayList();
        try {
            vim = VimUtil.getInstance(platformConfig.toProperties());
            if (!vim.isESX()) {
                return null; //no Pools in VMware server
            }
            ManagedEntity[] pools = vim.find(VimUtil.POOL);
            for (int i=0; i<pools.length; i++) {
                if (!(pools[i] instanceof ResourcePool)) {
                    _log.debug(pools[i] + " not a " + VimUtil.POOL +
                               ", type=" + pools[i].getMOR().getType());
                    continue;
                }
                ResourcePool pool = (ResourcePool)pools[i];
                try {
                    ServerResource server = discoverPool(vim, pool);
                    if (server != null) {
                        servers.add(server);
                    }
                } catch (Exception e) {
                    _log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            VimUtil.dispose(vim);
        }

        return servers;
    }
}

/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.vsphere;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.agent.server.ConfigStorage.Key;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class VCenterDetector extends DaemonDetector {
    private static final Log _log =
        LogFactory.getLog(VCenterDetector.class.getName());

    // TODO: these constants are part of RuntimeAutodiscoverer as private
    // constants, so we need to define them again here.
    private static final String STORAGE_PREFIX  = "runtimeautodiscovery";
    private static final String STORAGE_KEYLIST = "runtimeAD-keylist";
       
    protected VCenterPlatformDetector getPlatformDetector() {
        return new VMAndHostVCenterPlatformDetector();
    }

    /**
     * FIXME: This will be executed twice during a runtime scan,
     * once during getServerResources() and once during discoverServices()
     */
    private void discoverPlatforms(ConfigResponse config)
        throws PluginException {

        Properties props = new Properties();
        props.putAll(getManager().getProperties());
        props.putAll(config.toProperties());
        VSphereUtil vim= null;
		
        try {
            vim = VSphereUtil.getInstance(props);
            getPlatformDetector().discoverPlatforms(new RestApi(props), props, vim);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            VSphereUtil.dispose(vim);
		}
    }
    
    private void discoverPlatforms(AgentDaemon agent)
        throws PluginException {

        try {
            AgentStorageProvider storageProvider = agent.getStorageProvider();
            ConfigStorage storage = new ConfigStorage(storageProvider, 
                                                      STORAGE_KEYLIST, 
                                                      STORAGE_PREFIX);
            Map<Key, ConfigResponse> configs = storage.load();
            
            for (Map.Entry<Key, ConfigResponse> entry : configs.entrySet()) {
                Key key = entry.getKey();
                String type = key.getTypeName();

                if (Constants.VMWARE_VCENTER.equals(type)) {
                    ConfigResponse serverConfig = (ConfigResponse)entry.getValue();
                    discoverPlatforms(serverConfig);
                }
            }            
        } catch (Exception e) {
            _log.error("Could not discover platforms during the default scan: " 
                           + e.getMessage(), e);
        }
    }

    /**
     * Need to discover ESX hosts and VMs during the default scan
     * instead of the runtime scan to improve response time
     */
    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {
    
        // only discover ESX hosts and VMs if a vCenter process exists
        if (!getProcessResources(platformConfig).isEmpty()) {
            AgentDaemon agent = AgentDaemon.getMainInstance();
            discoverPlatforms(agent);
        }
        
        // discover new vCenter servers
        return super.getServerResources(platformConfig);
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //XXX this method only gets called once a day by default
        //but we won't have the vSphere sdk config until the server
        //resource is configured.
        discoverPlatforms(config);
        return super.discoverServices(config);
    }
}

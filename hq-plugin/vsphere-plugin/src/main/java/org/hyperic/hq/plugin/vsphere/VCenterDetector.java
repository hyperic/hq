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
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.HQApiFactory;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;

public class VCenterDetector extends DaemonDetector {

    private static final Log _log =
        LogFactory.getLog(VCenterDetector.class.getName());

    // TODO: these constants are part of RuntimeAutodiscoverer as private
    // constants, so we need to define them again here.
    private static final String STORAGE_PREFIX  = "runtimeautodiscovery";
    private static final String STORAGE_KEYLIST = "runtimeAD-keylist";
    
    private static final String AGENT_TOKEN = CommandsAPIInfo.PROP_AGENT_TOKEN;
    
    protected VCenterPlatformDetector getPlatformDetector() {
        return new VMAndHostVCenterPlatformDetector();
    }


    /**
     * FIXME: This will be executed twice during a runtime scan,
     * once during getServerResources() and once during discoverServices()
     */
    private void discoverPlatforms(AgentDaemon agent, ConfigResponse config)
        throws PluginException {

        Properties props = new Properties();
        props.putAll(getManager().getProperties());
        props.putAll(config.toProperties());
        
        try {
            ProviderInfo providerInfo = CommandsAPIInfo.getProvider(agent.getStorageProvider());
            props.put(AGENT_TOKEN, providerInfo.getAgentToken());
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
        
        VSphereUtil vim= null;
		
        try {
            vim = VSphereUtil.getInstance(props);
            HQApi api = HQApiFactory.getHQApi(agent, props);
            getPlatformDetector().discoverPlatforms(props, api, vim);
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
            Map configs = storage.load();
            
            for (Iterator i = configs.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                ConfigStorage.Key key = (ConfigStorage.Key)entry.getKey();
                String type = key.getTypeName();

                if (AuthzConstants.serverPrototypeVmwareVcenter.equals(type)) {
                    ConfigResponse serverConfig = (ConfigResponse)entry.getValue();
                    discoverPlatforms(agent, serverConfig);
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
    
        final AgentDaemon agent = AgentDaemon.getMainInstance();
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        if (debug) watch.markTimeBegin("discoverPlatforms");
        discoverPlatforms(agent);
        if (debug) watch.markTimeEnd("discoverPlatforms");
        if (debug) _log.debug(watch);
        
        // discover new vCenter servers
        return super.getServerResources(platformConfig);
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //XXX this method only gets called once a day by default
        //but we won't have the vSphere sdk config until the server
        //resource is configured.
    	AgentDaemon agent = AgentDaemon.getMainInstance();
        discoverPlatforms(agent, config);
        return super.discoverServices(config);
    }
}

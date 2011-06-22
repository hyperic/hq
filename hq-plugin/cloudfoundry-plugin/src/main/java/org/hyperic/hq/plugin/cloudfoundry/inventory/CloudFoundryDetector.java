/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 * 
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.plugin.cloudfoundry.inventory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.autoinventory.agent.client.AICommandsUtils;
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.InstanceStats;

public class CloudFoundryDetector extends DaemonDetector {

    private static final Log _log =
        LogFactory.getLog(CloudFoundryDetector.class.getName());

    private static final String SERVER_PROTOTYPE_CLOUD_FOUNDRY = "Cloud Foundry";
    private static final String HIERARCHY_KEY = "application.hierarchy";

    // TODO: these constants are part of RuntimeAutodiscoverer as private
    // constants, so we need to define them again here.
    private static final String STORAGE_PREFIX  = "runtimeautodiscovery";
    private static final String STORAGE_KEYLIST = "runtimeAD-keylist";

    private void runAutoDiscovery(ConfigResponse cf) {
        _log.debug("[runAutoDiscovery] >> start");
        _log.debug("[runAutoDiscovery] >> config=" + cf.toProperties());
        try {
            AgentRemoteValue configARV = AICommandsUtils.createArgForRuntimeDiscoveryConfig(0, 0, "cloudfoundry", null, cf);
            _log.debug("[runAutoDiscovery] configARV=" + configARV);
            AgentCommand ac = new AgentCommand(1, 1, "autoinv:pushRuntimeDiscoveryConfig", configARV);
            AgentDaemon.getMainInstance().getCommandDispatcher().processRequest(ac, null, null);
            _log.debug("[runAutoDiscovery] << OK");
        } catch (Exception ex) {
            _log.debug("[runAutoDiscovery]" + ex.getMessage(), ex);
        }
    }
    
    private void discoverServices(AgentDaemon agent)
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
	            
	            _log.debug("discoverServices type=" + type);
	
	            if (SERVER_PROTOTYPE_CLOUD_FOUNDRY.equals(type)) {
	                ConfigResponse serverConfig = (ConfigResponse)entry.getValue();
	                runAutoDiscovery(serverConfig);
	            }
	        }            
	    } catch (Exception e) {
	        _log.error("Could not discover apps and services during the default scan: " 
	                       + e.getMessage(), e);
	    }
    }
    
    /**
     * Need to discover during the default scan
     * instead of the runtime scan to improve response time
     */
    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {
            	
        // discover new servers
    	List servers = super.getServerResources(platformConfig);
    	
    	_log.debug("[getServerResources] servers=" + servers.size()
    				+ " , platformConfig=" + platformConfig);
    	
    	// get config for existing servers and discover
    	// any new apps or services
    	AgentDaemon agent = AgentDaemon.getMainInstance();
    	discoverServices(agent);

        return servers;
    }
    
    /**
     * Further configure the Cloud Foundry server resource.
     * @param server
     * @param pid
     */
    protected void discoverServerConfig(ServerResource server, long pid) {
        super.discoverServerConfig(server, pid);
        
        try {
            ConfigResponse serverConfig = server.getProductConfig();
            _log.debug("[discoverServerConfig] pid=" + pid
            			+ ", server=" + server
            			+ ", config=" + serverConfig);
	    	CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(serverConfig.toProperties());
	    	
	    	if (cf != null) {
	            ConfigResponse custom = new ConfigResponse();
	    		CloudInfo info = cf.getCloudInfo();
	    		    		
	    		custom.setValue("info.build", info.getBuild().toString());
	    		custom.setValue("info.version", info.getVersion());
	    		custom.setValue("info.name", info.getName());
	    		custom.setValue("info.support", info.getSupport());    				
	    		custom.setValue("info.limits.apps", Integer.toString(info.getLimits().getMaxApps()));
	    		custom.setValue("info.limits.services", Integer.toString(info.getLimits().getMaxServices()));
	    		
	    		// display max memory in GB
	    		double maxMemory = info.getLimits().getMaxTotalMemory() / 1000d;    			
	    		custom.setValue("info.limits.memory", Double.toString(maxMemory) + " GB");
	
	    		server.setCustomProperties(custom);
	    	}
        } catch (Exception e) {
        	_log.debug("Could not set custom server properties", e);
        }
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //return super.discoverServices(config);

        //XXX this method only gets called once a day by default
        //but we won't have the Cloud Foundry config until the server
        //resource is configured.
    	
    	_log.debug("discoverServices: config=" + config);
    	
        List services = new ArrayList();
    	CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(config.toProperties());

    	if (cf != null) {
    		services.addAll(discoverCloudApplications(cf));
    		services.addAll(discoverCloudServices(cf));
    	}
        return services;    	
    }
    
    private List discoverCloudApplications(CloudFoundryClient cf) {
        List services = new ArrayList();

		List<CloudApplication> apps = cf.getApplications();
        
		for (CloudApplication app : apps) {
			try {
	        	JSONObject jsonConfig = discoverResourceHierarchy(app);
	        	JSONArray jsonServices = jsonConfig.getJSONArray("service");
	
	            ServiceResource service = new ServiceResource();
	            service.setType("Cloud Foundry Application");
	            service.setServiceName(app.getName());
	
	            ConfigResponse productCfg = new ConfigResponse();
	            productCfg.setValue("application.name", app.getName());                
	        	productCfg.setValue(HIERARCHY_KEY, jsonConfig.toString());
	            service.setProductConfig(productCfg);
	            
	            service.setMeasurementConfig(new ConfigResponse());
	            
	            ConfigResponse custom = new ConfigResponse();
	        	custom.setValue("staging.model", app.getStaging().get("model"));
	        	custom.setValue("staging.stack", app.getStaging().get("stack"));
	        	custom.setValue("resource.name", app.getName());
	        	custom.setValue("resource.memory", app.getMemory() + " MB");
	        	
	        	String uris = app.getUris().toString();
	        	uris = uris.substring(1, uris.length()-1);
	        	custom.setValue("resource.uri", uris);

	    		// display max disk in GB
	    		double maxDisk = 0;
	    		int maxCores = 0;
            	ApplicationStats stats = cf.getApplicationStats(app.getName());
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		if (stat.getCores() > maxCores) {
            			maxCores = stat.getCores();
            		}
            		double quota = (double) stat.getDiskQuota();
            		if (quota > maxDisk) {
            			maxDisk = quota;
            		}
            	}
            	if (!records.isEmpty()) {
		    		maxDisk = maxDisk / (1024 * 1024 * 1024);
		        	custom.setValue("resource.disk", maxDisk + " GB");
		        	custom.setValue("resource.core", Integer.toString(maxCores));
            	}
	    		
	        	String appServices = "";
	        	if (jsonServices.length() > 0) {
	        		appServices = jsonServices.toString().replace("\"", "");
	        		appServices = appServices.substring(1, appServices.length()-1);
	        	}
	        	custom.setValue("resource.app.services", appServices);
	        	
	        	service.setCustomProperties(custom);
	        	        	
	            services.add(service);
			} catch (Exception e) {
				_log.debug("Could not discover cloud application: " + e.getMessage(), e);
			}
		}
		
		return services;
    }
    
    private JSONObject discoverResourceHierarchy(CloudApplication app) 
    	throws JSONException {
    	
		JSONObject jsonConfig = new JSONObject();
		JSONArray jsonServices = new JSONArray();
		
		_log.debug("appServices=" + app.getServices().size());
		
		for (String s : app.getServices()) {
			jsonServices.put(s);
		}

		jsonConfig.put("service", jsonServices);
		jsonConfig.put("createTs", System.currentTimeMillis());
		
    	return jsonConfig;
    }
    
    private List discoverCloudServices(CloudFoundryClient cf) {
    	List services = new ArrayList();
    	
    	List<CloudService> cloudServices = cf.getServices();
    	
    	_log.debug("services=" + cloudServices.size());
    	
    	for (CloudService cs : cloudServices) {
            ServiceResource service = new ServiceResource();
            String vendor = cs.getVendor();
            
            if ("mysql".equalsIgnoreCase(vendor)) {
            	service.setType("Cloud Foundry MySQL Service");
            } else if ("mongodb".equalsIgnoreCase(vendor)) {
            	service.setType("Cloud Foundry MongoDB Service");            	
            } else if ("redis".equalsIgnoreCase(vendor)) {
            	service.setType("Cloud Foundry Redis Service");            	
            } else if ("rabbitmq".equalsIgnoreCase(vendor)) {
            	service.setType("Cloud Foundry RabbitMQ Service");            	
            } else {
            	_log.info("Unsupported Cloud Foundry service: " + vendor);
            	continue;
            }

            service.setServiceName(cs.getName());

            ConfigResponse productCfg = new ConfigResponse();
            productCfg.setValue("service.name", cs.getName());                
            service.setProductConfig(productCfg);

            service.setMeasurementConfig(new ConfigResponse());
            
            ConfigResponse custom = new ConfigResponse();
        	custom.setValue("resource.name", cs.getName());
        	custom.setValue("resource.tier", cs.getTier());
        	custom.setValue("resource.type", cs.getType());
        	custom.setValue("resource.vendor", vendor);
        	custom.setValue("resource.version", cs.getVersion());
        	service.setCustomProperties(custom);

            services.add(service);
    	}
    	
    	return services;
    }
}

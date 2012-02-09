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
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryProxy;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.InstanceStats;

public class CloudFoundryDetector extends ServerDetector implements AutoServerDetector {

    private static final Log _log =
        LogFactory.getLog(CloudFoundryDetector.class.getName());

    private static final String PROTOTYPE_CLOUD_FOUNDRY = "Cloud Foundry";
    private static final String PROTOTYPE_CLOUD_FOUNDRY_APP = "Cloud Foundry Application";
    private static final String PROTOTYPE_CLOUD_FOUNDRY_MONGODB = "Cloud Foundry MongoDB Service";
    private static final String PROTOTYPE_CLOUD_FOUNDRY_MYSQL = "Cloud Foundry MySQL Service";
    private static final String PROTOTYPE_CLOUD_FOUNDRY_RABBITMQ = "Cloud Foundry RabbitMQ Service";
    private static final String PROTOTYPE_CLOUD_FOUNDRY_REDIS = "Cloud Foundry Redis Service";
    
    private static final String HIERARCHY_KEY = "resource.hierarchy";

    private static final int ARV_DUMMY_INSTANCE_ID = 0;
    
    // TODO: these constants are part of RuntimeAutodiscoverer as private
    // constants, so we need to define them again here.
    private static final String STORAGE_PREFIX  = "runtimeautodiscovery";
    private static final String STORAGE_KEYLIST = "runtimeAD-keylist";

    private void runAutoDiscovery(ConfigResponse cf) {
        _log.debug("[runAutoDiscovery] >> start");
        try {
            AgentRemoteValue configARV = AICommandsUtils.createArgForRuntimeDiscoveryConfig(0, ARV_DUMMY_INSTANCE_ID, PROTOTYPE_CLOUD_FOUNDRY, null, cf);
            _log.debug("[runAutoDiscovery] configARV=" + configARV);
            AgentCommand ac = new AgentCommand(1, 1, "autoinv:pushRuntimeDiscoveryConfig", configARV);
            AgentDaemon.getMainInstance().getCommandDispatcher().processRequest(ac, null, null);
            _log.debug("[runAutoDiscovery] << OK");
        } catch (Exception ex) {
            _log.debug("[runAutoDiscovery]" + ex.getMessage(), ex);
        }
    }
        
    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {
        
    	AgentDaemon agent = AgentDaemon.getMainInstance();
    	
    	return recreateServerResources(agent);
    }

    /**
     * Recreate manually-added server as an autodiscovered
	 * server so that custom properties can be autodiscovered.
	 * 
     * @param agent
     * @return
     */
    private List recreateServerResources(AgentDaemon agent) {
    	List<ServerResource> resources = new ArrayList<ServerResource>();
    	
	    try {
	    	// get config for existing servers
	        AgentStorageProvider storageProvider = agent.getStorageProvider();
	        ConfigStorage storage = new ConfigStorage(storageProvider, 
	                                                  STORAGE_KEYLIST, 
	                                                  STORAGE_PREFIX);
	        Map configs = storage.load();
	        
	        for (Iterator i = configs.entrySet().iterator(); i.hasNext();) {
	            Map.Entry entry = (Map.Entry)i.next();
	            ConfigStorage.Key key = (ConfigStorage.Key)entry.getKey();
	            String typeName = key.getTypeName();
	            
	            if (_log.isDebugEnabled()) {
	            	_log.debug("recreateServerResources typeName=" + typeName
		            			+ ", id=" + key.getId()
		            			+ ", key=" + key);
	            }
	
	            if (PROTOTYPE_CLOUD_FOUNDRY.equals(typeName) 
	            		&& key.getId() != ARV_DUMMY_INSTANCE_ID) {
	                ConfigResponse serverConfig = (ConfigResponse)entry.getValue();

	    	    	// discover any new apps or services during default scan
	                // to improve response time
	                runAutoDiscovery(serverConfig);

	                // recreate manually-added server as an autodiscovered
	                // server so that custom properties can be autodiscovered
	                ServerResource s = recreateServerResource(serverConfig);
	                if (s != null) {
	                	resources.add(s);
	                }
	            }
	        }            
	    } catch (Exception e) {
	        _log.error("Could not discover apps and services during the default scan: " 
	                       + e.getMessage(), e);
	    }
	    
    	_log.debug("[recreateServerResources] servers=" + resources.size());

	    return resources;    	
    }

    private ServerResource recreateServerResource(ConfigResponse serverConfig) {
    	ServerResource server = null;
    	
    	try {
	    	Properties props = new Properties();
	        props.putAll(serverConfig.toProperties());
	        props.putAll(getManager().getProperties());
	
	        CloudFoundryResourceManager manager = new CloudFoundryResourceManager(props);    	
	        server = manager.createServerResource();
	
	        if (server != null) {
	        	discoverServerConfig(server);
	        }
    	} catch (Exception e) {
    		_log.debug("Could not recreate server resource", e);
    		server = null;
    	}
        
        return server;
    }

    /**
     * Further configure the Cloud Foundry server resource.
     * @param server
     */
    private void discoverServerConfig(ServerResource server) {        
        try {
            ConfigResponse serverConfig = server.getProductConfig();
            CloudFoundryProxy cf = new CloudFoundryProxy(serverConfig.toProperties());
	    	
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
	    		double maxMemory = info.getLimits().getMaxTotalMemory() / 1024d;    			
	    		custom.setValue("info.limits.memory", Double.toString(maxMemory) + " GB");
		    		
	    		server.setCustomProperties(custom);
	    	}
        } catch (Exception e) {
        	_log.info("Could not set custom server properties", e);
        }
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {
    	    	
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        CloudFoundryProxy cf = new CloudFoundryProxy(serverConfig.toProperties());

		services.addAll(discoverCloudApplications(cf, serverConfig));
		services.addAll(discoverCloudServices(cf, serverConfig));
		
		// TODO: what if all apps and services were deleted in cloud foundry
		if (!services.isEmpty()) {
			syncServices(serverConfig, services);
		}

        return services;    	
    }
    
    private List<ServiceResource> discoverCloudApplications(CloudFoundryProxy cf,
    														ConfigResponse serverConfig) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        
		for (CloudApplication app : cf.getApplications()) {
			try {
	        	JSONObject jsonConfig = discoverResourceHierarchy(app);
	        	JSONObject jsonServices = jsonConfig.getJSONArray("service").getJSONObject(0);
	        	JSONArray jsonServiceNames = jsonServices.getJSONArray("name");
	
	            ServiceResource service = new ServiceResource();
	            service.setType(PROTOTYPE_CLOUD_FOUNDRY_APP);
	
	            ConfigResponse productCfg = new ConfigResponse();
	            productCfg.setValue("resource.name", app.getName());                
	        	productCfg.setValue(HIERARCHY_KEY, jsonConfig.toString());
	            service.setProductConfig(productCfg);
	            
	            service.setMeasurementConfig(new ConfigResponse());
	            
	            ConfigResponse custom = new ConfigResponse();
	        	custom.setValue("app.model", app.getStaging().get("model"));
	        	custom.setValue("app.stack", app.getStaging().get("stack"));
	        	custom.setValue("app.name", app.getName());
	        	custom.setValue("app.memory", app.getMemory() + " MB");
	        	
	        	String uris = app.getUris().toString();
	        	uris = uris.substring(1, uris.length()-1);
	        	custom.setValue("app.uri", uris);

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
		        	custom.setValue("app.disk", maxDisk + " GB");
		        	custom.setValue("app.core", Integer.toString(maxCores));
            	}
	    		
	        	String appServices = "";
	        	if (jsonServiceNames.length() > 0) {
	        		appServices = jsonServiceNames.toString().replace("\"", "");
	        		appServices = appServices.substring(1, appServices.length()-1);
	        	}
	        	custom.setValue("app.services", appServices);
	        	
	        	service.setCustomProperties(custom);

	        	String name = formatAutoInventoryName(service.getType(), serverConfig, productCfg, custom);
	            service.setName(name);

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
		
		jsonConfig.put("service", getApplicationServices(app));
		jsonConfig.put("createTs", System.currentTimeMillis());
		
    	return jsonConfig;
    }
    
    private JSONArray getApplicationServices(CloudApplication app) {
		JSONArray jsonServices = new JSONArray();
		
		try {
			JSONObject jsonService = new JSONObject();
			JSONArray jsonServiceNames = new JSONArray();
					
			for (String s : app.getServices()) {
				jsonServiceNames.put(s);
			}
	
			jsonService.put("rid", new JSONArray());
			jsonService.put("name", jsonServiceNames);
			jsonServices.put(jsonService);
		} catch (Exception e) {
			_log.debug("Cannot get application services for " + app.getName(), e);
		}
		
		return jsonServices;
    }
    
    private List<ServiceResource> discoverCloudServices(CloudFoundryProxy cf,
    													ConfigResponse serverConfig) {
    	List<ServiceResource> services = new ArrayList<ServiceResource>();    	
    	    	
    	for (CloudService cs : cf.getServices()) {
            ServiceResource service = new ServiceResource();
            String vendor = cs.getVendor();
            
            if ("mysql".equalsIgnoreCase(vendor)) {
            	service.setType(PROTOTYPE_CLOUD_FOUNDRY_MYSQL);
            } else if ("mongodb".equalsIgnoreCase(vendor)) {
            	service.setType(PROTOTYPE_CLOUD_FOUNDRY_MONGODB);            	
            } else if ("redis".equalsIgnoreCase(vendor)) {
            	service.setType(PROTOTYPE_CLOUD_FOUNDRY_REDIS);            	
            } else if ("rabbitmq".equalsIgnoreCase(vendor)) {
            	service.setType(PROTOTYPE_CLOUD_FOUNDRY_RABBITMQ);            	
            } else {
            	_log.info("Unsupported Cloud Foundry service: " + vendor);
            	continue;
            }

            ConfigResponse productCfg = new ConfigResponse();
            productCfg.setValue("resource.name", cs.getName());                
            service.setProductConfig(productCfg);

            service.setMeasurementConfig(new ConfigResponse());
            
            ConfigResponse custom = new ConfigResponse();
        	custom.setValue("service.name", cs.getName());
        	custom.setValue("service.tier", cs.getTier());
        	custom.setValue("service.type", cs.getType());
        	custom.setValue("service.vendor", vendor);
        	custom.setValue("service.version", cs.getVersion());
        	service.setCustomProperties(custom);

        	String name = formatAutoInventoryName(service.getType(), serverConfig, productCfg, custom);
            service.setName(name);

            services.add(service);
    	}
    	
    	return services;
    }
    
    private void syncServices(ConfigResponse serverConfig, List<ServiceResource> cloudResources) {
        // TODO: make auto-sync a configurable property?
       boolean autoSync = true;
    	_log.debug("[syncServices] autoSync=" + autoSync + ", resources=" + cloudResources.size());
        if (autoSync) {
            try {
                Properties props = new Properties();
                props.putAll(serverConfig.toProperties());
                props.putAll(getManager().getProperties());

                TransientResourceManager manager = new CloudFoundryResourceManager(props);
                manager.syncServices(cloudResources);
            } catch (Throwable e) {
                _log.debug("Could not sync transient services: " + e.getMessage(), e);
            }
        }
    }
}

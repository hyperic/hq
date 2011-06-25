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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.bizapp.agent.client.HQApiCommandsClient;
import org.hyperic.hq.bizapp.agent.client.HQApiFactory;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.ResourceApi;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourceConfig;
import org.hyperic.hq.hqapi1.types.ResourceInfo;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.ResourceResponse;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * CloudFoundryResourceManager deletes Cloud Foundry
 * applications and services from the HQ inventory. It requires
 * HQApi to be configured in the agent.properties for this 
 * functionality to work properly.
 * 
 */
public class CloudFoundryResourceManager implements TransientResourceManager {

    private static final Log _log = LogFactory.getLog(CloudFoundryResourceManager.class);
    
    private static final String PROTOTYPE_CLOUD_FOUNDRY = "Cloud Foundry";
    private static final String CONFIG_EMAIL = "email";
    private static final String CONFIG_PASSWORD = "password";
    private static final String CONFIG_URI = "uri";
    private static final String INFO_AUTOIDENTIFIER = "autoIdentifier";
    
    private HQApi api;
    private HQApiCommandsClient commandsClient;
    private Properties props;

    public CloudFoundryResourceManager(Properties props)
            throws PluginException {

        this.props = props;
        this.api = HQApiFactory.getHQApi(AgentDaemon.getMainInstance(), props);
        this.commandsClient = new HQApiCommandsClient(this.api);
    }
    
    public ServerResource createServerResource(ConfigStorage.Key key) {    	
    	if (key.getId() == 0) {
    		return null;    		
    	}

    	String aiid = null;

    	try {
        	String appdefKey = "2:" + key.getId();
	    	Resource resource = getResource(appdefKey);
	    	
	    	if (resource == null) {
	    		return null;
	    	}
	    	
	    	for (ResourceInfo info : resource.getResourceInfo()) {
	    		if (INFO_AUTOIDENTIFIER.equals(info.getKey())) {
	    			aiid = info.getValue();
	    			break;
	    		}
	    	}
    	} catch (Exception e) {
    		_log.debug(e);
    	}
    	        
        if (aiid == null) {
        	return null;
        }

        ServerResource server = new ServerResource();

        String serverType = key.getTypeName();
        String platformName = this.props.getProperty("platform.name");
        String installPath = this.props.getProperty("installpath");
        String email = this.props.getProperty("email");
        String password = this.props.getProperty("password");
        String uri = this.props.getProperty("uri");

        server.setIdentifier(aiid);
        server.setType(serverType);
        server.setName(platformName + " " + serverType);        
        server.setInstallPath(installPath);
                
        ConfigResponse productCfg = new ConfigResponse();
        productCfg.setValue("email", email);
        productCfg.setValue("password", password);
        productCfg.setValue("uri", uri);
        server.setProductConfig(productCfg);
        
        return server;
    }
    
    public void syncServices(List<ServiceResource> cloudResources)
    	throws Exception {
    	    	
        if (cloudResources == null) {
            return;
        }

        int numResourcesDeleted = 0;

        try {
            Resource cloudfoundry = getCloudFoundryServer(this.props);

            if (cloudfoundry == null) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Could not find a " + PROTOTYPE_CLOUD_FOUNDRY + " server");
                }
                return;
            }

            List<String> resources = new ArrayList();
            for (ServiceResource s : cloudResources) {
            	String sname = getResourceName(s);
            	if (sname != null) {
	                resources.add(sname);
	                if (_log.isDebugEnabled()) {
	                    _log.debug(PROTOTYPE_CLOUD_FOUNDRY + " aiq service={"
	                            + "name=" + sname
	                            + "}");
	                }
            	}
            }

            Collections.sort(resources);
            for (Resource service : cloudfoundry.getResource()) {
                String sname = getResourceName(service.getResourceConfig());
                if (sname != null) {
	                if (Collections.binarySearch(resources, sname) < 0) {
	                    commandsClient.deleteResource(service);
	                    _log.debug(PROTOTYPE_CLOUD_FOUNDRY + " service deleted={"
	                    		+ "hq name=" + service.getName()
	                            + ", cloud foundry name=" + sname
	                            + "}");
	                    numResourcesDeleted++;
	                    // TODO: Create event log when resources are deleted
	                }
                }
            }
        } catch (Exception e) {
            // TODO: log here?
            throw e;
        } finally {
            if (numResourcesDeleted > 0) {
                _log.info(numResourcesDeleted + " Cloud Foundry resources deleted");
            }
        }
    }

    private Resource getResource(String appdefKey) 
		throws IOException {
	
		ResourceApi api = this.api.getResourceApi();
		ResourceResponse response = api.getResource(appdefKey, false, false);
		this.commandsClient.assertSuccess(response, "Getting resource for " + appdefKey, false);
	
		return response.getResource();
    }
    
    private Resource getCloudFoundryServer(Properties serverProps)
            throws IOException, PluginException {

        Resource cloudfoundry = null;
        ResourcePrototype rezProto = commandsClient.getResourcePrototype(PROTOTYPE_CLOUD_FOUNDRY);
        List<Resource> resources = commandsClient.getResources(rezProto, true, true);

        for (Resource r : resources) {
            _log.debug(r.getName());
            if (isResourceConfigMatch(r.getResourceConfig(), serverProps)) {
                cloudfoundry = r;
                break;
            }
        }

        return cloudfoundry;
    }

    private boolean isResourceConfigMatch(List<ResourceConfig> configs, Properties props) {
        boolean uriMatches = false;
        boolean emailMatches = false;
        boolean pwMatches = false;

        for (ResourceConfig c : configs) {
        	if (CONFIG_URI.equals(c.getKey())) {
                if (c.getValue().equals(props.get(CONFIG_URI))) {
                	uriMatches = true;
                }            
        	} else if (CONFIG_EMAIL.equals(c.getKey())) {
                if (c.getValue().equals(props.get(CONFIG_EMAIL))) {
                	emailMatches = true;
                }
            } else if (CONFIG_PASSWORD.equals(c.getKey())) {
                if (c.getValue().equals(props.get(CONFIG_PASSWORD))) {
                	pwMatches = true;
                }
            }
        }

        return uriMatches && emailMatches && pwMatches;
    }
    
    private String getResourceName(ServiceResource s) {
    	String name = null;
    	
    	try {
    		// Hyperic 4.6+
    		ConfigResponse svcConfig = s.getProductConfig();
    		name = svcConfig.getValue("resource.name");
    	} catch (NoSuchMethodError ne) {
    		_log.debug(ne);
    		
    		// TODO: pre-Hyperic 4.6
    	}
    	
    	return name;
    }
    
    private String getResourceName(List<ResourceConfig> configs) {
    	String name = null;

    	for (ResourceConfig c : configs) {
    		if ("resource.name".equals(c.getKey())) {
    			name = c.getValue();
    			break;
    		}
    	}
    	
    	return name;
    }
}

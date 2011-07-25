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
package org.hyperic.hq.plugin.cloudfoundry.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.CrashesInfo;
import org.hyperic.hq.product.PluginException;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudFoundryProxy {

	private static final Log _log = LogFactory.getLog(CloudFoundryProxy.class);
	
    private static final long CACHE_TIMEOUT = 60000;

    // TODO: Move to constants file
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String URI = "uri";
	
    private static Map<String, String> tokenCache = new HashMap<String, String>();
	
    private Properties config = null;
	private CloudFoundryClient cf = null;		
	
	public CloudFoundryProxy(Properties config) throws PluginException {
		this.config = config;
		this.cf = getCloudFoundryClient(this.config);
	}

	public CloudInfo getCloudInfo() {
		CloudInfo info = null;
		
		try {
			info = this.cf.getCloudInfo();
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting cloud info. Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				info = this.cf.getCloudInfo();
			} catch (PluginException e2) {
				_log.error("Error getting cloud info", e2);
			}
		}
		
		return info;
	}
	
	public List<CloudApplication> getApplications() {
		List<CloudApplication> apps = null;
		
		try {
			apps = this.cf.getApplications();
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting apps. Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				apps = this.cf.getApplications();
			} catch (PluginException e2) {
				_log.error("Error getting services", e2);
			}
		}
		
		return apps;
	}
	
	public CloudApplication getApplication(String appName) {
		CloudApplication app = null;
		
		try {
			app = this.cf.getApplication(appName);
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting app " + appName 
							+ ". Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				app = this.cf.getApplication(appName);
			} catch (PluginException e2) {
				_log.error("Error getting app " + appName, e2);
			}
		}
		
		return app;
	}
	
	public ApplicationStats getApplicationStats(String appName) {
		ApplicationStats stats = null;

		try {
			stats = this.cf.getApplicationStats(appName);
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting app stats for "
							+ appName + ". Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				stats = this.cf.getApplicationStats(appName);
			} catch (PluginException e2) {
				_log.error("Error getting app stats for "
							+ appName, e2);
			}
		}
		
		return stats;
	}
	
	public List<CloudService> getServices() {
		List<CloudService> services = null;

		try {
			services = this.cf.getServices();
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting services. Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				services = this.cf.getServices();
			} catch (PluginException e2) {
				_log.error("Error getting services", e2);
			}
		}
		
		return services;
	}
	
	public CloudService getService(String serviceName) {		
		CloudService service = null;
		
		try {
			service = this.cf.getService(serviceName);
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting service " 
							+ serviceName + ". Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				service = this.cf.getService(serviceName);
			} catch (PluginException e2) {
				_log.error("Error getting service " + serviceName, e2);
			}
		}	
		
		return service;	
	}
	
	public CrashesInfo getCrashes(String appName) {
		CrashesInfo info = null;
		
		try {
			info = this.cf.getCrashes(appName);
		} catch (Exception e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting app crashes for "
							+ appName + ". Re-trying with new client.", e1);
			}
			
			try {
				this.cf = getNewCloudFoundryClient(this.config);
				info = this.cf.getCrashes(appName);
			} catch (PluginException e2) {
				_log.error("Error getting app crashes for "
							+ appName, e2);
			}
		}
		
		return info;
	}
	
	private CloudFoundryClient getCloudFoundryClient(Properties config) 
		throws PluginException {
		
		CloudFoundryClient cf = null;
		JSONObject key = getJSONKey(config);
		
		synchronized (tokenCache) {
			String token = tokenCache.get(key.toString());
		
			if (token == null) {
				cf = getNewCloudFoundryClient(config);
			} else {
				try {
					String uri = key.getString(URI);
					// generate new client with the cached token
					// to ensure data is fresh
					cf = new CloudFoundryClient(token, uri);
					if (_log.isDebugEnabled()) {
						_log.debug("Using cached token = " + token);
					}
				} catch (Exception e) {
					throw new PluginException("Error creating CloudFoundryClient", e);
				}
			}
		}
		
		return cf;
	}
	
	private CloudFoundryClient getNewCloudFoundryClient(Properties config) 
		throws PluginException {

		CloudFoundryClient cf = null;
		
		synchronized (tokenCache) {
			JSONObject key = getJSONKey(config);
			tokenCache.remove(key);

			cf = CloudFoundryFactory.getCloudFoundryClient(config);
	
	    	String token = null;
	        try {
	            token = cf.login();
	        } catch (Exception ex) {
	            _log.info(ex.getMessage());
	            throw new PluginException("Invalid Cloud Foundry credentials", ex);
	        }
	        
			tokenCache.put(key.toString(), token);
						
			if (_log.isDebugEnabled()) {
	    		_log.debug("key=" + key + ", new token=" + token);
			}
		}
				
		return cf;
	}
	
	/*
	private CloudAccount getCloudAccount(Properties config) 
		throws PluginException {
		
		CloudFoundryClient cf = getCloudFoundryClient(config);		
		CloudInfo info = null;
		
		try {
			info = cf.getCloudInfo();
		} catch (Exception e) {
			_log.info("Error getting cloud info. Re-trying with new client.", e);
			
			cf = getNewCloudFoundryClient(config);
			info = cf.getCloudInfo();
		}

		CloudAccount cloud = new CloudAccount(info);
		
		List<CloudApplication> apps = cf.getApplications();
		for (CloudApplication app : apps) {
			String appName = app.getName();
			ApplicationStats stats = cf.getApplicationStats(appName);
			cloud.addApplication(app);
			cloud.addApplicationStats(appName, stats);
		}
		
		List<CloudService> services = cf.getServices();
		for (CloudService s : services) {
			cloud.addService(s);
		}
		
		return cloud;
	}
	*/

    private JSONObject getJSONKey(Properties config) {
    	JSONObject jsonKey = null;

        String email = config.getProperty(EMAIL);
        String cloudControllerUrl = config.getProperty(URI);

        if (email.length() > 0 
        		&& cloudControllerUrl.length() > 0) {
        	try {
        		jsonKey = new JSONObject();
        		jsonKey.put(EMAIL, email);
        		jsonKey.put(URI, cloudControllerUrl);
        	} catch (JSONException je) {
        		throw new IllegalArgumentException(je);
        	}
        } else {
        	throw new IllegalArgumentException("Missing Cloud Foundry credentials");
        }

        return jsonKey;
    }
}

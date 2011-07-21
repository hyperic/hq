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

import java.util.Collection;
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
	
    private static Map<String, CloudFoundryClient> clientCache = new HashMap<String, CloudFoundryClient>();
	
    private static Map<String, ObjectCache<CloudAccount>> cloudCache = new HashMap<String, ObjectCache<CloudAccount>>();
        
    private CloudAccount cloud = null;
    private Properties config = null;
	
	public CloudFoundryProxy(Properties config) throws PluginException {
		this(config, true);
	}

	public CloudFoundryProxy(Properties config, boolean useCache) throws PluginException {
		this.config = config;
		String key = getKey(config);
				
		synchronized (cloudCache) {
			ObjectCache<CloudAccount> cached = cloudCache.get(key);
			
			if (!useCache || cached == null || cached.isExpired()) {
				cached = new ObjectCache<CloudAccount>(getCloudAccount(config), CACHE_TIMEOUT);
				cloudCache.put(key, cached);
				
				if (_log.isDebugEnabled()) {
					_log.debug("Updating cache with key = " + key);
				}				
			}
			
			this.cloud = cached.getEntity();			
		}
	}

	public CloudInfo getCloudInfo() {
		return cloud.getCloudInfo();
	}
	
	public Collection<CloudApplication> getApplications() {
		return cloud.getApplications();
	}
	
	public CloudApplication getApplication(String appName) {
		return cloud.getApplication(appName);
	}
	
	public ApplicationStats getApplicationStats(String appName) {
		return cloud.getApplicationStats(appName);
	}
	
	public Collection<CloudService> getServices() {
		return cloud.getServices();
	}
	
	public CloudService getService(String serviceName) {
		return cloud.getService(serviceName);
	}
	
	public CrashesInfo getCrashes(String appName) {
		CloudFoundryClient cf = null;		
		CrashesInfo info = null;
		
		try {
			cf = getCloudFoundryClient(this.config);
			info = cf.getCrashes(appName);
		} catch (PluginException e1) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error getting app crashes. Re-trying with new connection.", e1);
			}
			
			try {
				cf = getNewCloudFoundryClient(this.config);
				info = cf.getCrashes(appName);
			} catch (PluginException e2) {
				_log.error("Error getting app crashes.", e2);
			}
		}
		
		return info;
	}
	
	private CloudFoundryClient getCloudFoundryClient(Properties config) 
		throws PluginException {
		
		CloudFoundryClient cf = null;
		String key = getKey(config);
		
		synchronized (clientCache) {
			cf = clientCache.get(key);
		
			if (cf == null) {
				cf = getNewCloudFoundryClient(config);
			} else {
				if (_log.isDebugEnabled()) {
					_log.debug("Using cached CloudFoundryClient " + cf);
				}
			}
		}
		
		return cf;
	}
	
	private CloudFoundryClient getNewCloudFoundryClient(Properties config) 
		throws PluginException {
		
		String key = getKey(config);
		CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(config);
		
		synchronized (clientCache) {
			clientCache.put(key, cf);
		}
		
		if (_log.isDebugEnabled()) {
			_log.debug("Using new CloundFoundryClient " + cf);
		}
		
		return cf;
	}
	
	private CloudAccount getCloudAccount(Properties config) 
		throws PluginException {
		
		CloudFoundryClient cf = getCloudFoundryClient(config);		
		CloudInfo info = null;
		
		try {
			info = cf.getCloudInfo();
		} catch (Exception e) {
			_log.info("Error loading cache", e);
			
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

    private String getKey(Properties config) {
    	String key = null;

        String email = config.getProperty(EMAIL);
        String cloudControllerUrl = config.getProperty(URI);

        if (email.length() > 0 
        		&& cloudControllerUrl.length() > 0) {
        	try {
        		JSONObject jsonKey = new JSONObject();
        		jsonKey.put(EMAIL, email);
        		jsonKey.put(URI, cloudControllerUrl);
        		key = jsonKey.toString();
        	} catch (JSONException je) {
        		throw new IllegalArgumentException(je);
        	}
        } else {
        	throw new IllegalArgumentException("Missing Cloud Foundry account information");
        }

        return key;
    }
}

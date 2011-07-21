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

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.CloudService;

public class CloudAccount {

	private CloudInfo cloudInfo = null;
	private Map<String, CloudApplication> apps = new HashMap<String, CloudApplication>();
	private Map<String, ApplicationStats> stats = new HashMap<String, ApplicationStats>();
	private Map<String, CloudService> services = new HashMap<String, CloudService>();
	
	public CloudAccount(CloudInfo cloudInfo) {
		this.cloudInfo = cloudInfo;
	}
	
	public CloudInfo getCloudInfo() {
		return cloudInfo;
	}
	
	public Collection<CloudApplication> getApplications() {
		return apps.values();
	}
	
	public CloudApplication getApplication(String appName) {
		return apps.get(appName);
	}
	
	public void addApplication(CloudApplication cloudApplication) {
		apps.put(cloudApplication.getName(), cloudApplication);
	}
	
	public ApplicationStats getApplicationStats(String appName) {
		return stats.get(appName);
	}
	
	public void addApplicationStats(String appName, ApplicationStats applicationStats) {
		stats.put(appName, applicationStats);
	}
	
	public Collection<CloudService> getServices() {
		return services.values();
	}
	public CloudService getService(String serviceName) {
		return services.get(serviceName);
	}
	
	public void addService(CloudService service) {
		services.put(service.getName(), service);
	}
}

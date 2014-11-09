/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2014], Hyperic, Inc.
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
 *
 */

package org.hyperic.plugin.openstack;

import static org.hyperic.plugin.openstack.OpenstackConstants.CINDER_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_API;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_API_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_INSTALL_PATH;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_REGISTRY;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_REGISTRY_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.PROCESS_QUERY;
import static org.hyperic.plugin.openstack.OpenstackConstants.GLANCE_SERVER;
import static org.hyperic.plugin.openstack.OpenstackUtil.getGlancePtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getArgumentMatch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

/**
 * Class identifies Openstack Glance Processes to be monitored.
 */
public class OpenstackGlanceServerDetector extends ServerDetector implements
		AutoServerDetector {

	private static Log logger = LogFactory
			.getLog(OpenstackGlanceServerDetector.class);

	private boolean isGlanceApiServiceAvailable = false;
	private boolean isGlanceRegistryServiceAvailable = false;

	/**
	 * Function creates Server Resources by providing static install path & PTQL
	 * for Server Availability.
	 */
	@Override
	public List getServerResources(ConfigResponse platformConfig)
			throws PluginException {
		List servers = new ArrayList();
		String glanceService = getAvailableGlanceService();

		logger.debug("Glance Server monitoring PTQL:" + glanceService);

		if (glanceService != null) {
			ServerResource server = createServerResource(GLANCE_INSTALL_PATH);
			//server.setName(server.getName());
			String platformName = getPlatformName(server.getName());
			server.setName(platformName + GLANCE_SERVER);
			ConfigResponse productConfig = new ConfigResponse();
			productConfig.setValue(PROCESS_QUERY, glanceService);
			setProductConfig(server, productConfig);
			setMeasurementConfig(server, new ConfigResponse());
			servers.add(server);
		}
		return servers;
	}

	/**
	 * Function returns platform Name
	 * 
	 * @param serverName
	 * @return
	 */
	private String getPlatformName(String serverName) {
		String[] arr = serverName.split(" ");
		if(arr != null && arr.length > 0) {
			return arr[0] + " ";
		}
		return "";
	}
	
	/**
	 * Function queries a static list of PTQL to identify if Linux Platform has
	 * at least one of monitored Glance process running.
	 * 
	 * @return String
	 */
	private String getAvailableGlanceService() {
		List<String[]> glanceServicePtqls = getGlancePtql();
		for (String[] glanceServicePtql : glanceServicePtqls) {
			for (int i = 0; i < glanceServicePtql.length; i++) {
				long[] pids = getPids(glanceServicePtql[i]);
				if (pids != null && pids.length > 0) {
					return glanceServicePtql[i];
				}
			}
		}
		return null;
	}

	/**
	 * Function to discover Services being monitored for Server Resource
	 */
	@Override
	protected List discoverServices(ConfigResponse serverConfig)
			throws PluginException {
		logger.debug("[OpenstackGlanceServerDetector discoverServices] serverConfig="
				+ serverConfig);

		List services = new ArrayList();

		String glanceApiServicePtql = getArgumentMatch(GLANCE_API);
		if (isServiceAvailable(glanceApiServicePtql,
				isGlanceApiServiceAvailable)) {
			services.add(getService(GLANCE_API_SERVICE, glanceApiServicePtql));
			logProcess(GLANCE_API_SERVICE, glanceApiServicePtql);
		}

		String glanceRegistryServicePtql = getArgumentMatch(GLANCE_REGISTRY);
		if (isServiceAvailable(glanceRegistryServicePtql,
				isGlanceRegistryServiceAvailable)) {
			services.add(getService(GLANCE_REGISTRY_SERVICE,
					glanceRegistryServicePtql));
			logProcess(GLANCE_REGISTRY_SERVICE, glanceRegistryServicePtql);
		}

		return services;
	}

	/**
	 * Function checks if service was Available previously.
	 * 
	 * @param servicePtql
	 * @param serviceAvaiability
	 * @return
	 */
	private boolean isServiceAvailable(String servicePtql,
			boolean serviceAvaiability) {
		if (!serviceAvaiability) {
			long[] pids = getPids(servicePtql);
			if (pids != null && pids.length > 0) {
				serviceAvaiability = true;
			} else {
				logger.debug("No Match found for:" + servicePtql);
			}
		}
		return serviceAvaiability;
	}

	/**
	 * Function identifies PTQL for monitoring a service. Different PTQL is
	 * written as per environment to monitor.
	 * 
	 * @param servicePtqls
	 * @param defaultServicePtql
	 * @return
	 */
	private String getServicePtql(String[] servicePtqls,
			String defaultServicePtql) {
		for (int i = 0; i < servicePtqls.length; i++) {
			long[] pids = getPids(servicePtqls[i]);
			if ((pids != null && pids.length > 0)
					|| (i == (servicePtqls.length - 1))) {
				return servicePtqls[i];
			}
		}
		return defaultServicePtql;
	}

	/**
	 * Function creates Service Resource.
	 * 
	 * @param serviceDescriptorName
	 * @param serviceName
	 * @return
	 */
	private ServiceResource getService(String serviceDescriptorName,
			String servicePtql) {
		ServiceResource service = createServiceResource(serviceDescriptorName);
		service.setName(serviceDescriptorName);
		ConfigResponse productConfig = new ConfigResponse();
		productConfig.setValue(PROCESS_QUERY, servicePtql);
		setProductConfig(service, productConfig);
		setMeasurementConfig(service, new ConfigResponse());
		return service;
	}

	/**
	 * Function logs monitored process with ptql statement.
	 * 
	 * @param monitoringProcess
	 * @param processPTQL
	 */
	private void logProcess(String monitoringProcess, String processPTQL) {
		logger.debug("Monitoring Process: " + monitoringProcess + "with ptql:"
				+ processPTQL);
	}
}

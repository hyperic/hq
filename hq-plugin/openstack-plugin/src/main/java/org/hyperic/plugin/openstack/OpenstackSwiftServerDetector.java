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

import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.PROCESS_QUERY;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_ACCOUNT_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_ACCOUNT_SERVER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_CONTAINER_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_CONTAINER_SERVER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_INSTALL_PATH;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_OBJECT_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_OBJECT_SERVER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_PROXY_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_PROXY_SERVER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.SWIFT_SERVER;
import static org.hyperic.plugin.openstack.OpenstackUtil.getArgumentMatch;
import static org.hyperic.plugin.openstack.OpenstackUtil.getSwiftPtql;

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
 * Class identifies Openstack Swift Processes to be monitored.
 */
public class OpenstackSwiftServerDetector extends ServerDetector implements
		AutoServerDetector {

	private static Log logger = LogFactory
			.getLog(OpenstackSwiftServerDetector.class);

	private boolean isSwiftProxyServerServiceAvailable = false;
	private boolean isSwiftAccountServerServiceAvailable = false;
	private boolean isSwiftContainerServerServiceAvailable = false;
	private boolean isSwiftObjectServerServiceAvailable = false;

	/**
	 * Function creates Server Resources by providing static install path & PTQL
	 * for Server Availability.
	 */
	@Override
	public List getServerResources(ConfigResponse platformConfig)
			throws PluginException {
		List servers = new ArrayList();
		String swiftService = getAvailableSwiftService();

		logger.debug("Swift Server monitoring PTQL:" + swiftService);

		// Server Resources created if one of monitored process is available.
		if (swiftService != null) {
			ServerResource server = createServerResource(SWIFT_INSTALL_PATH);
			// server.setName(server.getName());
			String platformName = getPlatformName(server.getName());
			server.setName(platformName + SWIFT_SERVER);
			ConfigResponse productConfig = new ConfigResponse();
			productConfig.setValue(PROCESS_QUERY, swiftService);
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
	 * at least one of monitored Swift process running.
	 * 
	 * @return String
	 */
	private String getAvailableSwiftService() {
		List<String[]> swiftServicePtqls = getSwiftPtql();
		for (String[] swiftServicePtql : swiftServicePtqls) {
			for (int i = 0; i < swiftServicePtql.length; i++) {
				long[] pids = getPids(swiftServicePtql[i]);
				if (pids != null && pids.length > 0) {
					return swiftServicePtql[i];
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
		logger.debug("[OpenstackSwiftServerDetector discoverServices] serverConfig="
				+ serverConfig);

		List services = new ArrayList();

		String swiftProxyServerServicePtql = getArgumentMatch(SWIFT_PROXY_SERVER);
		if (isServiceAvailable(swiftProxyServerServicePtql,
				isSwiftProxyServerServiceAvailable)) {
			services.add(getService(SWIFT_PROXY_SERVER_SERVICE,
					swiftProxyServerServicePtql));
			logProcess(SWIFT_PROXY_SERVER_SERVICE, swiftProxyServerServicePtql);
		}

		String swiftAccountServerServicePtql = getArgumentMatch(SWIFT_ACCOUNT_SERVER);
		if (isServiceAvailable(swiftAccountServerServicePtql,
				isSwiftAccountServerServiceAvailable)) {
			services.add(getService(SWIFT_ACCOUNT_SERVER_SERVICE,
					swiftAccountServerServicePtql));
			logProcess(SWIFT_ACCOUNT_SERVER_SERVICE,
					swiftAccountServerServicePtql);
		}

		String swiftContainerServerservicePtql = getArgumentMatch(SWIFT_CONTAINER_SERVER);
		if (isServiceAvailable(swiftContainerServerservicePtql,
				isSwiftContainerServerServiceAvailable)) {
			services.add(getService(SWIFT_CONTAINER_SERVER_SERVICE,
					swiftContainerServerservicePtql));
			logProcess(SWIFT_CONTAINER_SERVER_SERVICE,
					swiftContainerServerservicePtql);
		}

		String swiftObjectServerService = getArgumentMatch(SWIFT_OBJECT_SERVER);
		if (isServiceAvailable(swiftObjectServerService,
				isSwiftObjectServerServiceAvailable)) {
			services.add(getService(SWIFT_OBJECT_SERVER_SERVICE,
					swiftObjectServerService));
			logProcess(SWIFT_OBJECT_SERVER_SERVICE, swiftObjectServerService);
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

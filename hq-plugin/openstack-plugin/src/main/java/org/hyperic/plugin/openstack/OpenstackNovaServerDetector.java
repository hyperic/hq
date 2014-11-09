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

import static org.hyperic.plugin.openstack.OpenstackConstants.NEUTRON_CONTROLLER_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_API;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_API_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CERT;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CERT_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CERT_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_COMPUTE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_COMPUTE_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_COMPUTE_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CONDUCTOR;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CONDUCTOR_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CONDUCTOR_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CONSOLE_AUTH_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_CONSOLE_AUTH_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_INSTALL_PATH;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_NETWORK_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_NETWORK_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_NOVNCPROXY;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_NOVNCPROXY_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_NOVNCPROXY_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_OBJECTSTORE_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_OBJECT_STORE_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_SCHEDULER;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_SCHEDULER_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_SCHEDULER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_XVPVNCPROXY_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.NOVA_XVPVNCPROXY_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.PROCESS_QUERY;
import static org.hyperic.plugin.openstack.OpenstackUtil.getArgumentMatch;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaCertPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaComputePtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaConductorPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaConsoleAuthPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaNetworkPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaNoVncProxyPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaObjectStorePtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaSchedulerPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getNovaXvpvncProxyPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getStateNamePtql;

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
 * Class identifies Openstack Nova Processes to be monitored.
 */
public class OpenstackNovaServerDetector extends ServerDetector implements
		AutoServerDetector {

	private static Log logger = LogFactory
			.getLog(OpenstackNovaServerDetector.class);

	private boolean isNovaApiServiceAvailable = false;
	private boolean isNovaSchedulerServiceAvailable = false;
	private boolean isNovaConductorServiceAvailable = false;
	private boolean isNovaConsoleAuthServiceAvailable = false;
	// private boolean isNovaConsoleServiceAvailable = false;
	private boolean isNovaCertServiceAvailable = false;
	private boolean isNovaObjectStoreServiceAvailable = false;
	private boolean isNovaComputeServiceAvailable = false;
	private boolean isNovaXvpvncProxyServiceAvailable = false;
	private boolean isNovaNoVncProxyServiceAvailable = false;
	private boolean isNovaNetworkServiceAvailable = false;

	@Override
	public List getServerResources(ConfigResponse platformConfig)
			throws PluginException {
		List servers = new ArrayList();
		String novaService = getAvailableNovaService();

		logger.debug("Nova Server monitoring PTQL:" + novaService);

		// Server Resources created if one of monitored process is available.
		if (novaService != null) {
			ServerResource server = createServerResource(NOVA_INSTALL_PATH);
			// server.setName(server.getName());
			String platformName = getPlatformName(server.getName());
			server.setName(platformName + NOVA_SERVER);
			ConfigResponse productConfig = new ConfigResponse();
			productConfig.setValue(PROCESS_QUERY, novaService);
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
	 * at least one of monitored Nova process running.
	 * 
	 * @return String
	 */
	private String getAvailableNovaService() {
		List<String[]> novaServicePtqls = getNovaPtql();
		for (String[] novaServicePtql : novaServicePtqls) {
			for (int i = 0; i < novaServicePtql.length; i++) {
				long[] pids = getPids(novaServicePtql[i]);
				if (pids != null && pids.length > 0) {
					return novaServicePtql[i];
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
		logger.debug("[OpenstackNovaServerDetector discoverServices] serverConfig="
				+ serverConfig);

		List services = new ArrayList();

		String novaApiServicePtql = getStateNamePtql(NOVA_API);
		if (isServiceAvailable(novaApiServicePtql, isNovaApiServiceAvailable)) {
			services.add(getService(NOVA_API_SERVICE, novaApiServicePtql));
			logProcess(NOVA_API_SERVICE, novaApiServicePtql);
		}

		String novaSchedulerServicePtql = getServicePtql(
				getNovaSchedulerPtql(), NOVA_SCHEDULER_PTQL);
		if (isServiceAvailable(getArgumentMatch(NOVA_SCHEDULER),
				isNovaSchedulerServiceAvailable)) {
			services.add(getService(NOVA_SCHEDULER_SERVICE,
					novaSchedulerServicePtql));
			logProcess(NOVA_SCHEDULER_SERVICE, novaSchedulerServicePtql);
		}

		String novaConductorServicePtql = getServicePtql(
				getNovaConductorPtql(), NOVA_CONDUCTOR_PTQL);
		if (isServiceAvailable(getArgumentMatch(NOVA_CONDUCTOR),
				isNovaConductorServiceAvailable)) {
			services.add(getService(NOVA_CONDUCTOR_SERVICE,
					novaConductorServicePtql));
			logProcess(NOVA_CONDUCTOR_SERVICE, novaConductorServicePtql);
		}

		String novaConsoleAuthServicePtql = getServicePtql(
				getNovaConsoleAuthPtql(), NOVA_CONSOLE_AUTH_PTQL);
		if (isServiceAvailable(NOVA_CONSOLE_AUTH_PTQL,
				isNovaConsoleAuthServiceAvailable)) {
			services.add(getService(NOVA_CONSOLE_AUTH_SERVICE,
					novaConsoleAuthServicePtql));
			logProcess(NOVA_CONSOLE_AUTH_SERVICE, novaConsoleAuthServicePtql);
		}

		/*
		 * String novaConsoleServicePtql = getServicePtql(getNovaConsolePtql(),
		 * NOVA_CONSOLE_PTQL); if
		 * (!novaConsoleServicePtql.equals(NOVA_CONSOLE_PTQL)) {
		 * isNovaConsoleServiceAvailable = true; } if
		 * (isNovaConsoleServiceAvailable) {
		 * services.add(getService(NOVA_CONSOLE_SERVICE,
		 * novaConsoleServicePtql)); logProcess(NOVA_CONSOLE_SERVICE,
		 * novaConsoleServicePtql); }
		 */

		String novaCertServicePtql = getServicePtql(getNovaCertPtql(),
				NOVA_CERT_PTQL);
		if (isServiceAvailable(getArgumentMatch(NOVA_CERT),
				isNovaCertServiceAvailable)) {
			services.add(getService(NOVA_CERT_SERVICE, novaCertServicePtql));
			logProcess(NOVA_CERT_SERVICE, novaCertServicePtql);
		}

		String novaObjectStoreServicePtql = getServicePtql(
				getNovaObjectStorePtql(), NOVA_OBJECT_STORE_PTQL);
		if (isServiceAvailable(NOVA_OBJECT_STORE_PTQL,
				isNovaObjectStoreServiceAvailable)) {
			services.add(getService(NOVA_OBJECTSTORE_SERVICE,
					novaObjectStoreServicePtql));
			logProcess(NOVA_OBJECTSTORE_SERVICE, novaObjectStoreServicePtql);
		}

		String novaComputeServicePtql = getServicePtql(getNovaComputePtql(),
				NOVA_COMPUTE_PTQL);
		if (isServiceAvailable(getArgumentMatch(NOVA_COMPUTE),
				isNovaComputeServiceAvailable)) {
			services.add(getService(NOVA_COMPUTE_SERVICE,
					novaComputeServicePtql));
			logProcess(NOVA_COMPUTE_SERVICE, novaComputeServicePtql);
		}

		String novaXvpvncServicePtql = getServicePtql(getNovaXvpvncProxyPtql(),
				NOVA_XVPVNCPROXY_PTQL);
		if (isServiceAvailable(NOVA_XVPVNCPROXY_PTQL,
				isNovaXvpvncProxyServiceAvailable)) {
			services.add(getService(NOVA_XVPVNCPROXY_SERVICE,
					novaXvpvncServicePtql));
			logProcess(NOVA_XVPVNCPROXY_SERVICE, novaXvpvncServicePtql);
		}

		String novaNoVncServicePtql = getServicePtql(getNovaNoVncProxyPtql(),
				NOVA_NOVNCPROXY_PTQL);
		if (isServiceAvailable(getArgumentMatch(NOVA_NOVNCPROXY),
				isNovaNoVncProxyServiceAvailable)) {
			services.add(getService(NOVA_NOVNCPROXY_SERVICE,
					novaNoVncServicePtql));
			logProcess(NOVA_NOVNCPROXY_SERVICE, novaNoVncServicePtql);
		}

		String novaNetworkPtql = getServicePtql(getNovaNetworkPtql(),
				NOVA_NETWORK_PTQL);
		if (isServiceAvailable(NOVA_NETWORK_PTQL, isNovaNetworkServiceAvailable)) {
			services.add(getService(NOVA_NETWORK_SERVICE, novaNetworkPtql));
			logProcess(NOVA_NETWORK_SERVICE, novaNetworkPtql);
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

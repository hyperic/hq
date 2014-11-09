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

import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_CENTRAL_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_CENTRAL_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_COMPUTE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_COMPUTE_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_COMPUTE_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_NOTIFICATION;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_AGENT_NOTIFICATION_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_ALARM_EVALUATOR_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_ALARM_EVALUATOR_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_ALARM_NOTIFIER_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_ALARM_NOTIFIER_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_API_PTQL;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_API_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_COLLECTOR;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_COLLECTOR_SERVICE;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_INSTALL_PATH;
import static org.hyperic.plugin.openstack.OpenstackConstants.KEYSTONE_SERVER;
import static org.hyperic.plugin.openstack.OpenstackConstants.PROCESS_QUERY;
import static org.hyperic.plugin.openstack.OpenstackConstants.CEILOMETER_SERVER;
import static org.hyperic.plugin.openstack.OpenstackUtil.getArgumentMatch;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerAgentCentralPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerAgentComputePtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerAlarmEvaluatorPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerAlarmNotifierPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerApiPtql;
import static org.hyperic.plugin.openstack.OpenstackUtil.getCeilometerPtql;

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
 * Class identifies Openstack Ceilometer Processes to be monitored.
 */
public class OpenstackCeilometerServerDetector extends ServerDetector implements
		AutoServerDetector {

	private static Log logger = LogFactory
			.getLog(OpenstackCeilometerServerDetector.class);

	private boolean isCeilometerAgentCentralServiceAvailable = false;
	private boolean isCeilometerAgentComputeServiceAvailable = false;
	private boolean isCeilometerAgentNotificationServiceAvailable = false;
	private boolean isCeilometerCollectorServiceAvailable = false;
	private boolean isCeilometerAlarmEvaluatorServiceAvailable = false;
	private boolean isCeilometerAlarmNotifierServiceAvailable = false;
	private boolean isCeilometerApiCentralServiceAvailable = false;

	/**
	 * Function creates Server Resources by providing static install path & PTQL
	 * for Server Availability.
	 */
	@Override
	public List getServerResources(ConfigResponse platformConfig)
			throws PluginException {
		List servers = new ArrayList();
		String ceilometerService = getAvailableCeilometerService();

		logger.debug("Ceilometer Server monitoring PTQL:" + ceilometerService);

		// Server Resources created if one of monitored process is available.
		if (ceilometerService != null) {
			ServerResource server = createServerResource(CEILOMETER_INSTALL_PATH);
			//server.setName(server.getName());
			String platformName = getPlatformName(server.getName());
			server.setName(platformName + CEILOMETER_SERVER);
			ConfigResponse productConfig = new ConfigResponse();
			productConfig.setValue(PROCESS_QUERY, ceilometerService);
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
	 * at least one of monitored Ceilometer process running.
	 * 
	 * @return String
	 */
	private String getAvailableCeilometerService() {
		List<String[]> ceilometerServicePtqls = getCeilometerPtql();
		for (String[] ceilometerServicePtql : ceilometerServicePtqls) {
			for (int i = 0; i < ceilometerServicePtql.length; i++) {
				long[] pids = getPids(ceilometerServicePtql[i]);
				if (pids != null && pids.length > 0) {
					return ceilometerServicePtql[i];
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

		logger.debug("[OpenstackCeilometerServerDetector discoverServices] serverConfig="
				+ serverConfig);

		List services = new ArrayList();

		String ceilometerAgentCentralServicePtql = getServicePtql(
				getCeilometerAgentCentralPtql(), CEILOMETER_AGENT_CENTRAL_PTQL);
		if (isServiceAvailable(CEILOMETER_AGENT_CENTRAL_PTQL,
				isCeilometerAgentCentralServiceAvailable)) {
			services.add(getService(CEILOMETER_AGENT_CENTRAL_SERVICE,
					ceilometerAgentCentralServicePtql));
			logProcess(CEILOMETER_AGENT_CENTRAL_SERVICE,
					ceilometerAgentCentralServicePtql);
		}

		String ceilometerAgentComputeServicePtql = getServicePtql(
				getCeilometerAgentComputePtql(), CEILOMETER_AGENT_COMPUTE_PTQL);
		if (isServiceAvailable(getArgumentMatch(CEILOMETER_AGENT_COMPUTE),
				isCeilometerAgentComputeServiceAvailable)) {
			services.add(getService(CEILOMETER_AGENT_COMPUTE_SERVICE,
					ceilometerAgentComputeServicePtql));
			logProcess(CEILOMETER_AGENT_COMPUTE_SERVICE,
					ceilometerAgentComputeServicePtql);
		}

		String ceilometerAgentNotificationServicePtql = getArgumentMatch(CEILOMETER_AGENT_NOTIFICATION);
		if (isServiceAvailable(ceilometerAgentNotificationServicePtql,
				isCeilometerAgentNotificationServiceAvailable)) {
			services.add(getService(CEILOMETER_AGENT_NOTIFICATION_SERVICE,
					ceilometerAgentNotificationServicePtql));
			logProcess(CEILOMETER_AGENT_NOTIFICATION_SERVICE,
					ceilometerAgentNotificationServicePtql);
		}

		String ceilometerCollectorServicePtql = getArgumentMatch(CEILOMETER_COLLECTOR);
		if (isServiceAvailable(ceilometerCollectorServicePtql,
				isCeilometerCollectorServiceAvailable)) {
			services.add(getService(CEILOMETER_COLLECTOR_SERVICE,
					ceilometerCollectorServicePtql));
			logProcess(CEILOMETER_COLLECTOR_SERVICE,
					ceilometerCollectorServicePtql);
		}

		String ceilometerAlarmEvaluatorServicePtql = getServicePtql(
				getCeilometerAlarmEvaluatorPtql(),
				CEILOMETER_ALARM_EVALUATOR_PTQL);
		if (isServiceAvailable(CEILOMETER_ALARM_EVALUATOR_PTQL,
				isCeilometerAlarmEvaluatorServiceAvailable)) {
			services.add(getService(CEILOMETER_ALARM_EVALUATOR_SERVICE,
					ceilometerAlarmEvaluatorServicePtql));
			logProcess(CEILOMETER_ALARM_EVALUATOR_SERVICE,
					ceilometerAlarmEvaluatorServicePtql);
		}

		String ceilometerAlarmNotifierServicePtql = getServicePtql(
				getCeilometerAlarmNotifierPtql(),
				CEILOMETER_ALARM_NOTIFIER_PTQL);
		if (isServiceAvailable(CEILOMETER_ALARM_NOTIFIER_PTQL,
				isCeilometerAlarmNotifierServiceAvailable)) {
			services.add(getService(CEILOMETER_ALARM_NOTIFIER_SERVICE,
					ceilometerAlarmNotifierServicePtql));
			logProcess(CEILOMETER_ALARM_NOTIFIER_SERVICE,
					ceilometerAlarmNotifierServicePtql);
		}

		String ceilometerApiServicePtql = getServicePtql(
				getCeilometerApiPtql(), CEILOMETER_API_PTQL);
		if (isServiceAvailable(CEILOMETER_API_PTQL,
				isCeilometerApiCentralServiceAvailable)) {
			services.add(getService(CEILOMETER_API_SERVICE,
					ceilometerApiServicePtql));
			logProcess(CEILOMETER_API_SERVICE, ceilometerApiServicePtql);
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

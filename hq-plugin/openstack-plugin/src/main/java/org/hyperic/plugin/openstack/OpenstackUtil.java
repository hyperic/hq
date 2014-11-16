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

import static org.hyperic.plugin.openstack.OpenstackConstants.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class defines Utility methods for creating PTQL expressions
 */
public class OpenstackUtil {

	/**
	 * Function returns Argument Contains Match PTQL Args.*.ct=<process name>
	 * 
	 * @param processName
	 * @return
	 */
	public static String getArgumentMatch(String processName) {
		return ARGS_REGEX + processName;
	}

	/**
	 * Function returns parent Argument Contains Match PTQL Args.*.Pct=<process
	 * name>
	 * 
	 * @param processName
	 * @return
	 */
	private static String getParentArgumentMatch(String processMatch) {
		return ARGS_PARENT_REGEX + processMatch;
	}

	/**
	 * 
	 * @param processName
	 * @return
	 */
	public static String getStateNamePtql(String processName) {
		String processExecutable = getProcessExecutable(processName);
		return STATE_NAME + processExecutable;
	}

	/**
	 * Function returns executable match State.Name.eq=<process
	 * name>,State.Name.Pne=<process name>
	 * 
	 * @param processName
	 * @return
	 */
	private static String getExecutableMatch(String executable) {
		return STATE_NAME + executable + COMMA + STATE_NAME_PNE + executable;
	}

	/**
	 * Function returns combination of Argument & Executable match
	 * Args.*.ct=<process name>,State.Name.eq=<process
	 * name>,State.Name.Pne=<process name>
	 * 
	 * @param processName
	 * @param executableName
	 * @return
	 */
	private static String getExecutablePtql(String processName,
			String executableName) {
		return getArgumentMatch(processName) + COMMA
				+ getExecutableMatch(executableName);
	}

	/**
	 * Function returns combination of Argument & Parent Argument match.
	 * Args.*.ct=cinder-api,Args.*.Pct=bash
	 * 
	 * @param processName
	 * @param parentProcessExecutableString
	 * @return
	 */
	private static String getArgumentPtql(String processName,
			String parentProcessExecutableString) {
		return getArgumentMatch(processName)
				+ ((parentProcessExecutableString != null) ? COMMA
						+ getParentArgumentMatch(parentProcessExecutableString)
						: "");
	}

	/**
	 * Function returns executable value ptql for process In Openstack it is
	 * observed executable length can be maximum of 15 characters.
	 * 
	 * @param processName
	 * @return
	 */
	private static String[] getServicePtql(String processName) {
		String processExecutable = getProcessExecutable(processName);
		return getServicePtql(processName, processExecutable);
	}

	/**
	 * 
	 * @param processName
	 * @return
	 */
	private static String[] getMultiServicePtql(String processName) {
		String[] multiServicePtql = { getStateNamePtql(processName),
				getArgumentPtql(processName, null) };
		return multiServicePtql;
	}

	/**
	 * In Openstack it is observed executable length can be maximum of 15
	 * characters.
	 * 
	 * @param processName
	 * @return
	 */
	private static String getProcessExecutable(String processName) {
		return (processName.length() > 15) ? processName.substring(0, 15)
				: processName;
	}

	/**
	 * Function returns PTQL combinations for each process depending on
	 * environments. 1]
	 * Args.*.ct=cinder-api,State.Name.eq=cinder-api,State.Name.Pne=cinder-api
	 * 2] Args.*.ct=cinder-api,Args.*.Pct=.sh 3]
	 * Args.*.ct=cinder-api,Args.*.Pct=bash 4] Args.*.ct=cinder-api
	 * 
	 * @param processName
	 * @param executableName
	 * @return
	 */
	private static String[] getServicePtql(String processName,
			String executableName) {
		String[] servicePtql = {
				getExecutablePtql(processName, executableName),
				getArgumentPtql(processName, SCRIPT),
				getArgumentPtql(processName, BASH),
				/* getArgumentPtql(processName, null) */
				getStateNamePtql(processName) };
		return servicePtql;
	}

	/**
	 * Function returns PTQL for Keystone-All process
	 * 
	 * @return
	 */
	public static String[] getKeystoneAllPtql() {
		return getServicePtql(KEYSTONE_ALL);
	}

	/**
	 * Function returns PTQL for monitored Keystone processes.
	 * 
	 * @return
	 */
	public static List<String[]> getKeystonePtql() {
		List<String[]> keystoneptql = new ArrayList<String[]>();
		keystoneptql.add(getKeystoneAllPtql());
		return keystoneptql;
	}

	/**
	 * Function returns PTQL for Neutron-Server process
	 * 
	 * @return
	 */
	public static String[] getNeutronServerPtql() {
		return getServicePtql(NEUTRON_SERVER);
	}

	/**
	 * Function returns PTQL for Neutron-Dhcp-Agent process
	 * 
	 * @return
	 */
	public static String[] getNeutronDhcpAgentPtql() {
		return getServicePtql(NEUTRON_DHCP_AGENT);
	}

	/**
	 * Function returns PTQL for Neutron-L3-Agent process
	 * 
	 * @return
	 */
	public static String[] getNeutronL3AgentPtql() {
		return getServicePtql(NEUTRON_L3_AGENT);
	}

	/**
	 * Function returns PTQL for Neutron-Metadata-Agent process
	 * 
	 * @return
	 */
	public static String[] getNeutronMetadataAgentPtql() {
		return getServicePtql(NEUTRON_METADATA_AGENT);
	}

	/**
	 * Function returns PTQL for Neutron-Lbaas-Agent process
	 * 
	 * @return
	 */
	public static String[] getNeutronLbaasAgentPtql() {
		return getServicePtql(NEUTRON_LBAAS_AGENT);
	}

	/**
	 * Function returns PTQL for Neutron-Openvswitch-Agent process
	 * 
	 * @return
	 */
	public static String[] getNeutronOpenvswitchAgentPtql() {
		return getServicePtql(NEUTRON_OPENVSWITCH_AGENT);
	}

	/**
	 * Function returns PTQL for monitored Neutron processes.
	 * 
	 * @return
	 */
	public static List<String[]> getNeutronPtql() {
		List<String[]> neutronptql = new ArrayList<String[]>();
		neutronptql.add(getNeutronServerPtql());
		neutronptql.add(getNeutronDhcpAgentPtql());
		neutronptql.add(getNeutronL3AgentPtql());
		neutronptql.add(getNeutronMetadataAgentPtql());
		neutronptql.add(getNeutronLbaasAgentPtql());
		neutronptql.add(getNeutronOpenvswitchAgentPtql());
		return neutronptql;
	}

	/**
	 * Function returns PTQL for Nova-Api process
	 * 
	 * @return
	 */
	public static String[] getNovaApiPtql() {
		return getServicePtql(NOVA_API);
	}

	/**
	 * Function returns PTQL for Nova-Scheduler process
	 * 
	 * @return
	 */
	public static String[] getNovaSchedulerPtql() {
		return getServicePtql(NOVA_SCHEDULER);
	}

	/**
	 * Function returns PTQL for Nova-Conductor process
	 * 
	 * @return
	 */
	public static String[] getNovaConductorPtql() {
		return getServicePtql(NOVA_CONDUCTOR);
	}

	/**
	 * Function returns PTQL for Nova-ConsoleAuth process
	 * 
	 * @return
	 */
	public static String[] getNovaConsoleAuthPtql() {
		return getServicePtql(NOVA_CONSOLE_AUTH);
	}

	/**
	 * Function returns PTQL for Nova-Console process
	 * 
	 * @return
	 */
	public static String[] getNovaConsolePtql() {
		return getServicePtql(NOVA_CONSOLE);
	}

	/**
	 * Function returns PTQL for Nova-Cert process
	 * 
	 * @return
	 */
	public static String[] getNovaCertPtql() {
		return getServicePtql(NOVA_CERT);
	}

	/**
	 * Function returns PTQL for Nova-ObjectStore process
	 * 
	 * @return
	 */
	public static String[] getNovaObjectStorePtql() {
		return getServicePtql(NOVA_OBJECT_STORE);
	}

	/**
	 * Function returns PTQL for Nova-Compute process
	 * 
	 * @return
	 */
	public static String[] getNovaComputePtql() {
		return getServicePtql(NOVA_COMPUTE);
	}

	/**
	 * Function returns PTQL for Nova-XvpvncProxy process
	 * 
	 * @return
	 */
	public static String[] getNovaXvpvncProxyPtql() {
		return getServicePtql(NOVA_XVPVNCPROXY);
	}

	/**
	 * Function returns PTQL for Nova-NoVncProxy process
	 * 
	 * @return
	 */
	public static String[] getNovaNoVncProxyPtql() {
		return getServicePtql(NOVA_NOVNCPROXY);
	}

	/**
	 * Function returns PTQL for Nova-Network process
	 * 
	 * @return
	 */
	public static String[] getNovaNetworkPtql() {
		return getServicePtql(NOVA_NETWORK);
	}

	/**
	 * Function returns PTQL for monitored Nova processes.
	 * 
	 * @return
	 */
	public static List<String[]> getNovaPtql() {
		List<String[]> novaptql = new ArrayList<String[]>();
		novaptql.add(getNovaApiPtql());
		novaptql.add(getNovaSchedulerPtql());
		novaptql.add(getNovaConductorPtql());
		novaptql.add(getNovaConsoleAuthPtql());
		// novaptql.add(getNovaConsolePtql());
		novaptql.add(getNovaCertPtql());
		novaptql.add(getNovaObjectStorePtql());
		novaptql.add(getNovaComputePtql());
		novaptql.add(getNovaXvpvncProxyPtql());
		novaptql.add(getNovaNoVncProxyPtql());
		novaptql.add(getNovaNetworkPtql());
		return novaptql;
	}

	/**
	 * Function returns PTQL for Cinder-Api process
	 * 
	 * @return
	 */
	public static String[] getCinderApiPtql() {
		return getServicePtql(CINDER_API);
	}

	/**
	 * Function returns PTQL for Cinder-Scheduler process
	 * 
	 * @return
	 */
	public static String[] getCinderSchedulerPtql() {
		return getServicePtql(CINDER_SCHEDULER);
	}

	/**
	 * Function returns PTQL for Cinder-Volume process
	 * 
	 * @return
	 */
	public static String[] getCinderVolumePtql() {
		return getServicePtql(CINDER_VOLUME);
	}

	/**
	 * Function returns PTQL for monitored Cinder processes.
	 * 
	 * @return
	 */
	public static List<String[]> getCinderPtql() {
		List<String[]> cinderptql = new ArrayList<String[]>();
		cinderptql.add(getCinderApiPtql());
		cinderptql.add(getCinderSchedulerPtql());
		cinderptql.add(getCinderVolumePtql());
		return cinderptql;
	}

	/**
	 * Function returns PTQL for Glance-Api process
	 * 
	 * @return
	 */
	public static String[] getGlanceApiPtql() {
		return getServicePtql(GLANCE_API);
	}

	/**
	 * Function returns PTQL for Glance-Registry process
	 * 
	 * @return
	 */
	public static String[] getGlanceRegistryPtql() {
		return getServicePtql(GLANCE_REGISTRY);
	}

	/**
	 * Function returns PTQL for monitored Glance processes.
	 * 
	 * @return
	 */
	public static List<String[]> getGlancePtql() {
		List<String[]> glanceptql = new ArrayList<String[]>();
		glanceptql.add(getGlanceApiPtql());
		glanceptql.add(getGlanceRegistryPtql());
		return glanceptql;
	}

	/**
	 * Function returns PTQL for Swift-Proxy-Server process
	 * 
	 * @return
	 */
	public static String[] getSwiftProxyServerPtql() {
		return getServicePtql(SWIFT_PROXY_SERVER);
	}

	/**
	 * Function returns PTQL for Swift-Account-Server process
	 * 
	 * @return
	 */
	public static String[] getSwiftAccountServerPtql() {
		return getServicePtql(SWIFT_ACCOUNT_SERVER);
	}

	/**
	 * Function returns PTQL for Swift-Container-Server process
	 * 
	 * @return
	 */
	public static String[] getSwiftContainerServerPtql() {
		return getServicePtql(SWIFT_CONTAINER_SERVER);
	}

	/**
	 * Function returns PTQL for Swift-Object-Server process
	 * 
	 * @return
	 */
	public static String[] getSwiftObjectServerPtql() {
		return getServicePtql(SWIFT_OBJECT_SERVER);
	}

	/**
	 * Function returns PTQL for monitored Swift processes.
	 * 
	 * @return
	 */
	public static List<String[]> getSwiftPtql() {
		List<String[]> swiftptql = new ArrayList<String[]>();
		swiftptql.add(getSwiftProxyServerPtql());
		swiftptql.add(getSwiftAccountServerPtql());
		swiftptql.add(getSwiftContainerServerPtql());
		swiftptql.add(getSwiftObjectServerPtql());
		return swiftptql;
	}

	/**
	 * Function returns PTQL for Ceilometer-Agent-Central process
	 * 
	 * @return
	 */
	public static String[] getCeilometerAgentCentralPtql() {
		return getServicePtql(CEILOMETER_AGENT_CENTRAL);
	}

	/**
	 * Function returns PTQL for Ceilometer-Agent-Compute process
	 * 
	 * @return
	 */
	public static String[] getCeilometerAgentComputePtql() {
		return getServicePtql(CEILOMETER_AGENT_COMPUTE);
	}

	/**
	 * Function returns PTQL for Ceilometer-Agent-Notification process
	 * 
	 * @return
	 */
	public static String[] getCeilometerAgentNotificationPtql() {
		return getServicePtql(CEILOMETER_AGENT_NOTIFICATION);
	}

	/**
	 * Function returns PTQL for Ceilometer-Collector process
	 * 
	 * @return
	 */
	public static String[] getCeilometerCollectorProcessPtql() {
		return getServicePtql(CEILOMETER_COLLECTOR);
	}

	/**
	 * Function returns PTQL for Ceilometer-Alarm-Evaluator process
	 * 
	 * @return
	 */
	public static String[] getCeilometerAlarmEvaluatorPtql() {
		return getServicePtql(CEILOMETER_ALARM_EVALUATOR);
	}

	/**
	 * Function returns PTQL for Ceilometer-Alarm-Notifier process
	 * 
	 * @return
	 */
	public static String[] getCeilometerAlarmNotifierPtql() {
		return getServicePtql(CEILOMETER_ALARM_NOTIFIER);
	}

	/**
	 * Function returns PTQL for Ceilometer-Api process
	 * 
	 * @return
	 */
	public static String[] getCeilometerApiPtql() {
		return getServicePtql(CEILOMETER_API);
	}

	/**
	 * Function returns PTQL for monitored Ceilometer processes.
	 * 
	 * @return
	 */
	public static List<String[]> getCeilometerPtql() {
		List<String[]> ceilometerptql = new ArrayList<String[]>();
		ceilometerptql.add(getCeilometerAgentCentralPtql());
		ceilometerptql.add(getCeilometerAgentComputePtql());
		ceilometerptql.add(getCeilometerAgentNotificationPtql());
		ceilometerptql.add(getCeilometerCollectorProcessPtql());
		ceilometerptql.add(getCeilometerAlarmEvaluatorPtql());
		ceilometerptql.add(getCeilometerAlarmNotifierPtql());
		ceilometerptql.add(getCeilometerApiPtql());
		return ceilometerptql;
	}
}

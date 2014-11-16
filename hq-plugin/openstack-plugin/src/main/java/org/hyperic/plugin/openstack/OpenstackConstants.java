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

/**
 * Class defines Constants
 */
public class OpenstackConstants {

	// Monitored Process
	/*
	 * public static final Map<String, String> NOVA_PROCESSES = new
	 * HashMap<String, String>(); public static final Map<String, String>
	 * NEUTRON_PROCESSES = new HashMap<String, String>(); public static final
	 * Map<String, String> KEYSTONE_PROCESSES = new HashMap<String, String>();
	 * public static final Map<String, String> CEILOMETER_PROCESSES = new
	 * HashMap<String, String>(); public static final Map<String, String>
	 * CINDER_PROCESSES = new HashMap<String, String>(); public static final
	 * Map<String, String> GLANCE_PROCESSES = new HashMap<String, String>();
	 * public static final Map<String, String> SWIFT_PROCESSES = new
	 * HashMap<String, String>();
	 */

	// Install Paths for Server Resources
	public static final String KEYSTONE_INSTALL_PATH = "/etc/keystone";
	public static final String NOVA_INSTALL_PATH = "/etc/nova";
	public static final String GLANCE_INSTALL_PATH = "/etc/glance";
	public static final String NEUTRON_INSTALL_PATH = "/etc/neutron";
	public static final String CINDER_INSTALL_PATH = "/etc/cinder";
	public static final String SWIFT_INSTALL_PATH = "/etc/swift";
	public static final String CEILOMETER_INSTALL_PATH = "/etc/ceilometer";

	// Server Resources
	public static final String KEYSTONE_SERVER = "Keystone Service";
	public static final String CEILOMETER_SERVER = "Ceilometer Service";
	public static final String NOVA_SERVER = "Nova Service";
	public static final String NEUTRON_CONTROLLER_SERVER = "Neutron Service";
	public static final String CINDER_SERVER = "Cinder Service";
	public static final String SWIFT_SERVER = "Swift Service";
	public static final String GLANCE_SERVER = "Glance Service";

	// Keystone Services
	public static final String KEYSTONE_ALL_SERVICE = "Keystone-All Process Metrics";

	// Neutron Services
	public static final String NEUTRON_SERVER_SERVICE = "Neutron-Server Process Metrics";
	public static final String NEUTRON_DHCP_AGENT_SERVICE = "Neutron-Dhcp-Agent Process Metrics";
	public static final String NEUTRON_L3_AGENT_SERVICE = "Neutron-L3-Agent Process Metrics";
	public static final String NEUTRON_METADATA_AGENT_SERVICE = "Neutron-Metadata-Agent Process Metrics";
	public static final String NEUTRON_LBAAS_AGENT_SERVICE = "Neutron-Lbaas-Agent Process Metrics";
	public static final String NEUTRON_OPENVSWITCH_AGENT_SERVICE = "Neutron-Openvswitch-Agent Process Metrics";

	// Nova Services
	public static final String NOVA_API_SERVICE = "Nova-Api Process Metrics";
	public static final String NOVA_SCHEDULER_SERVICE = "Nova-Scheduler Process Metrics";
	public static final String NOVA_CONDUCTOR_SERVICE = "Nova-Conductor Process Metrics";
	public static final String NOVA_CONSOLE_AUTH_SERVICE = "Nova-ConsoleAuth Process Metrics";
	public static final String NOVA_CONSOLE_SERVICE = "Nova-Console Process Metrics";
	public static final String NOVA_CERT_SERVICE = "Nova-Cert Process Metrics";
	public static final String NOVA_OBJECTSTORE_SERVICE = "Nova-ObjectStore Process Metrics";
	public static final String NOVA_COMPUTE_SERVICE = "Nova-Compute Process Metrics";
	public static final String NOVA_XVPVNCPROXY_SERVICE = "Nova-XvpvncProxy Process Metrics";
	public static final String NOVA_NOVNCPROXY_SERVICE = "Nova-NoVncProxy Process Metrics";
	public static final String NOVA_NETWORK_SERVICE = "Nova-Network Process Metrics";

	// Cinder Services
	public static final String CINDER_API_SERVICE = "Cinder-Api Process Metrics";
	public static final String CINDER_SCHEDULER_SERVICE = "Cinder-Scheduler Process Metrics";
	public static final String CINDER_VOLUME_SERVICE = "Cinder-Volume Process Metrics";

	// Glance Services
	public static final String GLANCE_API_SERVICE = "Glance-Api Process Metrics";
	public static final String GLANCE_REGISTRY_SERVICE = "Glance-Registry Process Metrics";

	// Swift Services
	public static final String SWIFT_PROXY_SERVER_SERVICE = "Swift-Proxy-Server Process Metrics";
	public static final String SWIFT_ACCOUNT_SERVER_SERVICE = "Swift-Account-Server Process Metrics";
	public static final String SWIFT_CONTAINER_SERVER_SERVICE = "Swift-Container-Server Process Metrics";
	public static final String SWIFT_OBJECT_SERVER_SERVICE = "Swift-Object-Server Process Metrics";

	// Ceilometer Services
	public static final String CEILOMETER_AGENT_CENTRAL_SERVICE = "Ceilometer-Agent-Central Process Metrics";
	public static final String CEILOMETER_AGENT_COMPUTE_SERVICE = "Ceilometer-Agent-Compute Process Metrics";
	public static final String CEILOMETER_AGENT_NOTIFICATION_SERVICE = "Ceilometer-Agent-Notification Process Metrics";
	public static final String CEILOMETER_COLLECTOR_SERVICE = "Ceilometer-Collector Process Metrics";
	public static final String CEILOMETER_ALARM_EVALUATOR_SERVICE = "Ceilometer-Alarm-Evaluator Process Metrics";
	public static final String CEILOMETER_ALARM_NOTIFIER_SERVICE = "Ceilometer-Alarm-Notifier Process Metrics";
	public static final String CEILOMETER_API_SERVICE = "Ceilometer-Api Process Metrics";

	// Separator
	public static final String SEPARATOR = " ";
	public static final String COMMA = ",";
	public static final String BASH = "bash";
	public static final String SCRIPT = ".sh";

	// PTQL
	public static final String PROCESS_QUERY = "process.query";
	public static final String ARGS_REGEX = "Args.*.ct=";
	public static final String ARGS_PARENT_REGEX = "Args.*.Pct=";
	public static final String STATE_NAME = "State.Name.eq=";
	public static final String STATE_NAME_PNE = "State.Name.Pne=";

	// Keystone Processes
	public static final String KEYSTONE_ALL = "keystone-all";

	/*
	 * static{ KEYSTONE_PROCESSES.put(KEYSTONE_ALL, KEYSTONE_ALL_SERVICE); }
	 */

	// Neutron Processes
	public static final String NEUTRON_SERVER = "neutron-server";
	public static final String NEUTRON_DHCP_AGENT = "neutron-dhcp-agent";
	public static final String NEUTRON_L3_AGENT = "neutron-l3-agent";
	public static final String NEUTRON_METADATA_AGENT = "neutron-metadata-agent";
	public static final String NEUTRON_LBAAS_AGENT = "neutron-lbaas-agent";
	public static final String NEUTRON_OPENVSWITCH_AGENT = "neutron-openvswitch-agent";

	/*
	 * static{ NEUTRON_PROCESSES.put(NEUTRON_SERVER, NEUTRON_SERVER_SERVICE); }
	 */

	// Nova Process
	public static final String NOVA_API = "nova-api";
	public static final String NOVA_SCHEDULER = "nova-scheduler";
	public static final String NOVA_CONDUCTOR = "nova-conductor";
	public static final String NOVA_CONSOLE_AUTH = "nova-consoleauth";
	public static final String NOVA_CONSOLE = "nova-console";
	public static final String NOVA_CERT = "nova-cert";
	public static final String NOVA_OBJECT_STORE = "nova-objectstore";
	public static final String NOVA_COMPUTE = "nova-compute";
	public static final String NOVA_XVPVNCPROXY = "nova-xvpvncproxy";
	public static final String NOVA_NOVNCPROXY = "nova-novncproxy";
	public static final String NOVA_NETWORK = "nova-network";

	/*
	 * static { NOVA_PROCESSES.put(NOVA_API, NOVA_API_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_SCHEDULER, NOVA_SCHEDULER_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_CONDUCTOR, NOVA_CONDUCTOR_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_CONSOLE_AUTH, NOVA_CONSOLE_AUTH_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_CONSOLE, NOVA_CONSOLE_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_CERT, NOVA_CERT_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_OBJECT_STORE, NOVA_OBJECTSTORE_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_COMPUTE, NOVA_COMPUTE_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_XVPVNCPROXY, NOVA_XVPVNCPROXY_SERVICE);
	 * NOVA_PROCESSES.put(NOVA_NOVNCPROXY, NOVA_NOVNCPROXY_SERVICE); }
	 */

	// Cinder Processes
	public static final String CINDER_API = "cinder-api";
	public static final String CINDER_SCHEDULER = "cinder-scheduler";
	public static final String CINDER_VOLUME = "cinder-volume";

	/*
	 * static { CINDER_PROCESSES.put(CINDER_API, CINDER_API_SERVICE);
	 * CINDER_PROCESSES.put(CINDER_SCHEDULER, CINDER_SCHEDULER_SERVICE);
	 * CINDER_PROCESSES.put(CINDER_VOLUME, CINDER_VOLUME_SERVICE); }
	 */

	// Glance Processes
	public static final String GLANCE_API = "glance-api";
	public static final String GLANCE_REGISTRY = "glance-registry";

	/*
	 * static { GLANCE_PROCESSES.put(GLANCE_API, GLANCE_API_SERVICE);
	 * GLANCE_PROCESSES.put(GLANCE_REGISTRY, GLANCE_REGISTRY_SERVICE); }
	 */

	// Swift Processes
	public static final String SWIFT_PROXY_SERVER = "swift-proxy-server";
	public static final String SWIFT_ACCOUNT_SERVER = "swift-account-server";
	public static final String SWIFT_CONTAINER_SERVER = "swift-container-server";
	public static final String SWIFT_OBJECT_SERVER = "swift-object-server";

	/*
	 * static { SWIFT_PROCESSES.put(SWIFT_PROXY_SERVER,
	 * SWIFT_PROXY_SERVER_SERVICE); SWIFT_PROCESSES.put(SWIFT_ACCOUNT_SERVER,
	 * SWIFT_ACCOUNT_SERVER_SERVICE);
	 * SWIFT_PROCESSES.put(SWIFT_CONTAINER_SERVER,
	 * SWIFT_CONTAINER_SERVER_SERVICE); SWIFT_PROCESSES.put(SWIFT_OBJECT_SERVER,
	 * SWIFT_OBJECT_SERVER_SERVICE); }
	 */

	// Ceilometer Processes
	public static final String CEILOMETER_AGENT_CENTRAL = "ceilometer-agent-central";
	public static final String CEILOMETER_AGENT_COMPUTE = "ceilometer-agent-compute";
	public static final String CEILOMETER_AGENT_NOTIFICATION = "ceilometer-agent-notification";
	public static final String CEILOMETER_COLLECTOR = "ceilometer-collector";
	public static final String CEILOMETER_API = "ceilometer-api";
	public static final String CEILOMETER_ALARM_EVALUATOR = "ceilometer-alarm-evaluator";
	public static final String CEILOMETER_ALARM_NOTIFIER = "ceilometer-alarm-notifier";

	/*
	 * static { CEILOMETER_PROCESSES.put(CEILOMETER_AGENT_CENTRAL,
	 * CEILOMETER_AGENT_CENTRAL_SERVICE);
	 * CEILOMETER_PROCESSES.put(CEILOMETER_AGENT_COMPUTE,
	 * CEILOMETER_AGENT_COMPUTE_SERVICE);
	 * CEILOMETER_PROCESSES.put(CEILOMETER_AGENT_NOTIFICATION,
	 * CEILOMETER_AGENT_NOTIFICATION_SERVICE);
	 * CEILOMETER_PROCESSES.put(CEILOMETER_COLLECTOR,
	 * CEILOMETER_COLLECTOR_SERVICE); CEILOMETER_PROCESSES.put(CEILOMETER_API,
	 * CEILOMETER_API_SERVICE);
	 * CEILOMETER_PROCESSES.put(CEILOMETER_ALARM_EVALUATOR,
	 * CEILOMETER_ALARM_EVALUATOR_SERVICE);
	 * CEILOMETER_PROCESSES.put(CEILOMETER_ALARM_NOTIFIER,
	 * CEILOMETER_ALARM_NOTIFIER_SERVICE); }
	 */

	// PTQL for Processes
	public static final String KEYSTONE_ALL_PTQL = ARGS_REGEX + KEYSTONE_ALL;

	public static final String NEUTRON_SERVER_PTQL = ARGS_REGEX
			+ NEUTRON_SERVER;
	public static final String NEUTRON_DHCP_AGENT_PTQL = ARGS_REGEX
			+ NEUTRON_DHCP_AGENT;
	public static final String NEUTRON_L3_AGENT_PTQL = ARGS_REGEX
			+ NEUTRON_L3_AGENT;
	public static final String NEUTRON_METADATA_AGENT_PTQL = ARGS_REGEX
			+ NEUTRON_METADATA_AGENT;
	public static final String NEUTRON_LBAAS_AGENT_PTQL = ARGS_REGEX
			+ NEUTRON_LBAAS_AGENT;
	public static final String NEUTRON_OPENVSWITCH_AGENT_PTQL = ARGS_REGEX
			+ NEUTRON_OPENVSWITCH_AGENT;

	public static final String NOVA_API_PTQL = STATE_NAME + NOVA_API + COMMA
			+ STATE_NAME_PNE + NOVA_API;
	public static final String NOVA_SCHEDULER_PTQL = STATE_NAME
			+ NOVA_SCHEDULER + COMMA + STATE_NAME_PNE + NOVA_SCHEDULER;
	public static final String NOVA_CONDUCTOR_PTQL = STATE_NAME
			+ NOVA_CONDUCTOR + COMMA + STATE_NAME_PNE + NOVA_CONDUCTOR;
	public static final String NOVA_CONSOLE_AUTH_PTQL = ARGS_REGEX
			+ NOVA_CONSOLE_AUTH;
	public static final String NOVA_CONSOLE_PTQL = STATE_NAME + NOVA_CONSOLE;
	public static final String NOVA_CERT_PTQL = STATE_NAME + NOVA_CERT + COMMA
			+ STATE_NAME_PNE + NOVA_CERT;
	public static final String NOVA_OBJECT_STORE_PTQL = ARGS_REGEX
			+ NOVA_OBJECT_STORE;
	public static final String NOVA_COMPUTE_PTQL = STATE_NAME + NOVA_COMPUTE
			+ COMMA + STATE_NAME_PNE + NOVA_COMPUTE;
	public static final String NOVA_XVPVNCPROXY_PTQL = ARGS_REGEX
			+ NOVA_XVPVNCPROXY;
	public static final String NOVA_NOVNCPROXY_PTQL = STATE_NAME
			+ NOVA_NOVNCPROXY + COMMA + STATE_NAME_PNE + NOVA_NOVNCPROXY;
	public static final String NOVA_NETWORK_PTQL = STATE_NAME + NOVA_NETWORK
			+ COMMA + STATE_NAME_PNE + NOVA_NETWORK;

	public static final String CINDER_API_PTQL = STATE_NAME + CINDER_API
			+ COMMA + STATE_NAME_PNE + CINDER_API;
	public static final String CINDER_SCHEDULER_PTQL = ARGS_REGEX
			+ CINDER_SCHEDULER;
	public static final String CINDER_VOLUME_PTQL = STATE_NAME + CINDER_VOLUME
			+ COMMA + STATE_NAME_PNE + CINDER_VOLUME;

	public static final String GLANCE_API_PTQL = STATE_NAME + GLANCE_API
			+ COMMA + STATE_NAME_PNE + GLANCE_API;
	public static final String GLANCE_REGISTRY_PTQL = STATE_NAME
			+ GLANCE_REGISTRY + COMMA + STATE_NAME_PNE + GLANCE_REGISTRY;

	public static final String SWIFT_PROXY_SERVER_PTQL = STATE_NAME
			+ SWIFT_PROXY_SERVER + COMMA + STATE_NAME_PNE + SWIFT_PROXY_SERVER;

	public static final String SWIFT_ACCOUNT_SERVER_PTQL = STATE_NAME
			+ SWIFT_ACCOUNT_SERVER + COMMA + STATE_NAME_PNE
			+ SWIFT_ACCOUNT_SERVER;

	public static final String SWIFT_CONTAINER_SERVER_PTQL = STATE_NAME
			+ SWIFT_CONTAINER_SERVER + COMMA + STATE_NAME_PNE
			+ SWIFT_CONTAINER_SERVER;

	public static final String SWIFT_OBJECT_SERVER_PTQL = STATE_NAME
			+ SWIFT_OBJECT_SERVER + COMMA + STATE_NAME_PNE
			+ SWIFT_OBJECT_SERVER;

	public static final String CEILOMETER_AGENT_CENTRAL_PTQL = ARGS_REGEX
			+ CEILOMETER_AGENT_CENTRAL;
	public static final String CEILOMETER_AGENT_COMPUTE_PTQL = STATE_NAME
			+ CEILOMETER_AGENT_COMPUTE + COMMA + STATE_NAME_PNE
			+ CEILOMETER_AGENT_COMPUTE;

	public static final String CEILOMETER_AGENT_NOTIFICATION_PTQL = STATE_NAME
			+ CEILOMETER_AGENT_NOTIFICATION + COMMA + STATE_NAME_PNE
			+ CEILOMETER_AGENT_NOTIFICATION;

	public static final String CEILOMETER_COLLECTOR_PTQL = STATE_NAME
			+ CEILOMETER_COLLECTOR + COMMA + STATE_NAME_PNE
			+ CEILOMETER_COLLECTOR;

	public static final String CEILOMETER_ALARM_EVALUATOR_PTQL = ARGS_REGEX
			+ CEILOMETER_ALARM_EVALUATOR;
	public static final String CEILOMETER_ALARM_NOTIFIER_PTQL = ARGS_REGEX
			+ CEILOMETER_ALARM_NOTIFIER;
	public static final String CEILOMETER_API_PTQL = ARGS_REGEX
			+ CEILOMETER_API;
}

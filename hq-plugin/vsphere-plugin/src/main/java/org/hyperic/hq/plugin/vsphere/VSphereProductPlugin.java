/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.vsphere;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.pdk.domain.ConfigOptionType;
import org.hyperic.hq.pdk.domain.MetricInfo;
import org.hyperic.hq.pdk.domain.OperationType;
import org.hyperic.hq.pdk.domain.PropertyType;
import org.hyperic.hq.pdk.domain.ResourceType;
import org.hyperic.hq.pdk.domain.PluginDefinition;
import org.hyperic.hq.product.FlexibleProductPlugin;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;

public class VSphereProductPlugin extends ProductPlugin implements FlexibleProductPlugin {

    public GenericPlugin getPlugin(String type, TypeInfo entity)
    {  
        if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY) &&
            entity.getName().equals(Constants.VMWARE_VCENTER)) {
            return new VCenterDetector();
        }
        return super.getPlugin(type, entity);
    }

	public PluginDefinition generateResourceTypeHierarchy() {
		// Define the plugin
		String plugin = "vsphere";
		
		// Define operation types
		Set<OperationType> vmOperationTypes = setupVMOperations();
			
		// Define config options
		Set<ConfigOptionType> sdkConfig = setupSdkConfig();
		Set<ConfigOptionType> vcenterConfig = setupVCenterConfig(sdkConfig);
		Set<ConfigOptionType> vmConfig = setupVmConfig(sdkConfig);
		Set<ConfigOptionType> hostConfig = setupHostConfig(sdkConfig);

		// Define property types
		Set<PropertyType> vmPropertyTypes = setupVMProperties();
		Set<PropertyType> hostPropertyTypes = setupHostProperties();
		PluginDefinition result = new PluginDefinition();
		
		// Define resource types
		ResourceType vCenterRT = new ResourceType("VMware vCenter", "", plugin, null, null, vcenterConfig);
		
		result.anchorToRootResourceType(vCenterRT);
		
		ResourceType hostRT = new ResourceType("VMware vSphere Host", "", plugin, hostPropertyTypes, null, hostConfig);
		
		result.addRelationship(vCenterRT, "MANAGES", hostRT);
		
		ResourceType vmRT = new ResourceType("VMware vSphere VM", "", plugin, vmPropertyTypes, vmOperationTypes, vmConfig);
		
		result.addRelationship(hostRT, "HOSTS", vmRT);

		// Define Plugin Classes
		result.addPluginClassMapping(vCenterRT, ProductPlugin.TYPE_MEASUREMENT, VSphereMeasurementPlugin.class.getName());
		
		result.addPluginClassMapping(hostRT, ProductPlugin.TYPE_MEASUREMENT, MeasurementPlugin.class.getName());
		result.addPluginClassMapping(hostRT, "collector", VSphereHostCollector.class.getName());
		result.addPluginClassMapping(hostRT, ProductPlugin.TYPE_LOG_TRACK, VSphereHostEventPlugin.class.getName());
		
		result.addPluginClassMapping(vmRT, ProductPlugin.TYPE_MEASUREMENT, MeasurementPlugin.class.getName());
		result.addPluginClassMapping(vmRT, "collector", VSphereVmCollector.class.getName());
		result.addPluginClassMapping(vmRT, ProductPlugin.TYPE_LOG_TRACK, VSphereHostEventPlugin.class.getName());
		result.addPluginClassMapping(vmRT, ProductPlugin.TYPE_CONTROL, VSphereVmControlPlugin.class.getName());
		
		// Define Metrics
		String templateName = "vcenter:url=%url%,user=%user%,pass=%pass%:";
		
		result.addMetricMapping(vCenterRT, new MetricInfo(templateName, "Availability", "availability", "", MeasurementConstants.CAT_AVAILABILITY, true, false));
		result.addMetricMapping(vCenterRT, new MetricInfo(templateName, "Connection Validation Time", "connectionvalidationtime", "ms", MeasurementConstants.CAT_PERFORMANCE, true, false));
		
		templateName = "VMware vSphere Host:host:url=%url%,user=%user%,pass=%pass%,hostname=%hostname%,uuid=%uuid%:";
		
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Availability", "", "", "", true, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Uptime", "sys.uptime.latest", "sec", "AVAILABILITY", false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Usage (Average)", "cpu.usage.average", "percent", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Disk Usage (Average)", "disk.usage.average", "KB", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Highest Disk Latency", "disk.maxTotalLatency.latest", "ms", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Usage (Average)", "mem.usage.average", "percent", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Network Usage (Average)", "net.usage.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Reserved Capacity", "cpu.reservedCapacity.average", "", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Throttled (1 min. Average)", "rescpu.maxLimited1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Throttled (5 min. Average)", "rescpu.maxLimited5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Throttled (15 min. Average)", "rescpu.maxLimited15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));		
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Running (1 min. Average)", "rescpu.runav1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Running (5 min. Average)", "rescpu.runav5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Running (15 min. Average)", "rescpu.runav15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Active (1 min. Average)", "rescpu.actav1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Active (5 min. Average)", "rescpu.actav5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "CPU Active (15 min. Average)", "rescpu.actav15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Swap In", "mem.swapin.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Swap Out", "mem.swapout.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Swap Used", "mem.swapused.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Balloon", "mem.vmmemctl.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Unreserved", "mem.unreserved.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Heap", "mem.heap.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Heap Free", "mem.heapfree.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Overhead", "mem.overhead.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Zero", "mem.zero.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Reserved Capacity", "mem.reservedCapacity.average", "MB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Active", "mem.active.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Shared", "mem.shared.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Granted", "mem.granted.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Consumed", "mem.consumed.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory State", "mem.state.latest", "", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Shared Common", "mem.sharedcommon.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(hostRT, new MetricInfo(templateName, "Memory Used by vmkernel", "mem.sysUsage.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));

		templateName = "VMware vSphere VM:vm:url=%url%,user=%user%,pass=%pass%,vm=%vm%,uuid=%uuid%:";
		
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Availability", "", "", MeasurementConstants.CAT_AVAILABILITY, true, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Uptime", "sys.uptime.latest", "sec", "AVAILABILITY", false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Usage (Average)", "cpu.usage.average", "percent", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Disk Usage (Average)", "disk.usage.average", "KB", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Usage (Average)", "mem.usage.average", "percent", MeasurementConstants.CAT_UTILIZATION, true, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Network Usage (Average)", "net.usage.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Throttled (1 min. Average)", "rescpu.maxLimited1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Throttled (5 min. Average)", "rescpu.maxLimited5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Throttled (15 min. Average)", "rescpu.maxLimited15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));		
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Running (1 min. Average)", "rescpu.runav1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Running (5 min. Average)", "rescpu.runav5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Running (15 min. Average)", "rescpu.runav15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Active (1 min. Average)", "rescpu.actav1.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Active (5 min. Average)", "rescpu.actav5.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "CPU Active (15 min. Average)", "rescpu.actav15.latest", "percent", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Swap In", "mem.swapin.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Swap Out", "mem.swapout.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Swap Target", "mem.swaptarget.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Swapped", "mem.swapped.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Overhead", "mem.overhead.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Balloon", "mem.vmmemctl.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, true));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Balloon Target", "mem.vmmemctltarget.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Zero", "mem.zero.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Active", "mem.active.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Shared", "mem.shared.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Granted", "mem.granted.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		result.addMetricMapping(vmRT, new MetricInfo(templateName, "Memory Consumed", "mem.consumed.average", "KB", MeasurementConstants.CAT_UTILIZATION, false, false));
		
		return result;
	}
	
	private Set<OperationType> setupVMOperations() {
		Set<OperationType> result = new HashSet<OperationType>();
		
		result.add(new OperationType("createSnapshot"));
		result.add(new OperationType("removeAllSnapshots"));
		result.add(new OperationType("revertToCurrentSnapshot"));
		result.add(new OperationType("stop"));
		result.add(new OperationType("start"));
		result.add(new OperationType("reset"));
		result.add(new OperationType("suspend"));
		result.add(new OperationType("rebootGuest"));
		result.add(new OperationType("guestHeartbeatStatus"));
		
		return result;
	}
	
	private Set<PropertyType> setupHostProperties() {
		Set<PropertyType> result = new HashSet<PropertyType>();
		
		result.add(new PropertyType("version", "VMware Version"));
		result.add(new PropertyType("build", "Build"));
		result.add(new PropertyType("ip", "IP Address"));
		result.add(new PropertyType("primaryDNS", "Primary DNS"));
		result.add(new PropertyType("secondaryDNS", "Secondary DNS"));
		result.add(new PropertyType("defaultGateway", "Default Gateway"));
		result.add(new PropertyType("hwVendor", "Manufacturer"));
		result.add(new PropertyType("hwModel", "Model"));
		result.add(new PropertyType("hwCpu", "Processor Type"));
		result.add(new PropertyType("hwSockets", "Processor Sockets"));
		result.add(new PropertyType("hwCores", "Cores per Socket"));
		result.add(new PropertyType("parent", "Data Center"));

		return result;
	}
	
	private Set<PropertyType> setupVMProperties() {
		Set<PropertyType> result = new HashSet<PropertyType>();
		
		result.add(new PropertyType("guestOS", "Guest OS"));
		result.add(new PropertyType("version", "VM Version"));
		result.add(new PropertyType("ip", "IP Address"));
		result.add(new PropertyType("macAddress", "MAC Address"));
		result.add(new PropertyType("hostName", "Hostname"));
		result.add(new PropertyType("esxHost", "ESX Host"));
		result.add(new PropertyType("pool", "Resource Pool"));
		result.add(new PropertyType("memsize", "Memory Size"));
		result.add(new PropertyType("numvcpus", "Virtual CPUs"));
		result.add(new PropertyType("toolsVersion", "Tools Version"));
		result.add(new PropertyType("installpath", "Config File"));

		return result;
	}
	
	private Set<ConfigOptionType> setupSdkConfig() {
		Set<ConfigOptionType> result = new HashSet<ConfigOptionType>();
		
		result.add(new ConfigOptionType("url", "vCenter sdk url", "https://localhost/sdk", false, false, false));
		result.add(new ConfigOptionType("user", "Username", "", false, false, false));
		result.add(new ConfigOptionType("pass", "Password", "", false, false, true));

		return result;
	}
	
	private Set<ConfigOptionType> setupVmConfig(Set<ConfigOptionType> config) {
		Set<ConfigOptionType> result = new HashSet<ConfigOptionType>();
		
		result.addAll(config);
		result.add(new ConfigOptionType("vm", "Virtual Machine Name", "", false, true, false));
		result.add(new ConfigOptionType("uuid", "Virtual Machine UUID", "", false, true, false));
		
		return result;
	}
	
	private Set<ConfigOptionType> setupHostConfig(Set<ConfigOptionType> config) {
		Set<ConfigOptionType> result = new HashSet<ConfigOptionType>();
		
		result.addAll(config);
		result.add(new ConfigOptionType("hostname", "Host System Name", "", false, true, false));
		result.add(new ConfigOptionType("uuid", "Host System UUID", "", false, true, false));
		
		return result;
	}
	
	private Set<ConfigOptionType> setupVCenterConfig(Set<ConfigOptionType> config) {
		Set<ConfigOptionType> result = new HashSet<ConfigOptionType>();
		
		result.addAll(config);
		result.add(new ConfigOptionType("process.query", "Process Query", "State.Name.eq=vpxd", false, false, false));
		
		return result;
	}
}

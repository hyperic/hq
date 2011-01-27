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

import org.hyperic.hq.pdk.domain.ConfigOptionType;
import org.hyperic.hq.pdk.domain.OperationType;
import org.hyperic.hq.pdk.domain.PropertyType;
import org.hyperic.hq.pdk.domain.ResourceType;
import org.hyperic.hq.pdk.domain.ResourceTypeRelationships;
import org.hyperic.hq.product.FlexibleProductPlugin;
import org.hyperic.hq.product.GenericPlugin;
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

	public ResourceTypeRelationships generateResourceTypeHierarchy() {
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
		ResourceTypeRelationships result = new ResourceTypeRelationships();
		
		// Define resource types
		ResourceType vCenterRT = new ResourceType("VMware vCenter", "", plugin, null, null, vcenterConfig);
		
		result.anchorToRootResourceType(vCenterRT);
		
		ResourceType hostRT = new ResourceType("VMware vSphere Host", "", plugin, hostPropertyTypes, null, hostConfig);
		
		result.addRelationship(vCenterRT, "MANAGES", hostRT);
		
		ResourceType vmRT = new ResourceType("VMware vSphere VM", "", plugin, vmPropertyTypes, vmOperationTypes, vmConfig);
		
		result.addRelationship(hostRT, "HOSTS", vmRT);
		
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

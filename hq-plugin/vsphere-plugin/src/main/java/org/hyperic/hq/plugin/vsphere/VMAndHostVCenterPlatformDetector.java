/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
 */

package org.hyperic.hq.plugin.vsphere;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.pdk.domain.Agent;
import org.hyperic.hq.pdk.domain.Resource;
import org.hyperic.hq.pdk.domain.ResourceType;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ToolsConfigInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * HQApi based auto-discovery for vSphere Host and VM platform types. 
 */
public class VMAndHostVCenterPlatformDetector implements VCenterPlatformDetector {
    //duplicating these constants as our build only depends on the pdk   
    static final String AGENT_IP = "agent.setup.agentIP";
    static final String AGENT_PORT = "agent.setup.agentPort";
    static final String AGENT_UNIDIRECTIONAL = "agent.setup.unidirectional";
    static final String ESX_HOST = "esxHost";

    private static final Log log =
        LogFactory.getLog(VMAndHostVCenterPlatformDetector.class.getName());
    private static final boolean isDump =
        "true".equals(System.getProperty("vsphere.dump"));

    private Agent getAgent(RestApi api, Properties props) throws IOException, PluginException {
        String host = props.getProperty(AGENT_IP);
        Integer port = Integer.valueOf(props.getProperty(AGENT_PORT, "2144"));
        String unidirectional = props.getProperty(AGENT_UNIDIRECTIONAL, "NO");
        
        if (unidirectional.equalsIgnoreCase("Y") || unidirectional.equalsIgnoreCase("YES")) {
            port = -1;
        }
        
        return api.getAgent(host, port);
    }

    //XXX might want to store these in memory rather than
    //the metric template, should any of the props change.
    private void mergeVSphereConfig(VSphereResource platform, Properties props) {
        String[] vprops = {
            VSphereUtil.PROP_URL,
            VSphereUtil.PROP_USERNAME,
            VSphereUtil.PROP_PASSWORD
        };
        for (int i=0; i<vprops.length; i++) {
            String val = props.getProperty(vprops[i]);
            if (val != null) {
                platform.addConfig(vprops[i], val);
            }
        }
    }

    private VSphereResource discoverVM(VirtualMachine vm) throws Exception {
        VirtualMachineConfigInfo info = vm.getConfig();

        if (info.isTemplate()) {
            return null; //filter out template VMs
        }

        VirtualMachineRuntimeInfo runtime = vm.getRuntime();
        GuestInfo guest = vm.getGuest();
        ResourcePool pool = vm.getResourcePool();
        VSphereResource platform = new VSphereResource();
        String uuid = info.getUuid();
        
        platform.setName(generatePlatformName(info.getName(), uuid));
        platform.setFqdn(uuid);
        platform.setDescription(info.getGuestFullName());

        ConfigResponse config = new ConfigResponse();
        
        config.setValue(VSphereVmCollector.PROP_VM, info.getName());
        config.setValue(VSphereCollector.PROP_UUID, uuid);
        platform.addConfig(config);
        
        //ConfigInfo
        ConfigResponse cprops = new ConfigResponse();
        VirtualMachineFileInfo files = info.getFiles();
        
        cprops.setValue(ProductPlugin.PROP_INSTALLPATH, files.getVmPathName());
        cprops.setValue("guestOS", info.getGuestFullName());
        cprops.setValue("version", info.getVersion());
        
        //HardwareInfo
        VirtualHardware hw = info.getHardware();
        
        cprops.setValue("numvcpus", hw.getNumCPU());
        cprops.setValue("memsize", hw.getMemoryMB());
        
        //ToolsInfo
        ToolsConfigInfo tools = info.getTools();
        Integer toolsVersion = tools.getToolsVersion();
        
        if (toolsVersion != null) {
            cprops.setValue("toolsVersion", toolsVersion.toString());
        }
        
        //PoolInfo
        cprops.setValue("pool", (String)pool.getPropertyByPath("name"));

        String state = runtime.getPowerState().toString();
        
        if ("poweredOn".equalsIgnoreCase(state)) {
            String name;
        
            if ((name = guest.getHostName()) != null) {
                cprops.setValue("hostName", name);
            }
            
            //NetInfo
            GuestNicInfo[] nics = guest.getNet();
            
            if (nics != null) {
                for (int i=0; i<nics.length; i++) {
                    String mac = nics[i].getMacAddress();
            
                    if (mac.equals("00:00:00:00:00:00")) {
                        log.info("Skipping " + Constants.VMWARE_VSPHERE_VM + "[name=" + info.getName()
                          + ", UUID=" + uuid
                          + ", NIC=" + nics[i].getIpAddress()
                          + ", MAC=" + mac
                          + "]. Will be re-discovered when the MAC address is valid.");
                        return null;
                    }
                    
                    String[] ips = nics[i].getIpAddress();
                    
                    if ((mac != null) && (ips != null) && (ips.length != 0)) {
                        cprops.setValue("macAddress", mac);
                        cprops.setValue("ip", ips[0]);
                        platform.addIp(ips[0], "", mac);
                    }
                }
            }
            
            /*
            if (platform.getIp().isEmpty()) {
                log.info("Skipping " + Constants.VMWARE_VSPHERE_VM + "[name=" + info.getName()
                    + ", UUID=" + uuid
                    + "] because the MAC address does not exist. "
                    + "Will be re-discovered when the MAC address is valid.");
                return null;
            }
            */
        }
        else {
            log.info("Skipping " + Constants.VMWARE_VSPHERE_VM + "[name=" + info.getName() 
                + ", UUID=" + uuid
                + ", powerState=" + state + "]. "
                + "Will be re-discovered when it is powered on.");
            return null;
        }

        ManagedObjectReference hmor = runtime.getHost();

        if (hmor != null) {
            HostSystem host = new HostSystem(vm.getServerConnection(), hmor);
            cprops.setValue(ESX_HOST, host.getName());
        }

        platform.addProperties(cprops);
        
        if (log.isDebugEnabled()) {
            log.debug("Discovered " + Constants.VMWARE_VSPHERE_VM + "[name=" + info.getName()
                          + ", UUID=" + uuid
                          + ", powerState=" + state + "]");
        }
        
        return platform;
    }
    
    private VSphereHostResource discoverHost(HostSystem host) throws Exception {
        HostRuntimeInfo runtime = host.getRuntime();
        String powerState = runtime.getPowerState().toString();
        
        if ("unknown".equalsIgnoreCase(powerState)) {
            // an unknown power state could indicate that the host
            // is disconnected from vCenter
            if (log.isDebugEnabled()) {
                log.debug("Skipping " + Constants.VMWARE_VSPHERE_HOST + "[name=" + host.getName() 
                              + ", powerState=" + powerState + "]");
            }
            
            return null;
        }
        
        HostConfigInfo info = host.getConfig();
        HostNetworkInfo netinfo = info.getNetwork();
        AboutInfo about = info.getProduct();
        HostHardwareSummary hw = host.getSummary().getHardware();
        String address = null;
        VSphereHostResource platform = new VSphereHostResource();
        String uuid = hw.getUuid();
        
        platform.setName(generatePlatformName(host.getName(), uuid));
        platform.setDescription(about.getFullName());
        platform.setFqdn(uuid);
        
        if (netinfo.getVnic() == null) {
            try {
                // Host name may be the IP address
                InetAddress inet = InetAddress.getByName(host.getName());
        
                address = inet.getHostAddress();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug(host.getName() + " does not have an IP address", e);
                }
            }
        } else {
            for (HostVirtualNic nic : netinfo.getVnic()) {
                HostVirtualNicSpec spec = nic.getSpec();
                HostIpConfig ip = spec.getIp();
                
                platform.addIp(ip.getIpAddress(), ip.getSubnetMask(), spec.getMac());
                
                if (address == null) {
                    address = ip.getIpAddress();
                }
            }
        }

        ConfigResponse cprops = new ConfigResponse();

        cprops.setValue("version", about.getVersion());
        cprops.setValue("build", about.getBuild());
        
        if (address != null) {
            cprops.setValue("ip", address);
        }
        
        cprops.setValue("defaultGateway", netinfo.getIpRouteConfig().getDefaultGateway());

        String[] dns = netinfo.getDnsConfig().getAddress();
        
        if (dns != null) {
            String[] dnsProps = { "primaryDNS", "secondaryDNS" };
        
            for (int i=0; i<dnsProps.length; i++) {
                if (i >= dns.length) {
                    break;
                }
            
                cprops.setValue(dnsProps[i], dns[i]);
            }
        }

        cprops.setValue("hwVendor", hw.getVendor());
        cprops.setValue("hwModel", hw.getModel());
        cprops.setValue("hwCpu", hw.getCpuModel());
        cprops.setValue("hwSockets", String.valueOf(hw.getNumCpuPkgs()));
        cprops.setValue("hwCores", String.valueOf(hw.getNumCpuCores() / hw.getNumCpuPkgs()));

        ManagedEntity mor = host.getParent();
        String prev = null;

        while (true) {
            if (mor.getName().equals("Datacenters")) {
                cprops.setValue("parent", prev); //Data Center
            } else {
                prev = mor.getName();
            }

            if ((mor = mor.getParent()) == null) {
                break;
            }
        }

        platform.addProperties(cprops);
        platform.addConfig(VSphereUtil.PROP_HOSTNAME, host.getName());
        platform.addConfig(VSphereCollector.PROP_UUID, uuid);

        if (log.isDebugEnabled()) {
            log.debug("Discovered " + Constants.VMWARE_VSPHERE_HOST + "[name=" + host.getName() 
                          + ", UUID=" + uuid
                          + ", powerState=" + powerState + "]");
        }
        
        return platform;
    }

    private List<Resource> discoverHosts(RestApi api, Agent agent, VSphereUtil vim, Properties props)
    throws IOException, PluginException {
        List<Resource> resources = new ArrayList<Resource>();
        ResourceType hostType =  api.getResourceType(Constants.VMWARE_VSPHERE_HOST);
        ResourceType vmType = api.getResourceType(Constants.VMWARE_VSPHERE_VM);

        try {
            ManagedEntity[] hosts = vim.find(VSphereUtil.HOST_SYSTEM);

            for (int i=0; i<hosts.length; i++) {
                if (! (hosts[i] instanceof HostSystem)) {
                    log.debug(hosts[i] + " not a HostSystem, type=" +
                              hosts[i].getMOR().getType());
                    continue;
                }

                HostSystem host = (HostSystem)hosts[i];
                
                try {
                    VSphereHostResource platform = discoverHost(host);
                    
                    if (platform == null) continue;
                    
                    platform.setType(hostType);
                    platform.setAgent(agent);
                    
                    mergeVSphereConfig(platform, props);
                    
                    VirtualMachine[] hostVms = host.getVms();
                   
                    for (int v=0; v<hostVms.length; v++) {
                        VSphereResource vm = discoverVM(hostVms[v]);
                        
                        if (vm != null) {
                            vm.setType(vmType);
                            vm.setAgent(agent);
                            
                            mergeVSphereConfig(vm, props);
                            
                            platform.getVirtualMachines().add(vm);
                        }
                    }
                    
                    resources.add(platform);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return resources;
    }

    public void discoverPlatforms(RestApi api, Properties props, VSphereUtil vim) throws IOException, PluginException {
        String vCenterUrl = VSphereUtil.getURL(props);
        Resource vCenter = getVCenterServer(api, vCenterUrl);
        
        if (vCenter == null) {
            if (log.isDebugEnabled()) {
                log.debug("Skip discovering hosts and VMs. "
                            + "No VMware vCenter server found with url=" 
                            + vCenterUrl);
            }
            
            return;
        }
       
        Agent agent = getAgent(api, props);
        List<Resource> hosts = discoverHosts(api, agent, vim, props);
        List<Resource> vms = new ArrayList<Resource>();
        Map<String, List<Resource>> vcHostVms = new HashMap<String, List<Resource>>();
            
        for (Resource resource : hosts) {
        	VSphereHostResource host = (VSphereHostResource) resource;
                
            vms.addAll(host.getVirtualMachines());
               
            String esxHost = getEsxHost(resource);

            if (esxHost != null) {
            	vcHostVms.put(esxHost, host.getVirtualMachines());
            }
            
            // Create host
            api.createResource(resource);
        }

        // Create vms
        api.createResources(vms);
            
        Map<String, Resource> existingHosts = new HashMap<String, Resource>();
        Map<String, List<Resource>> existingHostVms = new HashMap<String, List<Resource>>();

        syncResourceEdges(api, existingHosts, existingHostVms, props);
        removePlatformsFromInventory(api, vcHostVms, existingHosts, existingHostVms, vim);
    }
     
    private void syncResourceEdges(RestApi api, Map<String, Resource> existingHosts, Map<String, List<Resource>> existingHostVms, Properties props) 
    throws IOException, PluginException {
        String vCenterUrl = VSphereUtil.getURL(props);
        Resource vCenter = getVCenterServer(api, vCenterUrl);

        if (vCenter == null) {
            if (log.isDebugEnabled()) {
                log.debug("Skip syncing resource edges. "
                            + "No VMware vCenter server found with url=" 
                            + vCenterUrl);
            }
            
            return;
        }
        
        synchVCenterServerToHostResourceEdges(api, vCenter, existingHosts, props);
        
        List<Resource> resources = api.getResourcesByTypeName(Constants.VMWARE_VSPHERE_VM);
        
        synchHostToVmResourceEdges(api, existingHostVms, existingHosts, resources, props); 
    }
    
    private void synchVCenterServerToHostResourceEdges(RestApi api, Resource vCenter, Map<String, Resource> existingHosts, Properties props) 
    throws IOException, PluginException{
        String vCenterUrl = VSphereUtil.getURL(props);
        List<Resource> hosts = new ArrayList<Resource>();

        for (Resource resource : api.getResourcesByTypeName(Constants.VMWARE_VSPHERE_HOST)) {
            if (isVCenterManagedEntity(vCenterUrl, resource)) {
                Resource full = api.getResourceById(resource.getId());

            	hosts.add(full);

                String esxHost = getEsxHost(full);
                
                if (esxHost != null) {
                    existingHosts.put(esxHost, full);
                }
            }
        }
        
        api.makeRelationship(vCenter, hosts);
    }
     
    private void synchHostToVmResourceEdges(RestApi api, Map<String, List<Resource>> existingHostVms,  Map<String, Resource> existingHosts, List<Resource> vms, Properties props) 
    throws IOException, PluginException {
        String vCenterUrl = VSphereUtil.getURL(props);
        
        for (Resource resource : vms) {
            if (isVCenterManagedEntity(vCenterUrl, resource)) {
            	Resource full = api.getResourceById(resource.getId());

            	String esxHost = getEsxHost(full);
                List<Resource> vmResources = existingHostVms.get(esxHost);
                
                if (vmResources == null) {
                    vmResources = new ArrayList<Resource>();
                    existingHostVms.put(esxHost, vmResources);
                }
                
                vmResources.add(full);
            }
        }
                
        for (Resource host : existingHosts.values()) {
            List<Resource> vmResources = existingHostVms.get(getEsxHost(host));

            if (vmResources != null) {
            	api.makeRelationship(host, vmResources);
            }
        }
    }
    
    /**
     *  Delete resources that have been manually removed from vCenter
     */
    private void removePlatformsFromInventory(RestApi api,
    	Map<String, List<Resource>> vcHosts, 
        Map<String, Resource> existingHosts, 
        Map<String, List<Resource>> existingHostVms, 
        VSphereUtil vim)  
    throws IOException, PluginException {
    	for (String hostName : existingHostVms.keySet()) {
            List<Resource> hqVms = existingHostVms.get(hostName);
            List<Resource> vcVms = vcHosts.get(hostName);
            
            if (vcVms == null) {
                // not one of the hosts in vCenter
                Resource resource = existingHosts.get(hostName);
                
                if (resource != null) {
                    removeHost(api, resource, vim);
                }
            } else {
                // vm names may be the same, so use fqdn (uuid) to
                // determine whether vms should be deleted from hq
                List<String> vcVmFqdns = new ArrayList<String>();
                
                for (Resource resource : vcVms) {
                    String fqdn = getFqdn(resource);
                    
                    if (fqdn != null) {
                        vcVmFqdns.add(fqdn);
                    }
                }
                
                for (Resource resource : hqVms) {
                    String fqdn = getFqdn(resource);
                    
                    if (fqdn != null && !vcVmFqdns.contains(fqdn)) {
                        // Not one of the powered-on VMs from vCenter
                        removeVM(api, resource, vim);
                    }
                }
            }
        }
    }

    private boolean isVCenterManagedEntity(String vCenterUrl, Resource resource) {
        String vcUrl = (String) resource.getConfigs().get(VSphereUtil.PROP_URL);
        
        // TODO get configs working...
        return true; //vCenterUrl != null && vCenterUrl.equals(vcUrl);
    }
    
    private Resource getVCenterServer(RestApi api, String vCenterUrl) throws IOException, PluginException {
    	Resource vCenter = null;        
        
    	if (vCenterUrl != null) {
	        List<Resource> resources = api.getResourcesByTypeName(Constants.VMWARE_VCENTER);
	        
	        for (Resource resource : resources) {
	            if (isVCenterManagedEntity(vCenterUrl, resource)) {
	                vCenter = resource;
	                
	                break;
	            }
	        }
    	}        
    	
        return vCenter;
    }
    
    private String getEsxHost(Resource resource) {
        String esxHost = null;
        String typeName = resource.getType().getName();
        
        if (Constants.VMWARE_VSPHERE_VM.equals(typeName)) {
        	esxHost = (String) resource.getProperties().get(ESX_HOST);
        } else if (Constants.VMWARE_VSPHERE_HOST.equals(typeName)) {
        	esxHost = (String) resource.getConfigs().get(VSphereUtil.PROP_HOSTNAME);
        }
        
        return esxHost;
    }

    private String getFqdn(Resource resource) {
        return (String) resource.getProperties().get(VSphereResource.FQDN);
    }
    
    /**
     * Generate an unique platform name by appending the uuid
     */
    private String generatePlatformName(String name, String uuid) {
        return name + " {" + uuid + "}";
    }
    
    private void removeHost(RestApi api, Resource resource, VSphereUtil vim) 
    throws IOException, PluginException {
        try {
            // verify to see if it exists in vCenter
            vim.findByUuid(VSphereUtil.HOST_SYSTEM, getFqdn(resource));
            
            if (log.isDebugEnabled()) {
                log.debug(Constants.VMWARE_VSPHERE_HOST + "[name=" + resource.getName() 
                              + "] exists in vCenter. Not removing from HQ.");
            }
        } catch (ManagedEntityNotFoundException me) {
            removeResource(api, resource);
        }
    }

    private void removeVM(RestApi api, Resource resource, VSphereUtil vim) 
    throws IOException, PluginException {
        try {
            // verify to see if it exists in vCenter
            vim.findByUuid(VSphereUtil.VM, getFqdn(resource));
            
            if (log.isDebugEnabled()) {
                log.debug(Constants.VMWARE_VSPHERE_VM + "[name=" + resource.getName() 
                              + "] exists in vCenter. Not removing from HQ.");
            }
        } catch (ManagedEntityNotFoundException me) {
            removeResource(api, resource);
        }
    }
    
    private void removeResource(RestApi api, Resource resource) 
    throws IOException, PluginException {
        if (log.isDebugEnabled()) {
            log.debug("Managed entity (" + resource.getName() + ") no longer exists in vCenter. "
                         + " Removing from HQ inventory.");
        }
        
        // throttle requests to the hq server to minimize StaleStateExceptions
        // TODO: there needs to be a better way to do this 
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // Ignore
        }

        // TODO: As a final step, need to check resource availability
        // (must be DOWN) before deleting.
        
        api.deleteResource(resource.getId());
    }
}

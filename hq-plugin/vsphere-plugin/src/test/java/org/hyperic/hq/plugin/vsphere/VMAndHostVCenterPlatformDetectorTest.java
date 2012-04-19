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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.easymock.classextension.EasyMock;
import org.hyperic.hq.hqapi1.AgentApi;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.ResourceApi;
import org.hyperic.hq.hqapi1.ResourceEdgeApi;
import org.hyperic.hq.hqapi1.types.Agent;
import org.hyperic.hq.hqapi1.types.AgentResponse;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourceConfig;
import org.hyperic.hq.hqapi1.types.ResourceEdge;
import org.hyperic.hq.hqapi1.types.ResourceFrom;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.ResourcePrototypeResponse;
import org.hyperic.hq.hqapi1.types.ResourceTo;
import org.hyperic.hq.hqapi1.types.ResourcesResponse;
import org.hyperic.hq.hqapi1.types.ResponseStatus;
import org.hyperic.hq.hqapi1.types.StatusResponse;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.ReflectionEqualsArgumentMatcher;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Test;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostDnsConfig;
import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostIpRouteConfig;
import com.vmware.vim25.HostListSummary;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemPowerState;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.ToolsConfigInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Unit test of the {@link VMAndHostVCenterPlatformDetector}
 * TODO Add tests for deleting removed hosts and VMs from inventory
 * @author jhickey
 * 
 */
public class VMAndHostVCenterPlatformDetectorTest {

    private HQApi hqApi;

    private ResourceApi resourceApi;

    private AgentApi agentApi;

    private ResourceEdgeApi resourceEdgeApi;

    private static final String PROP_PROVIDER_URL = "covalent.CAMProviderURL";

    private static final String PROP_AGENT_TOKEN = "covalent.CAMAgentToken";
    
    private static final String AGENT_TOKEN = "someAgentTokenString";

    private static final String VCENTER_URL = "url";

    private static final String VCENTER_UNAME = "uname";

    private static final String VCENTER_PW = "pass";

    private VSphereUtil vim;

    private VMAndHostVCenterPlatformDetector detector;

    private Properties props = new Properties();

    private HostSystem host;

    private ManagedEntity dataCenter;

    private ManagedEntity dataCenters;

    private VirtualMachine vm;

    private Datastore ds;
    
    private DatastoreInfo dsInfo;

    private ResourcePool resourcePool;

    @Before
    public void setUp() throws Exception {
        props.put(PROP_PROVIDER_URL, "http://localhost:8080/lather");
        props.put(PROP_AGENT_TOKEN, AGENT_TOKEN);
        props.put(VSphereUtil.PROP_URL, VCENTER_URL);
        props.put(VSphereUtil.PROP_USERNAME, VCENTER_UNAME);
        props.put(VSphereUtil.PROP_PASSWORD, VCENTER_PW);
        this.hqApi = EasyMock.createMock(HQApi.class);
        this.resourceApi = EasyMock.createMock(ResourceApi.class);
        this.agentApi = EasyMock.createMock(AgentApi.class);
        this.resourceEdgeApi = EasyMock.createMock(ResourceEdgeApi.class);
        this.vim = EasyMock.createMock(VSphereUtil.class);
        this.host = EasyMock.createMock(HostSystem.class);
        this.dataCenter = EasyMock.createMock(ManagedEntity.class);
        this.dataCenters = EasyMock.createMock(ManagedEntity.class);
        this.vm = EasyMock.createMock(VirtualMachine.class);
        this.ds = EasyMock.createMock(Datastore.class);
        this.dsInfo = EasyMock.createMock(DatastoreInfo.class);
        this.resourcePool = EasyMock.createMock(ResourcePool.class);
        this.detector = new VMAndHostVCenterPlatformDetector();
    }

  //@Test
    public void testDiscoverPlatforms() throws Exception {
        EasyMock.expect(hqApi.getResourceApi()).andReturn(resourceApi).times(13);
        ResourcePrototypeResponse protoResponse = new ResourcePrototypeResponse();
        protoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vcType = new ResourcePrototype();
        vcType.setName(VMAndHostVCenterPlatformDetector.VC_TYPE);
        protoResponse.setResourcePrototype(vcType);
        EasyMock.expect(resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.VC_TYPE))
            .andReturn(protoResponse).times(2);

        ResourcesResponse vCenterResponse = getVCenterServerResourceResponse(vcType);
        EasyMock.expect(resourceApi.getResources(vcType, true, false)).andReturn(vCenterResponse)
            .times(2);
        EasyMock.expect(hqApi.getAgentApi()).andReturn(agentApi);
        AgentResponse agentResponse = new AgentResponse();
        agentResponse.setStatus(ResponseStatus.SUCCESS);
        Agent agent = new Agent();
        agentResponse.setAgent(agent);
        EasyMock.expect(agentApi.getAgent(AGENT_TOKEN)).andReturn(agentResponse);

        ResourcePrototypeResponse hostProtoResponse = new ResourcePrototypeResponse();
        hostProtoResponse.setStatus(ResponseStatus.SUCCESS);

        ResourcePrototype hostType = new ResourcePrototype();
        hostType.setName(VMAndHostVCenterPlatformDetector.HOST_TYPE);
        hostProtoResponse.setResourcePrototype(hostType);
        EasyMock.expect(
            resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.HOST_TYPE))
            .andReturn(hostProtoResponse).times(2);

        createHost();
        createVM();
        
        ResourcePrototypeResponse vmDsProtoResponse = new ResourcePrototypeResponse();
        vmDsProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vmDsType = new ResourcePrototype();
        vmDsType.setName(VMAndHostVCenterPlatformDetector.VM_DS_TYPE);
        vmDsProtoResponse.setResourcePrototype(vmDsType);
        EasyMock.expect(resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.VM_DS_TYPE))
            .andReturn(vmDsProtoResponse).times(1);

        ResourcePrototypeResponse hostDsProtoResponse = new ResourcePrototypeResponse();
        hostDsProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype hostDSType = new ResourcePrototype();
        hostDSType.setName(VMAndHostVCenterPlatformDetector.HOST_DS_TYPE);
        hostDsProtoResponse.setResourcePrototype(hostDSType);
        EasyMock.expect(resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.HOST_DS_TYPE))
            .andReturn(hostDsProtoResponse).times(1);

        ResourcePrototypeResponse vmProtoResponse = new ResourcePrototypeResponse();
        vmProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vmType = new ResourcePrototype();
        vmType.setName(VMAndHostVCenterPlatformDetector.VM_TYPE);
        vmProtoResponse.setResourcePrototype(vmType);
        EasyMock.expect(resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.VM_TYPE))
            .andReturn(vmProtoResponse).times(2);

        StatusResponse genericSuccess = new StatusResponse();
        genericSuccess.setStatus(ResponseStatus.SUCCESS);

      
        
        List<Resource> expectedVMs = new ArrayList<Resource>();
        VSphereResource expectedVM = getExpectedVM(vmType, agent);
        expectedVMs.add(expectedVM);
        EasyMock.expect(
            resourceApi.syncResources(ReflectionEqualsArgumentMatcher.eqObject(expectedVMs)))
            .andReturn(genericSuccess);
        
        
        List<Resource> expectedResources = new ArrayList<Resource>();
        VSphereHostResource expectedHost = getExpectedHost(hostType, vmType, agent);
        expectedResources.add(expectedHost);
        EasyMock.expect(
            resourceApi.syncResources(ReflectionEqualsArgumentMatcher.eqObject(expectedResources)))
            .andReturn(genericSuccess);
        
        EasyMock.expect(hqApi.getResourceEdgeApi()).andReturn(resourceEdgeApi).times(2);

        ResourcesResponse esxHostResponse = getEsxHostResourceResponse(hostType, vmType, agent);
        EasyMock.expect(resourceApi.getResources(hostType, true, false)).andReturn(esxHostResponse);

        List<ResourceEdge> expectedVCenterToHostEdges = new ArrayList<ResourceEdge>();
        expectedVCenterToHostEdges.add(getExpectedVirtualEdge(vCenterResponse.getResource().get(0),
            expectedHost));
        EasyMock.expect(
            resourceEdgeApi.syncResourceEdges(ReflectionEqualsArgumentMatcher
                .eqObject(expectedVCenterToHostEdges))).andReturn(genericSuccess);

        List<ResourceEdge> expectedHostToVMEdges = new ArrayList<ResourceEdge>();
        expectedHostToVMEdges.add(getExpectedVirtualEdge(expectedHost, expectedHost
            .getVirtualMachines().get(0)));
        EasyMock.expect(
            resourceEdgeApi.syncResourceEdges(ReflectionEqualsArgumentMatcher
                .eqObject(expectedHostToVMEdges))).andReturn(genericSuccess);

        ResourcesResponse vmResponse = getVMResourceResponse(vmType, agent);
        EasyMock.expect(resourceApi.getResources(vmType, true, false)).andReturn(vmResponse);
        
        replay();
        detector.discoverPlatforms(props, hqApi, vim);
        verify();
    }

    private ResourcesResponse getVCenterServerResourceResponse(ResourcePrototype vcType) {
        Resource resource = new Resource();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setKey(VSphereUtil.PROP_URL);
        resourceConfig.setValue(VCENTER_URL);
        resource.getResourceConfig().add(resourceConfig);
        resource.setResourcePrototype(vcType);
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(resource);
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }

    private ResourcesResponse getEsxHostResourceResponse(ResourcePrototype hostType,
                                                         ResourcePrototype vmType, Agent agent) {
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(getExpectedHost(hostType, vmType, agent));
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }

    private ResourcesResponse getVMResourceResponse(ResourcePrototype vmType, Agent agent) {
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(getExpectedVM(vmType, agent));
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }

    private void createHost() throws PluginException, InvalidProperty, RuntimeFault, RemoteException {
        EasyMock.expect(vim.find(VSphereUtil.HOST_SYSTEM)).andReturn(new ManagedEntity[] { host });
        HostRuntimeInfo hostInfo = new HostRuntimeInfo();
        hostInfo.setPowerState(HostSystemPowerState.poweredOn);
        EasyMock.expect(host.getRuntime()).andReturn(hostInfo);
        HostConfigInfo hostConfig = new HostConfigInfo();
        HostNetworkInfo networkInfo = new HostNetworkInfo();
        HostIpRouteConfig ipRouteConfig = new HostIpRouteConfig();
        ipRouteConfig.setDefaultGateway("1.2.3.4");
        networkInfo.setIpRouteConfig(ipRouteConfig);
        HostDnsConfig dnsConfig = new HostDnsConfig();

        networkInfo.setDnsConfig(dnsConfig);
        HostVirtualNic nic = new HostVirtualNic();

        HostVirtualNicSpec nicSpec = new HostVirtualNicSpec();
        nic.setSpec(nicSpec);
        HostIpConfig ipConfig = new HostIpConfig();
        ipConfig.setIpAddress("10.0.1.123");
        ipConfig.setSubnetMask("10.0.1.0");

        nicSpec.setIp(ipConfig);
        nicSpec.setMac("65.7.8.9");
        networkInfo.setVnic(new HostVirtualNic[] { nic });
        AboutInfo aboutInfo = new AboutInfo();
        aboutInfo.setFullName("ESX Host 4.1");
        aboutInfo.setVersion("4.1");
        aboutInfo.setBuild("5322");
        hostConfig.setNetwork(networkInfo);
        hostConfig.setProduct(aboutInfo);
        HostListSummary hostListSummary = new HostListSummary();
        HostHardwareSummary hardware = new HostHardwareSummary();
        hardware.setUuid("1234-5678-9012-34");
        hardware.setNumCpuCores((short) 4);
        hardware.setNumCpuPkgs((short) 2);
        hardware.setVendor("Home Depot");
        hardware.setModel("3G67");
        hardware.setCpuModel("Intel");
        hostListSummary.setHardware(hardware);
        EasyMock.expect(host.getConfig()).andReturn(hostConfig);
        EasyMock.expect(host.getSummary()).andReturn(hostListSummary);
        EasyMock.expect(host.getName()).andReturn("esxHost123").times(2);
        EasyMock.expect(host.getParent()).andReturn(dataCenter);
        EasyMock.expect(dataCenter.getName()).andReturn("Test DataCenter").times(2);
        EasyMock.expect(dataCenter.getParent()).andReturn(dataCenters);
        EasyMock.expect(dataCenters.getName()).andReturn("Datacenters");
        EasyMock.expect(dataCenters.getParent()).andReturn(null);
        EasyMock.expect(ds.getName()).andReturn("ds1").anyTimes();
        EasyMock.expect(ds.getInfo()).andReturn(dsInfo).anyTimes();
        EasyMock.expect(dsInfo.getUrl()).andReturn("ds://vmfs/volumes/4e9c6624-e4950d68-3b10-f4ce46bfd1c3/").anyTimes();
        EasyMock.expect(host.getDatastores()).andReturn(new Datastore[]{ ds });
    }

    private void createVM() throws InvalidProperty, RuntimeFault, RemoteException {
        EasyMock.expect(host.getVms()).andReturn(new VirtualMachine[] { vm });
        VirtualMachineConfigInfo config = new VirtualMachineConfigInfo();
        config.setTemplate(false);
        config.setUuid("4206c445-7eb4-a72f-38c8-d865c4c374f2");
        config.setName("Test VM");
        config.setGuestFullName("Linux OS");
        config.setVersion("8.1");
        VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
        fileInfo.setVmPathName("/pathto/vm");
        config.setFiles(fileInfo);
        VirtualHardware hardware = new VirtualHardware();
        hardware.setNumCPU(4);
        hardware.setMemoryMB(1000);
        config.setHardware(hardware);
        ToolsConfigInfo toolsConfig = new ToolsConfigInfo();
        toolsConfig.setToolsVersion(4);
        config.setTools(toolsConfig);
        EasyMock.expect(resourcePool.getPropertyByPath("name")).andReturn("Test vApp");
        VirtualMachineRuntimeInfo runtime = new VirtualMachineRuntimeInfo();
        runtime.setPowerState(VirtualMachinePowerState.poweredOn);
        GuestInfo guestInfo = new GuestInfo();
        guestInfo.setHostName("leela.local");
        GuestNicInfo nic = new GuestNicInfo();
        nic.setMacAddress("123.456.789");
        nic.setIpAddress(new String[] {"10.150.21.34"});
        guestInfo.setNet(new GuestNicInfo[] {nic});
        EasyMock.expect(vm.getConfig()).andReturn(config);
        EasyMock.expect(vm.getRuntime()).andReturn(runtime);
        EasyMock.expect(vm.getGuest()).andReturn(guestInfo);
        EasyMock.expect(vm.getResourcePool()).andReturn(resourcePool);
        EasyMock.expect(vm.getDatastores()).andReturn(new Datastore[]{ ds });
    }

    private VSphereHostResource getExpectedHost(ResourcePrototype hostPrototype,
                                                ResourcePrototype vmType, Agent agent) {
        VSphereHostResource expectedHost = new VSphereHostResource();
        expectedHost.setName("esxHost123 {1234-5678-9012-34}");
        expectedHost.setDescription("ESX Host 4.1");
        expectedHost.setFqdn("1234-5678-9012-34");
        expectedHost.addIp("10.0.1.123", "10.0.1.0", "65.7.8.9");
        expectedHost.addConfig(VSphereUtil.PROP_HOSTNAME, "esxHost123");
        expectedHost.addConfig(VSphereCollector.PROP_UUID, "1234-5678-9012-34");
        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue("version", "4.1");
        cprops.setValue("build", "5322");
        cprops.setValue("ip", "10.0.1.123");
        cprops.setValue("defaultGateway", "1.2.3.4");
        cprops.setValue("hwVendor", "Home Depot");
        cprops.setValue("hwModel", "3G67");
        cprops.setValue("hwCpu", "Intel");
        cprops.setValue("hwSockets", "2");
        cprops.setValue("hwCores", "2");
        cprops.setValue("parent", "Test DataCenter");
        expectedHost.addProperties(cprops);
        expectedHost.setResourcePrototype(hostPrototype);
        expectedHost.setAgent(agent);
        expectedHost.addConfig(VSphereUtil.PROP_URL, VCENTER_URL);
        expectedHost.addConfig(VSphereUtil.PROP_USERNAME, VCENTER_UNAME);
        expectedHost.addConfig(VSphereUtil.PROP_PASSWORD, VCENTER_PW);
        expectedHost.getVirtualMachines().add(getExpectedVM(vmType, agent));
        return expectedHost;
    }

    private VSphereResource getExpectedVM(ResourcePrototype vmType, Agent agent) {
        VSphereResource expectedVm = new VSphereResource();
        expectedVm.setName("Test VM {4206c445-7eb4-a72f-38c8-d865c4c374f2}");
        expectedVm.setFqdn("4206c445-7eb4-a72f-38c8-d865c4c374f2");
        expectedVm.setDescription("Linux OS");
        ConfigResponse config = new ConfigResponse();
        config.setValue(VSphereVmCollector.PROP_VM, "Test VM");
        config.setValue(VSphereCollector.PROP_UUID, "4206c445-7eb4-a72f-38c8-d865c4c374f2");
        expectedVm.addConfig(config);
        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue(ProductPlugin.PROP_INSTALLPATH, "/pathto/vm");
        cprops.setValue("guestOS", "Linux OS");
        cprops.setValue("version", "8.1");
        cprops.setValue("numvcpus", 4);
        cprops.setValue("memsize", 1000);
        cprops.setValue("toolsVersion", "4");
        cprops.setValue("pool", "Test vApp");
        cprops.setValue("hostName", "leela.local");
        expectedVm.addProperties(cprops);
        expectedVm.setResourcePrototype(vmType);
        expectedVm.setAgent(agent);
        expectedVm.addConfig(VSphereUtil.PROP_URL, VCENTER_URL);
        expectedVm.addConfig(VSphereUtil.PROP_USERNAME, VCENTER_UNAME);
        expectedVm.addConfig(VSphereUtil.PROP_PASSWORD, VCENTER_PW);
        return expectedVm;
    }

    private ResourceEdge getExpectedVirtualEdge(Resource parent, Resource child) {
        ResourceEdge edge = new ResourceEdge();
        ResourceFrom from = new ResourceFrom();
        from.setResource(parent);
        ResourceTo to = new ResourceTo();
        to.getResource().add(child);
        edge.setRelation("virtual");
        edge.setResourceFrom(from);
        edge.setResourceTo(to);
        return edge;
    }

    private void replay() {
        EasyMock.replay(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host, dataCenter,
            dataCenters, vm, resourcePool, ds, dsInfo);
    }

    private void verify() {
        EasyMock.verify(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host, dataCenter,
            dataCenters, vm, resourcePool, ds, dsInfo);
    }
}

package org.hyperic.hq.plugin.vsphere;

import java.rmi.RemoteException;
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
import org.hyperic.hq.hqapi1.types.ResourceInfo;
import org.hyperic.hq.hqapi1.types.ResourceProperty;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.ResourcePrototypeResponse;
import org.hyperic.hq.hqapi1.types.ResourcesResponse;
import org.hyperic.hq.hqapi1.types.ResponseStatus;
import org.hyperic.hq.hqapi1.types.StatusResponse;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Test;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.GuestInfo;
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
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.VirtualMachine;
/**
 * Unit test of the {@link VMAndHostVCenterPlatformDetector}
 * @author jhickey
 *
 */
public class VMAndHostVCenterPlatformDetectorTest {

    private HQApi hqApi;

    private ResourceApi resourceApi;

    private AgentApi agentApi;

    private ResourceEdgeApi resourceEdgeApi;

    private static final String VCENTER_URL = "url";

    private static final String VCENTER_UNAME = "uname";

    private static final String VCENTER_PW = "pass";
    private static final int ESX_HOST_ID = 12345;

    private VSphereUtil vim;

    private VMAndHostVCenterPlatformDetector detector;

    private Properties props = new Properties();

    private HostSystem host;
    
    private ManagedEntity dataCenter;
    
    private ManagedEntity dataCenters;
    
    private VirtualMachine vm;
    
    private ResourcePool resourcePool;

    @Before
    public void setUp() throws Exception {
        props.put(VMAndHostVCenterPlatformDetector.AGENT_IP, "127.0.0.1");
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
        this.resourcePool = EasyMock.createMock(ResourcePool.class);
        this.detector = new VMAndHostVCenterPlatformDetector();
    }

    @Test
    public void testDiscoverPlatforms() throws Exception {
        EasyMock.expect(hqApi.getResourceApi()).andReturn(resourceApi).times(12);
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
        EasyMock.expect(agentApi.getAgent("127.0.0.1", 2144)).andReturn(agentResponse);

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
        
        EasyMock.expect(vim.findByUuid(VSphereUtil.HOST_SYSTEM, "myPlat.testEsx")).andThrow(
            new ManagedEntityNotFoundException("Not Found"));

        ResourcePrototypeResponse vmProtoResponse = new ResourcePrototypeResponse();
        vmProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vmType = new ResourcePrototype();
        vmType.setName(VMAndHostVCenterPlatformDetector.VM_TYPE);
        vmProtoResponse.setResourcePrototype(vmType);
        EasyMock.expect(resourceApi.getResourcePrototype(VMAndHostVCenterPlatformDetector.VM_TYPE))
            .andReturn(vmProtoResponse).times(2);

        StatusResponse genericSuccess = new StatusResponse();
        genericSuccess.setStatus(ResponseStatus.SUCCESS);
        // TODO validate data being passed to syncResources
        EasyMock.expect(resourceApi.syncResources(EasyMock.isA(List.class))).andReturn(
            genericSuccess).times(2);
        EasyMock.expect(hqApi.getResourceEdgeApi()).andReturn(resourceEdgeApi).times(2);

        ResourcesResponse esxHostResponse = getEsxHostResourceResponse(hostType);
        EasyMock.expect(resourceApi.getResources(hostType, true, false)).andReturn(esxHostResponse);

        // TODO validate data being passed to syncResourceEdges
        EasyMock.expect(resourceEdgeApi.syncResourceEdges(EasyMock.isA(List.class))).andReturn(
            genericSuccess).times(2);

        ResourcesResponse vmResponse = getVMResourceResponse(vmType);
        EasyMock.expect(resourceApi.getResources(vmType, true, false)).andReturn(vmResponse);

        EasyMock.expect(resourceApi.deleteResource(ESX_HOST_ID)).andReturn(genericSuccess);
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

    private ResourcesResponse getEsxHostResourceResponse(ResourcePrototype hostType) {
        Resource resource = new Resource();
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setKey("fqdn");
        resourceInfo.setValue("myPlat.testEsx");
        resource.getResourceInfo().add(resourceInfo);
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setKey(VSphereUtil.PROP_URL);
        resourceConfig.setValue(VCENTER_URL);
        resource.getResourceConfig().add(resourceConfig);

        ResourceConfig hostConfig = new ResourceConfig();
        hostConfig.setKey(VSphereUtil.PROP_HOSTNAME);
        hostConfig.setValue("testEsx");
        resource.getResourceConfig().add(hostConfig);

        resource.setResourcePrototype(hostType);
        resource.setId(ESX_HOST_ID);
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(resource);
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }

    private ResourcesResponse getVMResourceResponse(ResourcePrototype vmType) {
        Resource resource = new Resource();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setKey(VSphereUtil.PROP_URL);
        resourceConfig.setValue(VCENTER_URL);
        resource.getResourceConfig().add(resourceConfig);

        ResourceConfig uuidConfig = new ResourceConfig();
        uuidConfig.setKey(VSphereCollector.PROP_UUID);
        uuidConfig.setValue("4206c445-7eb4-a72f-38c8-d865c4c374f2");
        resource.getResourceConfig().add(uuidConfig);

        ResourceProperty property = new ResourceProperty();
        property.setKey(VMAndHostVCenterPlatformDetector.ESX_HOST);
        property.setValue("testEsx");
        resource.getResourceProperty().add(property);

        resource.setResourcePrototype(vmType);
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(resource);
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }
    
    private void createHost() throws PluginException {
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
        hostConfig.setNetwork(networkInfo);
        hostConfig.setProduct(aboutInfo);
        HostListSummary hostListSummary = new HostListSummary();
        HostHardwareSummary hardware = new HostHardwareSummary();
        hardware.setUuid("1234-5678-9012-34");
        hardware.setNumCpuCores((short)4);
        hardware.setNumCpuPkgs((short)2);
        hostListSummary.setHardware(hardware);
        EasyMock.expect(host.getConfig()).andReturn(hostConfig);
        EasyMock.expect(host.getSummary()).andReturn(hostListSummary);
        EasyMock.expect(host.getName()).andReturn("esxHost123").times(2);
        EasyMock.expect(host.getParent()).andReturn(dataCenter);
        EasyMock.expect(dataCenter.getName()).andReturn("Test DataCenter").times(2);
        EasyMock.expect(dataCenter.getParent()).andReturn(dataCenters);
        EasyMock.expect(dataCenters.getName()).andReturn("Datacenters");
        EasyMock.expect(dataCenters.getParent()).andReturn(null);
    }
    
    private void createVM() throws InvalidProperty, RuntimeFault, RemoteException {
        EasyMock.expect(host.getVms()).andReturn(new VirtualMachine[]  {vm});
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
        EasyMock.expect(vm.getConfig()).andReturn(config);
        EasyMock.expect(vm.getRuntime()).andReturn(runtime);
        EasyMock.expect(vm.getGuest()).andReturn(guestInfo);
        EasyMock.expect(vm.getResourcePool()).andReturn(resourcePool);
    }

    private void replay() {
        EasyMock.replay(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host, dataCenter, dataCenters, vm, resourcePool);
    }

    private void verify() {
        EasyMock.verify(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host, dataCenter, dataCenters, vm, resourcePool);
    }
}

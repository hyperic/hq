package org.hyperic.hq.plugin.vsphere;

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
import org.junit.Before;
import org.junit.Test;

import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;

public class VCenterPlatformDetectorTest {

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
        
        EasyMock.expect(vim.find(VSphereUtil.HOST_SYSTEM)).andReturn(new ManagedEntity[] {host});
        HostRuntimeInfo hostInfo = new HostRuntimeInfo();
        EasyMock.expect(host.getRuntime()).andReturn(hostInfo);
        EasyMock.expect(vim.findByUuid(VSphereUtil.HOST_SYSTEM, "myPlat.testEsx")).andThrow(new ManagedEntityNotFoundException("Not Found"));

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

    private void replay() {
        EasyMock.replay(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host);
    }

    private void verify() {
        EasyMock.verify(hqApi, resourceApi, agentApi, resourceEdgeApi, vim, host);
    }
}

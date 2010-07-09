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
import org.junit.Ignore;
import org.junit.Test;
@Ignore
public class VCenterPlatformDetectorTest {
    
    private HQApi hqApi;
    
    private ResourceApi resourceApi;
    
    private AgentApi agentApi;
    
    private ResourceEdgeApi resourceEdgeApi;
    
    private static final String VCENTER_URL= "url";
    
    private static final String VCENTER_UNAME = "uname";
    
    private static final String VCENTER_PW = "pw";
    private static final int ESX_HOST_ID = 12345;
    
    private VSphereUtil vim;
    
    private VCenterPlatformDetector detector;
    
    private Properties props = new Properties();
    
    @Before
    public void setUp() throws Exception {
        props.put(VCenterPlatformDetector.AGENT_IP, "127.0.0.1");
        props.put(VSphereUtil.PROP_URL,VCENTER_URL);
        props.put(VSphereUtil.PROP_USERNAME,VCENTER_UNAME);
        props.put(VSphereUtil.PROP_PASSWORD,VCENTER_PW);
        this.hqApi = EasyMock.createMock(HQApi.class);
        this.resourceApi = EasyMock.createMock(ResourceApi.class);
        this.agentApi = EasyMock.createMock(AgentApi.class);
        this.resourceEdgeApi = EasyMock.createMock(ResourceEdgeApi.class);
        //TODO make a mock vim for true unit test, switch out to connect to a real vCenter
        this.vim = VSphereUtil.getInstance(props);
        this.detector = new VCenterPlatformDetector(props, hqApi,vim );
    }

    @Test
    public void testDiscoverPlatforms() throws Exception {
        EasyMock.expect(hqApi.getResourceApi()).andReturn(resourceApi).times(15);
        ResourcePrototypeResponse protoResponse = new ResourcePrototypeResponse();
        protoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vcType = new ResourcePrototype();
        vcType.setName(VCenterPlatformDetector.VC_TYPE);
        protoResponse.setResourcePrototype(vcType);
        EasyMock.expect(resourceApi.getResourcePrototype(VCenterPlatformDetector.VC_TYPE)).andReturn(protoResponse).times(2);
    
        ResourcesResponse vCenterResponse = getVCenterServerResourceResponse(vcType);
        EasyMock.expect(resourceApi.getResources(vcType, true,false)).andReturn(vCenterResponse).times(2);
        EasyMock.expect(hqApi.getAgentApi()).andReturn(agentApi);
        AgentResponse agentResponse = new AgentResponse();
        agentResponse.setStatus(ResponseStatus.SUCCESS);
        Agent agent = new Agent();
        agentResponse.setAgent(agent);
        EasyMock.expect(agentApi.getAgent("127.0.0.1", 2144)).andReturn(agentResponse);
        
        ResourcePrototypeResponse hostProtoResponse = new ResourcePrototypeResponse();
        hostProtoResponse.setStatus(ResponseStatus.SUCCESS);
        
        ResourcePrototype hostType = new ResourcePrototype();
        hostType.setName(VCenterPlatformDetector.HOST_TYPE);
        hostProtoResponse.setResourcePrototype(hostType);
        EasyMock.expect(resourceApi.getResourcePrototype(VCenterPlatformDetector.HOST_TYPE)).andReturn(hostProtoResponse).times(2);
        
        ResourcePrototypeResponse vmProtoResponse = new ResourcePrototypeResponse();
        vmProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vmType = new ResourcePrototype();
        vmType.setName(VCenterPlatformDetector.VM_TYPE);
        vmProtoResponse.setResourcePrototype(vmType);
        EasyMock.expect(resourceApi.getResourcePrototype(VCenterPlatformDetector.VM_TYPE)).andReturn(vmProtoResponse).times(2);
        
        ResourcePrototypeResponse vAppProtoResponse = new ResourcePrototypeResponse();
        vAppProtoResponse.setStatus(ResponseStatus.SUCCESS);
        ResourcePrototype vAppType = new ResourcePrototype();
        vAppType.setName(VCenterPlatformDetector.VAPP_TYPE);
        vAppProtoResponse.setResourcePrototype(vAppType);
        EasyMock.expect(resourceApi.getResourcePrototype(VCenterPlatformDetector.VAPP_TYPE)).andReturn(vAppProtoResponse).times(2);
        
        
        StatusResponse genericSuccess =  new StatusResponse();
        genericSuccess.setStatus(ResponseStatus.SUCCESS);
        //TODO validate data being passed to syncResources
        EasyMock.expect(resourceApi.syncResources(EasyMock.isA(List.class))).andReturn(genericSuccess).times(3);
        EasyMock.expect(hqApi.getResourceEdgeApi()).andReturn(resourceEdgeApi).times(3);
        
        ResourcesResponse esxHostResponse = getEsxHostResourceResponse(hostType);
        EasyMock.expect(resourceApi.getResources(hostType, true,false)).andReturn(esxHostResponse);
      
       //TODO validate data being passed to syncResourceEdges
        EasyMock.expect(resourceEdgeApi.syncResourceEdges(EasyMock.isA(List.class))).andReturn(genericSuccess).times(3);
        
        ResourcesResponse vmResponse = getVMResourceResponse(vmType);
        EasyMock.expect(resourceApi.getResources(vmType, true,false)).andReturn(vmResponse);
        
        ResourcesResponse vappResponse = getVAppResourceResponse(vAppType);
        EasyMock.expect(resourceApi.getResources(vAppType, true,false)).andReturn(vappResponse);
        
        EasyMock.expect(resourceApi.deleteResource(ESX_HOST_ID)).andReturn(genericSuccess);
        replay();
        detector.discoverPlatforms();
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
        
        ResourceProperty property =  new ResourceProperty();
        property.setKey(VCenterPlatformDetector.ESX_HOST);
        property.setValue("testEsx");
        resource.getResourceProperty().add(property);
        
        resource.setResourcePrototype(vmType);
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(resource);
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }
    
    private ResourcesResponse getVAppResourceResponse(ResourcePrototype vAppType) {
        Resource resource = new Resource();
        resource.setName("JenApp");
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setKey(VSphereUtil.PROP_URL);
        resourceConfig.setValue(VCENTER_URL);
        resource.getResourceConfig().add(resourceConfig);
        
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setKey("fqdn");
        resourceInfo.setValue("resgroup-v327");
        resource.getResourceInfo().add(resourceInfo);
        
        resource.setResourcePrototype(vAppType);
        ResourcesResponse response = new ResourcesResponse();
        response.getResource().add(resource);
        response.setStatus(ResponseStatus.SUCCESS);
        return response;
    }
    
    private void replay() {
        EasyMock.replay(hqApi, resourceApi, agentApi, resourceEdgeApi);
    }
    
    private void verify() {
        EasyMock.verify(hqApi, resourceApi, agentApi, resourceEdgeApi);
    }
}

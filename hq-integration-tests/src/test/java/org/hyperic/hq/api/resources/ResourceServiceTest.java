/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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
package org.hyperic.hq.api.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easymock.EasyMock;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.api.model.ConfigurationValue;
import org.hyperic.hq.api.model.PropertyList;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.FailedResource;
import org.hyperic.hq.api.model.resources.ComplexIp;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.resources.ResourceServiceTest.ResourceServiceTestDataPopulator;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator;
import org.hyperic.hq.api.rest.RestTestCaseBase;
import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingsIteration;
import org.hyperic.hq.api.services.ResourceService;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.agent.MeasurementCommandsAPI;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_args;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.tests.context.TestData;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.security.KeystoreConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@DirtiesContext
@ServiceBindingsIteration(ResourceServiceTest.CONTEXT_URL + "/rest/resource")
@ContextConfiguration(locations = { "ResourceServiceTest-context.xml"}) 
@TestData(ResourceServiceTestDataPopulator.class)
public class ResourceServiceTest extends RestTestCaseBase<ResourceService, ResourceServiceTestDataPopulator>{

    private static final int GENERATE_CONFIG_FLAG = 2<<1 ; 
    private static final int FAILED_RESOURCE_FLAG = 2<<2 ;
    private static final int USE_NATURAL_ID_FLAG = 2<<3 ; 
    
    @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain.around(new PlatformsIterationInterceptor(PlatformsIteration.class)) ;
    
	private Platform currentPlatform ;
	
	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD})
	private @interface PlatformsIteration { 
		int noOfPlatforms() default ResourceServiceTestDataPopulator.NO_OF_TEST_PLATFORMS ; 
	}//EO inner class PlaformsIterator 
	
    private final class PlatformsIterationInterceptor extends IterationInterceptor<PlatformsIteration> { 
    	
    	public PlatformsIterationInterceptor(final Class<PlatformsIteration> platformsIterationType) { 
    		super(platformsIterationType) ; 
    	}//EOM 
    	
    	 @Override
    	protected final void doBeforeEvaluation(final int iIterationIndex, PlatformsIteration metadata) {
    		currentPlatform = testBed.platforms.get(iIterationIndex) ; 
    	}//EOM 
    	
    	 @Override
    	protected final int getIterationLength(final PlatformsIteration metadata) {
    		return metadata.noOfPlatforms() ;
    	}//EOM 
    }//EO inner class PlatformsIterationInterceptor
    
    //@PlatformsIteration(noOfPlatforms=1)
    //@Test
    public final void testGetWADL() throws Throwable {
        final String WADL = this.getWADL(this.service) ; 
        System.out.println(WADL);
    }//EOM

    
    @PlatformsIteration
    @Test
    public final void testGetResourceWithInternalAndNaturalPlatformIDs() throws Throwable {
    	
    	final int hierarchyDepth = 3 ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes) ; 
		
		//now natural ID 
		resource = this.getApprovedResource(this.currentPlatform.getFqdn(), ResourceTypeModel.PLATFORM, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes) ; 
		
	}//EOM 
    



    @PlatformsIteration
    @Test
    public final void testGetResourceNoConfig() throws Throwable { 
    	final int hierarchyDepth = 3 ; 
    	
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC } ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform.getResource().getId()+"", null/*naturalID*/, null/*resource type*/, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, null) ; 
		
		//now natural ID 
		resource = this.getApprovedResource(null/*internal id*/, this.currentPlatform.getFqdn()/*naturalID*/,ResourceTypeModel.PLATFORM, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, null) ;
    }//EOM 
    
    @PlatformsIteration(noOfPlatforms=1) 
    @Test
    public final void testGetResourceOnlyConfig() throws Throwable { 
    	final int hierarchyDepth = 3 ;
    	//Note: should still contain an internal id 
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.PROPERTIES } ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform.getResource().getId()+"", null/*naturalID*/, null/*resource type*/,  hierarchyDepth, responseStructure) ;
    	
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes, ResourceDetailsType.PROPERTIES) ; 
    }//EOM   


    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetResourceNoDepth() throws Throwable { 
    	final int hierarchyDepth = 1 ;
    	//Note: should still contain an internal id 
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC } ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform.getResource().getId()+"", null/*naturalID*/, null/*resource type*/, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, null) ;
    }//EOM 
    
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetResourceDepth1() throws Throwable { 
    	final int hierarchyDepth = 2 ;
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes) ;
    }//EOM 
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetResourceDepth5() throws Throwable { 
    	final int hierarchyDepth = 5 ;
    	ResourceModel resource = this.getApprovedResource(this.currentPlatform, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes) ;
    }//EOM
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetResourceNegativeDepth() throws Throwable { 
    	//should error be raised or should the hierarchy be treated as 0 
    	final int hierarchyDepth = -5 ;
    	final ResourceModel resource = this.getApprovedResource(this.currentPlatform, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes, this.testBed.persistedListConfigAttributes) ;
    }//EOM
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetResourceByInvalidNaturalID() throws Throwable { 
    	this.errorInterceptor.expect(ServerWebApplicationException.class) ; 
    	final int hierarchyDepth = 1 ;
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC } ;
    	this.getApprovedResource(null/*internal id*/, "BOGUS_FQDN234",ResourceTypeModel.PLATFORM, hierarchyDepth, responseStructure) ;
    }//EOM
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test(expected=ServerWebApplicationException.class)
    public final void testGetResourceByInvalidInternalID() throws Throwable { 
    	final int hierarchyDepth = 3 ;
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC } ; 
    	
    	this.getApprovedResource("BOGUS_ID234", null/*naturalID*/, null/*resource type*/, hierarchyDepth, responseStructure) ;
    }//EOM 
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetServerByInternalID() throws Throwable { 
    	final int hierarchyDepth = 2;
    	//Note: should still contain an internal id 
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.ALL } ; 
    	
    	final Server server  = this.currentPlatform.getServers().iterator().next() ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(server.getResource().getId()+"", null/*naturalID*/, null/*resource type*/, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, server, hierarchyDepth, this.testBed.persistedConfigAttributes) ; 
    }//EOM 
    
    @PlatformsIteration(noOfPlatforms=1)
    @Test
    public final void testGetServiceByInternalID() throws Throwable { 
    	final int hierarchyDepth = 3 ;
    	//Note: should still contain an internal id 
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.ALL } ; 

    	final Service service = this.currentPlatform.getServers().iterator().next().getServices().iterator().next() ; 
    	
		//internal id first 
    	ResourceModel resource = this.getApprovedResource(service.getResource().getId()+"", null/*naturalID*/, null/*resource type*/, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, service, hierarchyDepth, this.testBed.persistedConfigAttributes) ; 
    }//EOM 
    
    @Test
    public final void testUpdateResources1Resource() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ (USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG) } ) ;
    }//EOM
    
    @Test
    public final void testUpdateResources2ResourcesSuccess() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG), 
    			(GENERATE_CONFIG_FLAG)
    		} 
    	) ;
    }//EOM
    
    @Test
    public final void testUpdateResources2ResourcesOneFailure() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG), 
    			(GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG)
    		} 
    	) ;
    }//EOM
    @Test
    public final void testUpdateResources2Resources2Failures() throws Throwable{
        this.errorInterceptor.expect(ServerWebApplicationException.class) ;
        this.errorInterceptor.expectMessage("Error on HTTP request: 500 Internal Error [http://localhost/tests/rest/resource]");

    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG), 
    			(GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG)
    		} 
    	) ;
    }//EOM
    
    @Test
    public final void testUpdateResourcesNoActualUpdateSent() throws Throwable{
    	//TODO: dont know what the expected behaviour here is 
    	this.innerTestUpdateResources(new int[]{}) ;
    }//EOM
    
    
    private final void innerTestUpdateResources(final int[] testHarnessMetadata) throws Throwable{
    	final int iNoOfResources = testHarnessMetadata.length ; 
    	
    	final Object[][] testHarness = new Object[iNoOfResources][]; //{ { this.platforms.get(0).getResource().getId(), null, "1" /*generateConfig*/, requestConfigAttributes} }  ;
    	
    	String platformID = null ; 
    	Platform platform = null ; 
    	for(int i=0; i < iNoOfResources; i++) {
    		testHarness[i] = new Object[5] ;

    		platform = this.testBed.platforms.get(i) ; 

    		//if the failed resource flag was set on the bit mask for the given platform index add a bogus prefix to the platform ID 
    		platformID = ( (testHarnessMetadata[i] & FAILED_RESOURCE_FLAG) == FAILED_RESOURCE_FLAG ? "BOGUS_" : "") ;   

    		//if the USE_NATURAL_ID flag was set on the bit mask for the given platform index use the
    		if ((testHarnessMetadata[i] & USE_NATURAL_ID_FLAG) == USE_NATURAL_ID_FLAG) { 
    			testHarness[i][1] = platformID + platform.getFqdn() ;
    			testHarness[i][2] = ResourceTypeModel.PLATFORM ; 
    		}else { 
    			testHarness[i][0] = platformID + platform.getResource().getId().toString() ; 
    		}//EO else if should use internal id 
    		
    		//set the generate config flag 
    		if ((testHarnessMetadata[i] & GENERATE_CONFIG_FLAG) == GENERATE_CONFIG_FLAG) testHarness[i][3] = "1" ; 
    		//set the requestConfigAttributes 
    		testHarness[i][4] = this.testBed.requestConfigAttributes ; 
    	}//EO while there are more resources to generate test harness for 
    	
    	final Resources resources = this.generateResources(iNoOfResources, testHarness); 
    	ResourceBatchResponse response = null ; 
    	
    	try{ 
    		response = service.updateResources(resources) ;
    		this.assertUpdate(response, resources, testHarness, testHarnessMetadata) ; 
    	}catch(Throwable t){ 
    		t.printStackTrace() ; 
    		throw t ; 
    	}//EO catch block 
    }//EOM 
    
    
  private final void assertResource(final ResourceModel responseResource, final AppdefResource expectedResource, int iHierarchyDepth, final Map<String,String> configMap) {
	  this.assertResource(responseResource, expectedResource, iHierarchyDepth, configMap, ResourceDetailsType.ALL) ; 
  }//EOM 
    
  private void assertResource(final ResourceModel responseResource, final AppdefResource expectedResource, int iHierarchyDepth, final Map<String,String> configMap, 
          final Map<String, PropertyList> configListsMap) {
      this.assertResource(responseResource, expectedResource, iHierarchyDepth, configMap, configListsMap, ResourceDetailsType.ALL) ;
      
  }
  
  private final void assertResource(final ResourceModel responseResource, final AppdefResource expectedResource, int iHierarchyDepth, final Map<String,String> configMap, 
          final Map<String, PropertyList> configListsMap, final ResourceDetailsType responseStructure) {
      
      try{ 
        final int expectedResouceID = expectedResource.getResource().getId() ;
        final String type = expectedResource.getResource().getResourceType().getLocalizedName() ; 
        
        Assert.assertEquals(type+ " id", expectedResouceID+"", responseResource.getId()) ;
        
        //assert erties 
        if(responseStructure == ResourceDetailsType.PROPERTIES) { 
            // In PROPERTIES view only ID might be present, without the type 
            if (null == responseResource.getResourceType())  {
                responseResource.setResourceType(ResourceTypeModel.valueOf(expectedResource.getResource().getResourceType().getAppdefType()));
            }
        } else {
            Assert.assertEquals(type +" name", expectedResource.getName(), responseResource.getName()) ; 
        } //EO else if response includes resource level properties
        
        this.assertConfig(responseResource, configMap, configListsMap) ; 

        final List<ResourceModel> responseChildren = responseResource.getSubResources() ; 
        List<AppdefResource> expectedChildren = null ;
    
        if(--iHierarchyDepth > 0) {
            
            ResourceTypeModel enumChildrenResourceType = null ; 
            switch(responseResource.getResourceType()) { 
                case PLATFORM : { 
                    expectedChildren = this.testBed.servers.get(expectedResource.getId()) ; 
                    enumChildrenResourceType = ResourceTypeModel.SERVER ; 
                }break ;
                case SERVER: { 
                    expectedChildren = this.testBed.services.get(expectedResource.getId()) ;
                    enumChildrenResourceType = ResourceTypeModel.SERVICE ; 
                }break ;
                case SERVICE: { 
                    return  ;
                }//EO case
            }//EO switch block 
            
            final int iNoOfExpectedChildren = expectedChildren.size() ;
            final int iNoOfResponseChildren = (responseChildren == null ? 0 : responseChildren.size()) ; 
            Assert.assertEquals("Number Of children", iNoOfExpectedChildren, iNoOfResponseChildren) ; 
            
            ResourceModel responseChild = null ; 
            int iIndex = 0 ; 
            for(AppdefResource expectedChild : (Collection<AppdefResource>) expectedChildren) {
                responseChild = responseChildren.get(iIndex++) ; 
                
                //if the response structure contains properties only, set the resource type 
                //in the child 
                if(responseStructure == ResourceDetailsType.PROPERTIES) {
                    responseChild.setResourceType(enumChildrenResourceType) ; 
                }//EO if properties only set the resource type 
                
                this.assertResource(responseChild, expectedChild, iHierarchyDepth, configMap, responseStructure) ; 
            }//EO while there are more children
        }//EO if should assert nested
        else { 
            Assert.assertTrue("Hierarchy Depth was " + iHierarchyDepth + " yet sub resources were found", 
                    (responseResource.getSubResources() == null || responseResource.getSubResources().isEmpty()) 
                    ) ; 
        }//EO else there should be no sub resources 
      }catch(Throwable t) { 
          t.printStackTrace() ; 
          throw new RuntimeException(t) ;  
      }//eOcatch b lok 
  } //EOM  
  
  private final void assertResource(final ResourceModel responseResource, final AppdefResource expectedResource, int iHierarchyDepth, final Map<String,String> configMap, 
		  final ResourceDetailsType responseStructure) { 
          assertResource(responseResource, expectedResource, iHierarchyDepth, configMap, null, responseStructure);
    }//EO while there are more resources to compare  
    
    private final void assertUpdate(final ResourceBatchResponse response, final Resources expectedResources, final Object[][] testHarness, final int[] testHarnessMetadata) throws Throwable { 
    	ResourceModel responseResource = null; 
    	
    	final List<ResourceModel> resourceList = expectedResources.getResources() ; 
    	if(resourceList == null) return ; 
    	final int iLength =  resourceList.size() ;
    	
    	ResourceModel resource = null ; 
    	String resourceID = null ; 
    	List<FailedResource> failedResources = null ; 
    	boolean bFoundFailedResouceMatch = false ;
    	
    	for(int i=0; i < iLength; i++) { 
    		resource = resourceList.get(i) ; 
    		
    		//if the resource at the current index was set to fail check the response for failed resource instance 
    		//and if not found fail the test. else verify update operation 
    		if( (testHarnessMetadata[i] & FAILED_RESOURCE_FLAG) == FAILED_RESOURCE_FLAG) { 
    			
    			resourceID = ( (testHarnessMetadata[i] & USE_NATURAL_ID_FLAG) == USE_NATURAL_ID_FLAG ? resource.getNaturalID() : resource.getId()) ; 
    			failedResources = response.getFailedResources() ; 
    			
    			if(failedResources != null) { 
    				
    				for(FailedResource failedResource : failedResources) { 
    					if(resourceID.equals(failedResource.getResourceId())) { 
    						bFoundFailedResouceMatch = true ; 
    						break ; 
    					}//EO if found a match 
    				}//EO while there are more failed resources 
    				
    			}//EO if there were failed resources 
    			
    			if(!bFoundFailedResouceMatch) 
    				Assert.fail("Update operation for resource with ID " + resourceID + " was supposed to have failed producing a failed resource instance but no such entity was found") ; 
    			
    			bFoundFailedResouceMatch = false ; 
    		}else { 
    			responseResource = this.getApprovedResource(resource.getId(), resource.getNaturalID(), resource.getResourceType(), 2 /*hierarchyDepth*/, new ResourceDetailsType[] { ResourceDetailsType.PROPERTIES } ) ;
    			this.assertConfig(responseResource, (Map<String,String>)testHarness[i][4]) ;
    		}//EO else if the update operation was supposed to be successful 
		}//EO while there are more resources 
    }//EOM 
    
    private final void assertConfig(final ResourceModel response, final Map<String,String> expectedConfigMap) {
        assertConfig(response, expectedConfigMap, null);
    }
    
    private final void assertConfig(final ResourceModel response, final Map<String,String> expectedConfigMap, final Map<String,PropertyList> expectedConfigListMap) { 
    	
    	if(response == null) Assert.fail("No Response Resource was found by service"); 

    	Map<String,String> responseConfigMap = null ;  
    	final ResourceConfig resourceConfig = response.getResourceConfig() ; 
    	if(expectedConfigMap == null && resourceConfig == null) return ;
    	
    	if(resourceConfig == null || (responseConfigMap = resourceConfig.getMapProps()) == null) Assert.fail("No Configurations were returned for resource with ID " + response.getId());
    	//else 
    	
    	String key = null, expectedValue = null, responseValue = null ; 
    	
    	for(Map.Entry<String,String> entry : expectedConfigMap.entrySet()) { 
    		key = entry.getKey() ; 
    		expectedValue = entry.getValue() ; 
    		responseValue = responseConfigMap.get(key) ;  
    		Assert.assertEquals("Config response Value differs for key " + key, expectedValue, responseValue) ;
    	}//EO while there are more expected properties 
    	
    	if (expectedConfigListMap == null) return;
    	
    	Map<String,PropertyList> responseConfigListMap = resourceConfig.getMapListProps();
    	if (((null == expectedConfigListMap) || expectedConfigListMap.isEmpty()) ^
    	        ((null == responseConfigListMap) || responseConfigListMap.isEmpty())) {
    	    if ((null == responseConfigListMap) || responseConfigListMap.isEmpty()) Assert.fail("No Configuration Lists were returned for resource with ID " + response.getId());
    	    // else
    	    Assert.fail("No Configuration Lists were expected for resource with ID " + response.getId());
    	}
    	PropertyList expectedValueList = null, responseValueList = null;
    	if (null != expectedConfigListMap) {
    	    for (Map.Entry<String, PropertyList> entry : expectedConfigListMap.entrySet()) {
    	        key = entry.getKey();
    	        expectedValueList = entry.getValue();
    	        responseValueList = responseConfigListMap.get(key);
    	        compareValueLists(responseValueList, expectedValueList, key, response.getId());
    	    }
    	}
    	
    }//EOM 
    
    private static void compareValueLists(final PropertyList responseValueList, final PropertyList expectedValueList, final String key, final String id) {
        List<ConfigurationValue> responseProperties = responseValueList.getProperties();
        List<ConfigurationValue> expectedPoperties = expectedValueList.getProperties();
        String errorMessageSuffix = " for key " + key + " for resource ID " + id;
        Assert.assertFalse("Empty property list received " + errorMessageSuffix, 
                ((null == responseProperties) || responseProperties.isEmpty()));
        int expectedPropertiesListSize = expectedPoperties.size();
        Assert.assertEquals("Mismatch in the length of the property list " + errorMessageSuffix, expectedPropertiesListSize, responseProperties.size());
        for (int i = 0; i < expectedPropertiesListSize; ++i) {
            Assert.assertEquals("Unexpected value in the property list " + errorMessageSuffix, expectedPoperties.get(i), responseProperties.get(i));
        }
    }


    private final ResourceModel getApprovedResource(final AppdefResource appdefResource, final int hierarchyDepth) throws Throwable {
    	return this.getApprovedResource(appdefResource.getResource().getId()+"", hierarchyDepth) ; 
    }//EOM 
    
    private final ResourceModel getApprovedResource(final String ID, final int hierarchyDepth) throws Throwable {
    	return this.getResource(ID, null /*naturalID*/, null /*resourceType*/, ResourceStatusType.APPROVED, hierarchyDepth, new ResourceDetailsType[]{ ResourceDetailsType.ALL } ) ;
    }//EOM 
    
    private final ResourceModel getApprovedResource(final String naturalID, final ResourceTypeModel enumResourceType, final int hierarchyDepth) throws Throwable {
    	return this.getResource(null/*ID*/, naturalID, enumResourceType, ResourceStatusType.APPROVED, hierarchyDepth, new ResourceDetailsType[]{ ResourceDetailsType.ALL } ) ;
    }//EOM 
    
    private final ResourceModel getApprovedResource(final String ID, final String naturalID, final ResourceTypeModel enumResourceType, final int hierarchyDepth, final ResourceDetailsType[] responseStructure) throws Throwable { 
    	return this.getResource(ID, naturalID, enumResourceType, ResourceStatusType.APPROVED, hierarchyDepth, responseStructure) ; 
    }
    
    private final ResourceModel getResource(final String ID, final String naturalID, final ResourceTypeModel enumResourceType, 
    						final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseStructure) throws Throwable {
    	ResourceModel resource = null;
    	 	
    	if(ID != null) { 
                resource = service.getResource(ID, resourceStatusType, hierarchyDepth, responseStructure) ;

    	}else { 
    		resource = service.getResource(naturalID, enumResourceType, resourceStatusType, hierarchyDepth, responseStructure) ;
    	}//EO else if natural ID 

    	return resource ; 
    }//EOM 
    
    
    private final Resources generateResources(final int iNoOfResources, final Object[][] arrResources ) { 
    	final Resources resources = new Resources() ;
    	
    	ResourceModel resource = null ; 
    	for(int i=0; i < iNoOfResources; i++) { 
    		resource = this.generateResource(
    				(String)arrResources[i][0]/*internalID*/, 
    				(String)arrResources[i][1]/*natural ID*/,
    				(ResourceTypeModel)arrResources[i][2], 
    				(arrResources[i][3] != null /*generate resource*/), 
    				(HashMap<String,String>)arrResources[i][4]) ; 
    		resources.addResource(resource)  ; 
    	}//EO while there are more resources 
    	return resources ; 
    }//EOM 
    
    private final ResourceConfig generateResourceConfig(final HashMap<String,String> configMap) { 
    	return new ResourceConfig(null, configMap, null) ; 
    }//EOM 
    
    private final ResourceModel generateResource(final String ID, final String naturalID, final ResourceTypeModel enumResourceType, 
    														final boolean generateConfig, final HashMap<String,String> configMap) { 
		final ResourceModel resource = new ResourceModel(ID) ; 
		resource.setNaturalID(naturalID) ; 
		resource.setResourceType(enumResourceType); 
		resource.setResourceStatusType(ResourceStatusType.APPROVED) ; 
		if(generateConfig) resource.setResourceConfig(this.generateResourceConfig(configMap)) ;
		return resource ;
    }//EOM 
    
	public static class ResourceServiceTestDataPopulator extends AbstractRestTestDataPopulator<ResourceService>{ 
		
		private static final String IP_MAC_ADDRESS = "IP_MAC_ADDRESS";

        static final int NO_OF_TEST_PLATFORMS = 4 ;
		
		private Method addConfigSchemaMethod ; 
	    private Method setTypeInfoMethod ;
	    private Map<String,String> persistedConfigAttributes ; 	    
	    private HashMap<String,String> requestConfigAttributes ;	    
	    // these  are readonly properties, so won't be mapped from reqest, just have to be fetched
	    private Map<String, PropertyList> persistedListConfigAttributes;
        
		private Agent testAgent;
		
		private List<Platform> platforms ;  
	    private Map<Integer, List<AppdefResource>> servers ; 
	    private Map<Integer, List<AppdefResource>> services ;
	    private PlatformType platformType ; 
	    private ServerType serverType ; 
	    private ServiceType serviceType ; 
	    
	    @Autowired
	    private ConfigManager configManager ;
	    @Autowired
	    private ProductManager productManager;
	    @Autowired
	    private AppdefBoss appdefBoss ;
	    @Autowired
	    private SessionManager sessionManager;
	    
	    public ResourceServiceTestDataPopulator() { 
	    	super(ResourceService.class, CONTEXT_URL + "/rest/resource") ;
	    }//EOM 
	    
		@Override
		public final void populate() throws Exception {
			
			try{  
				persistedConfigAttributes  = new HashMap<String,String>() ; 
		    	persistedConfigAttributes.put("test.log_track.level", "Warn") ;
		    	//persistedConfigAttributes.put("config_track.files", "/etc/hq") ;
		    	persistedConfigAttributes.put("some.config.prop", "property.existing.val") ;
		    	
		    	requestConfigAttributes  = new HashMap<String,String>() ;
		    	requestConfigAttributes.put("test.log_track.level", "BOGUS_LEVEL_" + System.currentTimeMillis()) ;
		    	requestConfigAttributes.put("some.config.prop", "BOGUS_PATH_" + + System.currentTimeMillis()) ;
		    	
		    	persistedListConfigAttributes = new HashMap<String, PropertyList>();		    	
		    	ComplexIp[] macAddresses = { new ComplexIp("netmask1", "mac1", "address1"), new ComplexIp("netmask2", "mac2", "address2") };
                PropertyList macAddressAttributes = new PropertyList(macAddresses);
                persistedListConfigAttributes.put(IP_MAC_ADDRESS, macAddressAttributes );
		    	
	    		setTypeInfoMethod = ProductPluginManager.class.getDeclaredMethod("setTypeInfo", String.class,String.class, TypeInfo.class) ;
	 	        setTypeInfoMethod.setAccessible(true) ; 
	 	        
	 	        addConfigSchemaMethod = PluginData. class.getDeclaredMethod("addConfigSchema", String.class, int.class, ConfigSchema.class); 
		        addConfigSchemaMethod.setAccessible(true) ; 
	 	        
		        final AuthzSubject subject = this.getAuthzSubject() ; 
		        
		        String agentToken = "agentToken" + System.currentTimeMillis(); 
		        testAgent = this.createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
		       
		        final String pluginName = "Test_Plugin" ;
		        final String platformName = "Linux1" ; 
		        final String serverTypeName = "Tomcat1" ; 
		        final String serverTypeinfoName = serverTypeName + " " + platformName ; 
		        final String serviceTypeName = "Spring JDBC Template1" ;
		        final String serviceTypeinfoName = serviceTypeName + " " + platformName ; 
		        
		        this.platformType = this.platformManager.createPlatformType(platformName, pluginName) ; 
		        this.serverType = this.createServerType(serverTypeName, "6.0", new String[]{ platformName }, pluginName, false);
		        this.serviceType = createServiceType(serviceTypeName, pluginName, serverType);
		        //create the platform, server and service plugins for configSchema support 
		        this.registerMeasurementConfigSchema(platformName, platformName, new PlatformTypeInfo(platformName)) ;
		        
		        final ServerTypeInfo serverTypeInfo = new ServerTypeInfo(serverTypeinfoName, serverTypeName, "x") ; 
		        this.registerMeasurementConfigSchema(serverTypeName, platformName, serverTypeInfo) ; 
		        this.registerMeasurementConfigSchema(serviceTypeName, platformName, new ServiceTypeInfo(serviceTypeinfoName, serviceTypeName, serverTypeInfo)) ; 
		        
		        final int iNoOfPlatforms = NO_OF_TEST_PLATFORMS, iNoOfServesPerPlatform = 2, iNoOfServicesPerServer = 2 ; 
		        
		        //create the test platforms, servers and services 
		        this.platforms = new ArrayList<Platform>() ; 
		        this.servers = new HashMap<Integer, List<AppdefResource>>() ;
		        this.services = new HashMap<Integer, List<AppdefResource>>() ;
		    	List<AppdefResource> serversPerPlatfom = null ;
		    	List<AppdefResource> servicesPerServer = null ; 
		    	
		    	Platform platform= null ; 
		    	Server server = null ;
		    	Service service = null ; 
		    	String name = null ;
		    	
		    	int iServerCounter = 0, iServiceCounter = 0;  

		    	for(int i=0; i < iNoOfPlatforms; i++) {
		    		name = "test.ubuntu.eng.vmware.com." + i ; 
		    		platform = this.createPlatform(agentToken, platformName, name, name, subject) ;  
		    		this.platforms.add(platform) ; 
		    		
		    		//add configuration 
		    		this.createConfig(platform.getEntityId(), persistedConfigAttributes, subject) ;
		    		List<ConfigurationValue> ipPropertyList = persistedListConfigAttributes.get(IP_MAC_ADDRESS).getProperties();
		    		for (Object ip : ipPropertyList) {
                        ComplexIp complexIp = (ComplexIp)ip;
                        platform.addIp(complexIp.getAddress(), complexIp.getNetmask(), complexIp.getMac()); 
                    }		    		
		    		
		    		serversPerPlatfom = new ArrayList<AppdefResource>(iNoOfServesPerPlatform) ; 
		    		this.servers.put(platform.getId(), serversPerPlatfom) ; 
		    		
		    		for(int j=0; j < iNoOfServesPerPlatform; j++) { 
		    			iServerCounter++ ; 
			    		
		    			server = this.createServer(platform, serverType, serverTypeName+ "_instance_"+iServerCounter, subject) ; 
		    			serversPerPlatfom.add(server) ; 
		    			
		    			//add configuration
		    			//TODO: cannot create config for server 
			    		this.createConfig(server.getEntityId(), persistedConfigAttributes, subject) ;
		    			
		    			servicesPerServer = new ArrayList<AppdefResource>(iNoOfServicesPerServer) ;
		    			this.services.put(server.getId(), servicesPerServer) ;
		    			
		    			for(int k=0; k < iNoOfServicesPerServer; k++) {
		    				iServiceCounter++ ; 
		    				
		    				service = this.createService(server, serviceType, serviceTypeName+"_Instance_"+iServiceCounter, serviceTypeName + "_Instance_"+iServiceCounter, "my computer", subject);
		    				servicesPerServer.add(service) ; 
		    				
		    				//add configuration 
		    	    		this.createConfig(service.getEntityId(), persistedConfigAttributes, subject) ;
		    			}//EO while there are more services to create 
		    		}//EO while more servers 
		    	}//EO while there are more platforms
		    	
		    	super.populate() ; 
		        
	    	}catch(Throwable t) { 
	    		t.printStackTrace() ; 
 	    		throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;  
	    	}//EO catch block 
		}//EOM 
		
		private final AuthzSubject getAuthzSubject() { 
			AuthzSubject subject = this.authzSubjectManager.findSubjectByName("hqadmin") ;
			return (subject != null ? subject : authzSubjectManager.getOverlordPojo()) ; 
		}//EOM 

//		@Override
		public void destroy() throws Exception {}//EOM 
		
		private final void registerMeasurementConfigSchema(final String pluginName, final String platformName,
				final TypeInfo typeinfo) throws Throwable{ 
		 
			final PluginManager measurementPluginManager = this.productManager.getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
			
			//TODO: plugins are not discarded with the transaction, must either explicitly remove in the @after or check for existence here
			final String pluginTypeInfoName = typeinfo.getName() ; 
			if(measurementPluginManager.isRegistered(pluginTypeInfoName)) return ;  
			
			final ProductPluginManager productPluginManager = (ProductPluginManager) this.productManager.getPluginManager(ProductPlugin.TYPE_PRODUCT) ; 

			final int iNoOfConfigKeys = persistedConfigAttributes.size() ; 
		    //create config schema + additional bogus 
		    final ConfigOption[] configOptions = new ConfigOption[iNoOfConfigKeys+1] ;
		   
		    String configKey = null ; 
		    int iIndex = 0 ;
		    for(Map.Entry<String, String> entry : persistedConfigAttributes.entrySet()){
		    	configKey = entry.getKey() ;  
		    	configOptions[iIndex++] = new StringConfigOption(configKey, configKey, entry.getValue() + ".def") ;  
		    }//EO while there are more config options to define 
		    
		    //add the bogus additional key 
		    configKey = "some.other.property" ; 
		    configOptions[iNoOfConfigKeys] = new StringConfigOption(configKey, configKey, configKey+".def") ;  
		    
		    //create a measurement plugin, add a config schema to it and register it with the measurement plugin manager 
		    final MeasurementPlugin plugin = new MeasurementPlugin() ;
		    //must be the qualified name e.g. 'Tomcat Linux'
		    plugin.setName(pluginTypeInfoName) ; 
		    
		    PluginData pluginData = new PluginData() ; 
		    addConfigSchemaMethod.invoke(pluginData, pluginTypeInfoName, 1 /*ProductPlugin.TYPE_MEASUREMENT*/, new ConfigSchema(configOptions)) ;
		    plugin.setData(pluginData) ; 
		    measurementPluginManager.registerPlugin(plugin) ; 
		    
		    
		    //add the type info to the product manager (must be the actual name e.g. Tomcat)
		    setTypeInfoMethod.invoke(productPluginManager, platformName, pluginName, typeinfo) ;
		}//EOM 
		
		private final void createConfig(final AppdefEntityID entityID, final Map<String,String> configMap, final AuthzSubject subject) throws Throwable{
		
			final ConfigResponse configResponse = new ConfigResponse(configMap) ; 
			this.configManager.setConfigResponse(subject, entityID, 
							configResponse, ProductPlugin.CONFIGURABLE_TYPES[1], false, false);
		}//EOM
		
	}//EOC ResourceServiceTestDataPopulator
	
	@Component("mockFactory")
	public static class MockFactory { 
	    
	    private SecureAgentConnection secureAgentConnectionSingleton ;
	    
	    public final SecureAgentConnection newSecureAgentConnection(String agentAddress, int agentPort, final String authToken, 
	            final KeystoreConfig keystoreConfig, final boolean acceptUnverifiedCertificate) {
	        
	        if(this.secureAgentConnectionSingleton == null) { 
	            try{ 
	                final MeasurementCommandsAPI verAPI = new MeasurementCommandsAPI() ; 
	                final TrackPluginRemove_args args = new TrackPluginRemove_args() ;
	                //final Method sendCommandMethod = AgentConnection.class.getDeclaredMethod("sendCommand", String.class, int.class, AgentRemoteValue.class) ;
	                //AgentConnection.class.
	                this.secureAgentConnectionSingleton = EasyMock.createMock(SecureAgentConnection.class)  ;
	                EasyMock.expect(secureAgentConnectionSingleton.getAgentAddress()).andReturn("").anyTimes() ; 
	                EasyMock.expect(secureAgentConnectionSingleton.getAgentPort()).andReturn(0).anyTimes() ; 
	                EasyMock.expect(secureAgentConnectionSingleton.sendCommand(EasyMock.anyObject(String.class), 
	                        EasyMock.anyInt(), EasyMock.anyObject(AgentRemoteValue.class))).andReturn(null).anyTimes() ;
	                EasyMock.replay(secureAgentConnectionSingleton) ;
	                
	            }catch(Throwable t) { 
	                throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
	            }//EO catch 
	        }//EO if not yet initialized  
	         
	      return secureAgentConnectionSingleton ;
	    }//EOM 
	    
	}//EO inner class MockFactory
	
}//EOC 

package org.hyperic.hq.api.resources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.FailedResource;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.resources.ResourceServiceTest.ResourceServiceTestDataPopulator;
import org.hyperic.hq.api.rest.cxf.TestHttpConduit;
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
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
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
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.hq.test.TestHelper;
import org.hyperic.hq.tests.context.TestData;
import org.hyperic.hq.tests.context.TestDataPopulator;
import org.hyperic.hq.tests.context.WebContainerContextLoader;
import org.hyperic.hq.tests.context.WebContextConfiguration;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.meterware.servletunit.ServletRunner;

@DirtiesContext
@ContextConfiguration(locations = { "classpath:META-INF/hqapi-context.xml" }, loader=WebContainerContextLoader.class)
@WebContextConfiguration(contextRoot=ResourceServiceTest.CONTEXT, contextUrl=ResourceServiceTest.CONTEXT_URL, webXml=ResourceServiceTest.WEB_XML)
@TestData(ResourceServiceTestDataPopulator.class)
public class ResourceServiceTest extends BaseInfrastructureTest{

	protected static final String WEB_XML = "/WEB-INF/web-spring.xml" ; 
	protected static final String CONTEXT = "/tests" ; 
	protected static final String CONTEXT_URL = "http://localhost" + CONTEXT ;
	
    
    private static final int GENERATE_CONFIG_FLAG = 2<<1 ; 
    private static final int FAILED_RESOURCE_FLAG = 2<<2 ;
    private static final int USE_NATURAL_ID_FLAG = 2<<3 ; 
    
    @Rule 
    public TestRule chain = RuleChain.outerRule(new ServiceBindingsIterationInterceptor(ServiceBindingsIteration.class)).
    		around(new PlatformsIterationInterceptor(PlatformsIteration.class)) ;
    		
	@Autowired
	private ResourceServiceTestDataPopulator testBed ; 

	private Platform currentPlatform ; 
	private ResourceService resourceService ;

	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD})
	private @interface PlatformsIteration { 
		int noOfPlatforms() default ResourceServiceTestDataPopulator.NO_OF_TEST_PLATFORMS ; 
	}//EO inner class PlaformsIterator 
	
	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD})
	private @interface ServiceBindingsIteration { 
		String value() ; 
	}//EO inner class ServiceBindingProviderIterator

    private abstract class IterationInterceptor<T extends Annotation> implements TestRule { 
    	
    	private Class<T> annotationType ; 
    	
    	public IterationInterceptor(final Class<T> annotationType) { 
    		this.annotationType = annotationType ; 
    	}//EOM 
    	
    	@Override
    	public Statement apply(final Statement base, final Description description) {
    		return new Statement() { 
    			
    			@Override
    			public final void evaluate() throws Throwable {
    				final T metadata = description.getAnnotation(annotationType) ; 
    				if(metadata == null) base.evaluate() ; 
    				else {
    					try{ 
	    					final int iIterationLength = getIterationLength(metadata) ; 
	    					
	    					for(int i=0; i<iIterationLength; i++) {
	    						
	    						doBeforeEvaluation(i, metadata) ; 
	    						
	    						base.evaluate() ; 
	    						
	    					}//EO while there are more platforms  
    					}finally{ 
    						
    					}//EO catch block 
    					
    				}//EO if the getresource annotation exists 
    			}//EOM 
    		};  
    	}//EOM 
    	
    	protected abstract int getIterationLength(final T metadata) ; 
    	
    	protected abstract void doBeforeEvaluation(final int iIterationIndex, final T metadata) ;
    		
    }//EO inner calss IterationInterceptor 
    
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
    
    private final class ServiceBindingsIterationInterceptor extends IterationInterceptor<ServiceBindingsIteration> { 
    	
    	public ServiceBindingsIterationInterceptor(final Class<ServiceBindingsIteration> serviceBindingsIterationType) { 
    		super(serviceBindingsIterationType) ; 
    	}//EOM 
    	
    	 @Override
    	protected final void doBeforeEvaluation(final int iIterationIndex, ServiceBindingsIteration metadata) {
    		resourceService = testBed.arrResourcesServices[iIterationIndex] ;
    		final Client client =  WebClient.client(resourceService) ;
    		WebClient.getConfig(client).getConduitSelector().getEndpoint().getEndpointInfo().setAddress(metadata.value()) ; 
    	}//EOM 
    	
    	 @Override
    	protected final int getIterationLength(final ServiceBindingsIteration metadata) {
    		return testBed.arrResourcesServices.length ; 
    	}//EOM 
    }//EO inner class PlatformsIterationInterceptor

    @ServiceBindingsIteration(ResourceServiceTest.CONTEXT_URL + "/rest-api/inventory/resources")
    @PlatformsIteration
   // @Test
    @Transactional(propagation=Propagation.NESTED)
    public final void testGetResourceWithInternalAndNaturalPlatformIDs() throws Throwable {
    	
    	final int hierarchyDepth = 3 ; 
    	
		//internal id first 
    	Resource resource = this.getApprovedResource(this.currentPlatform, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes) ; 
		
		//now natural ID 
		resource = this.getApprovedResource(this.currentPlatform.getFqdn(), ResourceType.PLATFORM, hierarchyDepth) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes) ; 
		
	}//EOM 
    
    @ServiceBindingsIteration(ResourceServiceTest.CONTEXT_URL + "/rest-api/inventory/resources")
    @PlatformsIteration
    //@Test
    public final void testGetResourceNoConfig() throws Throwable { 
    	final int hierarchyDepth = 3 ; 
    	
    	final ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC } ; 
    	
		//internal id first 
    	Resource resource = this.getApprovedResource(this.currentPlatform.getId()+"", null/*naturalID*/, null/*resourc type*/, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes) ; 
		
		//now natural ID 
		resource = this.getApprovedResource(null/*internal id*/, this.currentPlatform.getFqdn()/*naturalID*/,ResourceType.PLATFORM, hierarchyDepth, responseStructure) ;
		this.assertResource(resource, this.currentPlatform, hierarchyDepth, this.testBed.persistedConfigAttributes) ;
    }//EOM 
    
    @Test
    public final void testGetResourceOnlyConfig() throws Throwable { 
    	//Note: should still contain an internal id 
    }//EOM
    
    public final void testGetResourceNoDepth() throws Throwable { 
    	
    }//EOM 
    
    public final void testGetResourceDepth1() throws Throwable { 
    	
    }//EOM 
    
    public final void testGetResourceDepth2() throws Throwable { 
    	
    }//EOM
    
    public final void testGetResourceDepth5() throws Throwable { 
    	
    }//EOM
    
    public final void testGetResourceNegativeDepth() throws Throwable { 
    	
    }//EOM
    
    public final void testGetResourceByInvalidNaturalID() throws Throwable { 
    	
    }//EOM
    
    public final void testGetResourceByInvalidInternalID() throws Throwable { 
    	
    }//EOM 
    
    @Ignore
    //@Test
    public final void testUpdateResources1Resource() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ (USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG) } ) ;
    }//EOM
    @Ignore
    //@Test
    public final void testUpdateResources2ResourcesSuccess() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG), 
    			(GENERATE_CONFIG_FLAG)
    		} 
    	) ;
    }//EOM
    @Ignore
    //@Test
    public final void testUpdateResources2ResourcesOneFailure() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG), 
    			(GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG)
    		} 
    	) ;
    }//EOM
    @Ignore
    //@Test
    public final void testUpdateResources2Resources2Failures() throws Throwable{
    	this.innerTestUpdateResources(new int[]{ 
    			(USE_NATURAL_ID_FLAG | GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG), 
    			(GENERATE_CONFIG_FLAG | FAILED_RESOURCE_FLAG)
    		} 
    	) ;
    }//EOM
    
    //@Test
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
    			testHarness[i][2] = ResourceType.PLATFORM ; 
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
    		response = resourceService.updateResources(resources) ;
    		this.assertUpdate(response, resources, testHarness, testHarnessMetadata) ; 
    	}catch(Throwable t){ 
    		t.printStackTrace() ; 
    		throw t ; 
    	}//EO catch block 
    }//EOM 
    
  private final void assertResource(final Resource responseResource, final AppdefResource expectedResource, int iHierarchyDepth, final Map<String,String> configMap) { 
    	
    	final int expectedResouceID = expectedResource.getResource().getId() ;
    	final String type = expectedResource.getResource().getResourceType().getLocalizedName() ; 
    	
    	Assert.assertEquals(type+ " id", expectedResouceID+"", responseResource.getId()) ;
    	Assert.assertEquals(type +" name", expectedResource.getName(), responseResource.getName()) ; 
    	
    	//if(responseResource.getResourceType() != ResourceType.SERVER) 
   		this.assertConfig(responseResource, configMap) ; 

    	final List<Resource> responseChildren = responseResource.getSubResources() ; 
    	List<AppdefResource> expectedChildren = null ;
    
    	if(--iHierarchyDepth > 0) {
    		
    		switch(responseResource.getResourceType()) { 
    			case PLATFORM : { 
	    			expectedChildren = this.testBed.servers.get(expectedResource.getId()) ; 
	    		}break ;
				case SERVER: { 
					expectedChildren = this.testBed.services.get(expectedResource.getId()) ;			
				}break ;
    		}//EO switch block 
    		
    		final int iNoOfExpectedChildren = expectedChildren.size() ;
        	final int iNoOfResponseChildren = (responseChildren == null ? 0 : responseChildren.size()) ; 
        	Assert.assertEquals("Number Of children", iNoOfExpectedChildren, iNoOfResponseChildren) ; 
    		
	    	Resource responseChild = null ; 
	    	int iIndex = 0 ; 
	    	for(AppdefResource expectedChild : (Collection<AppdefResource>) expectedChildren) {
	    		responseChild = responseChildren.get(iIndex++) ; 
	    		
	    		this.assertResource(responseChild, expectedChild, iHierarchyDepth, configMap) ; 
	    	}//EO while there are more children
    	}//EO if should assert nested
    	
    }//EO while there are more resources to compare  
    
    private final void assertUpdate(final ResourceBatchResponse response, final Resources expectedResources, final Object[][] testHarness, final int[] testHarnessMetadata) { 
    	Resource responseResource = null; 
    	
    	final List<Resource> resourceList = expectedResources.getResources() ; 
    	if(resourceList == null) return ; 
    	final int iLength =  resourceList.size() ;
    	
    	Resource resource = null ; 
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
    
    private final void assertConfig(final Resource response, final Map<String,String> expectedConfigMap) { 
    	
    	if(response == null) Assert.fail("No Response Resource was found by service"); 

    	Map<String,String> responseConfigMap = null ;  
    	final ResourceConfig resourceConfig = response.getResourceConfig() ; 
    	
    	if(resourceConfig == null || (responseConfigMap = resourceConfig.getMapProps()) == null) Assert.fail("No Configurations were returned for resource with ID " + response.getId());
    	//else 
    	
    	String key = null, expectedValue = null, responseValue = null ; 
    	
    	for(Map.Entry<String,String> entry : expectedConfigMap.entrySet()) { 
    		key = entry.getKey() ; 
    		expectedValue = entry.getValue() ; 
    		responseValue = responseConfigMap.get(key) ;  
    		Assert.assertEquals("Config response Value differs for key " + key, expectedValue, responseValue) ;
    	}//EO while there are more expected properties 
    	
    }//EOM 
    
    private final Resource getApprovedResource(final AppdefResource appdefResource, final int hierarchyDepth) {
    	return this.getApprovedResource(appdefResource.getResource().getId()+"", hierarchyDepth) ; 
    }//EOM 
    
    private final Resource getApprovedResource(final String ID, final int hierarchyDepth) {
    	return this.getResource(ID, null /*naturalID*/, null /*resourceType*/, ResourceStatusType.APPROVED, hierarchyDepth, new ResourceDetailsType[]{ ResourceDetailsType.ALL } ) ;
    }//EOM 
    
    private final Resource getApprovedResource(final String naturalID, final ResourceType enumResourceType, final int hierarchyDepth) {
    	return this.getResource(null/*ID*/, naturalID, enumResourceType, ResourceStatusType.APPROVED, hierarchyDepth, new ResourceDetailsType[]{ ResourceDetailsType.ALL } ) ;
    }//EOM 
    
    private final Resource getApprovedResource(final String ID, final String naturalID, final ResourceType enumResourceType, final int hierarchyDepth, final ResourceDetailsType[] responseStructure) { 
    	return this.getResource(ID, naturalID, enumResourceType, ResourceStatusType.APPROVED, hierarchyDepth, responseStructure) ; 
    }
    
    private final Resource getResource(final String ID, final String naturalID, final ResourceType enumResourceType, 
    						final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseStructure) {
    	Resource resource = null; 
    	
    	if(ID != null) { 
    		resource = resourceService.getResource(ID, resourceStatusType, hierarchyDepth, responseStructure) ;
    	}else { 
    		resource = resourceService.getResource(naturalID, enumResourceType, resourceStatusType, hierarchyDepth, responseStructure) ;
    	}//EO else if natural ID 
    	return resource ; 
    }//EOM 
    
    private final Resources generateResources(final int iNoOfResources, final Object[][] arrResources ) { 
    	final Resources resources = new Resources() ;
    	
    	Resource resource = null ; 
    	for(int i=0; i < iNoOfResources; i++) { 
    		resource = this.generateResource(
    				(String)arrResources[i][0]/*internalID*/, 
    				(String)arrResources[i][1]/*natural ID*/,
    				(ResourceType)arrResources[i][2], 
    				(arrResources[i][3] != null /*generate resource*/), 
    				(Map<String,String>)arrResources[i][4]) ; 
    		resources.addResource(resource)  ; 
    	}//EO while there are more resources 
    	return resources ; 
    }//EOM 
    
    private final ResourceConfig generateResourceConfig(final Map<String,String> configMap) { 
    	return new ResourceConfig(null, configMap) ; 
    }//EOM 
    
    private final Resource generateResource(final String ID, final String naturalID, final ResourceType enumResourceType, 
    														final boolean generateConfig, final Map<String,String> configMap) { 
		final Resource resource = new Resource(ID) ; 
		resource.setNaturalID(naturalID) ; 
		resource.setResourceType(enumResourceType); 
		resource.setResourceStatusType(ResourceStatusType.APPROVED) ; 
		if(generateConfig) resource.setResourceConfig(this.generateResourceConfig(configMap)) ;
		return resource ;
    }//EOM 
    
    
	public static final class ResourceServiceTestDataPopulator extends TestHelper implements TestDataPopulator{ 
		
		static final int NO_OF_TEST_PLATFORMS = 4 ;
		
		private Method addConfigSchemaMethod ; 
	    private Method setTypeInfoMethod ;
	    private Map<String,String> persistedConfigAttributes ; 
	    private Map<String,String> requestConfigAttributes ;
		
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
		@Autowired
		private ServletRunner servletRunner ; 
		
	    private static ResourceService resourceServiceXML ;
	    private static ResourceService resourceServiceJSON ;
	    
	    private ResourceService[] arrResourcesServices ; 
	    
	    
	    public ResourceServiceTestDataPopulator(){super();}//EOM
	     
		@Override
		public final void populate() throws Exception {
			try{  
				persistedConfigAttributes  = new HashMap<String,String>() ; 
		    	persistedConfigAttributes.put("log_track.level", "Warn") ;
		    	persistedConfigAttributes.put("config_track.files", "/etc/hq") ;
		    	
		    	requestConfigAttributes  = new HashMap<String,String>() ;
		    	requestConfigAttributes.put("log_track.level", "BOGUS_LEVEL_" + System.currentTimeMillis()) ;
		    	requestConfigAttributes.put("config_track.files", "BOGUS_PATH_" + + System.currentTimeMillis()) ;
		    	
	    		setTypeInfoMethod = ProductPluginManager.class.getDeclaredMethod("setTypeInfo", String.class,String.class, TypeInfo.class) ;
	 	        setTypeInfoMethod.setAccessible(true) ; 
	 	        
	 	        addConfigSchemaMethod = PluginData. class.getDeclaredMethod("addConfigSchema", String.class, int.class, ConfigSchema.class); 
		        addConfigSchemaMethod.setAccessible(true) ; 
	 	        
		        String agentToken = "agentToken" + System.currentTimeMillis(); 
		        testAgent = this.createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
		       
		        final String pluginName = "Test_Plugin" ;
		        final String platformName = "Linux" ; 
		        final String serverTypeName = "Tomcat" ; 
		        final String serverTypeinfoName = serverTypeName + " " + platformName ; 
		        final String serviceTypeName = "Spring JDBC Template" ;
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
		    		platform = this.createPlatform(agentToken, platformName, name, name) ;  
		    		this.platforms.add(platform) ; 
		    		
		    		//add configuration 
		    		this.createConfig(platform.getEntityId(), persistedConfigAttributes) ; 
		    		
		    		serversPerPlatfom = new ArrayList<AppdefResource>(iNoOfServesPerPlatform) ; 
		    		this.servers.put(platform.getId(), serversPerPlatfom) ; 
		    		
		    		for(int j=0; j < iNoOfServesPerPlatform; j++) { 
		    			iServerCounter++ ; 
			    		
		    			server = this.createServer(platform, serverType, serverTypeName+ "_instance_"+iServerCounter) ; 
		    			serversPerPlatfom.add(server) ; 
		    			
		    			//add configuration
		    			//TODO: cannot create config for server 
			    		this.createConfig(server.getEntityId(), persistedConfigAttributes) ;
		    			
		    			servicesPerServer = new ArrayList<AppdefResource>(iNoOfServicesPerServer) ;
		    			this.services.put(server.getId(), servicesPerServer) ;
		    			
		    			for(int k=0; k < iNoOfServicesPerServer; k++) {
		    				iServiceCounter++ ; 
		    				
		    				service = this.createService(server, serviceType, serviceTypeName+"_Instance_"+iServiceCounter, serviceTypeName + "_Instance_"+iServiceCounter, "my computer");
		    				servicesPerServer.add(service) ; 
		    				
		    				//add configuration 
		    	    		this.createConfig(service.getEntityId(), persistedConfigAttributes) ;
		    			}//EO while there are more services to create 
		    		}//EO while more servers 
		    	}//EO while there are more platforms
		    	
		    	this.generateResourceServices(); 
		        
	    	}catch(Throwable t) { 
	    		t.printStackTrace() ; 
 	    		throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;  
	    	}//EO catch block 
		}//EOM 

		@Override
		public void destroy() throws Exception {
			/*final AuthzSubject subject = this.authzSubjectManager.getOverlordPojo() ; 
			
			try{ 
				final Integer sessionID = this.sessionManager.put(subject) ;
				if(this.platforms != null) { 
					for(Platform platform : this.platforms) {
						try{
							this.appdefBoss.removeAppdefEntity(sessionID, AppdefUtil.newAppdefEntityId(platform.getResource())) ;
						}catch(Throwable t) {
							t.printStackTrace() ; 
						}//EO catch block 
					}//EO while there are more platforms
				}//EO if there were platforms 
				  
			}catch(Throwable t) { 
				t.printStackTrace() ; 
			}//EO catch block 
			
			Thread.currentThread().sleep(3000) ; 
			
			try{ 
				if(this.serviceType != null) this.serviceManager.deleteServiceType(this.serviceType, subject, this.resourceGroupManager, this.resourceManager) ;
			}catch(Throwable t) {
				t.printStackTrace() ; 
			}//EO catch block
			
			try{ 
				if(this.serverType != null) this.serverManager.deleteServerType(this.serverType, subject, this.resourceGroupManager, this.resourceManager) ;
			}catch(Throwable t) {
				t.printStackTrace() ; 
			}//EO catch block
			
			try{
				Field platformsCollectionField = this.platformType.getClass().getDeclaredField("_platforms") ;
				platformsCollectionField.setAccessible(true) ; 
				final Collection platformsCollection = (Collection) platformsCollectionField.get(this.platformType) ;
				if(platformsCollection != null) platformsCollection.clear() ; 
				if(this.platformType != null) this.platformManager.deletePlatformType(this.platformType) ;
			}catch(Throwable t) {
				t.printStackTrace() ; 
			}//EO catch block
*/			
		}//EOM
		
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
		
		private final void createConfig(final AppdefEntityID entityID, final Map<String,String> configMap) throws Throwable{
		
			final ConfigResponse configResponse = new ConfigResponse(configMap) ; 
			this.configManager.setConfigResponse(this.authzSubjectManager.getOverlordPojo(), entityID, 
							configResponse, ProductPlugin.CONFIGURABLE_TYPES[1], false);
		}//EOM
		
		@BeforeClass 
	    public final void generateResourceServices() { 
			final String sURL = CONTEXT_URL + "/rest-api/inventory/resources" ; 
	    	resourceServiceXML = generateServiceClient(ResourceService.class, ServiceBindingType.XML, sURL) ;
	    	resourceServiceJSON = generateServiceClient(ResourceService.class, ServiceBindingType.JSON, sURL) ;
	    	arrResourcesServices = new ResourceService[] { resourceServiceJSON, resourceServiceXML } ;
	    }//EOM 

		private final <T> T generateServiceClient(final Class<T> interfaceType, final ServiceBindingType enumServiceBindingType, final String url) { 
			
			final T serviceProxy = JAXRSClientFactory.create(url, interfaceType, enumServiceBindingType.getProviders());
	        final Client client = WebClient.client(serviceProxy) ; 
	        client.accept(enumServiceBindingType.getContentType()) ; 
	        client.type(enumServiceBindingType.getContentType()) ; 
	        
	        final ClientConfiguration configuration = WebClient.getConfig(client) ; 
	        configuration.getConduitSelector().getEndpoint().getEndpointInfo().setTransportId("test") ; 
	        
	        final ConduitInitiator conduitInitiator = new HTTPTransportFactory(configuration.getBus()) {
	        	
	        	public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
	        		final HTTPConduit delegate = (HTTPConduit) super.getConduit(endpointInfo, target) ;
	        		return new TestHttpConduit(this.bus, delegate, endpointInfo, target, servletRunner) ; 
	        	}//EOM 
	        };//EO conduitInitiator
	        
	        ((ConduitInitiatorManager)configuration.getBus().getExtension(ConduitInitiatorManager.class)).registerConduitInitiator("test", conduitInitiator) ;
	        return serviceProxy ; 
		}//EOM 
	    
	    public enum ServiceBindingType { 
	    	
	    	JSON(Arrays.asList(new Object[]{ new JacksonJsonProvider(), new JacksonJaxbJsonProvider()}), MediaType.APPLICATION_JSON_TYPE){ 
	    	},//EO JSON 
	    	XML(Arrays.asList(new Object[]{ new JAXBElementProvider()}), MediaType.APPLICATION_XML_TYPE){ 
	    	};//EO XML 
	    	
	    	private List providers ;
	    	private MediaType contentType ; 
	    	
	    	ServiceBindingType(final List providers, final MediaType contentType) { 
	    		this.providers = providers ; 
	    		this.contentType = contentType ; 
	    	};//EOM 
	    	
	    	public final List getProviders() { return this.providers ; }//EOM 
	    	public final MediaType getContentType() { return this.contentType ; }//EOM 
	    	
	    }//EO enum servicebindingType 
		
	}//EOC ResourceServiceTestDataPopulator 
    
}//EOC 

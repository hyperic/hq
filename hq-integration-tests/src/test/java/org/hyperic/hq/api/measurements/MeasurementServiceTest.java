package org.hyperic.hq.api.measurements;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.api.measurements.MeasurementServiceTest.MeasurementServiceTestDataPopulator;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.MeasurementsRequest;
import org.hyperic.hq.api.model.measurements.MeasurementsResponse;
import org.hyperic.hq.api.resources.ResourceServiceTest.PlatformsIteration;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator;
import org.hyperic.hq.api.rest.RestTestCaseBase;
import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingsIteration;
import org.hyperic.hq.api.services.MeasurementService;
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
import org.hyperic.hq.tests.context.TestData;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import edu.emory.mathcs.backport.java.util.Arrays;

@DirtiesContext
@ServiceBindingsIteration(MeasurementServiceTest.CONTEXT_URL + "/rest-api/data/measuremenet")
@TestData(MeasurementServiceTestDataPopulator.class)
public class MeasurementServiceTest extends RestTestCaseBase<MeasurementService, MeasurementServiceTestDataPopulator> {
    @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain ;
    
    public static class MeasurementServiceTestDataPopulator extends AbstractRestTestDataPopulator<MeasurementService>{
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
	    
		public MeasurementServiceTestDataPopulator(Class<MeasurementService> serviceInterface, String serviceURL) {
	    	super(MeasurementService.class, CONTEXT_URL + "/rest-api/data/measuremenet") ;
		}

		@Override
		public final void populate() throws Exception {
			try {
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
	 	        
		        final AuthzSubject subject = this.getAuthzSubject() ; 
		        
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
		    		platform = this.createPlatform(agentToken, platformName, name, name, subject) ;  
		    		this.platforms.add(platform) ; 
		    		
		    		//add configuration 
		    		this.createConfig(platform.getEntityId(), persistedConfigAttributes, subject) ; 
		    		
		    		serversPerPlatfom = new ArrayList<AppdefResource>(iNoOfServesPerPlatform) ; 
		    		this.servers.put(platform.getId(), serversPerPlatfom) ; 
		    		
		    		for(int j=0; j < iNoOfServesPerPlatform; j++) { 
		    			iServerCounter++ ; 
			    		
		    			server = this.createServer(platform, serverType, serverTypeName+ "_instance_"+iServerCounter, subject) ; 
		    			serversPerPlatfom.add(server) ; 
		    			
		    			//add configuration
			    		this.createConfig(server.getEntityId(), persistedConfigAttributes, subject) ;
		    			
		    			servicesPerServer = new ArrayList<AppdefResource>(iNoOfServicesPerServer) ;
		    			this.services.put(server.getId(), servicesPerServer) ;
		    			
		    			for(int k=0; k < iNoOfServicesPerServer; k++) {
		    				iServiceCounter++ ; 
		    				
		    				service = this.createService(server, serviceType, serviceTypeName+"_Instance_"+iServiceCounter, serviceTypeName + "_Instance_"+iServiceCounter, "my computer", subject);
		    				servicesPerServer.add(service) ; 
		    				
		    				//add configuration 
		    	    		this.createConfig(service.getEntityId(), persistedConfigAttributes, subject) ;
		    			} 
		    		} 
		    	}
		    					
		    	super.populate() ; 
	    	}catch(Throwable t) { 
	    		t.printStackTrace() ; 
		    		throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;  
	    	} 
		} 
		
		private final AuthzSubject getAuthzSubject() { 
			AuthzSubject subject = this.authzSubjectManager.findSubjectByName("hqadmin") ;
			return (subject != null ? subject : authzSubjectManager.getOverlordPojo()) ; 
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
		} 
		
		private final void createConfig(final AppdefEntityID entityID, final Map<String,String> configMap, final AuthzSubject subject) throws Throwable{
		
			final ConfigResponse configResponse = new ConfigResponse(configMap) ; 
			this.configManager.setConfigResponse(subject, entityID, 
							configResponse, ProductPlugin.CONFIGURABLE_TYPES[1], false);
		}

		@Override
		public void destroy() throws Exception {} 
	}
    
    protected void baseTest(String rscId, String[] tmpNames,
    		int sMin, int sh, int sd, int sm, int sy, 
    		int eMin, int eh, int ed, int em, int ey) throws Throwable {
    	Calendar begin = new GregorianCalendar(),
    			 end = new GregorianCalendar();
    	begin.set(Calendar.MINUTE, sMin);
    	begin.set(Calendar.HOUR_OF_DAY, sh);
    	begin.set(Calendar.DAY_OF_MONTH, sd);
    	begin.set(Calendar.MONTH, sm);
    	begin.set(Calendar.YEAR, sy);
    	end.set(Calendar.MINUTE, eMin);
    	end.set(Calendar.HOUR_OF_DAY, eh);
    	end.set(Calendar.DAY_OF_MONTH, ed);
    	end.set(Calendar.MONTH, em);
    	end.set(Calendar.YEAR, ey);

    	baseTest(rscId, tmpNames, begin, end);
    }
    
    protected void baseTest(String rscId, String[] tmpNames,
    		Calendar begin,	Calendar end) throws Throwable {
    	List<String> tmpNamesList = Arrays.asList(tmpNames);
    	MeasurementRequest req = new MeasurementRequest(rscId,tmpNamesList);

    	MeasurementResponse res = service.getMetrics(req, begin, end);
    }

    /**
     * ......|--|...
     * 		...........
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinStrtBeforePrgSmallerThan400() throws Throwable {
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	begin.add(Calendar.HOUR_OF_DAY,-1);

    	baseTest(RES_TOMCAT_01_ID, new String[] {TMP_CPU},begin,end);
    }
    
    
    /**
     * ............p
     * 		a.|--|......
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinStrtBeforePrgBiggerThan400() throws Throwable { 
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	begin.add(Calendar.DAY_OF_MONTH,-2);

    	baseTest(RES_TOMCAT_01_ID, new String[] {TMP_CPU},begin,end);
    }     
    
    /**
     * ........p
     * 		a.....|--|..
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinEndsAfterPurge() throws Throwable { 
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	end.add(Calendar.DAY_OF_MONTH,-2);
    	end.add(Calendar.HOUR,-1);
    	begin.add(Calendar.DAY_OF_MONTH,-2);
    	begin.add(Calendar.HOUR,-2);

    	baseTest(RES_TOMCAT_01_ID, new String[] {TMP_CPU},begin,end);
    }
    
    /**
     * ........|--p  |
     * 		a..........
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinEndsBeforePurgeStartsAfterPurge() throws Throwable { 
    }
    
    /**
     * ............p
     * 		|  a--|.......
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinEndsBeforeAgg() throws Throwable { 
    }

    /**
     * ...........p
     * 		a..||.......
     * 
     * @throws Throwable
     */
    @Test
    public final void testGetMetricsWinSmallerThanInterval() throws Throwable { 
    }
}
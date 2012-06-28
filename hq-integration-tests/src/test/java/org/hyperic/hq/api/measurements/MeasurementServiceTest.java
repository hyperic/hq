package org.hyperic.hq.api.measurements;
/*
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.api.measurements.MeasurementServiceTest.MeasurementServiceTestDataPopulator;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.Metric;
//import org.hyperic.hq.api.resources.ResourceServiceTest.PlatformsIteration;
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
import org.hyperic.hq.measurement.MeasurementConstants;
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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import edu.emory.mathcs.backport.java.util.Arrays;
*/
//@DirtiesContext
//@ServiceBindingsIteration(MeasurementServiceTest.CONTEXT_URL + "/rest-api/data/measuremenet")
//@TestData(MeasurementServiceTestDataPopulator.class)
public class MeasurementServiceTest {//extends RestTestCaseBase<MeasurementService, MeasurementServiceTestDataPopulator> {
 /*   @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain ;
    
    public static class MeasurementServiceTestDataPopulator extends AbstractRestTestDataPopulator<MeasurementService>{
		static final int NO_OF_TEST_PLATFORMS = 1 ;
		
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
//	    private ServiceType serviceType ; 
	    
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
		        
		        this.platformType = this.platformManager.createPlatformType(platformName, pluginName) ; 
		        this.serverType = this.createServerType(serverTypeName, "6.0", new String[]{ platformName }, pluginName, false);
		        //create the platform, server and service plugins for configSchema support 
		        final int iNoOfPlatforms = NO_OF_TEST_PLATFORMS, iNoOfServesPerPlatform = 1;//, iNoOfServicesPerServer = 2 ; 
		        
		        //create the test platforms, servers and services 
		        this.platforms = new ArrayList<Platform>() ; 
		        this.servers = new HashMap<Integer, List<AppdefResource>>() ;
		    	List<AppdefResource> serversPerPlatfom = null ;
		    	
		    	Platform platform= null ; 
		    	Server server = null ;
		    	String name = null ;
		    	
		    		name = "test.ubuntu.eng.vmware.com.";// + i ; 
		    		platform = this.createPlatform(agentToken, platformName, name, name, subject) ;  
		    		this.platforms.add(platform) ; 
		    		
		    		serversPerPlatfom = new ArrayList<AppdefResource>(iNoOfServesPerPlatform) ; 
		    		this.servers.put(platform.getId(), serversPerPlatfom) ; 
		    		
			    		
		    			server = this.createServer(platform, serverType, serverTypeName/*+ "_instance_"+iServerCounter*//*, subject) ; 
		    			serversPerPlatfom.add(server) ; 
		    			
		    			
		    					
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

        public void destroy() throws Exception {}
    }
    
//    protected void baseTest(String rscId, String[] tmpNames,
//    		int sMin, int sh, int sd, int sm, int sy, 
//    		int eMin, int eh, int ed, int em, int ey,
//    		MeasurementResponse expectedRes) throws Throwable {
//    	Calendar begin = new GregorianCalendar(),
//    			 end = new GregorianCalendar();
//    	begin.set(Calendar.MINUTE, sMin);
//    	begin.set(Calendar.HOUR_OF_DAY, sh);
//    	begin.set(Calendar.DAY_OF_MONTH, sd);
//    	begin.set(Calendar.MONTH, sm);
//    	begin.set(Calendar.YEAR, sy);
//    	end.set(Calendar.MINUTE, eMin);
//    	end.set(Calendar.HOUR_OF_DAY, eh);
//    	end.set(Calendar.DAY_OF_MONTH, ed);
//    	end.set(Calendar.MONTH, em);
//    	end.set(Calendar.YEAR, ey);
//
//    	baseTest(rscId, tmpNames, begin, end,expectedRes);
//    }
    
    protected void baseTest(Calendar begin,	Calendar end,
    		MeasurementRequest req, MeasurementResponse expectedRes) throws Throwable {
//    	MeasurementResponse res = service.getMetrics(req, begin, end);
//    	Assert.assertEquals(res,expectedRes);
    }
*/
    /**
     * +[+-+]+-+-x
     * 
     * @throws Throwable
     */
    //@Test
 /*   public final void testGetMetricsWinStartsBeforePrgSmallerThan400() throws Throwable {
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	begin.add(Calendar.HOUR_OF_DAY,-1);
    	// build req
    	MeasurementRequest req = new MeasurementRequest();
//    	req.setResourceId(this.tomcatResourceId);
    	List<String> tmpNames = new ArrayList<String>();
//    	tmpNames.add(this.cpuUsageTmpName);
    	req.setMeasurementTemplateNames(tmpNames);
    	// build expected res
//    	org.hyperic.hq.measurement.server.session.Measurement[] msmts = {this.cpuUsageMsmt};
//    	MeasurementResponse expRes = produceResponseObj(msmts,begin,end,MeasurementConstants.TAB_DATA_1D);
    	
//    	baseTest(begin,end,req,expRes);
    }
    

    MeasurementResponse produceResponseObj(org.hyperic.hq.measurement.server.session.Measurement[] hqMsmts,
    		Calendar begin, Calendar end, String aggTable) {
    	MeasurementResponse svcRes = new MeasurementResponse();
    	for (int i = 0; i < hqMsmts.length; i++) {
    		Measurement svcMsmt = produceServiceMeasurement(hqMsmts[i],begin,end, aggTable);
    		svcRes.add(svcMsmt);
		}
 
    	return svcRes;
    }
    
    Measurement produceServiceMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
    		Calendar begin, Calendar end, String aggTable) {
    	Measurement svcMsmt=  new Measurement();
    	svcMsmt.setInterval(hqMsmt.getInterval());
//    	svcMsmt.setName(hqMsmt.getName?);
//    	svcMsmt.setId(hqMsmt.getId/instanceId);
    	List<Metric> svcMetrics = produceServiceMetrics(hqMsmt,begin,end, aggTable);
		svcMsmt.setMetrics(svcMetrics);
		return svcMsmt;
    }
    
    List<Metric> produceServiceMetrics(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
    		Calendar begin, Calendar end, String aggTable) {
        return null;//    	Arrays.copyOfRange(original, from, to, newType)
    }
 */   
    /**
     * +-+-+-+-+-x
     * +[-+--+]-+--+--+
     * 
     * @throws Throwable
     */
    //@Test
 /*   public final void testGetMetricsWinStartsBeforePrgBiggerThan400() throws Throwable { 
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	begin.add(Calendar.DAY_OF_MONTH,-2);

//    	baseTest(begin,end,req,expRes);
    }     
  */  
    /**
     * +-+-x
     * +--+--+[-+-]+--+
     *   
     * @throws Throwable
     */
//    @Test
/*    public final void testGetMetricsWinEndsAfterPrg() throws Throwable { 
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	end.add(Calendar.DAY_OF_MONTH,-2);
    	end.add(Calendar.HOUR,-1);
    	begin.add(Calendar.DAY_OF_MONTH,-2);
    	begin.add(Calendar.HOUR,-2);

//    	baseTest(begin,end,req,expRes);
    }*/
    
    /**
     * +-+-+-+[+-x  ]
     * 
     * @throws Throwable
     */
  //  @Test
    //public final void testGetMetricsWinEndsBeforePrgStartsAfterPrg() throws Throwable { 
    //}
    
    /**
     *   [  a-+]+-+-+-
     * 
     * @throws Throwable
     */
    //@Test
  //  public final void testGetMetricsWinEndsBeforeAgg() throws Throwable { 
    //}

    /**
     * +--+-[+]-+--+--+
     * 
     * @throws Throwable
     */
    //@Test
    //public final void testGetMetricsWinSmallerThanInterval() throws Throwable { 
  //  }
}

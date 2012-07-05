package org.hyperic.hq.api.measurements;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hyperic.hq.api.measurements.MeasurementServiceTest.MeasurementServiceTestDataPopulator;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.Metric;
//import org.hyperic.hq.api.resources.ResourceServiceTest.PlatformsIteration;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator;
import org.hyperic.hq.api.rest.RestTestCaseBase;
import org.hyperic.hq.api.rest.RestTestCaseBase.SecurityInfo;
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
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.MetricValue;
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

@DirtiesContext
@ServiceBindingsIteration(MeasurementServiceTest.CONTEXT_URL + "/rest-api/data/measuremenet")
@TestData(MeasurementServiceTestDataPopulator.class)
public class MeasurementServiceTest extends RestTestCaseBase<MeasurementService, MeasurementServiceTestDataPopulator> {
    @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain ;
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat() ;

    public static class MeasurementServiceTestDataPopulator extends AbstractRestTestDataPopulator<MeasurementService>{
		static final int NO_OF_TEST_PLATFORMS = 1 ;
	    protected final static long HOUR_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.HOURS);
	    protected final static long SIX_HOURS_IN_MILLI = TimeUnit.MILLISECONDS.convert(6L, TimeUnit.HOURS);
	    protected final static long DAY_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);
        protected final static long RAW_DATA_PURGE_TIME =  172800000L;
        protected final static long HOURLY_DATA_PURGE_TIME =  1209600000L;
        protected final static long SIX_HOURLY_DATA_PURGE_TIME = 2678400000L;
        protected final static long DAILY_DATA_PURGE_TIME =  31536000000L;
        protected Date now;

        protected TemplateManager tmpltMgr;

//		protected Method addConfigSchemaMethod ; 
//		protected Method setTypeInfoMethod ;
//	    protected Map<String,String> persistedConfigAttributes ; 
//	    protected Map<String,String> requestConfigAttributes ;
		
	    protected Agent testAgent;
		
		protected Platform platform;//List<AppdefResource> platforms = new ArrayList<AppdefResource>(); 
//        protected List<AppdefResource> servers = new ArrayList<AppdefResource>(); 
        protected AppdefResource rsc; 
//        protected PlatformType platformType ; 
//	    protected ServerType serverType ; 
//	    protected ServiceType serviceType ; 
	    
	    protected List<MeasurementTemplate> tmps;
        protected List<org.hyperic.hq.measurement.server.session.Measurement> msmts;
//        protected List<org.hyperic.hq.measurement.server.session.Metric> dtps;
        protected Map<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<DataPoint>>> hqMetrics = 
                new HashMap<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<DataPoint>>>();
//        protected Map<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<Metric>>> expApiMetrics = 
//                new HashMap<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<Metric>>>();
        
		public MeasurementServiceTestDataPopulator(Class<MeasurementService> serviceInterface, String serviceURL) throws ParseException {
	    	super(MeasurementService.class, CONTEXT_URL + "/rest-api/data/measuremenet") ;
	        now = DATE_FORMAT.parse("16/06/79 00:00 AM");
		}

		@Override
		public final void populate() throws Exception {
		    try {
//		        persistedConfigAttributes  = new HashMap<String,String>() ; 
//		        persistedConfigAttributes.put("log_track.level", "Warn") ;
//		        persistedConfigAttributes.put("config_track.files", "/etc/hq") ;

//		        requestConfigAttributes  = new HashMap<String,String>() ;
//		        requestConfigAttributes.put("log_track.level", "BOGUS_LEVEL_" + System.currentTimeMillis()) ;
//		        requestConfigAttributes.put("config_track.files", "BOGUS_PATH_" + + System.currentTimeMillis()) ;

//		        setTypeInfoMethod = ProductPluginManager.class.getDeclaredMethod("setTypeInfo", String.class,String.class, TypeInfo.class) ;
//		        setTypeInfoMethod.setAccessible(true) ; 

//		        addConfigSchemaMethod = PluginData. class.getDeclaredMethod("addConfigSchema", String.class, int.class, ConfigSchema.class); 
//		        addConfigSchemaMethod.setAccessible(true) ; 

		        final AuthzSubject subject = this.getAuthzSubject() ; 

		        String agentToken = "agentToken" + System.currentTimeMillis(); 
		        this.testAgent = this.createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");

		        final String pluginName = "Test_Plugin" ;
		        final String platformName = "Linux" ; 
		        final String serverTypeName = "Tomcat" ; 

		        PlatformType platformType = this.platformManager.createPlatformType(platformName, pluginName) ; 
		        ServerType serverType = this.createServerType(serverTypeName, "6.0", new String[]{ platformName }, pluginName, false);
		        //create the test platforms, servers and services 

		        String name = "test.ubuntu.eng.vmware.com."; 
		        platform = this.createPlatform(agentToken, platformName, name, name, subject) ;  
//		        this.platforms.add(platform);
		        this.rsc = this.createServer(platform, serverType, serverTypeName, subject) ; 

		        
		        
                // create measurement templates
		        final List<String> tmpNames = new ArrayList<String>() ; 
		        tmpNames.add("Availability") ; 
		        tmpNames.add("JVM Free Memory") ; 
		        
		        List<MeasurementTemplate> tmps = this.tmpltMgr.findTemplatesByName(tmpNames);
		        
                // create measurements
		        List<org.hyperic.hq.measurement.server.session.Measurement> msmts = this.createMeasurements(this.rsc, tmps, 5*60*1000L);
		        
		        for (org.hyperic.hq.measurement.server.session.Measurement msmt : msmts) {
                    Map<String,List<DataPoint>> aggTableToMetrics = new HashMap<String,List<DataPoint>>();
                    // raw
                    List<DataPoint> rawDTPs = new ArrayList<DataPoint>();
                    long msmtInterval = msmt.getInterval();
                    long beginRaw = now.getTime()-RAW_DATA_PURGE_TIME;
                    long numOfRawDTPsTillNow = (int) Math.floor(beginRaw/msmtInterval);
                    
                    for (long i = 0 ; i<numOfRawDTPsTillNow  ; i++) {
                        rawDTPs.add(new DataPoint(msmt.getId(), new MetricValue(i+1,beginRaw+(i*msmtInterval))));
                    }
                    this.dataManager.addData(rawDTPs);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA, rawDTPs);
                    // 1 hour
                    List<DataPoint> hourlyData = new ArrayList<DataPoint>();
                    long beginHourly = now.getTime()-HOURLY_DATA_PURGE_TIME;
                    long endHourly = now.getTime()-HOUR_IN_MILLI ;
                    long numOfHourlyDTPsTillEnd = (int) Math.floor((endHourly-beginHourly)/HOUR_IN_MILLI);
                    for (long i = 0 ; i<numOfHourlyDTPsTillEnd  ; i++) {
                        double val = 10*(i+1);
                        hourlyData.add(new DataPoint(msmt.getId(), new HighLowMetricValue(val,val+5,val-5,beginHourly+(i*HOUR_IN_MILLI))));
                    }
                    this.dataManager.addData(hourlyData, MeasurementConstants.TAB_DATA_1H);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA_1H, hourlyData);
                    // six hours
                    List<DataPoint> sixHourlyData = new ArrayList<DataPoint>();
                    long beginSixHourly = now.getTime()-SIX_HOURLY_DATA_PURGE_TIME;
                    long endSixHourly = now.getTime()-SIX_HOURS_IN_MILLI ;
                    long numOfSixHourlyDTPsTillEnd = (int) Math.floor((endSixHourly-beginSixHourly)/SIX_HOURS_IN_MILLI);
                    for (long i = 0 ; i<numOfSixHourlyDTPsTillEnd  ; i++) {
                        double val = 100*(i+1);
                        sixHourlyData.add(new DataPoint(msmt.getId(), new HighLowMetricValue(val,val+5,val-5,beginSixHourly+(i*SIX_HOURS_IN_MILLI))));
                    }
                    this.dataManager.addData(sixHourlyData, MeasurementConstants.TAB_DATA_6H);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA_6H, sixHourlyData);
                    // day
                    List<DataPoint> dailyData = new ArrayList<DataPoint>();
                    long beginDaily = now.getTime()-DAILY_DATA_PURGE_TIME;
                    long endDaily = now.getTime()-DAY_IN_MILLI ;
                    long numOfdailyDTPsTillEnd = (int) Math.floor((endDaily-beginDaily)/DAY_IN_MILLI);
                    for (long i = 0 ; i<numOfdailyDTPsTillEnd  ; i++) {
                        double val = 1000*(i+1);
                        dailyData.add(new DataPoint(msmt.getId(), new HighLowMetricValue(val,val+5,val-5,beginDaily+(i*DAY_IN_MILLI))));
                    }
                    this.dataManager.addData(dailyData, MeasurementConstants.TAB_DATA_1D);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA_1D, dailyData);

                    hqMetrics.put(msmt, aggTableToMetrics);
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
    
    protected void baseTest(Date begin,	Date end, 
    		MeasurementRequest req, MeasurementResponse expectedRes) throws Throwable {
    	MeasurementResponse res = service.getMetrics(req, String.valueOf(this.testBed.rsc.getId()), DATE_FORMAT.format(begin), DATE_FORMAT.format(end));
    	Assert.assertEquals(res,expectedRes);
    }

    /**
     * +[+-+]+-+-x
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    //@Test
    public final void testGetMetricsWinStartsBeforePrgSmallerThan400() throws Throwable {
        Date begin = new Date();
        Date end = new Date();
        begin.setHours(begin.getHours()-9);
    	// build req
    	List<String> tmpNames = new ArrayList<String>();
    	for (MeasurementTemplate tmp : this.testBed.tmps) {
    	    tmpNames.add(tmp.getName());
        }
        final MeasurementRequest req = new MeasurementRequest(tmpNames) ; 
        // build expected res
        final MeasurementResponse expRes = generateResponseObj(this.testBed.msmts,begin,end,MeasurementConstants.TAB_DATA_1D);
        baseTest(begin, end, req, expRes);
    }
    

    MeasurementResponse generateResponseObj(List<org.hyperic.hq.measurement.server.session.Measurement> hqMsmts,
    		Date begin, Date end, String aggTable) {
    	MeasurementResponse svcRes = new MeasurementResponse();
    	for (org.hyperic.hq.measurement.server.session.Measurement msmt : hqMsmts) {
    		Measurement svcMsmt = generateServiceMeasurement(msmt,begin,end, aggTable);
    		svcRes.add(svcMsmt);
		}
 
    	return svcRes;
    }
    
    Measurement generateServiceMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
    		Date begin, Date end, String aggTable) {
    	Measurement svcMsmt=  new Measurement();
    	svcMsmt.setInterval(hqMsmt.getInterval());
    	svcMsmt.setName(hqMsmt.getTemplate().getName());
    	List<Metric> svcMetrics = generateServiceMetrics(hqMsmt,begin,end, aggTable);
		svcMsmt.setMetrics(svcMetrics);
		return svcMsmt;
    }
    
    List<Metric> generateServiceMetrics(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
    		Date begin, Date end, String aggTable) {
        Map<String,List<DataPoint>> hqAggTableToDTPs = this.testBed.hqMetrics.get(hqMsmt);
        List<DataPoint> hqDTPs = hqAggTableToDTPs.get(aggTable);
        List<Metric> metrics = new ArrayList<Metric>();
        long beginMilli = begin.getTime(),
                endMilli = end.getTime();
        
        for (DataPoint hqDtp : hqDTPs) {
            long hqDtpTime = hqDtp.getTimestamp(); 
            if (beginMilli<=hqDtpTime && hqDtpTime<=endMilli) {
                Metric metric = new Metric();
                metric.setTimestamp(hqDtpTime);
                MetricValue hqMetricVal = hqDtp.getMetricValue();
                metric.setValue(hqMetricVal.getValue());
                if (!MeasurementConstants.TAB_DATA.equals(aggTable)) {
                    HighLowMetricValue hqHighLowMetricValue = (HighLowMetricValue) hqMetricVal;
                    metric.setHighValue(hqHighLowMetricValue.getHighValue());
                    metric.setLowValue(hqHighLowMetricValue.getLowValue());
                }
                metrics.add(metric);
            }
        }
        return metrics;
    }
    
    /**
     * +-+-+-+-+-x
     * +[-+--+]-+--+--+
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    //@Test
    public final void testGetMetricsWinStartsBeforePrgBiggerThan400() throws Throwable { 
//    	Calendar begin = GregorianCalendar.getInstance();
//    	Calendar end = GregorianCalendar.getInstance();
//    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
//    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
//    	begin.add(Calendar.DAY_OF_MONTH,-2);

//    	baseTest(begin,end,req,expRes);
    }     
  
    /**
     * +-+-x
     * +--+--+[-+-]+--+
     *   
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
//    @Test
    public final void testGetMetricsWinEndsAfterPrg() throws Throwable { 
    	Calendar begin = GregorianCalendar.getInstance();
    	Calendar end = GregorianCalendar.getInstance();
    	end.set(Calendar.SECOND, begin.get(Calendar.SECOND));
    	end.set(Calendar.MINUTE, begin.get(Calendar.MINUTE));
    	end.add(Calendar.DAY_OF_MONTH,-2);
    	end.add(Calendar.HOUR,-1);
    	begin.add(Calendar.DAY_OF_MONTH,-2);
    	begin.add(Calendar.HOUR,-2);

//    	baseTest(begin,end,req,expRes);
    }
    
    /**
     * +-+-+-+[+-x  ]
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
  //  @Test
    public final void testGetMetricsWinEndsBeforePrgStartsAfterPrg() throws Throwable { 
    }
    
    /**
     *   [  a-+]+-+-+-
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    //@Test
    public final void testGetMetricsWinEndsBeforeAgg() throws Throwable { 
    }

    /**
     * +--+-[+]-+--+--+
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    //@Test
    public final void testGetMetricsWinSmallerThanInterval() throws Throwable { 
    }
}

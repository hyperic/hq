package org.hyperic.hq.api.measurements;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hyperic.hq.api.measurements.MeasurementServiceTest.MeasurementServiceTestDataPopulator;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator;
import org.hyperic.hq.api.rest.RestTestCaseBase;
import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingsIteration;
import org.hyperic.hq.api.services.MeasurementService;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.tests.context.TestData;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.springframework.test.annotation.DirtiesContext;


@DirtiesContext
@ServiceBindingsIteration(MeasurementServiceTest.CONTEXT_URL + "/rest-api/measurements")
@TestData(MeasurementServiceTestDataPopulator.class)
public class MeasurementServiceTest extends RestTestCaseBase<MeasurementService, MeasurementServiceTestDataPopulator> {
    @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain ;

    public static class MeasurementServiceTestDataPopulator extends AbstractRestTestDataPopulator<MeasurementService>{
        static final int NO_OF_TEST_PLATFORMS = 1 ;
        protected final static long MIN_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
        protected final static long HOUR_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.HOURS);
        protected final static long SIX_HOURS_IN_MILLI = TimeUnit.MILLISECONDS.convert(6L, TimeUnit.HOURS);
        protected final static long DAY_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);
        protected final static long RAW_DATA_PURGE_TIME =  172800000L;
        protected final static long HOURLY_DATA_PURGE_TIME =  1209600000L;
        protected final static long SIX_HOURLY_DATA_PURGE_TIME = 2678400000L;
        protected final static long DAILY_DATA_PURGE_TIME =  31536000000L;
        protected final Calendar now = Calendar.getInstance();
        protected Agent testAgent;
        protected Platform platform;
        protected AppdefResource rsc; 
        protected List<MeasurementTemplate> tmps;
        protected List<org.hyperic.hq.measurement.server.session.Measurement> msmts;
        protected Map<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<DataPoint>>> hqMetrics = 
                new HashMap<org.hyperic.hq.measurement.server.session.Measurement,Map<String,List<DataPoint>>>();

        public MeasurementServiceTestDataPopulator() throws ParseException {
            super(MeasurementService.class, CONTEXT_URL + "/rest-api/measurements") ;
        }
        
        @Override
        public final void populate() throws Exception {
            try {
                final AuthzSubject subject = this.getAuthzSubject() ; 

                String agentToken = "agentToken" + System.currentTimeMillis(); 
                this.testAgent = this.createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
                final String pluginName = "Test_Plugin";
                final String platName = "test.ubuntu.eng.vmware.com."; 
                final String platType = "platTypeTest";
                final String serverName = "svrTest";
                final String serverVer = "1.0";
                final String tmpName = "Template Test";
                PlatformType platformType = this.platformManager.createPlatformType(platType, pluginName) ; 
                platform = this.createPlatform(agentToken, platType, platName, platName, subject) ;  
                ServerType serverType = this.createServerType(serverName, serverVer, new String[]{ platType }, pluginName, false);
 
                //create the test platforms, servers and services 
                this.rsc = this.createServer(platform, serverType, serverName, subject) ; 

                // create measurement templates
                final List<String> tmpNames = new ArrayList<String>() ; 
                tmpNames.add(tmpName);
                MonitorableType monitorableType = this.tmpMgr.createMonitorableType(pluginName, new ServerTypeInfo(serverName, "test", serverVer));
                this.sessionFactory.getCurrentSession().flush();
                MeasurementInfo measurementInfo = new MeasurementInfo();
                Map<String, Object> measAttributes = new HashMap<String, Object>();
                measAttributes.put(MeasurementInfo.ATTR_ALIAS, "templateTest");
                measAttributes.put(MeasurementInfo.ATTR_CATEGORY, MeasurementConstants.CAT_UTILIZATION);
                measAttributes.put(MeasurementInfo.ATTR_COLLECTION_TYPE, "dynamic");
                measAttributes.put(MeasurementInfo.ATTR_DEFAULTON, "true");
                measAttributes.put(MeasurementInfo.ATTR_INDICATOR, "true");
                measAttributes.put(MeasurementInfo.ATTR_INTERVAL, String.valueOf(MIN_IN_MILLI));
                measAttributes.put(MeasurementInfo.ATTR_NAME, tmpName);
                measAttributes.put(MeasurementInfo.ATTR_TEMPLATE, "service:queueSize");
                measAttributes.put(MeasurementInfo.ATTR_UNITS, MeasurementConstants.UNITS_KBYTES);
                measurementInfo.setAttributes(measAttributes);

                List<MonitorableMeasurementInfo> measInfos = new ArrayList<MonitorableMeasurementInfo>();
                measInfos.add(new MonitorableMeasurementInfo(monitorableType, measurementInfo));
                Map<MonitorableType,List<MonitorableMeasurementInfo>> typeToInfos = new HashMap<MonitorableType,List<MonitorableMeasurementInfo>>();
                typeToInfos.put(monitorableType, measInfos);
                this.tmpMgr.createTemplates(pluginName, typeToInfos);
                this.tmps = this.tmpMgr.findTemplatesByName(tmpNames);

                // create measurements
                this.msmts = this.createMeasurements(this.rsc, tmps, MIN_IN_MILLI, new ConfigResponse());

                for (org.hyperic.hq.measurement.server.session.Measurement msmt : msmts) {
                    Map<String,List<DataPoint>> aggTableToMetrics = new HashMap<String,List<DataPoint>>();
                    // raw
                    List<DataPoint> rawDTPs = new ArrayList<DataPoint>();
                    long msmtInterval = msmt.getInterval();
                    long beginRaw = now.getTimeInMillis()-RAW_DATA_PURGE_TIME;
                    long numOfRawDTPsTillNow = (int) Math.floor(RAW_DATA_PURGE_TIME/msmtInterval);

                    for (long i = 0 ; i<numOfRawDTPsTillNow  ; i++) {
                        rawDTPs.add(new DataPoint(msmt.getId(), new HighLowMetricValue(i+1,i+1,i+1,beginRaw+(i*msmtInterval))));
                    }
                    this.dataManager.addData(rawDTPs);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA, rawDTPs);
                    // 1 hour
                    List<DataPoint> hourlyData = new ArrayList<DataPoint>();
                    long beginHourly = now.getTimeInMillis()-HOURLY_DATA_PURGE_TIME;
                    long endHourly = now.getTimeInMillis()-HOUR_IN_MILLI ;
                    long numOfHourlyDTPsTillEnd = (int) Math.floor((endHourly-beginHourly)/HOUR_IN_MILLI);
                    for (long i = 0 ; i<numOfHourlyDTPsTillEnd  ; i++) {
                        double val = 10*(i+1);
                        hourlyData.add(new DataPoint(msmt.getId(), new HighLowMetricValue(val,val+5,val-5,beginHourly+(i*HOUR_IN_MILLI))));
                    }
                    this.dataManager.addData(hourlyData, MeasurementConstants.TAB_DATA_1H);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA_1H, hourlyData);
                    // six hours
                    List<DataPoint> sixHourlyData = new ArrayList<DataPoint>();
                    long beginSixHourly = now.getTimeInMillis()-SIX_HOURLY_DATA_PURGE_TIME;
                    long endSixHourly = now.getTimeInMillis()-SIX_HOURS_IN_MILLI ;
                    long numOfSixHourlyDTPsTillEnd = (int) Math.floor((endSixHourly-beginSixHourly)/SIX_HOURS_IN_MILLI);
                    for (long i = 0 ; i<numOfSixHourlyDTPsTillEnd  ; i++) {
                        double val = 100*(i+1);
                        sixHourlyData.add(new DataPoint(msmt.getId(), new HighLowMetricValue(val,val+5,val-5,beginSixHourly+(i*SIX_HOURS_IN_MILLI))));
                    }
                    this.dataManager.addData(sixHourlyData, MeasurementConstants.TAB_DATA_6H);
                    aggTableToMetrics.put(MeasurementConstants.TAB_DATA_6H, sixHourlyData);
                    // day
                    List<DataPoint> dailyData = new ArrayList<DataPoint>();
                    long beginDaily = now.getTimeInMillis()-DAILY_DATA_PURGE_TIME;
                    long endDaily = now.getTimeInMillis()-DAY_IN_MILLI ;
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

    MeasurementResponse generateResponseObj(List<org.hyperic.hq.measurement.server.session.Measurement> hqMsmts,
            Calendar begin, Calendar end, String aggTable) {
        MeasurementResponse svcRes = new MeasurementResponse();
        for (org.hyperic.hq.measurement.server.session.Measurement msmt : hqMsmts) {
            Measurement svcMsmt = generateServiceMeasurement(msmt,begin,end, aggTable);
            svcRes.add(svcMsmt);
        }

        return svcRes;
    }

    Measurement generateServiceMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
            Calendar begin, Calendar end, String aggTable) {
        Measurement svcMsmt=  new Measurement();
        svcMsmt.setInterval(hqMsmt.getInterval());
        svcMsmt.setName(hqMsmt.getTemplate().getName());
        List<Metric> svcMetrics = generateServiceMetrics(hqMsmt,begin,end, aggTable);
        svcMsmt.setMetrics(svcMetrics);
        return svcMsmt;
    }

    List<Metric> generateServiceMetrics(org.hyperic.hq.measurement.server.session.Measurement hqMsmt,
            Calendar begin, Calendar end, String aggTable) {
        Map<String,List<DataPoint>> hqAggTableToDTPs = this.testBed.hqMetrics.get(hqMsmt);
        List<DataPoint> hqDTPs = hqAggTableToDTPs.get(aggTable);
        List<Metric> metrics = new ArrayList<Metric>();
        long beginMilli = begin.getTimeInMillis(),
                endMilli = end.getTimeInMillis();
        for (DataPoint hqDtp : hqDTPs) {
            long hqDtpTime = hqDtp.getTimestamp();
            if (beginMilli<=hqDtpTime && hqDtpTime<=endMilli) {
                Metric metric = new Metric();
                metric.setTimestamp(hqDtpTime);
                HighLowMetricValue hqMetricVal = (HighLowMetricValue) hqDtp.getMetricValue();
                metric.setValue(hqMetricVal.getValue());
                metric.setHighValue(hqMetricVal.getHighValue());
                metric.setLowValue(hqMetricVal.getLowValue());
                metrics.add(metric);
            }
        }
        return metrics;
    }

    protected static String toISO8601Str(Calendar c) {
        int     month = c.get(Calendar.MONTH),
                day = c.get(Calendar.DAY_OF_MONTH),
                hour = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                utcOffset = (c.get(Calendar.ZONE_OFFSET)+c.get(Calendar.DST_OFFSET))/(60*60*1000);
        return new StringBuilder()
            .append(c.get(Calendar.YEAR))
            .append('-').append(month<9?'0':"").append(month+1)   // gregorian calendaric month begins at 0 instead of 1
            .append('-').append(day<10?'0':"").append(day)
            .append('T').append(hour<10?'0':"").append(hour) 
            .append(':').append(minute<10?'0':"").append(minute)
            .append('+').append(utcOffset<10?'0':"").append(utcOffset).append("00")
            .toString();
    }
    
    protected void baseTest(Calendar begin,	Calendar end, String aggTable) throws Throwable {
        // build req
        List<String> tmpNames = new ArrayList<String>();
        for (MeasurementTemplate tmp : this.testBed.tmps) {
            tmpNames.add(tmp.getName());
        }
        final MeasurementRequest req = new MeasurementRequest(tmpNames) ; 
        // build expected res
        final MeasurementResponse expRes = generateResponseObj(this.testBed.msmts,begin,end,aggTable);
        MeasurementResponse res = service.getMetrics(req, String.valueOf(this.testBed.rsc.getResource().getId()), toISO8601Str(begin), toISO8601Str(end));
        Assert.assertEquals(res,expRes);
    }

    /**
     * +[+-+]+-+-x
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @Test
    public final void testGetMetricsWinStartsBeforePrgSmallerThan400() throws Throwable {
        Calendar end = (Calendar) this.testBed.now.clone();
        Calendar begin = (Calendar) this.testBed.now.clone();
        begin.add(Calendar.HOUR_OF_DAY, -1);
        baseTest(begin, end, MeasurementConstants.TAB_DATA);
    }


    /**
     * +-+-+-+-+-x
     * +[-+--+]-+--+--+
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @Test
    public final void testGetMetricsWinStartsBeforePrgBiggerThan400() throws Throwable { 
        Calendar end = (Calendar) this.testBed.now.clone();
        end.add(Calendar.HOUR, -1);
        Calendar begin = (Calendar) end.clone();
        begin.add(Calendar.HOUR_OF_DAY, -7);
        baseTest(begin, end, MeasurementConstants.TAB_DATA_1H);
    }     

    /**
     * +-+-x
     * +--+--+[-+-]+--+
     *   
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @Test
    public final void testGetMetricsWinEndsAfterPrg() throws Throwable { 
        Calendar end = (Calendar) this.testBed.now.clone();
        end.add(Calendar.MILLISECOND, (int) (-1*MeasurementServiceTestDataPopulator.RAW_DATA_PURGE_TIME)-(60*1000));
        Calendar begin = (Calendar) end.clone();
        begin.add(Calendar.MINUTE, -90);
        baseTest(begin, end, MeasurementConstants.TAB_DATA_1H);
    }


    /**
     *   [  a-+]+-+-+-
     * 
     * @throws Throwable
     */
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @Test
    public final void testGetMetricsWinEndsBeforeAgg() throws Throwable { 
        Calendar end = (Calendar) this.testBed.now.clone();
        Calendar begin = (Calendar) end.clone();
        begin.add(Calendar.HOUR_OF_DAY, -7);
        baseTest(begin, end, MeasurementConstants.TAB_DATA_1H);
    }
}

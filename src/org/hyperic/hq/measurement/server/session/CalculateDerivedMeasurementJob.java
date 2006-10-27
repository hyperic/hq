/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.FinderException;

import org.hyperic.hq.measurement.EvaluationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementPK;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.measurement.shared.RawMeasurementLocal;
import org.hyperic.hq.product.MetricValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;

/**
 * This job is responsible for calculating derived measurements.
 *
 */
public class CalculateDerivedMeasurementJob
    // we are not a session EJB, but we want the utility methods
    extends SessionEJB
    implements StatefulJob
{
    public static final String SCHEDULER_GROUP = "net.covalent.measurement.derived";
    public static final String JOB_PREFIX = "calculate.";
    public static final String SCHEDULE_PREFIX = "interval.";
    public static final int MAX_ATTEMPTS_RECENT = 3;

    protected Log log =
        LogFactory.getLog(CalculateDerivedMeasurementJob.class);
    protected Log timingLog =
        LogFactory.getLog(MeasurementConstants.MEA_TIMING_LOG);

    private Integer measurementId = null;
    private DerivedMeasurementValue measurement;
    private long reservationTime;

    // key=execTime, value=numAttempts
    private MissedJobMap recentlyMissedCalculations;

    public CalculateDerivedMeasurementJob() { }

    /**
     * Entry-point for the job.
     */
    public void execute(JobExecutionContext context)
        throws JobExecutionException {
        final String err =
            "An error occurred during derived measurement calculation.";
            
        long executeTimeBegin = System.currentTimeMillis();
        try {
            // set up our properties
            init(context);

            if ( log.isDebugEnabled() ) {
                log.debug("Woke up at " + new Date() + " ...");
            }
            
            // first try to backfill any recently missed calculations
            calculateRecentlyMissed();
            
            // try to calculate for the reservation time
            // if the dependent data is not there, add to missedCalculations
            if (! calculate(measurement, reservationTime) ) {
                recentlyMissedCalculations.put( new Long(reservationTime), new Integer(1) );
            }

            // Quartz now requires string type properties. Store the recently
            // missed calculation map as a string
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            dataMap.put("recentlyMissedCalculations", 
                recentlyMissedCalculations.encode());
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new JobExecutionException(err, e, false);
        }
        logTime("execute",executeTimeBegin);
    }

    //--------------------------------------------------------------------------
    //-- private helpers
    //--------------------------------------------------------------------------
    // Don't know if there's a better/faster/more efficient way of doing this
    // but what we're trying to accomplish is as follows:
    // GOAL: DBFetch the data for each argument required to perform the evaluation
    //       of the expression. This is admittedly a little convoluted since some
    //       of the data we need belongs to the RM, other properties apply to
    //       the instance of a RM as it applies to a DM, some data belongs
    //       solely to the template and some data belongs to the tmp arguments.
    private Map getMeasurementData(DerivedMeasurementValue measurement,
                                   long reqTime)
        throws FinderException, DataNotAvailableException {
        long    getDataStartTime = System.currentTimeMillis();
        Map     retVal          = new LinkedHashMap();

        // Get our hands on the DM Local
        DerivedMeasurementPK pk =
            new DerivedMeasurementPK( measurement.getId() );
        DerivedMeasurementLocal dmLocal = getDmHome().findByPrimaryKey(pk);

        // get an array of the DM's MeasurementArg VOs.
        MeasurementArgValue[] dmMavArr =  dmLocal.getTemplate().
                                          getMeasurementTemplateValue().
                                          getMeasurementArgs();

        // handle case of incomplete seed/registered templates with no arguments
        if (dmMavArr == null) {
            log.warn ("Invalid measurement template. All templates must have "+
                      "at least one argument. (" +measurement.getId()+" )");
            return retVal;
        }

        // Now take a spin around args using the (Raw Template) argument_id
        // and our instance id to lookup the actual RM.
        for (int argIdx=0;argIdx<dmMavArr.length;argIdx++) {

            // The array of MeasurementArguementValues should always contain
            // references to non-null MAV objects.
            MeasurementArgValue dmMav = dmMavArr[argIdx];
            if (dmMav == null) {
                throw new DataNotAvailableException( "Could not find an associated "+
                           "MeasurementArgValue with this template "+
                            dmMav.getId().intValue());
            }

            // Lookup the RawMeasurement's associated template.
            RawMeasurementLocal rmLocal = getRmHome().
                findByTemplateForInstance(dmMav.getMeasurementTemplateArg().getId(),
                                          dmLocal.getInstanceId());

            if ( log.isDebugEnabled() ) {
                if ( dmMav.getTicks() == null|| dmMav.getTicks().intValue()==0 ) {
                    log.debug("This MeasurementArgValue is not an aggregate (ticks=null or 0)");
                } else {
                    log.debug("This MeasurementArgValue is an aggregate (ticks=" +
                               dmMav.getTicks().toString()+")" );
                }
            }

            // Fetch the rows using datamanager.
            MetricValue mv = null;
            if (dmMav.getTicks() != null && dmMav.getTicks().intValue()>1)
            { // If ticks is set, then we're an aggregate function.
                if (log.isDebugEnabled())
                    log.debug("DataManagerEJB.getTimedDataAggregate(" +
                              rmLocal.getId() + "," + reqTime + "," +
                              dmLocal.getInterval() + "," + dmMav.getTicks() +
                              ")");

                mv = getDataMan().getTimedDataAggregate(rmLocal.getId(),
                                                        reqTime,
                                                        dmLocal.getInterval(),
                                                        dmMav.getTicks()
                                                             .intValue());
            }
            else   // Otherwise, we're a non-aggregate function
            {
                // Set the interval marker ("prevVal") 0=now=current
                int prevVal = (dmMav.getPrevious() == null) ? 0 :
                                dmMav.getPrevious().intValue();

                // fetch/test the data
                mv = getDataMan().getTimedData(rmLocal.getId(), reqTime,
                                               dmLocal.getInterval(), prevVal);

            }

            if (mv == null)
                throw new DataNotAvailableException(
                    "DataManagerEJB.getTimedDataAggregate() couldn't locate " +
                    "rows for RM: " + rmLocal.getId());

            retVal.put(dmMav.getId(), mv);
        }
        logTime("getMeasurementData", getDataStartTime);
        return retVal;
    }

    /**
     * Calculate a derived measurement at a given scheduled interval.
     *
     * @param measurement the measurement to calculate
     * @param when the scheduled interval to calculate
     * @return true if the calculation was successful or already done,
     * false otherwise
     */
    public boolean calculate(DerivedMeasurementValue measurement, long when)
        throws FinderException, DataNotAvailableException {
        long calculateStartTime = System.currentTimeMillis();

        try {

            // First of all, let's see if it's already there
            Integer[] measId = new Integer[] { measurement.getId() };

            // Use linked hashmap to store our values. Default load capacity
            // of .75 should suffice for avg expressions with 3-4 members.
            Map mData = getDataMan().getTimedData(measId, when, measurement.getInterval());

            if (mData.size() > 0) {
                log.debug("Calculations are already done for these measurements.");
                return true;
            }

            // The data wasn't there, so we'll start the collection process
            // First, let's make sure we have the right number of data points
            try {
                mData = getMeasurementData(measurement,when);
            }
            catch (DataNotAvailableException dna) {
                log.debug(dna.getMessage());
                return false;
            }

            // Ensure the data was found and if not, avert this course.
            if (mData == null) {
                log.debug("Could not find dependent data for calculation.");
                return false;
            }

            // If the template string consists of just RawMeasurement (ARG1)
            // then bypass the expression evaluation. Otherwise, evaluate.
            MetricValue calcVal;
            if (measurement.getTemplate().getTemplate().equals(
                MeasurementConstants.TEMPL_IDENTITY)) {
                // fetch the first (and only) value out of the map
                calcVal = (MetricValue) mData.values().toArray()[0];
            } else {
                Double result = evaluateExpression(measurement, mData);
                calcVal = new MetricValue(result, when);
            }

            // Finally store the DM result back in the database
            getDataMan().addData(measurement.getId(), calcVal, true);

        } catch (EvaluationException e) {
            if (log.isErrorEnabled())
                log.error("Couldn't evaluate expression for derived measurement " +
                          measurementId + " at time " + when + ".", e);
            return false;
        }

        logTime("calculate", calculateStartTime);

        return true;
    }

    private void calculateRecentlyMissed()
        throws FinderException, DataNotAvailableException {
        long crmStartTime = System.currentTimeMillis();

        for (Iterator it=recentlyMissedCalculations.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            long execTime = ( (Long)entry.getKey() ).longValue();
            int attempts = ( (Integer)entry.getValue() ).intValue();

            if ( calculate(measurement, execTime) ) {
                // if we succeed, remove this from the failed entries
                it.remove();
            } else {
                if (attempts++ >= MAX_ATTEMPTS_RECENT) {
                    // remove from the failed entries so we don't try anymore
                    it.remove();
                } else {
                    // update # of attempts
                    entry.setValue( new Integer(attempts) );
                }
            }
        }
        logTime("calculateRecentlyMissed", crmStartTime);
    }

    private void init(JobExecutionContext context)
        throws SchedulerException, FinderException {
        long initStart = System.currentTimeMillis();

        // when we were triggered
        long now = System.currentTimeMillis();

        // state information
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        measurementId = new Integer(dataMap.getString("measurementId"));
        recentlyMissedCalculations = 
            new MissedJobMap((String)dataMap.get("recentlyMissedCalculations"));

        // the measurement
        DerivedMeasurementPK pk = new DerivedMeasurementPK( measurementId );
        DMValueCache cache = DMValueCache.getInstance();
        try {
            // Get the measurement from the cache first
            measurement = cache.get(measurementId);

            // Verify that the measurement still exists
            DerivedMeasurementLocal mlocal = getDmHome().findByPrimaryKey(pk);

            if (measurement == null) {
                try {
                    measurement = mlocal.getDerivedMeasurementValue();
                } catch (NullPointerException e) {
                    throw new FinderException(
                        "Measurement Template has been removed");
                }
                cache.put(measurement);
            }
        } catch (FinderException e) {
            log.error("Derived measurement " + measurementId + " not found");
            // No measurement found, remove it, then
            getScheduler().deleteJob(getJobName(measurementId),
                CalculateDerivedMeasurementJob.SCHEDULER_GROUP);
            getScheduler().unscheduleJob(getScheduleName(measurementId),
                CalculateDerivedMeasurementJob.SCHEDULER_GROUP);
            throw e;
        }

        // Calculate the reservation time as now - the modulus of the interval
        // + interval. Now is adjusted to account for latency between the agent
        // and our server. Since roundDownTime takes the difference of systime
        // - modulus of systime, this has the effect of getting a slightly later
        // reservation time.
        long interval = measurement.getInterval();

        reservationTime = TimingVoodoo.roundDownTime
            ( now - TimingVoodoo.percOfInterval( measurement.getInterval() ),
              measurement.getInterval() );

        logTime("init", initStart);
    }

    public static String getJobName(Integer mid) {
        return JOB_PREFIX + mid;
    }
    
    public static String getJobName(DerivedMeasurementValue measurement) {
        return getJobName( measurement.getId() );
    }

    public static String getScheduleName(Integer mid) {
        return SCHEDULE_PREFIX + mid;
    }
    
    public static String getScheduleName(DerivedMeasurementValue measurement) {
        return getScheduleName( measurement.getId() );
    }

    /**
     * Helper method to get a job for a given measurement.
     */
    public static JobDetail getJob(DerivedMeasurementValue measurement) {
        JobDetail job = new JobDetail(getJobName(measurement),
                                      SCHEDULER_GROUP,
                                      CalculateDerivedMeasurementJob.class);
        job.getJobDataMap().put( "measurementId", measurement.getId().toString() );

        return job;
    }

    /**
     * Helper method to get a schedule for a given measurement.
     */
    public static SimpleTrigger getSchedule(DerivedMeasurementValue measurement)
    {
        // make sure that the scheduled time(s) are evenly divisible
        // by the interval
        long interval = measurement.getInterval();
        long now = System.currentTimeMillis();
        long nextTime = TimingVoodoo.roundDownTime(now, interval) + interval;
        nextTime = nextTime + TimingVoodoo.percOfInterval(interval);

        // repeat indefinitely starting at nextTime using the given interval
        SimpleTrigger schedule = new SimpleTrigger
            ( getScheduleName(measurement),
              SCHEDULER_GROUP,
              new Date(nextTime),
              null,
              SimpleTrigger.REPEAT_INDEFINITELY,
              measurement.getInterval() );
        return schedule;
    }

    //---------------------------------------------------------------------
    //-- private helpers --------------------------------------------------
    //---------------------------------------------------------------------
    private void logTime (String method, long start) {
        if (timingLog.isDebugEnabled()) {
            long end=System.currentTimeMillis();
            timingLog.debug("CDMJ."+method+"() - "+end+"-"+start+"="+
                (end-start));
        }
    }

    class MissedJobMap extends HashMap {
        private final static String DELIM_EQUALS = "=";
        private final static String DELIM_PAIR = ":";
    
        public MissedJobMap () {
            super();
        }

        public MissedJobMap (String mapStr) {
            super();
            decode(mapStr);
        }

        public String encode () {
            StringBuffer sb = new StringBuffer();
            for (Iterator it=entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                long execTime = ( (Long)entry.getKey() ).longValue();
                int attempts = ( (Integer)entry.getValue() ).intValue();
                sb.append(execTime).append(DELIM_EQUALS)
                  .append(attempts).append(DELIM_PAIR);
            }
            return sb.toString();
        }

        private void decode (String str) {
            StringTokenizer st;
            if (str != null) {
                st = new StringTokenizer(str,DELIM_PAIR);
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    int eqPos = tok.indexOf(DELIM_EQUALS);
                    Long execTime = new Long(tok.substring(0,eqPos));
                    Integer attempts = new Integer(tok.substring(eqPos+1));
                    put(execTime, attempts);
                }
            }
        }
    }
}

// EOF

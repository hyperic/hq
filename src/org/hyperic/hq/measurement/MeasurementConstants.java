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

package org.hyperic.hq.measurement;

import org.hyperic.util.math.Average;
import org.hyperic.util.math.Sum;

import java.util.HashMap;

/**
 * Constant measurement values shared by other subsystems
 */
public final class MeasurementConstants  {

    /**
     *  Lock to be used when backfill data points are
     *  inserted and backfill truncation occurs
     */
    public static final Object BACKFILL_TRUNCATE_LOCK = new Object();
 
    /** Measurement category availability */
    public static final String CAT_AVAILABILITY   = "AVAILABILITY";

    /** Measurement category performance */
    public static final String CAT_PERFORMANCE    = "PERFORMANCE";

    /** Measurement category throughput */
    public static final String CAT_THROUGHPUT     = "THROUGHPUT";

    /** Measurement category utilization */
    public static final String CAT_UTILIZATION    = "UTILIZATION";

    /* Sorted categories array - useful for Arrays.binarySearch() */
    public static final String[] VALID_CATEGORIES = {
        CAT_AVAILABILITY,
        CAT_PERFORMANCE,
        CAT_THROUGHPUT,
        CAT_UTILIZATION,
    };


    /** Measurement baseline mean option. */
    public static final String BASELINE_OPT_MEAN  = "mean";

    /** Measurement baseline min option. */
    public static final String BASELINE_OPT_MIN   = "min";

    /** Measurement baseline max option. */
    public static final String BASELINE_OPT_MAX   = "max";
    
    /** Availability error */
    public static final double AVAIL_DOWN         = 0.0;

    /** Availability warning */
    public static final double AVAIL_WARN         = 0.5;
    
    /** Availability ok */
    public static final double AVAIL_UP           = 1.0;

    /** Availability unknown */
    public static final double AVAIL_UNKNOWN      = 2.0;    

    /** Availability paused */
    public static final double AVAIL_PAUSED       = -0.01;    

    /** The category for measurement method timing markers
     *  Set level to DEBUG */
    public static final String MEA_TIMING_LOG     = "measurement.timing";

    /** First agent remote method invocation to a Jboss server starting
     *  up will cause a Thread.sleep for this amount of time. This gives
     *  Jboss a chance to finish binding its beans and services. */
    public static final long   MEA_SUBSYS_BOOT_TIME_MS  = 45000;

    /** The argument prefix for template arguments in an expression */
    public static final String TEMPL_IDENTITY_PFX = "ARG";

    /** The identity/passthrough template argument */
    public static final String TEMPL_IDENTITY     = "ARG1";

    /** The default result of evaluation of an expression */
    public static final Double EXPR_EVAL_RESULT_DEFAULT = new Double(0.0);

    /** Package imports for instantJ */
    public static final String[] EXPMGR_PACKAGE_IMPORTS = {
        Average.class.getName(), Sum.class.getName(),
        HashMap.class.getName() 
    };

    /** The default timing window percentage for scheduling */
    public static final double DEFAULT_TIMING_WINDOW_PERCENTAGE = 0.1;

    /** An estimated latency (b/t agent and server) value percentage. */
    public static final double DATA_DEFAULT_LATENCY_PERC= 0.1;

    /** Default capacity for the FIFO expression cache */
    public static final int    EXPRESSION_CACHE_SIZE    = 128;

    public static final long MINUTE   = 60000;
    public static final long HOUR     = MINUTE * 60;
    public static final long DAY      = HOUR   * 24;
    public static final long SIX_HOUR = HOUR   * 6;

    /** Default measurement interval. */
    public static final long   INTERVAL_DEFAULT_MILLIS = MINUTE;

    /** Acceptable live data time difference */
    public static final long   ACCEPTABLE_PLATFORM_LIVE_MILLIS = MINUTE * 5;
    public static final long   ACCEPTABLE_LIVE_MILLIS          = MINUTE * 15;
    public static final long   ACCEPTABLE_SERVICE_LIVE_MILLIS  = MINUTE * 30;

    /** Current health summary data window (8 hours). */
    public static final long   HEALTH_WINDOW_MILLIS  = HOUR * 8;

    /** Data purge daemon life span (4 hours). */
    public static final long   DATA_PURGE_RUN_MILLIS = HOUR * 4;
    
    /** No time range constraint */
    public static final long   TIMERANGE_UNLIMITED   = 0;

    /** Metric units labels */
    // Simple metric types
    public static final String UNITS_NONE          = "none";
    /** value 0.0 .. 1.0 */
    public static final String UNITS_PERCENTAGE    = "percentage";
    /** value 0.0 .. 100.0 */
    public static final String UNITS_PERCENT       = "percent";

    // when units are expressed as bytes (utilization)
    public static final String UNITS_BYTES         = "B";
    public static final String UNITS_KBYTES        = "KB";
    public static final String UNITS_MBYTES        = "MB";
    public static final String UNITS_GBYTES        = "GB";
    public static final String UNITS_TBYTES        = "TB";
    public static final String UNITS_PBYTES        = "PB";

    // convert bytesToBits
    public static final String UNITS_BYTES_TO_BITS = "bytesToBits";

    // when units are expressed as bits (throughput)
    public static final String UNITS_BITS          = "b";
    public static final String UNITS_KBITS         = "Kb";
    public static final String UNITS_MBITS         = "Mb";
    public static final String UNITS_GBITS         = "Gb";
    public static final String UNITS_TBITS         = "Tb";
    public static final String UNITS_PBITS         = "Pb";

    // when units are expressed as time since January 1, 1970
    // not intended for display but hints to the UI as to how to display
    public static final String UNITS_EPOCH_MILLIS  = "epoch-millis";
    public static final String UNITS_EPOCH_SECONDS = "epoch-seconds";

    // when units are expressed in elapsed time (response time)
    public static final String UNITS_NANOS         = "ns"; 
    public static final String UNITS_MICROS        = "mu";
    public static final String UNITS_MILLIS        = "ms";
    public static final String UNITS_JIFFYS        = "jiffys";
    public static final String UNITS_SECONDS       = "sec";

    // Currency units
    public static final String UNITS_CENTS         = "cents";
    
    public static final String[] VALID_UNITS       = {
        UNITS_NONE,
        UNITS_PERCENTAGE,
        UNITS_PERCENT,
        UNITS_BYTES,
        UNITS_KBYTES,
        UNITS_MBYTES,
        UNITS_GBYTES,
        UNITS_TBYTES,
        UNITS_PBYTES,
        UNITS_BYTES_TO_BITS,
        UNITS_BITS,
        UNITS_KBITS,
        UNITS_MBITS,
        UNITS_GBITS,
        UNITS_TBITS,
        UNITS_PBITS,
        UNITS_EPOCH_MILLIS,
        UNITS_EPOCH_SECONDS,
        UNITS_NANOS,
        UNITS_MICROS,
        UNITS_MILLIS,
        UNITS_JIFFYS,
        UNITS_SECONDS,
        UNITS_CENTS,
    };

    /** The measurement report queue */
    public static final String REPORT_QUEUE      = "queue/agentReportQueue";
    
    /** The measurement schedule queue */
    public static final String SCHEDULE_QUEUE    = "queue/agentScheduleQueue";
    
    /** The aggregate data indices */
    public static final int IND_MIN              = 0;
    public static final int IND_AVG              = 1;
    public static final int IND_MAX              = 2;
    public static final int IND_CFG_COUNT        = 3;
    public static final int IND_LAST_TIME        = 4;
    
    /** The collection type values */
    public static final int COLL_TYPE_DYNAMIC    = 0;
    public static final int COLL_TYPE_STATIC     = 1;    
    public static final int COLL_TYPE_TRENDSUP   = 2;
    public static final int COLL_TYPE_TRENDSDOWN = 3;

    public static final int[] VALID_COLL_TYPE    = {
        COLL_TYPE_DYNAMIC,
        COLL_TYPE_STATIC,
        COLL_TYPE_TRENDSUP,
        COLL_TYPE_TRENDSDOWN,
    };

    //string versions used in hq-plugin.xml
    //converted to the int values above by MeasurementInfoXML
    public static final String[] COLL_TYPE_NAMES = {
        "dynamic",
        "static",
        "trendsup",
        "trendsdown",
    };

    // Metric collection interval defaults for each collection type
    public static final long INTERVAL_AVAIL_PLAT = MINUTE;      /* 1 minute   */
    public static final long INTERVAL_AVAIL_SVR  = MINUTE * 5;  /* 5 minutes  */
    public static final long INTERVAL_AVAIL_SVC  = MINUTE * 10; /* 10 minutes */
    public static final long INTERVAL_DYNAMIC    = MINUTE * 5;  /* 5 minutes  */
    public static final long INTERVAL_TRENDING   = MINUTE * 10; /* 10 minutes */
    public static final long INTERVAL_STATIC     = MINUTE * 30; /* 30 minutes */

    public static final int PROBLEM_TYPE_ALERT   = 1;
    public static final int PROBLEM_TYPE_OVER    = 2;
    public static final int PROBLEM_TYPE_UNDER   = 3;

    // Filter bits
    public static final long FILTER_AVAIL        = 0x00000001;
    public static final long FILTER_UTIL         = 0x00000010;
    public static final long FILTER_THRU         = 0x00000100;
    public static final long FILTER_PERF         = 0x00001000;

    public static final long FILTER_DYN          = 0x00010000;
    public static final long FILTER_TREND_UP     = 0x00100000;
    public static final long FILTER_TREND_DN     = 0x01000000;
    public static final long FILTER_STATIC       = 0x10000000;
    
    public static final long FILTER_NONE         = 0x11111111;
    
    // Data tables
    public static final String TAB_DATA          = "EAM_MEASUREMENT_DATA";
    public static final String TAB_DATA_1H       = "EAM_MEASUREMENT_DATA_1H";
    public static final String TAB_DATA_6H       = "EAM_MEASUREMENT_DATA_6H";
    public static final String TAB_DATA_1D       = "EAM_MEASUREMENT_DATA_1D";
    public static final String TAB_MEAS          = "EAM_MEASUREMENT";
    public static final String TAB_PROB          = "EAM_METRIC_PROB";
}


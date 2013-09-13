package org.hyperic.hq.api;

/**
*
* Numeral constants used in the API.
* Matches those used in the UI as defined
* in org.hyperic.hq.ui.NumberConstants
*/
public interface ApiNumberConstants {   
    /*
     * show units in terms of milliseconds
     */
    public static final long MINUTES =  60000; 
    public static final long HOURS   =  3600000;
    public static final long DAYS    =  86400000;

    /**
     * Number of seconds in a minute.
     */
    public static final long SECS_IN_MINUTE = 60l;

    /**
     * Number of seconds in an hour.
     */
    public static final long SECS_IN_HOUR = SECS_IN_MINUTE * 60l;

    /**
     * Number of seconds in a day.
     */
    public static final long SECS_IN_DAY = SECS_IN_HOUR * 24l;

    /**
     * Number of seconds in a week.
     */
    public static final long SECS_IN_WEEK = SECS_IN_DAY * 7l;

    /**
     * five minutes in millisecond increments
     */
    public static final long FIVE_MINUTES = 300000;
    
    public static final String UNKNOWN = "??";    
    
}

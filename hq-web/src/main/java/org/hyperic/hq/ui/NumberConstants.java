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

package org.hyperic.hq.ui;

/**
 *
 * Numeral constants used in the UI
 */

public interface NumberConstants {
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

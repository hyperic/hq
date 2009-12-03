/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util.units;

public interface UnitsConstants {
    public static final int UNIT_NONE       = 0;
    public static final int UNIT_CURRENCY   = 1;
    public static final int UNIT_BYTES      = 2;
    public static final int UNIT_BITS       = 3;
    public static final int UNIT_DURATION   = 4;
    public static final int UNIT_DATE       = 5;
    public static final int UNIT_PERCENTAGE = 6;
    public static final int UNIT_PERCENT    = 7;
    public static final int UNIT_APPROX_DUR = 8;
    public static final int UNIT_BYTES2BITS = 9;
    public static final int UNIT_MAX        = 10; //used for checkValidUnits()

    public static final int SCALE_NONE  = 0;

    // Binary based scaling factors
    public static final int SCALE_KILO  = 1;
    public static final int SCALE_MEGA  = 2;
    public static final int SCALE_GIGA  = 3;
    public static final int SCALE_TERA  = 4;
    public static final int SCALE_PETA  = 5;

    // Time based scaling factors
    public static final int SCALE_YEAR  = 6;
    public static final int SCALE_WEEK  = 7;
    public static final int SCALE_DAY   = 8;
    public static final int SCALE_HOUR  = 9;
    public static final int SCALE_MIN   = 10;
    public static final int SCALE_SEC   = 11;
    public static final int SCALE_JIFFY = 12;
    public static final int SCALE_MILLI = 13;
    public static final int SCALE_MICRO = 14;
    public static final int SCALE_NANO  = 15;

    public static final int SCALE_BIT   = 16;
}

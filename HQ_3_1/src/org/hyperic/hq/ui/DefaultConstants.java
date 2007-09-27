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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

/**
 *
 * Default values used in the UI
 */

public interface DefaultConstants {

    //---------------------------------------default values
    
    /**
     * Default size for add to list tables
     */
    public static final Integer ADDTOLIST_SIZE_DEFAULT = new Integer(15);

    /**
     * Availabiity check timeout defaults to 20 seconds 
     */
    public static final long AVAILABILITY_DEFAULT_TIMEOUT = 20000; 
    
    /**
     * Default number of data points to show on chart
     */
    public static final int DEFAULT_CHART_POINTS = 60;

    /**
     * Default resource type to return
     */
    public static final int FILTER_BY_DEFAULT =
        AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    
    /** the maximum nuber of pages(dots) to be rendered
     */
    public static final Integer MAX_PAGES = new Integer(15);

    /**
     * static indicating no filtering.
     */
    public static final String NO_FILTER_FLAG = "-1";

    /**
     * Default page number for list tables
     */
    public static final Integer PAGENUM_DEFAULT = new Integer(0);

    /** Display all items in list
     */
    public static final Integer PAGESIZE_ALL = new Integer(-1);

    /**
     * Default number of rows for list tables
     */
    public static final Integer PAGESIZE_DEFAULT = new Integer(15);
    
    /**
     * Default sort column for list tables
     */
    public static final Integer SORTCOL_DEFAULT = new Integer(1);
    
    /**
     * ascending  sort order for list tables
     */
    public static final String SORTORDER_ASC = "asc";
    
    /**
     * ascending  sort order for list tables
     */
    public static final String SORTORDER_DEC = "dec";
    
    /**
     * default sort order for list tables
     */
    public static final String SORTORDER_DEFAULT = SORTORDER_ASC;
    
}

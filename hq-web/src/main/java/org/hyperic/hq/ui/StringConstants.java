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
 * Simple string constants
 */
public interface StringConstants {

    public static final String DASHBOARD_DELIMITER = "|";
    public static final String EMPTY_DELIMITER = DASHBOARD_DELIMITER +
                                                 DASHBOARD_DELIMITER;

    public static final String GUIDE_WINDOW_PROPS =
        "height=500,width=350,menubar=no,toolbar=no,status=no,resizable=yes,scrollbars=yes";

    //---------------------------------------file names

    /**
     * Filesystem location for the application configuration
     * properties file.
     */
    public static final String PROPS_FILE_NAME = "/WEB-INF/classes/hq.properties";
    
    /**
     * Filesystem location for the application configuration
     * properties file.
     */
    public static final String PROPS_TAGLIB = "/WEB-INF/classes/taglib.properties";
    
    /** Filesystem location for the application configuration
     * properties file.
     */
    public static final String PROPS_USER_PREFS =
        "/WEB-INF/classes/DefaultUserPreferences.properties";
    
    /**
     * file list delimitor
     */
    public static String DIR_PIPE_SYM = "|";
    public static String DIR_COMMA_SYM = ",";

    public static final String UNIT_FORMAT_PREFIX_KEY = "unit.format.";
    
    /**
     * CONFIG FILTERS
     */
    public static final String MINUTES_LABEL = "minutes";
    public static final String HOURS_LABEL = "hours";
    public static final String DAYS_LABEL = "days";
    
    public static String SMALL_LOGO = "smallLogo";
    public static String SMALL_LOGO_NAME ="smallLogoName";
    public static final String LARGE_LOGO = "largeLogo";
    public static final String LARGE_LOGO_NAME = "largeLogoName";    
   
    public static final String COOKIE_HIDE_HELP = "hq-hide-help";
    public static final String INTROHELP_LOC = "/firstlogin.jsp";
    
    public static final String CHANGE_OWNER_TITLE = "common.title.Edit";
    
    /**
     * AJAX Constants
     */
    public static final String AJAX_ELEMENT = "element";
    public static final String AJAX_OBJECT  = "object";
}

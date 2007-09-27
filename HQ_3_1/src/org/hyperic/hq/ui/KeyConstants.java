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
 * Constant values used as keys in maps
 */
public interface KeyConstants {
    public static final String USERPREF_KEY_PROBLEMS_IGNORELIST = ".dashContent.problems.ignoreList";

    public static final String USERPREF_KEY_PROBLEMS_SHOWIGNORED = ".dashContent.problems.showIgnored";

    public static final String USERPREF_KEY_PROBLEMS_RANGE = ".dashContent.problemResources.range";

    /*
     * Keys in the WebUser.preferences (i.e., in the userprefs ConfigResponse)
     */
    
    /**
     * The key that holds the user's favorite resources
     */
    public static final String USERPREF_KEY_FAVORITE_RESOURCES = ".dashContent.resourcehealth.resources";

    /**
     * The key that holds the user's recent resources
     */
    public static final String USERPREF_KEY_RECENT_RESOURCES = ".dashContent.recent.resources";

    /**
     * The key that holds the user's chart queries
     */
    public static final String USER_DASHBOARD_CHARTS = ".dashContent.charts";

    /**
     * the the user preferences key that holds the users portal  second column choices.
     */
    public static final String USER_PORTLETS_SECOND = ".dashcontent.portal.portlets.second";

    /**
     * the the user preferences key that holds the users portal  first column portlet choices.
     */
    public static final String USER_PORTLETS_FIRST = ".dashcontent.portal.portlets.first";

    public static final String HELP_BASE_URL_KEY = "helpBaseURL";
    
    /** key that will contain the cam specific page title context. */
    public static final String PAGE_TITLE_KEY = "camTitle";
        
    public static final String LOGON_URL_KEY = "forwardURL";
    
    /*
     * The top level tab controls for the monitoring screen link to the "Current
     * Health" view
     */
    public static final String MODE_MON_CUR = "currentHealth";

    /*
     * The top level tab controls for the monitoring screen link to the
     * "Resource Metrics" view
     */
    public static final String MODE_MON_RES_METS = "resourceMetrics";
    
    /*
     * The top level tab controls for the monitoring screen link to the
     * "Deployed Services" view
     */
    public static final String MODE_MON_DEPL_SVRS = "deployedServers";
    
    /*
     * The top level tab controls for the monitoring screen link to the
     * "Deployed Services" view
     */
    public static final String MODE_MON_DEPL_SVCS = "deployedServices";
    
    /*
     * The top level tab controls for the monitoring screen link to the
     * "Internal Services" view
     */
    public static final String MODE_MON_INTERN_SVCS = "internalServices";

    /*
     * The top level tab controls for the monitoring screen link to the
     * "Performance" view
     */
    public static final String MODE_MON_PERF = "performance";

    /*
     * The top level tab controls for the monitoring screen link to the
     * "URL Detail" view
     */
    public static final String MODE_MON_URL = "url";

    /*
     * The current health page links to the deployed child resource types
     */
    public static final String INTERN_CHILD_MODE_ATTR = "internal";
    
    /*
     * The current health page links to the internal child resource types
     */
    public static final String DEPL_CHILD_MODE_ATTR = "deployed";
    
    /*
     * The top level tab controls for the monitoring screen link to the
     * "Internal Services" or "deployed" view
     */
    public static final String MON_SVC_TYPE = "serviceType";
    
    /*
     * The top level tab controls for the monitoring screen link to
     * the "Edit Metric Value Range" view
     */
    public static final String MODE_MON_EDIT_RANGE = "editRange";

    /*
     * Mode for adding metrics to a resource.
     */
    public static final String MODE_ADD_METRICS = "addMetrics";
    
    /**
     * key value for testMode string stored in System.properties
     */
    public static final String SYSTEM_VARIABLE_PATH = "org.hyperic.hq.system";

    /**
     * key values for indicator views
     */
    public static final String INDICATOR_VIEWS = 
        "monitor.visibility.indicator.views.";
    
    public static final String DEFAULT_INDICATOR_VIEW = 
        "resource.common.monitor.visibility.defaultview";

    public static final String RSS_TOKEN = ".dashContent.rssToken";
}

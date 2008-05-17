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

package org.hyperic.hq.common.shared;


/**
 * Global constants file to be used for Config Properties, 
 * as well as any other constant used across subsystems
 */
public class HQConstants {

    public static final String ServerVersion = "CAM_SERVER_VERSION";
    public static final String SchemaVersion = "CAM_SCHEMA_VERSION";
    public static final String JAASProvider  = "CAM_JAAS_PROVIDER";

    /** Application realm.  Used to authenticate users **/
    public static final String ApplicationName = "CAM";

    /** Valid JAAS Providers **/
    public static final String JDBCJAASProvider = "JDBC";
    /** JAAS Provider class names **/
    public static final String JDBCJAASProviderClass = 
        "org.hyperic.hq.auth.server.JDBCLoginModule";

    /** Base URL for the application **/
    public static final String BaseURL = "CAM_BASE_URL";

    // Data storage options (All in ms)
    // How long do we keep raw metric data?
    public static final String DataPurgeRaw    = "CAM_DATA_PURGE_RAW";
    // How long do we keep data compressed in hourly intervals?
    public static final String DataPurge1Hour  = "CAM_DATA_PURGE_1H";
    // How long do we keep data compressed in 6 hour intervals?
    public static final String DataPurge6Hour  = "CAM_DATA_PURGE_6H";
    // How long do we keep data compressed in 1 day intervals?
    public static final String DataPurge1Day   = "CAM_DATA_PURGE_1D";
    // How often to perform database maintainence
    public static final String DataMaintenance = "CAM_DATA_MAINTENANCE";
    // Whether or not to reindex nightly
    public static final String DataReindex     = "DATA_REINDEX_NIGHTLY";

    // How long do we keep alerts
    public static final String AlertPurge      = "ALERT_PURGE";

    // email related
    public static final String EmailSender = "CAM_EMAIL_SENDER";
    
    // Help related
    public static final String HelpUser = "CAM_HELP_USER";
    public static final String HelpUserPassword = "CAM_HELP_PASSWORD";

    // Syslog Actions enabled
    public static final String SyslogActionsEnabled = "CAM_SYSLOG_ACTIONS_ENABLED";

    public static final String SNMPVersion           = "SNMP_VERSION";
    
    public static final String DATASOURCE = "java:/HypericDS";
    public static final String EJB_MODULE_PATTERN = "hq-";
    
    public static final String JBOSSCACHE = "jboss.cache:service=hqTreeCache";
    public static final String EventLogPurge = "EVENT_LOG_PURGE";
    public static final String ExternalHelp = "EXTERNAL_HELP";
    public static final String OOBEnabled = "OOB_ENABLED";
    
    // The config prop key for the directory on the HQ server where the 
    // agent upgrade bundles reside.
    public static final String AgentBundleRepositoryDir = "AGENT_BUNDLE_REPOSITORY_DIR";
    
    // The directory on the HQ agent where the agent upgrade bundles will be copied.
    // This value is hard coded on the agent side so we are hard coding it on 
    // the server.
    // TODO should have a better way to resolve this based on agent.bundle.home
    // agent property
    public static final String AgentBundleDropDir = "../../bundles";
    
}

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
 * Global constants file to be used for Config Properties, as well as any other
 * constant used across subsystems
 */
public class HQConstants {
    
    //Moved from DBUpgrader
    public static final String SCHEMA_MOD_IN_PROGRESS
    = " *** UPGRADE IN PROGRESS: migrating to version ";
    
    //Movd from PlatformServiceDetector
    public static final String PROP_IPADDRESS = "ipaddress";

    public static final String ServerVersion = "CAM_SERVER_VERSION";

    public static final String SchemaVersion = "CAM_SCHEMA_VERSION";

    public static final String AUTHENTICATION_TYPE="CAM_JAAS_PROVIDER";

    public static final String JDBC_AUTHENTICATION_TYPE="JDBC";
    
    /** Application realm. Used to authenticate users **/
    public static final String ApplicationName = "CAM";

    /** Base URL for the application **/
    public static final String BaseURL = "CAM_BASE_URL";

    // Data storage options (All in ms)
    // How long do we keep raw metric data?
    public static final String DataPurgeRaw = "CAM_DATA_PURGE_RAW";

    // How long do we keep data compressed in hourly intervals?
    public static final String DataPurge1Hour = "CAM_DATA_PURGE_1H";

    // How long do we keep data compressed in 6 hour intervals?
    public static final String DataPurge6Hour = "CAM_DATA_PURGE_6H";

    // How long do we keep data compressed in 1 day intervals?
    public static final String DataPurge1Day = "CAM_DATA_PURGE_1D";

    // How often to perform database maintainence
    public static final String DataMaintenance = "CAM_DATA_MAINTENANCE";

    // Whether or not to reindex nightly
    public static final String DataReindex = "DATA_REINDEX_NIGHTLY";

    // How long do we keep alerts
    public static final String AlertPurge = "ALERT_PURGE";

    // How long do we keep TopN
    public static final String TopNPurge = "TOPN_PURGE";

    // Are alerts globally enabled?
    public static final String AlertsEnabled = "HQ_ALERTS_ENABLED";

    // Are alert notifications globally enabled?
    public static final String AlertNotificationsEnabled =
        "HQ_ALERT_NOTIFICATIONS_ENABLED";
    
    // Is hierarchical alerting enabled?
    public static final String HIERARCHICAL_ALERTING_ENABLED =
        "HQ_HIERARCHICAL_ALERTING_ENABLED";

    public static final String ALERT_THROTTLING_THRESHOLD =
        "HQ_ALERT_THRESHOLD";

    public static final String ALERT_THROTTLING_EMAILS =
        "HQ_ALERT_THRESHOLD_EMAILS";

    // email related
    public static final String EmailSender = "CAM_EMAIL_SENDER";

    // Help related
    public static final String HelpUser = "CAM_HELP_USER";

    public static final String HelpUserPassword = "CAM_HELP_PASSWORD";

    // Syslog Actions enabled
    public static final String SyslogActionsEnabled =
        "CAM_SYSLOG_ACTIONS_ENABLED";

    public static final String SNMPVersion = "SNMP_VERSION";
    
    public static final String CAS_URL = "CAS_URL";


    public static final String EventLogPurge = "EVENT_LOG_PURGE";

    public static final String ExternalHelp = "EXTERNAL_HELP";

    public static final String OOBEnabled = "OOB_ENABLED";

    // The config prop key for the directory on the HQ server where the
    // agent upgrade bundles reside.
    public static final String AgentBundleRepositoryDir =
        "AGENT_BUNDLE_REPOSITORY_DIR";

    // The directory on the HQ agent where the agent upgrade bundles will be
    // copied. This value is hard coded on the agent side so we are hard coding
    // it on the server.
    // TODO should have a better way to resolve this based on agent.bundle.home
    // agent property
    public static final String AgentBundleDropDir = "../../bundles";
    
    // license expiration warning
    public static final int LICENSE_EXPIRATION_WARNING_MAX_DAYS = 45;
    
    //SSL keystore config
    public static final String SSL_SERVER_KEYSTORE = "SSL_SERVER_KEYSTORE";
    public static final String SSL_SERVER_KEYPASS = "SSL_SERVER_KEYPASS";
    
    public static final String ORGANIZATION_AUTHENTICATION = "orgAuth";
    public static final String ORG_AUTH_PREFIX = "org/";
    
    //vCenter settings
    public static final String vCenterURL = "VCENTER_URL";
    public static final String vCenterUser = "VCENTER_USER";
    public static final String vCenterPassword = "VCENTER_PASSWORD";
    
    //vCenter mapping 
    public static final String MOID = "MOID";
    public static final String VCUUID = "VCUUID";
    public static final String RESOURCE_NAME = "resource name";
    
    public static final String HQ_GUID = "HQ_GUID";

    public static final String TOPN_PURGE = "TOPN_PURGE";
    public static final String TOPN_DEFAULT_INTERVAL = "TOPN_DEFAULT_INTERVAL";
    public static final String TOPN_NUMBER_OF_PROCESSES = "TOPN_NUMBER_OF_PROCESSES";

}

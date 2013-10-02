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

package org.hyperic.hq.appdef.shared;

/**
 * Defines some constants for working with the autoinventory queues.
 */
public class AIQueueConstants {

    // Decision constants
    public static final int Q_DECISION_DEFER    = 0;
    public static final int Q_DECISION_APPROVE  = 1;
    public static final int Q_DECISION_IGNORE   = 2;
    public static final int Q_DECISION_UNIGNORE = 3;
    public static final int Q_DECISION_PURGE    = 4;

    public static final String[] Q_DECISIONS 
        = { "defer", "approve", "ignore", "unignore", "purge" };

    public static final int Q_STATUS_PLACEHOLDER  = 0;
    public static final int Q_STATUS_ADDED        = 1;
    public static final int Q_STATUS_CHANGED      = 2;
    public static final int Q_STATUS_REMOVED      = 3;

    // Special constant for "no differences"
    public static final long Q_DIFF_NONE = 0;

    // These constants tell us how a particular platform 
    // has changed.
    public static final long Q_PLATFORM_FQDN_CHANGED    = 1L<<1;
    public static final long Q_PLATFORM_IPS_CHANGED     = 1L<<2;
    public static final long Q_PLATFORM_SERVERS_CHANGED = 1L<<3;
    public static final long Q_PLATFORM_PROPERTIES_CHANGED = 1L<<4;

    // These constants tell us why a particular IP has changed.
    public static final long Q_IP_NETMASK_CHANGED    = 1L<<0;
    public static final long Q_IP_MAC_CHANGED        = 1L<<1;

    // These constants tell us why a particular server
    // found in the server AI queue.
    public static final long Q_SERVER_SERVICES_CHANGED    = 1L<<0;
    public static final long Q_SERVER_NAME_CHANGED        = 1L<<1;
    public static final long Q_SERVER_INSTALLPATH_CHANGED = 1L<<2;
    public static final long Q_SERVER_CONFIG_CHANGED      = 1L<<3;

    public static String getQueueStatusString ( int qstat ) {
        // XXX hard-coded english string constants here
        switch (qstat) {
        case AIQueueConstants.Q_STATUS_PLACEHOLDER:
            return "unchanged";
        case AIQueueConstants.Q_STATUS_ADDED:
            return "new";
        case AIQueueConstants.Q_STATUS_CHANGED:
            return "modified";
        case AIQueueConstants.Q_STATUS_REMOVED:
            return "removed";
        default:
            return "unknown (error?)";
        }
    }

    public static String getPlatformDiffString ( int qstat, long diff ) {
        // XXX hard-coded english string constants here
        String diffString = "";
        if ( qstat == AIQueueConstants.Q_STATUS_ADDED ) {
            diffString = "N/A";
        } else {
            if ( (diff & AIQueueConstants.Q_PLATFORM_FQDN_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "fqdn changed";
            }
            if ( (diff & AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", "; 
                diffString += "platform properties changed";
            }
            if ( (diff & AIQueueConstants.Q_PLATFORM_IPS_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "IP set changed";
            }
            if ( (diff & AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "server set changed";
            }
            if ( diffString.length() == 0 ) diffString = "none";
        }
        return diffString;
    }

    public static String getIPDiffString ( int qstat, long diff ) {
        // XXX hard-coded english string constants here
        String diffString = "";
        if ( qstat == AIQueueConstants.Q_STATUS_ADDED ) {
            diffString = "N/A";
        } else {
            if ( (diff & AIQueueConstants.Q_IP_MAC_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "MAC changed";
            }
            if ( (diff & AIQueueConstants.Q_IP_NETMASK_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "netmask changed";
            }
            if ( diffString.length() == 0 ) diffString = "none";
        }
        return diffString;
    }

    public static String getServerDiffString ( int qstat, long diff ) {
        // XXX hard-coded english string constants here
        String diffString = "";
        if ( qstat == AIQueueConstants.Q_STATUS_ADDED ) {
            diffString = "N/A";
        } else {
            if ( (diff & AIQueueConstants.Q_SERVER_SERVICES_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "services changed";
            }
            if ( (diff & AIQueueConstants.Q_SERVER_NAME_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "Name changed";
            }
            if ( (diff & AIQueueConstants.Q_SERVER_INSTALLPATH_CHANGED) != 0 ) {
                if ( diffString.length() > 0 ) diffString += ", ";
                diffString += "Installpath changed";
            }
            if ( diffString.length() == 0 ) diffString = "none";
        }
        return diffString;
    }

    public static int getActionValue ( String actionString ) {
        for ( int i=0; i<Q_DECISIONS.length; i++ ) {
            if ( Q_DECISIONS[i].equalsIgnoreCase(actionString) ) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid action: " 
                                           + actionString);
    }

    public static boolean diffContains ( long diff, long attr ) {
        return (( diff & attr ) != 0);
    }
}

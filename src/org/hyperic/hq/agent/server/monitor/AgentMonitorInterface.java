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

package org.hyperic.hq.agent.server.monitor;

import org.hyperic.hq.agent.AgentMonitorValue;

/**
 * An interface which objects can use when they declare themselves
 * to be monitorable.  The Agent can periodically make requests
 * to these methods when the client of the agent requests monitoring
 * information.
 */
public interface AgentMonitorInterface {
    /**
     * KEY_* are keys which monitors should know about in order to
     * describe what kinds of values can be fetched from them
     *
     * KEY_KEYS  returns an array of strings that can be passed into
     *           getMonitorValues to return actual values
     * KEY_TYPES returns the types associated with the cooresponding KEYS
     */
    public static final String KEY_KEYS  = "MonitorKeys";
    public static final String KEY_TYPES = "MonitorTypes";

    /**
     * Get a value of monitorKeys
     *
     * @param monitorKeys Keys that the monitor recognizes
     * @return A value for each monitorKey presented
     */
    public AgentMonitorValue[] getMonitorValues(String[] monitorKeys);
}

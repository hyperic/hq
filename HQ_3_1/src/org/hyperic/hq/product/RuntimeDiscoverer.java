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

package org.hyperic.hq.product;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.util.config.ConfigResponse;

/**
 * Classes that implement this interface know how to discover
 * resources at runtime for a particular product.  This differs from
 * the regular autoinventory scans in that regular AI scans (i.e. those
 * driven using the ServerDetectors) don't necessarily require the
 * servers to be running to detect them.  Classes that implement the
 * RuntimeDiscoverer interface communicate directly with the running
 * server software to discover resources.
 */
public interface RuntimeDiscoverer {

    /**
     * This method is called by the autoinventory code that runs
     * within the agent.  It is used to discover resources 
     * for a particular server by communicating directly with the
     * server as it is running.
     * @param serverId The server ID to use when constructing
     * AIServerValue objects and other resources to put into the
     * RuntimeResourceReport.
     * @param aiplatform The current platform.  This should be
     * used when detecting servers on the current platform.
     * @param config The config response to use when querying 
     * for resources.
     * @return A RuntimeResourceReport object that describes the
     * services, groups and other resources that are detected.
     * This method can return null if nothing is detected.
     * @throws PluginException TODO
     */
    public RuntimeResourceReport discoverResources (int serverId, 
                                                    AIPlatformValue aiplatform,
                                                    ConfigResponse config) 
        throws PluginException;
}

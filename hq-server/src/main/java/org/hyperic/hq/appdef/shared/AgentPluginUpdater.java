/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.appdef.shared;

import java.util.Collection;
import java.util.Map;

import org.hyperic.hq.product.Plugin;

public interface AgentPluginUpdater {
    
    public static final String AGENT_PLUGIN_TRANSFER = "Agent Plugin Transfer";
    public static final String AGENT_PLUGIN_REMOVE = "Agent Plugin Remove";
    
    /**
     * @param updateMap {@link Map} of {@link Integer} = agentId to {@link Collection}
     *  of {@link Plugin}
     * @param removeMap {@link Map} of {@link Integer} = agentId to {@link Collection}
     *  of {@link String} = plugin filename or {@link Plugin}.getPath()
     * @param restartAgents instructs the mechanism to restart the associated agents which are
     * passed in via updateMap and removeMap.  If true all associated {@link AgentPluginStatus}
     * objs will be changed to SYNC_IN_PROGRESS or else, if false, SYNC_FAILURE
     */
    public void queuePluginTransfer(final Map<Integer, Collection<Plugin>> updateMap,
                                    final Map<Integer, Collection<String>> removeMap,
                                    boolean restartAgents);

    /**
     * queues a plugin for removal from the the specified agent and removes the associated
     * {@link AgentPluginStatus} object
     */
    public void queuePluginRemoval(Map<Integer, Collection<String>> agentToFileNames);

}

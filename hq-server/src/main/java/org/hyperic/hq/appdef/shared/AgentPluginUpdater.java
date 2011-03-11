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
    
    /**
     * @param updateMap {@link Map} of {@link Integer} = agentId to {@link Collection}
     *  of {@link Plugin}
     * @param removeMap {@link Map} of {@link Integer} = agentId to {@link Collection}
     *  of {@link String} = plugin filename or {@link Plugin}.getPath()
     */
    public void queuePluginTransfer(final Map<Integer, Collection<Plugin>> updateMap,
                                    final Map<Integer, Collection<String>> removeMap);

    /**
     * queues a plugin for removal from the the specified agent
     */
    public void queuePluginRemoval(Integer agentId, Collection<String> pluginFileNames);

    /**
     * returns true is the Agent Plugin Sync Mechanism is disabled
     */
    public boolean isDisabled();

    /**
     * disable the Agent Plugin Sync Mechanism
     */
    public void setDisabled(boolean disabled);

}

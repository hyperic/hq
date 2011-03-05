/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
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
 */

package org.hyperic.hq.product.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.product.Plugin;

public interface PluginManager {

    Plugin getByJarName(String jarName);

    /**
     * @param jarInfo {@link Map} of {@link String} = filename to {@link Collection} of bytes
     * that represent the file contents
     * @throws {@link PluginDeployException}
     */
    void deployPluginIfValid(AuthzSubject subj, Map<String, Collection<byte[]>> jarInfo)
    throws PluginDeployException;

// XXX javadoc!
    Map<Integer, Map<AgentPluginStatusEnum, Integer>> getPluginRollupStatus();

// XXX javadoc!
    Collection<AgentPluginStatus> getErrorStatusesByPluginId(int pluginId);

// XXX javadoc!
    boolean isPluginDeploymentOff();

    Plugin getPluginById(Integer id);

// XXX javadoc!
    Map<Plugin, Collection<AgentPluginStatus>> getOutOfSyncAgentsByPlugin();

// XXX javadoc!
    List<Plugin> getAllPlugins();

// XXX javadoc!
    Collection<String> getOutOfSyncPluginNamesByAgentId(Integer agentId);
    
 // XXX javadoc!
     void updateAgentPluginSyncStatusInNewTran(AgentPluginStatusEnum s, Integer agentId,
                                               Collection<Plugin> plugins);
}

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
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.product.Plugin;

public interface PluginManager {

    Plugin getByJarName(String jarName);

    /**
     * Deploys the specified plugin to the hq-plugins dir.  Checks if the jar file is corrupt
     * and if the hq-plugin.xml is well-formed.  If these checks fail a {@link PluginDeployException}
     * will be thrown.
     * @param jarInfo {@link Map} of {@link String} = filename to {@link Collection} of bytes
     * that represent the file contents
     * @throws {@link PluginDeployException} if the plugin file is corrupt and/or the hq-plugin.xml
     * is not well-formed.
     */
    void deployPluginIfValid(AuthzSubject subj, Map<String, byte[]> pluginInfo)
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

 // XXX javadoc!
    void removePlugins(AuthzSubject subj, Collection<String> pluginFilenames)
    throws PermissionException;

 // XXX javadoc!
    void removeAgentPluginStatuses(Integer agentId, Collection<String> pluginFileNames);
}

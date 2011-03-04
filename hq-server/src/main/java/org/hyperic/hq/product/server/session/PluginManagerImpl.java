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

package org.hyperic.hq.product.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusDAO;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.emory.mathcs.backport.java.util.Collections;

@Service
@Transactional(readOnly=true)
public class PluginManagerImpl implements PluginManager {
    
    private PluginDAO pluginDAO;
    private AgentPluginStatusDAO agentPluginStatusDAO;

    @Autowired
    public PluginManagerImpl(PluginDAO pluginDAO, AgentPluginStatusDAO agentPluginStatusDAO) {
        this.pluginDAO = pluginDAO;
        this.agentPluginStatusDAO = agentPluginStatusDAO;
    }
    
    public Plugin getByJarName(String jarName) {
        return pluginDAO.getByJarName(jarName);
    }
    
    public void deployPluginIfValid(AuthzSubject subj, Map<String, Collection<byte[]>> jarInfo)
    throws PluginDeployException {
        // XXX need to fill this in
    }
    
    public Map<Integer, Map<AgentPluginStatusEnum, Integer>> getPluginRollupStatus() {
        final Map<String, Plugin> pluginsByName = getAllPluginsByName();
        final List<AgentPluginStatus> statuses = agentPluginStatusDAO.findAll();
        final Map<Integer, Map<AgentPluginStatusEnum, Integer>> rtn =
            new HashMap<Integer, Map<AgentPluginStatusEnum, Integer>>();
        for (final AgentPluginStatus status : statuses) {
            final String name = status.getPluginName();
            final Plugin plugin = pluginsByName.get(name);
            if (plugin == null) {
                continue;
            }
            setPluginRollup(status, plugin.getId(), rtn);
        }
        return rtn;
    }
    
    private void setPluginRollup(AgentPluginStatus status, Integer pluginId,
                                 Map<Integer, Map<AgentPluginStatusEnum, Integer>> map) {
        Map<AgentPluginStatusEnum, Integer> tmp;
        if (null == (tmp = map.get(pluginId))) {
            tmp = new HashMap<AgentPluginStatusEnum, Integer>();
            tmp.put(AgentPluginStatusEnum.SYNC_FAILURE, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 0);
            tmp.put(AgentPluginStatusEnum.SYNC_SUCCESS, 0);
            map.put(pluginId, tmp);
        }
        final String lastSyncStatus = status.getLastSyncStatus();
        if (lastSyncStatus == null) {
// XXX need to handle this case
            return;
        }
        final AgentPluginStatusEnum e = AgentPluginStatusEnum.valueOf(lastSyncStatus);
        tmp.put(e, tmp.get(e)+1);
    }
    
    public Plugin getPluginById(Integer id) {
        return pluginDAO.get(id);
    }

    private Map<String, Plugin> getAllPluginsByName() {
        final List<Plugin> plugins = pluginDAO.findAll();
        final Map<String, Plugin> rtn = new HashMap<String, Plugin>(plugins.size());
        for (final Plugin plugin : plugins) {
            rtn.put(plugin.getName(), plugin);
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Collection<AgentPluginStatus> getErrorStatusesByPluginId(int pluginId) {
        final Plugin plugin = pluginDAO.get(pluginId);
        if (plugin == null) {
            return Collections.emptyList();
        }
        return agentPluginStatusDAO.getErrorPluginStatusByJarName(plugin.getPath());
    }
    
    public boolean isPluginDeploymentOff() {
// XXX need to implement
        return false;
    }

}

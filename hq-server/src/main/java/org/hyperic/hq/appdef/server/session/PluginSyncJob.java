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

package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.server.session.AgentDataTransferJob;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PluginSyncJob implements AgentDataTransferJob {
    
    private static final String AGENT_PLUGIN_TRANSFER = AgentPluginUpdater.AGENT_PLUGIN_TRANSFER;
    private Integer agentId;
    private Collection<Plugin> plugins;
    private Collection<String> toRemove;
    private AgentManager agentManager;
    private AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private AuthzSubject overlord;
    private PluginManager pluginManager;

    @Autowired
    public PluginSyncJob(AgentManager agentManager,
                         PluginManager pluginManager,
                         AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                         AuthzSubjectManager authzSubjectManager) {
        this.agentManager = agentManager;
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.pluginManager = pluginManager;
        this.overlord = authzSubjectManager.getOverlordPojo();
    }
    
    public String getJobDescription() {
        return AGENT_PLUGIN_TRANSFER;
    }

    public int getAgentId() {
        return agentId;
    }

    public void execute() {
        try {
            final Collection<String> pluginNames = getPluginFileNames(getPlugins());
            final FileDataResult[] transferResult =
                agentManager.transferAgentPlugins(overlord, getAgentId(), pluginNames);
            Map<String, Boolean> removeResult = null;
            if (getToRemove() != null && !getToRemove().isEmpty()) {
                removeResult = agentManager.agentRemovePlugins(overlord, getAgentId(), getToRemove());
            }
            restartAgentIfFilesUpdated(transferResult, removeResult, agentManager);
        } catch (Exception e) {
            pluginManager.updateAgentPluginSyncStatusInNewTran(
                AgentPluginStatusEnum.SYNC_FAILURE, getAgentId(), getPlugins());
            throw new SystemException(
                "error transferring agent plugins to agentId=" + getAgentId(), e);
        }
    }

    private Collection<String> getPluginFileNames(Collection<Plugin> plugins) {
        if (plugins == null) {
            return Collections.emptyList();
        }
        final Collection<String> rtn = new HashSet<String>(plugins.size());
        for (final Plugin plugin : plugins) {
            if (plugin == null) {
                continue;
            }
            rtn.add(plugin.getPath());
        }
        return rtn;
    }

    private void restartAgentIfFilesUpdated(FileDataResult[] transferResult,
                                            Map<String, Boolean> removeResult,
                                            AgentManager agentManager) {
        if (removeResult != null && !removeResult.isEmpty()) {
            for (final Boolean removed : removeResult.values()) {
                if (removed) {
                    agentPluginSyncRestartThrottle.restartAgent(getAgentId());
                    return;
                }
            }
        } else {
            for (final FileDataResult res : transferResult) {
                if (res.getSendBytes() > 0) {
                    agentPluginSyncRestartThrottle.restartAgent(getAgentId());
                    return;
                }
            }
        }
    }

    public void setPlugins(Collection<Plugin> plugins) {
        this.plugins = plugins;
    }

    public Collection<Plugin> getPlugins() {
        return plugins;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public void setToRemove(Collection<String> toRemove) {
        this.toRemove = toRemove;
    }

    public Collection<String> getToRemove() {
        return toRemove;
    }

}

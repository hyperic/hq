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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("agentPluginUpdater")
@Transactional
public class AgentPluginUpdaterImpl
implements AgentPluginUpdater, ApplicationContextAware {
    
    private static final Log log = LogFactory.getLog(AgentPluginUpdaterImpl.class);
    private AgentSynchronizer agentSynchronizer;
    private PluginManager pluginManager;
    private ApplicationContext ctx;
    
    @Autowired
    public AgentPluginUpdaterImpl(AgentSynchronizer agentSynchronizer,
                                  PluginManager pluginManager) {
        this.agentSynchronizer = agentSynchronizer;
        this.pluginManager = pluginManager;
    }

    public void queuePluginTransfer(Map<Integer, Collection<Plugin>> updateMap,
                                    Map<Integer, Collection<String>> removeMap) {
        if (isDisabled()) {
            return;
        }
        if (updateMap == null) {
            updateMap = Collections.emptyMap();
        } if (removeMap == null) {
            removeMap = Collections.emptyMap();
        }
        pluginManager.updateAgentPluginSyncStatus(
            AgentPluginStatusEnum.SYNC_IN_PROGRESS, updateMap, removeMap);
        final Set<Integer> agentIds = new HashSet<Integer>();
        agentIds.addAll(updateMap.keySet());
        agentIds.addAll(removeMap.keySet());
        final boolean debug = log.isDebugEnabled();
        for (final Integer agentId : agentIds) {
            Collection<Plugin> plugins = updateMap.get(agentId);
            Collection<String> toRemove = removeMap.get(agentId);
            if (plugins == null) {
                plugins = Collections.emptyList();
            }
            if (toRemove == null) {
                toRemove = Collections.emptyList();
            }
            if (debug) {
                log.debug("queue plugin transfer for agentId=" + agentId +
                          " plugins=" + plugins);
                log.debug("queue plugin remove for agentId=" + agentId +
                          " filenames=" + toRemove);
            }
            final PluginSyncJob job = ctx.getBean(PluginSyncJob.class);
            job.setAgentId(agentId);
            job.setPlugins(plugins);
            job.setToRemove(toRemove);
            agentSynchronizer.addAgentJob(job);
        }
    }

    public void queuePluginRemoval(Map<Integer, Collection<String>> agentToFileNames) {
        if (agentToFileNames == null || agentToFileNames.isEmpty()) {
            return;
        }
        pluginManager.updateAgentPluginSyncStatus(
            AgentPluginStatusEnum.SYNC_IN_PROGRESS, null, agentToFileNames);
        for (final Entry<Integer, Collection<String>> entry : agentToFileNames.entrySet()) {
            final Integer agentId = entry.getKey();
            final Collection<String> pluginFileNames = entry.getValue();
            final AgentRemovePluginJob job = ctx.getBean(AgentRemovePluginJob.class);
            job.setAgentId(agentId);
            job.setPluginFileNames(pluginFileNames);
            agentSynchronizer.addAgentJob(job);
        }
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    private boolean isDisabled() {
        return !pluginManager.isPluginSyncEnabled();
    }

}
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;
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
    private ZeventManager zeventManager;
    
    @Autowired
    public AgentPluginUpdaterImpl(AgentSynchronizer agentSynchronizer,
                                  PluginManager pluginManager,
                                  ZeventManager zeventManager) {
        this.agentSynchronizer = agentSynchronizer;
        this.pluginManager = pluginManager;
        this.zeventManager = zeventManager;
    }

    @PostConstruct
    public void postConstruct() {
        zeventManager.addBufferedListener(PluginStatusUpdatedZevent.class,
            new ZeventListener<PluginStatusUpdatedZevent>() {
                public void processEvents(List<PluginStatusUpdatedZevent> events) {
                    for (final PluginStatusUpdatedZevent event : events) {
                        queueJobs(event.restartAgents(), event.getUpdateMap(), event.getRemoveMap());
                    }
                }
            }
        );
    }

    private void queueJobs(boolean restartAgents,
                           Map<Integer, Collection<Plugin>> updateMap,
                           Map<Integer, Collection<String>> removeMap) {
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
            removeDuplicates(plugins, toRemove);
            final PluginSyncJob job = ctx.getBean(PluginSyncJob.class);
            job.setAgentId(agentId);
            job.setPlugins(plugins);
            job.setToRemove(toRemove);
            job.restartAgent(restartAgents);
            agentSynchronizer.addAgentJob(job);
        }
    }

    @Transactional(readOnly=false)
    public void queuePluginTransfer(Map<Integer, Collection<Plugin>> updates,
                                    Map<Integer, Collection<String>> removes,
                                    boolean restartAgents) {
        if (isDisabled()) {
            return;
        }
        @SuppressWarnings("unchecked")
        final Map<Integer, Collection<Plugin>> updateMap =
            (updates == null) ? Collections.EMPTY_MAP : updates;
        @SuppressWarnings("unchecked")
        final Map<Integer, Collection<String>> removeMap =
            (removes == null) ? Collections.EMPTY_MAP : removes;
        if (restartAgents) {
            pluginManager.updateAgentPluginSyncStatus(
                AgentPluginStatusEnum.SYNC_IN_PROGRESS, updateMap, removeMap);
        } else {
            pluginManager.updateAgentPluginSyncStatus(
                AgentPluginStatusEnum.SYNC_FAILURE, updateMap, removeMap);
        }
        // want jobs to get added to the agentSynchronizer after commit to ensure that we don't
        // have agents restarting and updating status before the status is updated as a result
        // of the updateAgentPluginSyncStatus() call. If the queuing was not called this way we
        // could have agents sitting in the "IN-PROGRESS" state indefinitely because they were
        // updated out of order
        zeventManager.enqueueEventAfterCommit(
            new PluginStatusUpdatedZevent(restartAgents, updateMap, removeMap));
    }

    private void removeDuplicates(Collection<Plugin> plugins, Collection<String> toRemove) {
        final Set<String> filenames = new HashSet<String>();
        for (final Plugin plugin : plugins) {
            filenames.add(plugin.getPath());
        }
        final boolean debug = log.isDebugEnabled();
        for (final Iterator<String> it=toRemove.iterator(); it.hasNext(); ) {
            final String file = it.next();
            if (filenames.contains(file)) {
                if (debug) log.debug("will not remove " + file + " since it is being transferred");
                it.remove();
            }
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

    private class PluginStatusUpdatedZevent extends Zevent {
        private Map<Integer, Collection<Plugin>> updateMap;
        private Map<Integer, Collection<String>> removeMap;
        private boolean restartAgents;
        @SuppressWarnings("serial")
        private PluginStatusUpdatedZevent(boolean restartAgents,
                                          Map<Integer, Collection<Plugin>> updateMap,
                                          Map<Integer, Collection<String>> removeMap) {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
            this.updateMap = updateMap;
            this.removeMap = removeMap;
            this.restartAgents = restartAgents;
        }
        public boolean restartAgents() {
            return restartAgents;
        }
        private Map<Integer, Collection<Plugin>> getUpdateMap() {
            return updateMap;
        }
        private Map<Integer, Collection<String>> getRemoveMap() {
            return removeMap;
        }
    }

}
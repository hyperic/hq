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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.server.session.AgentDataTransferJob;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("agentPluginUpdater")
@Transactional
public class AgentPluginUpdaterImpl
implements AgentPluginUpdater, ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {
    
    private static final Log log = LogFactory.getLog(AgentPluginUpdaterImpl.class);
    private AuthzSubject overlord;
    private AgentSynchronizer agentSynchronizer;
    private PluginManager pluginManager;
    private ApplicationContext ctx;
    // used AtomicBoolean so that a groovy script may disable the mechanism live, no restarts
    private final AtomicBoolean isDisabled =
        new AtomicBoolean(new Boolean(System.getProperty("hq.saps.disable", "false")));
    private AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    
    @Autowired
    public AgentPluginUpdaterImpl(AuthzSubjectManager authzSubjectManager,
                                  AgentSynchronizer agentSynchronizer,
                                  PluginManager pluginManager,
                                  AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle) {
        this.agentSynchronizer = agentSynchronizer;
        this.pluginManager = pluginManager;
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
    }

    public void queuePluginTransfer(final Map<Integer, Collection<Plugin>> updateMap,
                                    final Map<Integer, Collection<String>> removeMap) {
        if (isDisabled() || updateMap == null || updateMap.isEmpty()) {
            return;
        }
        final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
        for (final Entry<Integer, Collection<Plugin>> entry : updateMap.entrySet()) {
            final Integer agentId = entry.getKey();
            final Collection<Plugin> plugins = entry.getValue();
            final Collection<String> pluginNames = getPluginFileNames(plugins);
// XXX create a spring managed prototype instead of an anon class
            final AgentDataTransferJob job =
                getPluginSyncJob(agentId, plugins, pluginNames, removeMap, agentManager);
            agentSynchronizer.addAgentJob(job);
        }
    }

    private Collection<String> getPluginFileNames(Collection<Plugin> plugins) {
        final Collection<String> rtn = new HashSet<String>(plugins.size());
        for (final Plugin plugin : plugins) {
            if (plugin == null) {
                continue;
            }
            rtn.add(plugin.getPath());
        }
        return rtn;
    }

    private AgentDataTransferJob getPluginSyncJob(final Integer agentId,
                                                  final Collection<Plugin> plugins,
                                                  final Collection<String> pluginNames,
                                                  final Map<Integer, Collection<String>> removeMap,
                                                  final AgentManager agentManager) {
        return new AgentDataTransferJob() {
            public String getJobDescription() {
                return AGENT_PLUGIN_TRANSFER;
            }
            public int getAgentId() {
                return agentId;
            }
            public void execute() {
                try {
                    pluginManager.updateAgentPluginSyncStatusInNewTran(
                        AgentPluginStatusEnum.SYNC_IN_PROGRESS, agentId, plugins);
                    final FileDataResult[] transferResult =
                        agentManager.transferAgentPlugins(overlord, agentId, pluginNames);
                    pluginManager.updateAgentPluginSyncStatusInNewTran(
                        AgentPluginStatusEnum.SYNC_SUCCESS, agentId, plugins);
                    final Collection<String> pluginFileNames =
                        (removeMap == null) ? null : removeMap.get(agentId);
                    if (pluginFileNames != null && !pluginFileNames.isEmpty()) {
                        agentManager.agentRemovePlugins(overlord, agentId, pluginFileNames);
                    }
                    restartAgentIfFilesUpdated(transferResult, pluginFileNames, agentManager);
                } catch (Exception e) {
                    pluginManager.updateAgentPluginSyncStatusInNewTran(
                        AgentPluginStatusEnum.SYNC_FAILURE, agentId, plugins);
                    throw new SystemException(
                        "error transferring agent plugins to agentId=" + agentId, e);
                }
            }
            private void restartAgentIfFilesUpdated(FileDataResult[] transferResult,
                                                    Collection<String> pluginFileNames,
                                                    AgentManager agentManager) {
                if (pluginFileNames != null && !pluginFileNames.isEmpty()) {
                    agentPluginSyncRestartThrottle.restartAgent(agentId);
                } else {
                    for (final FileDataResult res : transferResult) {
                        if (res.getSendBytes() > 0) {
                            agentPluginSyncRestartThrottle.restartAgent(agentId);
                        }
                    }
                }
            }
        };
    }

    public void queuePluginRemoval(final Integer agentId, final Collection<String> pluginFileNames) {
        if (isDisabled() || agentId == null || pluginFileNames == null || pluginFileNames.isEmpty()) {
            return;
        }
        final AgentDataTransferJob job = new AgentDataTransferJob() {
            public String getJobDescription() {
                return AGENT_PLUGIN_REMOVE;
            }
            public int getAgentId() {
                return agentId;
            }
            public void execute() {
                final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
                try {
                    pluginManager.removeAgentPluginStatuses(agentId, pluginFileNames);
                    final Map<String, Boolean> result =
                        agentManager.agentRemovePlugins(overlord, agentId, pluginFileNames);
                    // only reboot the agent if we actually removed a plugin
                    for (Boolean res : result.values()) {
                        if (res.booleanValue()) {
                            agentPluginSyncRestartThrottle.restartAgent(agentId);
                        }
                    }
                } catch (Exception e) {
                    throw new SystemException("error removing pluginFiles=" + pluginFileNames +
                                              " from agentId=" + agentId, e);
                }
            }
        };
        agentSynchronizer.addAgentJob(job);
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(event.getApplicationContext() != this.ctx) {
            return;
        }
        // don't want the main thread to hang the startup so put it in a new thread
        Thread thread = new Thread("AgentPluginStartupSync") {
            public void run() {
                try {
                    log.info("starting agent plugin sync");
                    final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
	                agentManager.syncAllAgentPlugins();
                    log.info("agent plugin sync complete");
                } catch (Throwable t) {
                    log.error("error running plugin sync to agents",t);
                }
            }
        };
        thread.start();
    }

    public void setDisabled(boolean disabled) {
        isDisabled.set(disabled);
    }

    public boolean isDisabled() {
        return isDisabled.get();
    }

}
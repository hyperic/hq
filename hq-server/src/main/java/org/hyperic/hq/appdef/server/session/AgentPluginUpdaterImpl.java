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
import java.util.Set;

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
import org.hyperic.hq.measurement.MeasurementConstants;
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
        final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
        final Set<Integer> agentIds = new HashSet<Integer>();
        agentIds.addAll(updateMap.keySet());
        agentIds.addAll(removeMap.keySet());
        for (final Integer agentId : agentIds) {
            final Collection<Plugin> plugins = updateMap.get(agentId);
            // SYNC_SUCCESS is set on the status when in AgentPluginSyncRestartThrottle,
            // after the agent restarts
            if (plugins != null) {
                pluginManager.updateAgentPluginSyncStatusInNewTran(
                    AgentPluginStatusEnum.SYNC_IN_PROGRESS, agentId, plugins);
            }
// XXX create a spring managed prototype instead of an anon class
            final AgentDataTransferJob job =
                getPluginSyncJob(agentId, plugins, removeMap.get(agentId), agentManager);
            agentSynchronizer.addAgentJob(job);
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

    private AgentDataTransferJob getPluginSyncJob(final Integer agentId,
                                                  final Collection<Plugin> plugins,
                                                  final Collection<String> toRemove,
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
                    final Collection<String> pluginNames = getPluginFileNames(plugins);
                    final FileDataResult[] transferResult =
                        agentManager.transferAgentPlugins(overlord, agentId, pluginNames);
                    Map<String, Boolean> removeResult = null;
                    if (toRemove != null && !toRemove.isEmpty()) {
                        removeResult = agentManager.agentRemovePlugins(overlord, agentId, toRemove);
                    }
                    restartAgentIfFilesUpdated(transferResult, removeResult, agentManager);
                } catch (Exception e) {
                    pluginManager.updateAgentPluginSyncStatusInNewTran(
                        AgentPluginStatusEnum.SYNC_FAILURE, agentId, plugins);
                    throw new SystemException(
                        "error transferring agent plugins to agentId=" + agentId, e);
                }
            }
            private void restartAgentIfFilesUpdated(FileDataResult[] transferResult,
                                                    Map<String, Boolean> removeResult,
                                                    AgentManager agentManager) {
                if (removeResult != null && !removeResult.isEmpty()) {
                    for (final Boolean removed : removeResult.values()) {
                        if (removed) {
                            agentPluginSyncRestartThrottle.restartAgent(agentId);
                            return;
                        }
                    }
                } else {
                    for (final FileDataResult res : transferResult) {
                        if (res.getSendBytes() > 0) {
                            agentPluginSyncRestartThrottle.restartAgent(agentId);
                            return;
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
                // NOTE: AgentPluginStatus objects are removed when the agent restarts and
                // doesn't check in a Plugin
                pluginManager.updateAgentPluginStatusByFileNameInNewTran(
                    AgentPluginStatusEnum.SYNC_IN_PROGRESS, agentId, pluginFileNames);
                try {
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
        final Thread thread = new Thread("AgentPluginStartupSync") {
            public void run() {
                try {
                    // want all agents to be able to check in their PluginInfo before this runs
                    log.info("will start agent plugin sync in 5 minutes");
                    Thread.sleep(5*MeasurementConstants.MINUTE);
                } catch (InterruptedException e) {
                    log.debug(e,e);
                }
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

    private boolean isDisabled() {
        return pluginManager.isPluginDeploymentOff();
    }

}
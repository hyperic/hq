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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentDataTransferJob;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.shared.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AgentRemovePluginJob implements AgentDataTransferJob {
    
    private static final Log log = LogFactory.getLog(AgentRemovePluginJob.class);
    private static final String AGENT_PLUGIN_REMOVE = AgentPluginUpdater.AGENT_PLUGIN_REMOVE;
    private Integer agentId;
    private Collection<String> pluginFileNames;
    private AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private AgentManager agentManager;
    private PluginManager pluginManager;
    private AuthzSubjectManager authzSubjectManager;
    private AtomicBoolean success = new AtomicBoolean(false);

    @Autowired
    public AgentRemovePluginJob(AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                                AuthzSubjectManager authzSubjectManager,
                                PluginManager pluginManager,
                                AgentManager agentManager) {
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.authzSubjectManager = authzSubjectManager;
        this.agentManager = agentManager;
        this.pluginManager = pluginManager;
    }

    public String getJobDescription() {
        return AGENT_PLUGIN_REMOVE;
    }

    public int getAgentId() {
        return agentId;
    }

    public void execute() {
        try {
            final Map<String, Boolean> result =
                agentManager.agentRemovePlugins(authzSubjectManager.getOverlordPojo(), agentId, pluginFileNames);
            for (final Entry<String, Boolean> entry : result.entrySet()) {
                final String file = entry.getKey();
                final Boolean res = entry.getValue();
                // even though the removal may have failed, just log and restart.  Don't interrupt
                // the full process of sync'ing the agent.
                if (!res.booleanValue()) {
                    log.error("error removing plugin file=" + file + " from agentId=" + agentId);
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (!pluginFileNames.isEmpty()) {
                agentPluginSyncRestartThrottle.restartAgent(agentId);
            }
            success.set(true);
        } catch (Exception e) {
            throw new SystemException("error removing pluginFiles=" + pluginFileNames + " from agentId=" + agentId, e);
        }
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public void setPluginFileNames(Collection<String> pluginFileNames) {
        this.pluginFileNames = pluginFileNames;
    }

    public Collection<String> getPluginFileNames() {
        return pluginFileNames;
    }

    public void onFailure(String reason) {
        log.warn("failed remove plugin job on agent " + agentId + ": " + reason);
        pluginManager.updateAgentPluginSyncStatus(
            AgentPluginStatusEnum.SYNC_FAILURE, null, Collections.singletonMap(agentId, pluginFileNames));
    }

    public boolean wasSuccessful() {
        return success.get();
    }

}

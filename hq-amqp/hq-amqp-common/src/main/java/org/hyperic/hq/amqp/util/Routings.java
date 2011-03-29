package org.hyperic.hq.amqp.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * hq.#, hq.agents.#, hq.servers.#
 * hq.agents.*.operations.* , hq.agent.*.operations.#, hq.agent.*.operations.metrics.#
 * @author Helena Edelson
 */
public class Routings {

    private final String OPERATIONS_PREFIX = ".operations.";

    private final String agentRoutingKeyPrefix = "hq.agents.agent-";

    /**
     * For future requirements
     */
    private String serverRoutingKeyPrefix = "hq.servers.server-";

    /**
     * Currently all operations are marked as request-response. As each use case and
     * related code is assessed and migrated, response keys not needed will be removed.
     */
    private final String[] agentOperations = {
            "metrics.report.request", "metrics.availability.request", "metrics.schedule.response", "metrics.unschedule.response", "metrics.config.response",
            "scans.runtime.request", "scans.default.request", "scans.autodiscovery.start.response", "scans.autodiscovery.stop.response", "scans.autodiscovery.config.response",
            "ping.request", "user.authentication.request", "config.authentication.request", "config.registration.request",
            "config.upgrade.response", "config.bundle.request", "config.restart.response", "config.update.request",
            "events.track.log.request", "events.track.config.request",
            "controlActions.results.request", "controlActions.config.response", "controlActions.execute.response",
            "plugin.metadata.request", "plugin.liveData.request",
            "plugin.control.add.response", "plugin.track.add.response", "plugin.track.remove.response"
    };

    private final String[] serverOperations = {
            "metrics.report.response", "metrics.availability.response", "metrics.schedule.request", "metrics.unschedule.request", "metrics.config.request",
            "scans.runtime.response", "scans.default.response", "scans.autodiscovery.start.request", "scans.autodiscovery.stop.request", "scans.autodiscovery.config.request",
            "ping.response", "user.authentication.response", "config.authentication.response", "config.registration.response",
            "config.upgrade.request", "config.bundle.response", "config.restart.request", "config.update.response",
            "events.track.log.response", "events.track.config.response",
            "controlActions.results.response", "controlActions.config.request", "controlActions.execute.request",
            "plugin.metadata.response", "plugin.liveData.response", "plugin.control.add.request",
            "plugin.track.add.request", "plugin.track.remove.request"
    };

    /**
     * Temporary solution.
     */
    public Routings() {
        try {
            this.serverRoutingKeyPrefix += InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // todo logger.error("", e);
        }
    }

    public List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();

        for (String operation : getAgentOperations()) {
            keys.add(new StringBuilder(getAgentRoutingKeyPrefix()).append(agentToken).append(OPERATIONS_PREFIX).append(operation).toString()); 
        }
        return keys;
    }

    public List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();

        for (String operation : getAgentOperations()) {
            keys.add(new StringBuilder(getServerRoutingKeyPrefix()).append(OPERATIONS_PREFIX).append(operation).toString());
        }
        return keys;
    }

    public String getAgentRoutingKeyPrefix() {
        return agentRoutingKeyPrefix;
    }

    public String getServerRoutingKeyPrefix() {
        return serverRoutingKeyPrefix;
    }

    public String[] getAgentOperations() {
        return agentOperations;
    }

    public String[] getServerOperations() {
        return serverOperations;
    }
}

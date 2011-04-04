package org.hyperic.hq.operation.rabbit.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * @author Helena Edelson
 */
public final class RoutingKeys { 

    private static final Log logger = LogFactory.getLog(RoutingKeys.class);

    private final String agentPrefix = "hq.agents.agent-";

    private String serverPrefix = "hq.servers.server-";

    public RoutingKeys() {
        this(getDefaultServerId());
    }

    public RoutingKeys(String serverId) {
        this.serverPrefix += serverId;
    }

    /**
     * Returns the IP address as a String. If an error occurs getting
     * the host IP, a random UUID as String is used.
     * @return the IP address string in textual presentation
     */
    private static String getDefaultServerId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Unable to get the host IP address for use as Server Id.", e);
            return UUID.randomUUID().toString();
        }
    }

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

    public String[] getAgentOperations() {
        return agentOperations;
    }

    public String[] getServerOperations() {
        return serverOperations;
    }

    public String getAgentRoutingKeyPrefix() {
        return agentPrefix;
    }

    public String getServerRoutingKeyPrefix() {
        return serverPrefix;
    } 
}

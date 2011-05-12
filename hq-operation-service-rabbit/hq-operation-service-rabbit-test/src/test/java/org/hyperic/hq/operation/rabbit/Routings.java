package org.hyperic.hq.operation.rabbit;

import org.hyperic.hq.operation.rabbit.util.AgentConstants;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.ServerConstants;
import org.junit.Ignore;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hyperic.hq.operation.rabbit.util.BindingConstants.OPERATION_PREFIX;


@Ignore("TODO remove - deprecated")
public final class Routings {

    public List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();

        for (String operation : AGENT_OPERATIONS) {
            keys.add(new StringBuilder(AGENT_ROUTING_KEY_PREFIX).append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    public List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();

        for (String operation : SERVER_OPERATIONS) {
            keys.add(new StringBuilder(SERVER_ROUTING_KEY_PREFIX).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    /**
     * Returns the IP address as a String. If an error occurs getting
     * the host IP, a random UUID as String is used.
     * @return the IP address string in textual presentation
     */
    public static String getDefaultServerId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return UUID.randomUUID().toString();
        }
    }

    public String getSharedExchangeType() {
        return MessageConstants.SHARED_EXCHANGE_TYPE;
    }

    public String getOperationRequest() {
        return MessageConstants.REQUEST;
    }

    public String getOperationResponse() {
        return MessageConstants.RESPONSE;
    }

    public String getOperationPrefix() {
        return OPERATION_PREFIX;
    }

    public String getToServerUnauthenticatedExchange() {
        return AgentConstants.EXCHANGE_TO_SERVER;
    }

    public String getToServerExchange() {
        return AgentConstants.EXCHANGE_TO_SERVER_SECURE;
    }

    public String getToAgentUnauthenticatedExchange() {
        return ServerConstants.EXCHANGE_TO_AGENT;
    }

    public String getToAgentExchange() {
        return ServerConstants.EXCHANGE_TO_AGENT_SECURE;
    }

    public static final String[] AGENT_OPERATIONS = {
            "metrics.report.request",
            "metrics.availability.request",
            "metrics.schedule.response",
            "metrics.unschedule.response",
            "metrics.config.response",
            "scans.runtime.request",
            "scans.default.request",
            "scans.autodiscovery.start.response",
            "scans.autodiscovery.stop.response",
            "scans.autodiscovery.config.response",
            "ping.request",
            "user.authentication.request",
            "config.authentication.request",
            "config.registration.request",
            "config.upgrade.response",
            "config.bundle.request",
            "config.restart.response",
            "config.update.request",
            "events.track.log.request",
            "events.track.config.request",
            "controlActions.results.request",
            "controlActions.config.response",
            "controlActions.execute.response",
            "plugin.metadata.request",
            "plugin.liveData.request",
            "plugin.control.add.response",
            "plugin.track.add.response",
            "plugin.track.remove.response"
    };
    /**
     * Prefix for agent routings
     */
    public static final String AGENT_ROUTING_KEY_PREFIX = "hq.agent.";

     public static final String SERVER_ROUTING_KEY_PREFIX = "hq.server.";

    public static final String[] SERVER_OPERATIONS = {
            "metrics.report.response",
            "metrics.availability.response",
            "metrics.schedule.request",
            "metrics.unschedule.request",
            "metrics.config.request",
            "scans.runtime.response",
            "scans.default.response",
            "scans.autodiscovery.start.request",
            "scans.autodiscovery.stop.request",
            "scans.autodiscovery.config.request",
            "ping.response",
            "user.authentication.response",
            "config.authentication.response",
            "config.registration.response",
            "config.upgrade.request",
            "config.bundle.response",
            "config.restart.request",
            "config.update.response",
            "events.track.log.response",
            "events.track.config.response",
            "controlActions.results.response",
            "controlActions.config.request",
            "controlActions.execute.request",
            "plugin.metadata.response",
            "plugin.liveData.response",
            "plugin.control.add.request",
            "plugin.track.add.request",
            "plugin.track.remove.request"
    };
}

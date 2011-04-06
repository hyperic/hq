package org.hyperic.hq.operation.rabbit.util;

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

    private final String serverPrefix;

    private final String[] agentOperations;

    private final String[] serverOperations;

    public final String agentRoutingKeyPrefix;

    public RoutingKeys() {
        this(getDefaultServerId());
    }

    public RoutingKeys(String serverId) {
        this.agentOperations = OperationConstants.AGENT_OPERATIONS;
        this.serverOperations = OperationConstants.SERVER_OPERATIONS;
        this.agentRoutingKeyPrefix = RoutingConstants.AGENT_ROUTING_KEY_PREFIX;
        this.serverPrefix = RoutingConstants.SERVER_ROUTING_KEY_PREFIX + serverId;
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


    public String[] getAgentOperations() {
        return agentOperations;
    }

    public String[] getServerOperations() {
        return serverOperations;
    }

    public String getAgentRoutingKeyPrefix() {
        return agentRoutingKeyPrefix;
    }

    public String getServerRoutingKeyPrefix() {
        return serverPrefix;
    }
}

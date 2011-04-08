package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.rabbit.util.OperationConstants;
import org.hyperic.hq.operation.rabbit.util.RoutingConstants;
import org.junit.Ignore;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hyperic.hq.operation.rabbit.util.BindingPatternConstants.OPERATION_PREFIX;


@Ignore
public final class Routings {

 
    private final String serverPrefix;

    private final String[] agentOperations;

    private final String[] serverOperations;

    public final String agentRoutingKeyPrefix;

    public final String defaultToAgentBindingKey;

    public final String defaultToServerBindingKey;

    public Routings() {
        this(getDefaultServerId());
    }

    /**
     * Much of this is temporary. There is a ticket to
     * automate registration of these.
     * @param serverId
     */
    public Routings(String serverId) {
        this.agentOperations = OperationConstants.AGENT_OPERATIONS;
        this.serverOperations = OperationConstants.SERVER_OPERATIONS;
        this.agentRoutingKeyPrefix = RoutingConstants.AGENT_ROUTING_KEY_PREFIX;
        this.serverPrefix = RoutingConstants.SERVER_ROUTING_KEY_PREFIX + serverId;
        this.defaultToAgentBindingKey = agentRoutingKeyPrefix + "";
        this.defaultToServerBindingKey = agentRoutingKeyPrefix + "";
    }

    public List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();

        for (String operation : getAgentOperations()) {
            keys.add(new StringBuilder(getAgentRoutingKeyPrefix()).append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    public List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();

        for (String operation : getServerOperations()) {
            keys.add(new StringBuilder(getServerRoutingKeyPrefix()).append(OPERATION_PREFIX).append(operation).toString());
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

    public String getSharedExchangeType() {
        return RoutingConstants.SHARED_EXCHANGE_TYPE;
    }

    public String getOperationRequest() {
        return RoutingConstants.OPERATION_REQUEST;
    }

    public String getOperationResponse() {
        return RoutingConstants.OPERATION_RESPONSE;
    }

    public String getOperationPrefix() {
        return OPERATION_PREFIX;
    }

    public String getToServerUnauthenticatedExchange() {
        return RoutingConstants.TO_SERVER_EXCHANGE;
    }

    public String getToServerExchange() {
        return RoutingConstants.TO_SERVER_AUTHENTICATED_EXCHANGE;
    }

    public String getToAgentUnauthenticatedExchange() {
        return RoutingConstants.TO_AGENT_EXCHANGE;
    }

    public String getToAgentExchange() {
        return RoutingConstants.TO_AGENT_AUTHENTICATED_EXCHANGE;
    } 
}

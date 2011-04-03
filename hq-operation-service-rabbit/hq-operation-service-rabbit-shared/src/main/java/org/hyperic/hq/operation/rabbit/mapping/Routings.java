package org.hyperic.hq.operation.rabbit.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * hq.#, hq.agents.#, hq.servers.#
 * hq.agents.*.operations.* , hq.agent.*.operations.#, hq.agent.*.operations.metrics.#
 * @author Helena Edelson
 */
public class Routings {

    /**
     * The exchange type for shared agent-server exchanges 
     */
    public static final String SHARED_EXCHANGE_TYPE = "topic";

    public static final String OPERATION_REQUEST = "request";

    public static final String OPERATION_RESPONSE = "response";

    private static final String OPERATION_PREFIX = ".operations.";

    private RoutingKeys routingKeys = new RoutingKeys();

    public List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();

        for (String operation : routingKeys.getAgentOperations()) {
            keys.add(new StringBuilder(routingKeys.getAgentRoutingKeyPrefix()).append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    public List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();

        for (String operation : routingKeys.getServerOperations()) {
            keys.add(new StringBuilder(routingKeys.getServerRoutingKeyPrefix()).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }
}

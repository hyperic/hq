package org.hyperic.hq.operation.rabbit.util;

/**
 * @author Helena Edelson
 */
public class RoutingConstants {

    /**
     * Prefix for agent routings
     */
    public static final String AGENT_ROUTING_KEY_PREFIX = "hq-agents.agent-";

    /**
     * Prefix for server routings
     */
    public static final String SERVER_ROUTING_KEY_PREFIX = "hq-servers.server-";
    
    /**
     * The exchange type for shared agent-server exchanges
     */
    public static final String SHARED_EXCHANGE_TYPE = "topic";
  
    /**
     * Exchange name to use to send to the server as guest
     * such as for a registration operation
     */
    public static final String TO_SERVER_EXCHANGE = "to.server.exchange";

    /**
     * Exchange name to use to send to the server as an authenticated agent
     */
    public static final String TO_SERVER_AUTHENTICATED_EXCHANGE = "to.server.authenticated.exchange";

    /**
     * Exchange name to use to send to an agent as guest
     */
    public static final String TO_AGENT_EXCHANGE = "to.agent.exchange";

    /**
     * Exchange name to use to send to an authenticated agent
     */
    public static final String TO_AGENT_AUTHENTICATED_EXCHANGE = "to.agent.authenticated.exchange";

    public static final String OPERATION_REQUEST = ".request";

    public static final String OPERATION_RESPONSE = ".response";


}

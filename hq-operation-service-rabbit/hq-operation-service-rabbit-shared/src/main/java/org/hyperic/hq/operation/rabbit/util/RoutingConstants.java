/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
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
     * The default exchange
     */
    public static final String DEFAULT_EXCHANGE = "";

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

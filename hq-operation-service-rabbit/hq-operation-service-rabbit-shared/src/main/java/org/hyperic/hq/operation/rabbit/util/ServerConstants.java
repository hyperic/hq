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
 */

package org.hyperic.hq.operation.rabbit.util;


public class ServerConstants {
 
    /**
     * The shared exchange to send to an agent as guest.
     * The routing key must include a unique id to route
     * to the right agent. This is for guest agents (pre-register)
     * so agentToken is not used yet.
     */
    public static final String EXCHANGE_TO_AGENT = "to.agent"; //"hq.agent.guest";

    /**
     * Exchange to send to authenticated agents.
     * Routing key must include the agentToken.
     */
    public static final String EXCHANGE_TO_AGENT_SECURE = "hq.agent.secure";

    public static final String ROUTING_KEY_REGISTER_AGENT = "response.register"; //"hq.server.config.register" + RESPONSE;

    public static final String BINDING_REGISTER_AGENT = "response.*"; //"hq.server.config.register" + RESPONSE;

    public static final String ROUTING_KEY_SERVER_GUEST_REQUEST = "hq.server.guest.request";

    public static final String ROUTING_KEY_SERVER_GUEST_RESPONSE = "hq.server.guest.response";

    /**
     * Allows monitoring to begin on hq.#
     */
    public static final String ROUTING_KEY_PING_REQUEST = "hq.guest.response";

    /**
     * Allows monitoring to begin on hq.#
     */
    public static final String ROUTING_KEY_PING_RESPONSE = "hq.guest.response";

    /* deprecated, TODO remove the following 2 */
    public static final String SERVER_ROUTING_KEY_PREFIX = "hq.server.";


}

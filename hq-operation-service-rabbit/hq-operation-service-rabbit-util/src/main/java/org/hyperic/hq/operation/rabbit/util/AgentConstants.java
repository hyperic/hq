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


public class AgentConstants {

    /**
     * Exchange name to use to send to the server as guest
     * such as for a registration operation
     */
    public static final String EXCHANGE_TO_SERVER = "to.server";

    /**
     * Exchange name to use to send to the server as an authenticated agent
     */
    public static final String EXCHANGE_TO_SERVER_SECURE = "to.server.secure";

    /**
     * Allows monitoring on hq.agent.#, hq.agent.config.*, hq.*.config.register* etc
     */
    public static final String ROUTING_KEY_REGISTER_AGENT = "request.register"; //"hq.agent.config.register.request";

    public static final String BINDING_REGISTER_AGENT = "request.*"; //"hq.agent.config.register.request";

    public static final String ROUTING_KEY_GUEST_REQUEST = "hq.agent.guest.request";

    public static final String ROUTING_KEY_GUEST_RESPONSE = "hq.agent.guest.response";

    public static final String ROUTING_KEY_AGENT_START = "start";

    public static final String ROUTING_KEY_AGENT_RESTART = "restart";

    public static final String ROUTING_KEY_AGENT_DIE = "die";

    public static final String ROUTING_KEY_AGENT_UPGRADE = "upgrade";

    public static final String ROUTING_KEY_GET_AGENT_BUNDLE = "getCurrentAgentBundle";

    public static final String ROUTING_KEY_SEND_FILE = "agentSendFileData";

    public static final String ROUTING_KEY_REMOVE_FILE = "agentRemoveFile";

    public static final String ROUTING_KEY_AGENT_METRICS_REPORT = "hq.agent.metrics.report.request";
}

/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, fromObject to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.operation.rabbit.util;

import com.rabbitmq.client.AMQP;

import java.nio.charset.Charset;

/**
 * @author Helena Edelson
 */
public final class MessageConstants {

    /**
     * Default virtual host
     */
    public static final String DEFAULT_VHOST = "/";

    /**
     * Default RabbitMQ port
     */
    public static final int DEFAULT_PORT = AMQP.PROTOCOL.PORT;

    /**
     * The encoding of the message
     */
    public static final String ENCODING = "UTF-8";

    /**
     * The {@link Charset} that maps to the encoding
     */
    public static final Charset CHARSET = Charset.forName(ENCODING);
 
    /**
     * The content type of the message
     */
    public static final String JSON_CONTENT_TYPE = "application/json";

    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * The delivery mode of the message
     */
    public static final Integer DELIVERY_MODE_NON_PERSISTENT = 1;

    public static final Integer DELIVERY_MODE_PERSISTENT = 2;

    /**
     * The priority of the message
     */
    public static final Integer PRIORITY = 0;

    /**
     * The standard message properties
     */
    public static final AMQP.BasicProperties DEFAULT_MESSAGE_PROPERTIES;
 
    static {
        DEFAULT_MESSAGE_PROPERTIES = new AMQP.BasicProperties();
        DEFAULT_MESSAGE_PROPERTIES.setContentType(JSON_CONTENT_TYPE); // plain for early dev work
        DEFAULT_MESSAGE_PROPERTIES.setContentEncoding(ENCODING);
        DEFAULT_MESSAGE_PROPERTIES.setDeliveryMode(DELIVERY_MODE_NON_PERSISTENT);
        DEFAULT_MESSAGE_PROPERTIES.setPriority(PRIORITY);
    }

    private MessageConstants() {
    }

    /**
     * The exchange type for shared agent-server exchanges
     */
    public static final String SHARED_EXCHANGE_TYPE = "topic";

    public static final String OPERATION_REQUEST = ".request";

    public static final String OPERATION_RESPONSE = ".response";

    public static final String OPERATION_PREFIX = ".operations.";

    public static final String TO_SERVER_UNAUTHENTICATED_EXCHANGE = "toServerUnauthenticatedExchange";

    public static final String TO_SERVER_EXCHANGE = "toServerExchange";

    public static final String TO_AGENT__UNAUTHENTICATED_EXCHANGE = "toAgentUnauthenticatedExchange";

    public static final String TO_AGENT_EXCHANGE = "toAgentExchange";

    public static final String AGENT_ROUTING_KEY_PREFIX = "hq.agents.agent-";

    public static final String SERVER_ROUTING_KEY_PREFIX = "hq.servers.server-";

    /* Temporary - just finishing the api for automatic registration of operations */
    public static final String[] AGENT_OPERATIONS = {
            "metrics.report.request", "metrics.availability.request", "metrics.schedule.response", "metrics.unschedule.response", "metrics.config.response",
            "scans.runtime.request", "scans.default.request", "scans.autodiscovery.start.response", "scans.autodiscovery.stop.response", "scans.autodiscovery.config.response",
            "ping.request", "user.authentication.request", "config.authentication.request", "config.registration.request",
            "config.upgrade.response", "config.bundle.request", "config.restart.response", "config.update.request",
            "events.track.log.request", "events.track.config.request",
            "controlActions.results.request", "controlActions.config.response", "controlActions.execute.response",
            "plugin.metadata.request", "plugin.liveData.request",
            "plugin.control.add.response", "plugin.track.add.response", "plugin.track.remove.response"
    };


    /* Temporary - just finishing the api for automatic registration of operations */
    public static final String[] SERVER_OPERATIONS = {
            "metrics.report.response", "metrics.availability.response", "metrics.schedule.request", "metrics.unschedule.request", "metrics.config.request",
            "scans.runtime.response", "scans.default.response", "scans.autodiscovery.start.request", "scans.autodiscovery.stop.request", "scans.autodiscovery.config.request",
            "ping.response", "user.authentication.response", "config.authentication.response", "config.registration.response",
            "config.upgrade.request", "config.bundle.response", "config.restart.request", "config.update.response",
            "events.track.log.response", "events.track.config.response",
            "controlActions.results.response", "controlActions.config.request", "controlActions.execute.request",
            "plugin.metadata.response", "plugin.liveData.response", "plugin.control.add.request",
            "plugin.track.add.request", "plugin.track.remove.request"
    };
}

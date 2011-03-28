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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;

/**
 * AMQPTypes
 * @author Helena Edelson
 */
public class AMQPTypes {

    public static final String QUEUE = "Queue";

    public static final String EXCHANGE = "Exchange";

    public static final String CHANNEL = "Channel";

    public static final String USER = "User";

    public static final String CONNECTION = "Connection";

    public static final String VIRTUAL_HOST = "VirtualHost";

    public static final String BINDING = "Binding";

    /**
     * Hyperic requires a name for each inventory item.
     * RabbitMQ produces several default Exchanges, one
     * of which has no name. This is a hack so that the service
     * does not show as grey availability. Having no name
     * as other repurcussions in the plugin as well - around
     * transient resource handling.
     */
    public static final String DEFAULT_EXCHANGE_NAME = "amq.default";

}

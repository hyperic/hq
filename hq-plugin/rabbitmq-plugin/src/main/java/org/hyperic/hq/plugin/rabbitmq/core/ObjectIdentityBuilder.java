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

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;

/**
 * ObjectIdentityBuilder
 * @author Helena Edelson
 */
public class ObjectIdentityBuilder implements IdentityBuilder {

    public String buildIdentity(Object obj, Object... args) {
        String virtualHost = (String) args[0];

        if (obj instanceof QueueInfo) {
           return new QueueIdentityBuilder().buildIdentity(obj, virtualHost);
        }
        else if (obj instanceof RabbitConnection) {
           return new ConnectionIdentityBuilder().buildIdentity(obj, virtualHost);
        }
        else if (obj instanceof Exchange) {
           return new ExchangeIdentityBuilder().buildIdentity(obj, virtualHost);
        }
        else if (obj instanceof RabbitChannel) {
           return new ChannelIdentityBuilder().buildIdentity(obj, virtualHost);
        }
        else if (obj instanceof RabbitVirtualHost) {
            return new VirtualHostIdentityBuilder().buildIdentity(obj, virtualHost);
        }
        return null;
    }

    protected class QueueIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            QueueInfo queue = (QueueInfo) obj;
            return new StringBuilder("queue://").append(queue.getName()).append("@").append(virtualHost).toString();
        }
    }

    protected class VirtualHostIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            RabbitVirtualHost vh = (RabbitVirtualHost) obj;

            return new StringBuilder("VirtualHost ").append(vh.getNode()).append(vh.getName()).toString();
        }
    }

    protected class ConnectionIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            RabbitConnection hc = (RabbitConnection) obj;
            com.rabbitmq.client.Address peerAddress = hc.getPeerAddress();
            return new StringBuilder("amqp://").append(hc.getUsername()).append("@").append(peerAddress.getHost())
                    .append(":").append(peerAddress.getPort()).append(hc.getVhost()).toString();
        }
    }

    protected class ExchangeIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            Exchange exchange = (Exchange)obj;
            return new StringBuilder("exchange://").append(exchange.getName()).append("@").append(exchange.getType()).append(virtualHost).toString();
        }
    }

    protected class ChannelIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            RabbitChannel channel = (RabbitChannel) obj;
            return new StringBuilder("channel://").append(channel.getUser()).append("@").append(channel.getPid()).append(channel.getvHost()).toString();
        }
    }

    protected class UserIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            String username = (String)obj;
            return new StringBuilder("amqp://").append(username).append("@").append(virtualHost).toString();
        }
    }

    /** unfinished */
    protected class BindingIdentityBuilder {

        private String buildIdentity(Object obj, String virtualHost) {
            RabbitBinding binding = (RabbitBinding)obj;
            return new StringBuilder("binding://").append("").append("@").append(virtualHost).toString();
        }
    }
}

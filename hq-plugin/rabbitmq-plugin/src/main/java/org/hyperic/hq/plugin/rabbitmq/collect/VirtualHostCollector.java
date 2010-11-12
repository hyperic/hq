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
package org.hyperic.hq.plugin.rabbitmq.collect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitVirtualHost;
import org.hyperic.util.config.ConfigResponse;

import java.util.Properties;

/**
 * VirtualHostCollector
 * @author Helena Edelson
 */
public class VirtualHostCollector extends RabbitMQDefaultCollector {

    private static final Log logger = LogFactory.getLog(QueueCollector.class);

    public void collect() {
        Properties props = getProperties();
        String vhost = (String) props.get(MetricConstants.VHOST);
        String node = (String) props.get(MetricConstants.NODE);
        if (logger.isDebugEnabled()) {
            logger.debug("[collect] vhost=" + vhost + " node=" + node);
        }

        HypericRabbitAdmin rabbitAdmin = getAdmin();

        RabbitVirtualHost virtualHost = new RabbitVirtualHost(vhost, rabbitAdmin);

        if (virtualHost != null) {
            setAvailability(virtualHost.isAvailable());
            setValue("queueCount", virtualHost.getQueueCount());
            setValue("exchangeCount", virtualHost.getExchangeCount());
            setValue("connectionCount", virtualHost.getConnectionCount());
            setValue("channelCount", virtualHost.getChannelCount());
            setValue("consumerCount", virtualHost.getConsumerCount());
        }
    }

    /**
     * Assemble custom key/value data for each object to set
     * as custom properties in the ServiceResource to display
     * in the UI.
     * @param vh
     * @return
     */
    public static ConfigResponse getAttributes(RabbitVirtualHost vh) {
        ConfigResponse res = new ConfigResponse();
        res.setValue("name", vh.getName());
        res.setValue("node", vh.getNode());
        res.setValue("users", vh.getUsers());
        return res;
    }

    @Override
    public Log getLog() {
        return logger;
    }
}

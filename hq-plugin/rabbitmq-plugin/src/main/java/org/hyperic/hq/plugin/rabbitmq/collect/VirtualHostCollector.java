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
import org.hyperic.hq.plugin.rabbitmq.core.RabbitChannel;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitConnection;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitVirtualHost;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.AverageRateCumulativeHistory;
import org.hyperic.hq.plugin.rabbitmq.volumetrics.MovingAverageCumulativeHistory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.QueueInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VirtualHostCollector
 * @author Helena Edelson
 */
public class VirtualHostCollector extends Collector {

    private static final Log logger = LogFactory.getLog(QueueCollector.class);

    private final AtomicLong consumerCount = new AtomicLong();

    private final AtomicLong channelCount = new AtomicLong();

    private final AtomicLong octetsReceivedCount = new AtomicLong();

    private final AtomicLong octetsSentCount = new AtomicLong();

    private final AtomicLong pendingSendsCount = new AtomicLong();

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        logger.debug("[init] props=" + props);
        super.init();
    }

    public void collect() {
        Properties props = getProperties();
        logger.debug("[collect] props=" + props);

        String vhost = (String) props.get(MetricConstants.VIRTUALHOST);
        String node = (String) props.get(MetricConstants.NODE);

        if (RabbitProductPlugin.isInitialized()) {
            HypericRabbitAdmin rabbitAdmin = RabbitProductPlugin.getVirtualHostForNode(vhost, node);

            RabbitVirtualHost virtualHost = rabbitAdmin.getRabbitVirtualHost(vhost);
            if (virtualHost != null) {
                setAvailability(true);

                if (virtualHost.getConnections() != null) {
                    logger.debug("Connections=" + virtualHost.getConnections().size());
                    for (RabbitConnection conn : virtualHost.getConnections()) {

                        setValue("channelCount", channelCount.addAndGet(conn.getChannels()));
                        setValue("octetsReceived", octetsReceivedCount.addAndGet(conn.getOctetsReceived()));
                        setValue("octetsSent",  octetsSentCount.addAndGet(conn.getOctetsSent()));
                        setValue("pendingSends",  pendingSendsCount.addAndGet(conn.getPendingSends()));
                    }
                }
                if (virtualHost.getChannels() != null) {
                    logger.debug("Channels=" + virtualHost.getChannels().size());
                    for (RabbitChannel c : virtualHost.getChannels()) {
                        setValue("consumerCount", this.consumerCount.addAndGet(c.getConsumerCount()));
                    }
                }
            }
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

        List<RabbitConnection> connections = vh.getConnections();
        if (connections != null) {
            res.setValue("totalConnections", connections.size());
        }

        List<RabbitChannel> channels = vh.getChannels();
        if (channels != null) {
            res.setValue("totalChannels", channels.size());
        }
        return res;
    }

}

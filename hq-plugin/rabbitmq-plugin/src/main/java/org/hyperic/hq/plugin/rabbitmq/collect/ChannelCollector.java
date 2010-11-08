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
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import java.util.List;
import java.util.Properties;

/**
 * ChannelCollector
 * @author Helena Edelson
 */
public class ChannelCollector extends Collector {

    private static final Log logger = LogFactory.getLog(ConnectionCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        logger.debug("[init] props=" + props);
        super.init();
    }

    public void collect() {
        Properties props = getProperties();
        logger.debug("[collect] props=" + props);
        String channelPid = (String) props.get(MetricConstants.CHANNEL);
        String vhost = (String) props.get(MetricConstants.VIRTUALHOST);
        String node = (String) props.get(MetricConstants.NODE);

        if (RabbitProductPlugin.isInitialized()) {
            HypericRabbitAdmin rabbitAdmin = RabbitProductPlugin.getVirtualHostForNode(vhost, node);
            List<RabbitChannel> channels = rabbitAdmin.getChannels();
            if (channels != null) {
                for (RabbitChannel c : channels) {
                    if (c.getPid().equalsIgnoreCase(channelPid)) {
                        setAvailability(true);
                        setValue("consumerCount", c.getConsumerCount());
                        setValue("prefetchCount", c.getPrefetchCount());
                        setValue("acksUncommitted", c.getAcksUncommitted());
                        setValue("messagesUnacknowledged", c.getMessagesUnacknowledged());
                    }
                }
            }
        }
    }

    /**
     * Assemble custom key/value data for each object to set
     * as custom properties in the ServiceResource to display
     * in the UI.
     * @param channel
     * @return
     */
    public static ConfigResponse getAttributes(RabbitChannel channel) {
        ConfigResponse res = new ConfigResponse();
        res.setValue("pid", channel.getPid());
        res.setValue("connection", channel.getConnection().getPid());
        res.setValue("number", channel.getNumber());
        res.setValue("user", channel.getUser());
        res.setValue("transactional", channel.getTransactional());


        return res;
    }

}

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

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.*;

import java.util.Properties;
import org.hyperic.hq.product.Metric;

/**
 * ChannelCollector
 * @author Helena Edelson
 */
public class ChannelCollector extends RabbitStatsCollector {

    private static final Log logger = LogFactory.getLog(ChannelCollector.class);

    public RabbitStatsObject collectStats(HypericRabbitAdmin rabbitAdmin) {
        Properties props = getProperties();
        String chName = props.getProperty(MetricConstants.CHANNEL);
        logger.debug("[collect] RabbitChannel=" + chName);
        RabbitStatsObject res = null;
        try {
            RabbitChannel c = rabbitAdmin.getChannel(chName);
            setAvailability(c.getIdleSince() == null ? Metric.AVAIL_UP : Metric.AVAIL_PAUSED);
            setValue("idleTime", c.getIdleSince() == null ? 0 : new Date().getTime() - c.getIdleSince().getTime());
            setValue("consumerCount", c.getConsumerCount());
            setValue("prefetchCount", c.getPrefetchCount());
            setValue("acksUncommitted", c.getAcksUncommitted());
            setValue("messagesUnacknowledged", c.getMessagesUnacknowledged());
            res = c;
        } catch (Exception ex) {
            setAvailability(false);
            logger.debug(ex.getMessage(), ex);
        }
        return res;
    }

    @Override
    public Log getLog() {
        return logger;
    }
}

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

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.product.PluginException;

/**
 * RabbitServiceCollector
 * @author Helena Edelson
 * @author German Laullon
 */
public class RabbitServerCollector extends RabbitStatsCollector {

    private static final Log logger = LogFactory.getLog(RabbitServerCollector.class);

    public RabbitStatsObject collectStats(HypericRabbitAdmin rabbitAdmin) {
        Properties props = getProperties();
        String node = (String) props.get(MetricConstants.NODE);
        if (logger.isDebugEnabled()) {
            logger.debug("[collect] node=" + node);
        }

        RabbitStatsObject res = null;

        try {
            RabbitNode n = rabbitAdmin.getNode(node);
            RabbitOverview o = rabbitAdmin.getOverview();

            setAvailability(n.isRunning());

            setValue("mem_ets", n.getMemEts());
            setValue("proc_used", n.getProcUsed());
            setValue("proc_used_percentage", (double) n.getProcUsed() / (double) n.getProcTotal());
            setValue("fd_percentage", (double) n.getFdUsed() / (double) n.getFdTotal());
            setValue("connectionCount", rabbitAdmin.getConnections().size());
            setValue("channelCount", rabbitAdmin.getChannels().size());

            QueueTotals queueTotals = o.getQueueTotals();
            if (queueTotals != null) {
                setValue("messages", queueTotals.getMessages());
                setValue("messages_ready", queueTotals.getMessagesReady());
                setValue("messages_unacknowledged", queueTotals.getMessagesUnacknowledged());
            }

            res = o;

        } catch (PluginException ex) {
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

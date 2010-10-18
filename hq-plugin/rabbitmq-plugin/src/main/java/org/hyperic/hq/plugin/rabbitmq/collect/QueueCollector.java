/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.QueueInfo;

import java.util.List;

/**
 * QueueCollector
 * @author Helena Edelson
 */
public class QueueCollector extends Collector {

    private static final Log logger = LogFactory.getLog(QueueCollector.class);

    @Override
    public void collect() {
        Configuration configuration = Configuration.toConfiguration(getProperties());
        boolean isAvailable = false;

        try {
            isAvailable = RabbitProductPlugin.isNodeAvailabile(configuration);
        } catch (PluginException e) {
            logger.error(e.getMessage());
        }

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();

        if (rabbitGateway != null) {
          
            try {
                List<String> virtualHosts = rabbitGateway.getVirtualHosts();
                if (virtualHosts != null) {
                    for (String virtualHost : virtualHosts) {
                        List<QueueInfo> queues = rabbitGateway.getQueues(virtualHost);
                        if (queues != null) {
                            for (QueueInfo queue : queues) { 
                                setAvailability(isAvailable);
                                setValue("messages", queue.getMessages());
                                setValue("consumers", queue.getConsumers());
                                setValue("transactions", queue.getTransactions());
                                setValue("memory", queue.getMemory());
                            }
                        } else {
                            setAvailability(false);
                        }
                    }
                }
            }
            catch (Exception ex) {
                logger.error(ex);
            }
        } else {
            setAvailability(false);
        }
    }

    /**
     * Assemble custom key/value data for each object to set
     * as custom properties in the ServiceResource to display
     * in the UI.
     * @param queue
     * @return
     */
    public static ConfigResponse getAttributes(QueueInfo queue) {
        String durable = queue.isDurable() ? "durable" : "not durable";
        ConfigResponse res = new ConfigResponse();
        res.setValue("durable", durable);
        res.setValue("acksUncommitted", queue.getAcksUncommitted());
        res.setValue("messagesReady", queue.getMessagesReady());
        res.setValue("messagesUnacknowledged", queue.getMessagesUnacknowledged());
        res.setValue("messagesUncommitted", queue.getMessageUncommitted());
        res.setValue("name", queue.getName());
        res.setValue("pid", queue.getPid().substring(5, queue.getPid().length() - 1));
        return res;
    }

}

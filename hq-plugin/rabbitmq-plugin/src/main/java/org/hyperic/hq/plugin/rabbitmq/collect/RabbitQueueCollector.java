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
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.Collector;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.QueueInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * RabbitQueueCollector
 *
 * @author Helena Edelson
 */
public class RabbitQueueCollector extends Collector {

    private static final Log logger = LogFactory.getLog(RabbitQueueCollector.class);
 
    @Override
    public void collect() {
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        if (rabbitGateway != null) {

            try {
                List<QueueInfo> queues = rabbitGateway.getQueues();
                if (queues != null) {

                    for (QueueInfo queue : queues) {

                        Map<String, Object> props = new HashMap<String, Object>();
                        props.put("messages", queue.getMessages());
                        props.put("consumers", queue.getConsumers());
                        props.put("transactions", queue.getTransactions());
                        props.put("memory", queue.getMemory());
                        addValues(props);

                        setValue("messages", queue.getMessages());
                        setValue("consumers", queue.getConsumers());
                        setValue("transactions", queue.getTransactions());
                        setValue("memory", queue.getMemory());


                        setAvailability(true);
                    }
                }
            }
            catch (Exception ex) {
                logger.error(ex);
            }
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
        res.setValue("pid", queue.getPid().substring(5, queue.getPid().length()-1));
        return res;        
    }   

}

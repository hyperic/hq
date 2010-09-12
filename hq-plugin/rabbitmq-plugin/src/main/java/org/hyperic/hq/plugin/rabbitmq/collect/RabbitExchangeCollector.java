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
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitProductPlugin;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.Exchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitExchangeCollector
 *
 * @author Helena Edelson
 */
public class RabbitExchangeCollector extends Collector {

    private static final Log logger = LogFactory.getLog(RabbitExchangeCollector.class);

    @Override
    public void collect() {
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        if (rabbitGateway != null) {

            try {
                List<Exchange> exchanges = rabbitGateway.getExchanges();
                if (exchanges != null) {
                    for (Exchange e : exchanges) {
                         
                        setAvailability(true);
                    }
                }
            }
            catch (PluginException ex) {
                logger.error(ex);
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * Assemble custom key/value data for each object to set
     * as custom properties in the ServiceResource to display
     * in the UI.
     * @param e
     * @return
     */
    public static ConfigResponse getAttributes(Exchange e) {
        String durable = e.isDurable() ? "durable" : "not durable";
        ConfigResponse res = new ConfigResponse();
        res.setValue("durable", durable);
        res.setValue("exchangeType", e.getExchangeType().name());
        res.setValue("autoDelete", e.isAutoDelete());
        return res;
    }


}

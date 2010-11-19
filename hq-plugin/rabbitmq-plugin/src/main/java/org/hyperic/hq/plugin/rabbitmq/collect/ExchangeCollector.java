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
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.Exchange;

import java.util.List;
import java.util.Properties;
import org.hyperic.hq.product.Metric;

/**
 * ExchangeCollector
 * @author Helena Edelson
 */
public class ExchangeCollector extends RabbitMQListCollector {

    private static final Log logger = LogFactory.getLog(ExchangeCollector.class);

    public void collect(HypericRabbitAdmin rabbitAdmin) {
        Properties props = getProperties();
        String vhost = (String) props.get(MetricConstants.VHOST);
        if (logger.isDebugEnabled()) {
            String node = (String) props.get(MetricConstants.NODE);
            logger.debug("[collect] vhost=" + vhost + " node=" + node);
        }

        List<Exchange> exchanges = rabbitAdmin.getExchanges(vhost);
        if (exchanges != null) {
            for (Exchange e : exchanges) {
                logger.debug("[collect] Exchange="+e.getName());
                setValue(e.getName() + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
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
        res.setValue("exchangeType", e.getType());
        res.setValue("autoDelete", e.isAutoDelete());
        return res;
    }

    @Override
    public Log getLog() {
        return logger;
    }
}

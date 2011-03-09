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

import java.util.Properties;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitExchange;

/**
 * ExchangeCollector
 * @author Helena Edelson
 */
public class ExchangeCollector extends RabbitMQDefaultCollector {

    private static final Log logger = LogFactory.getLog(ExchangeCollector.class);

    public void collect(HypericRabbitAdmin rabbitAdmin) {
        Properties props = getProperties();
        String vhost = (String) props.get(MetricConstants.VHOST);
        String exch = (String) props.get(MetricConstants.EXCHANGE);
        if (exch == null) {
            exch = "";
        }
        if (logger.isDebugEnabled()) {
            String node = (String) props.get(MetricConstants.NODE);
            logger.debug("[collect] exch='" + exch + "' vhost='" + vhost + "' node='" + node + "'");
        }

        try {
            RabbitExchange e = rabbitAdmin.getExchange(vhost, exch);
            setAvailability(true);
            if (e.getMessageStatsIn() != null) {
                setValue("in_publish_details", e.getMessageStatsIn().getPublishDetails().get("rate"));
            } else {
                setValue("in_publish_details", 0);
            }
            if (e.getMessageStatsOut() != null) {
                setValue("out_publish_details", e.getMessageStatsOut().getPublishDetails().get("rate"));
            } else {
                setValue("out_publish_details", 0);
            }

        } catch (Exception ex) {
            setAvailability(false);
            logger.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return logger;

    }
}

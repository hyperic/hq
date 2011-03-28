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
import org.hyperic.hq.plugin.rabbitmq.core.RabbitConnection;

import java.util.List;
import java.util.Properties;
import org.hyperic.hq.product.Metric;
import org.hyperic.util.config.ConfigResponse;

/**
 * ConnectionCollector
 * @author Helena Edelson
 */
public class ConnectionCollector extends RabbitMQDefaultCollector {

    private static final Log logger = LogFactory.getLog(ConnectionCollector.class);

    public void collect(HypericRabbitAdmin rabbitAdmin) {
        Properties props = getProperties();
        String cName = props.getProperty(MetricConstants.CONNECTION);
        logger.debug("[collect] ConnectionName=" + cName);

        try {
            logger.debug("[collect] RabbitConnection=" + cName);
            RabbitConnection conn = rabbitAdmin.getConnection(cName);
            setValue("Availability", Metric.AVAIL_UP);
            setValue("packetsReceived", conn.getRecvCnt());
            setValue("packetsSent", conn.getSendCnt());
            setValue("channelCount", conn.getChannels());
            setValue("octetsReceived", conn.getRecvOct());
            setValue("octetsSent", conn.getSendOct());
            setValue("pendingSends", conn.getSendPend());
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

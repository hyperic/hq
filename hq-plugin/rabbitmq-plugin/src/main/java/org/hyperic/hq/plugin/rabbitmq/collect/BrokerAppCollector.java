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
import org.hyperic.util.config.ConfigResponse; 
import org.springframework.erlang.core.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitAppCollector
 *
 * @author Helena Edelson
 */
public class BrokerAppCollector extends Collector {
 
    private static final Log logger = LogFactory.getLog(BrokerAppCollector.class);

    @Override
    public void collect() {
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        if (rabbitGateway != null) {

            try {
                List<Application> apps = rabbitGateway.getRunningApplications();
                if (apps != null) {
                    for (Application a : apps) { 
                        setAvailability(true);
                    }
                }
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * Assemble custom key/value data for each object to set
     * as custom properties in the ServiceResource to display
     * in the UI.
     * @param app
     * @return
     */
    public static ConfigResponse getAttributes(Application app) {
        ConfigResponse res = new ConfigResponse();
        res.setValue("name", app.getId());
        res.setValue("description", app.getDescription());
        res.setValue("version", app.getVersion());
        return res;
    }


}

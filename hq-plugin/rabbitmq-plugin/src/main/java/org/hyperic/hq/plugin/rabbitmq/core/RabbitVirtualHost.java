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
package org.hyperic.hq.plugin.rabbitmq.core;

import org.hyperic.hq.plugin.rabbitmq.collect.MetricConstants;
import org.hyperic.util.config.ConfigResponse;

/**
 * RabbitVirtualHost
 * @author Helena Edelson
 */
public class RabbitVirtualHost implements RabbitObject {

    private String name;

    public RabbitVirtualHost() {
    }

    public RabbitVirtualHost(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RabbitVirtualHost{name=" + getName() + "}";
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getServiceType() {
        return AMQPTypes.VIRTUAL_HOST;
    }

    public String getServiceName() {
        return getServiceType() + " " + getName();
    }

    public ConfigResponse getProductConfig() {
        ConfigResponse c = new ConfigResponse();
        c.setValue(MetricConstants.VHOST, getName());
        return c;
    }

    public ConfigResponse getCustomProperties() {
        ConfigResponse c = new ConfigResponse();
        return c;
    }

    public boolean isDurable() {
        return true;
    }
}

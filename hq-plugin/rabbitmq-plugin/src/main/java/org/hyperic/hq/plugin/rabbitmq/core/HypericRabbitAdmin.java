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

import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * A HypericRabbitAdmin is created for each node/virtualHost.
 * HypericRabbitAdmin
 * @author Helena Edelson
 */
public class HypericRabbitAdmin {

    private static final Log logger = LogFactory.getLog(HypericRabbitAdmin.class);

    public HypericRabbitAdmin(Properties props) {
    }

    public HypericRabbitAdmin(ConfigResponse props) {
        this(props.toProperties());
    }

    public void destroy() {
        logger.debug("[HypericRabbitAdmin] destroy()");
    }

    public List<String> getVirtualHosts() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List getQueues(String virtualHost) {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List getExchanges(String virtualHost) {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List getBindings(String virtualHost) {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List<RabbitConnection> getConnections() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List<RabbitChannel> getChannels() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public boolean getStatus() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public String getPeerNodeName() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public boolean virtualHostAvailable(String virtualHost, String node) {
        throw new RuntimeException("XXXXXXXXXX");
    }
}

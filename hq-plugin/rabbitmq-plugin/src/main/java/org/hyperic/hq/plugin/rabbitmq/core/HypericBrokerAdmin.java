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

import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.exec.Os;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HypericBrokerAdmin
 * @author Helena Edelson
 */
public class HypericBrokerAdmin extends RabbitBrokerAdmin {

    /**
     * Constructor uses the Node's cookie and the node name such as:
     * 'rabbit_1' from where 'rabbit_1' refers to 'rabbit_1@vmhost'
     * @param connectionFactory
     * @param erlangCookie
     */
    public HypericBrokerAdmin(ConnectionFactory connectionFactory, String erlangCookie, String peerNodeName) {
        super(connectionFactory);

        Assert.notNull(connectionFactory, this.getClass().getSimpleName() + ": connectionFactory must not be null.");
        Assert.notNull(erlangCookie, this.getClass().getSimpleName() + ": erlangCookie must not be null.");
        Assert.notNull(peerNodeName, this.getClass().getSimpleName() + ": peerNodeName must not be null.");

        initializeDefaultErlangTemplate(new RabbitTemplate(connectionFactory), erlangCookie, peerNodeName);
    }

    /**
     * Uses the Node's cookie to create ConnectionFactory
     * Note: from Hyperic this can be rabbit_3@vmhost
     * Before: String peerNodeName = "rabbit@" + host
     * @param rabbitTemplate
     * @param erlangCookie
     * @param peerNodeName
     */
    public void initializeDefaultErlangTemplate(RabbitTemplate rabbitTemplate, String erlangCookie, String peerNodeName) {
        String validatedPeerNodeName = getValidatedPeerNodeName(rabbitTemplate.getConnectionFactory().getHost(), peerNodeName);
        logger.debug("Using peer node name: " + validatedPeerNodeName);

        SingleConnectionFactory otpCf = new SingleConnectionFactory("rabbit-spring-monitor", erlangCookie, peerNodeName);
        otpCf.afterPropertiesSet();
        createErlangTemplate(otpCf);
    }

    private String getValidatedPeerNodeName(String hostname, String peerNodeName) {
        if (Os.isFamily("windows")) {
            Pattern p = Pattern.compile("([^@]+)@");
            Matcher m = p.matcher(peerNodeName);
             /** Prefix could be rabbit or something like rabbit_3 vs rabbit */
            String prefix = m.find() ? m.group(1) : null;
            return prefix + hostname.toUpperCase();
        }
        else {
            return peerNodeName;
        }
    }

}

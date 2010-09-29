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
import org.springframework.amqp.rabbit.admin.RabbitControlErlangConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.erlang.connection.SimpleConnectionFactory;
import org.springframework.erlang.core.ErlangTemplate;
import org.springframework.util.Assert;

/**
 * HypericBrokerAdmin
 * @author Helena Edelson
 */
public class HypericBrokerAdmin extends RabbitBrokerAdmin {


    private RabbitTemplate rabbitTemplate;

	private RabbitAdmin rabbitAdmin;

	private ErlangTemplate erlangTemplate;

	private String virtualHost;



    public HypericBrokerAdmin(ConnectionFactory connectionFactory, String erlangCookie) {
        super(connectionFactory);

        initializeDefaultErlangTemplate(new RabbitTemplate(connectionFactory), erlangCookie);
    }
    
    public void initializeDefaultErlangTemplate(RabbitTemplate rabbitTemplate, String erlangCookie) {
        String peerNodeName = "rabbit@" + rabbitTemplate.getConnectionFactory().getHost();

        if (erlangCookie == null) {
            throw new IllegalArgumentException("Erlang cookie for " + peerNodeName + " must not be null.");
        }

        logger.debug("Creating jinterface connection with peerNodeName = [" + peerNodeName + "]");
        SimpleConnectionFactory otpCf = new SimpleConnectionFactory("rabbit-spring-monitor", erlangCookie, peerNodeName);

        Assert.notNull(otpCf, this.getClass().getSimpleName() + ".SimpleConnectionFactory must not be null.");

        otpCf.afterPropertiesSet();
        createErlangTemplate(otpCf);

        Assert.notNull(this.getErlangTemplate(), this.getClass().getSimpleName() + ".ErlangTemplate must not be null.");
    }

}

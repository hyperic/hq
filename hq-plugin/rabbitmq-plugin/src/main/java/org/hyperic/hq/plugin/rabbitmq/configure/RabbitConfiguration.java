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
package org.hyperic.hq.plugin.rabbitmq.configure;

import org.hyperic.hq.plugin.rabbitmq.core.*;

import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

/**
 * RabbitConfiguration is a prototype configuration bean:
 * one for each node/virtualHost.
 * @author Helena Edelson
 */
public class RabbitConfiguration {
      
    @Autowired
    private Configuration configuration;

    @Autowired
    private ConfigurationManager configurationManager;

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory ccf = new CachingConnectionFactory(configuration.getHostname());
        ccf.setUsername(configuration.getUsername());
        ccf.setPassword(configuration.getPassword());
        ccf.setVirtualHost(configuration.getVirtualHost());
        return ccf;
    }

    @Bean
    public RabbitBrokerAdmin rabbitBrokerAdmin() {
        return new HypericBrokerAdmin(connectionFactory(), configuration.getAuthentication());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public ErlangConverter erlangConverter() {
        return new HypericErlangConverter(rabbitBrokerAdmin().getErlangTemplate());
    }

    @Bean
    public RabbitGateway rabbitGateway() {
        return new RabbitBrokerGateway(rabbitTemplate(), rabbitBrokerAdmin(), erlangConverter());
    }

}
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
package org.hyperic.hq.plugin.rabbitmq.configure;

import org.hyperic.hq.plugin.rabbitmq.core.RabbitBrokerGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitBrokerManager;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;

/**
 * PluginConfiguration
 * @author Helena Edelson
 */
@ImportResource("classpath:/etc/rabbitmq-context.xml")
public class PluginConfiguration {

    @Autowired
    private Configuration configuration;

    @Bean
    public com.rabbitmq.client.ConnectionFactory targetConnectionFactory() {
        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        cf.setHost(configuration.getHostname());
        cf.setUsername(configuration.getUsername());
        cf.setPassword(configuration.getPassword());
        return cf;
    }
    
    @Bean 
    public RabbitGateway rabbitGateway() throws PluginException {
        return new RabbitBrokerGateway(configuration);
        //return new RabbitBrokerGateway(configuration, targetConnectionFactory());
    }

    @Bean
    public RabbitManager rabbitManager() throws PluginException {
        return new RabbitBrokerManager(rabbitGateway());
    }

}

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
 
import java.util.List;

/**
 * BrokerServiceGateway
 * @author Helena Edelson
 */
public class RabbitConfigurationManager implements ConfigurationManager {

    private static final Log logger = LogFactory.getLog(PluginContextCreator.class);
    
    @Autowired
    private Configuration configuration;

    private CachingConnectionFactory connectionFactory;

    private RabbitBrokerAdmin rabbitBrokerAdmin;

    private RabbitTemplate rabbitTemplate;

    private RabbitGateway rabbitGateway;

    private ErlangConverter erlangConverter;

    private volatile List<String> virtualHosts;

    private volatile boolean active;

    private int port;

    public void initialize() throws PluginException {

        if (configuration.isConfigured()) {
            this.connectionFactory = new CachingConnectionFactory(configuration.getHostname());
            this.connectionFactory.setUsername(configuration.getUsername());
            this.connectionFactory.setPassword(configuration.getPassword());
            this.connectionFactory.setChannelCacheSize(10);
            Assert.notNull(connectionFactory, "connectionFactory must not be null.");

            this.rabbitBrokerAdmin = new HypericBrokerAdmin(connectionFactory, configuration.getAuthentication());
            Assert.notNull(rabbitBrokerAdmin, "rabbitBrokerAdmin must not be null.");

            this.rabbitTemplate = new RabbitTemplate(connectionFactory);
            Assert.notNull(rabbitTemplate, "rabbitTemplate must not be null.");

            this.erlangConverter = new HypericErlangConverter(rabbitBrokerAdmin.getErlangTemplate());
            Assert.notNull(erlangConverter, "erlangConverter must not be null.");

            this.rabbitGateway = new RabbitBrokerGateway(rabbitTemplate, rabbitBrokerAdmin, erlangConverter);
            Assert.notNull(rabbitGateway, "rabbitGateway must not be null.");

            this.active = rabbitGateway.getRabbitStatus() != null; 
        }
    }

    public CachingConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public RabbitGateway getRabbitGateway() {
        return rabbitGateway;
    }

    public RabbitBrokerAdmin getRabbitBrokerAdmin() {
        return rabbitBrokerAdmin;
    }

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    public ErlangConverter getErlangConverter() {
        return erlangConverter;
    }
 
    /**
     * make these cleaner
     */
    public void configureUsernamePassword(ConfigResponse conf) {
        Assert.hasText(conf.getValue(DetectorConstants.USERNAME), connectionFactory + ": username must not be null");
        this.connectionFactory.setUsername(conf.getValue(DetectorConstants.USERNAME));

        Assert.hasText(conf.getValue(DetectorConstants.PASSWORD), connectionFactory + ": password must not be null");
        this.connectionFactory.setPassword(conf.getValue(DetectorConstants.PASSWORD));
    }

    public void configureVirtualHost(ConfigResponse conf) {
        Assert.hasText(conf.getValue(DetectorConstants.VIRTUAL_HOST), connectionFactory + ": virtualHost must not be null");
        this.connectionFactory.setVirtualHost(conf.getValue(DetectorConstants.VIRTUAL_HOST));
    }

    public void configurePort(ConfigResponse conf) {
        this.connectionFactory.setPort(Integer.valueOf(conf.getValue(DetectorConstants.PORT)));
    }

    public void configureBrokerAdmin() {

    }


    public boolean isActive() {
        return active;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}

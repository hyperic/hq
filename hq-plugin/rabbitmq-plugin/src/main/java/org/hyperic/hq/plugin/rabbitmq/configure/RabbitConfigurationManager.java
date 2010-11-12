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

import com.rabbitmq.client.Connection;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.validate.ConfigurationValidator;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This does not currently but should also control hqapi calls
 * to remove resources from inventory when no longer existing in the broker.
 * RabbitConfigurationManager
 * @author Helena Edelson
 * @deprecated
 */
public class RabbitConfigurationManager implements ConfigurationManager, DisposableBean {

    private static final Log logger = LogFactory.getLog(RabbitConfigurationManager.class);

    private final Map<String, HypericRabbitAdmin> virtualHostsByNode = Collections.synchronizedMap(new HashMap<String, HypericRabbitAdmin>());

    private SingleConnectionFactory otpConnectionFactory;

    public RabbitConfigurationManager(Configuration key) {
        setSharedOtpConnectionFactory(key);
        setSharedRabbitAdminsForVirtualHosts(key);
        Assert.notNull(otpConnectionFactory, "otpConnectionFactory must not be null.");
        Assert.notEmpty(virtualHostsByNode, "virtualHostsByNode must not be empty.");
    }


    private void setSharedOtpConnectionFactory(Configuration key) {
        this.otpConnectionFactory = new SingleConnectionFactory("rabbit-monitor", key.getAuthentication(), key.getNodename());
        this.otpConnectionFactory.afterPropertiesSet();
    }

    private void setSharedRabbitAdminsForVirtualHosts(Configuration key) {
        if (key.getVirtualHost() == null) {
            key.setDefaultVirtualHost(true);
        }

        try {
            HypericRabbitAdmin rabbitAdmin = createVirtualHostForNode(key);

            if (isInitialized() && rabbitAdmin != null) {
                List<String> virtualHosts = rabbitAdmin.getVirtualHosts();
                createVirtualHostsForNode(virtualHosts, key);
            }
        }
        catch (PluginException e) {
            logger.error(e,e);
        }
    }

    public boolean isInitialized() {
        return virtualHostsByNode.size() > 0;
    }

    /**
     *
     * @throws Exception
     */
    public void destroy() throws PluginException {
        resetConfiguration();
    }

    /**
     * Create each config per virtual host.
     * @param virtualHosts
     * @param comparableKey
     * @throws PluginException
     */
    public void createVirtualHostsForNode(List<String> virtualHosts, Configuration comparableKey) throws PluginException {
        if (virtualHosts != null && comparableKey != null) {
            for (String virtualHost : virtualHosts) {
                comparableKey.setVirtualHost(virtualHost);
                createVirtualHostForNode(comparableKey);
            }
        }
    }

    /**
     * For a node, set the virtual host in the CCF so that each
     * RabbitBrokerGateway, RabbitAdmin, Converter etc is aligned with a specific
     * node/virtualHost so that we can do queries by virtual host.
     * @param key
     * @return
     * @throws PluginException
     */
    public HypericRabbitAdmin createVirtualHostForNode(Configuration key) throws PluginException {
//        if (isCandidate(key)) {
//            HypericRabbitAdmin admin = new HypericRabbitAdmin(otpConnectionFactory, key);
//            virtualHostsByNode.put(key.getVirtualHost(), admin);
//            return admin;
//        }
        return null;
    }

    /**
     * Insure is valid candidate for action
     * @param key
     * @return
     * @throws PluginException
     */
    private boolean isCandidate(Configuration key) throws PluginException {
        return  otpConnectionFactory != null && key != null
                && key.isConfigured() && !virtualHostsByNode.containsKey(key.getVirtualHost());
    }


    /**
     * @param key
     */
    public void removeVirtualHostForNode(String key) {
        if (key != null) synchronized (virtualHostsByNode) {
            if (virtualHostsByNode.containsKey(key)) {
                virtualHostsByNode.remove(key);
            }
        }
    }

    /**
     *
     * @param virtualHost
     * @return
     */
    public HypericRabbitAdmin getVirtualHostForNode(String virtualHost, String node) {
        if (virtualHostsByNode.containsKey(virtualHost)) {
            HypericRabbitAdmin hra = virtualHostsByNode.get(virtualHost);
            if (hra.getPeerNodeName().equalsIgnoreCase(node)) {
                return hra;
            }
        }
        return null;
    }

    /**
     * @return
     */
    public Map<String, HypericRabbitAdmin> getVirtualHostsForNode() {
        return virtualHostsByNode;
    }

    /**
     * Return to pre-initialized state for invalid configurations.
     */
    public void resetConfiguration() throws PluginException {
        try {
            if (virtualHostsByNode != null) {
                for(Map.Entry entry : virtualHostsByNode.entrySet()) {
                    removeVirtualHostForNode((String) entry.getKey());
                }
            }
            if (otpConnectionFactory != null) {
                otpConnectionFactory.destroy();
                otpConnectionFactory = null;
            }
        } catch (Throwable t) {
            throw new PluginException("Unable to reset all configuations.", t);
        }
    }
}

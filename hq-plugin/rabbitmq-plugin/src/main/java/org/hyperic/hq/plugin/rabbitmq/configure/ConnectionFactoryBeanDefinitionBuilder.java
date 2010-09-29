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

import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.Assert;

/**
 * ConnectionFactoryBeanDefinitionBuilder creates the BeanDefinition from initialization
 * config values to generate Spring SimpleConnectionFactory (RabbitMQ ConnectionFactory).
 *
 * @author Helena Edelson
 */
public class ConnectionFactoryBeanDefinitionBuilder {
 
    /**
     * Pre-test or (as below) internally test that the specific config
     * values have been populated by a user in the UI.
     *
     * @param config
     * @return true if ConfigResponse has values for host,username,password.
     */
    public static boolean hasConfigValues(ConfigResponse config) {
        return (config.getValue(DetectorConstants.HOST) != null) &&
                (config.getValue(DetectorConstants.USERNAME) != null) &&
                    (config.getValue(DetectorConstants.PASSWORD) != null) &&
                        (config.getValue(DetectorConstants.NODE_COOKIE_VALUE) != null);
    }

    /**
     * Create a BeanDefinition for org.springframework.amqp.rabbit.connection.SingleConnectionFactory
     * programmatically to pre-initialize pending dependent beans initialized normally by Spring.
     * </p>
     * Replace SingleConnectionFactory with CachingConnectionFactory when it's ready.
     *
     * @return org.springframework.beans.factory.config.BeanDefinition
     * @see org.springframework.amqp.rabbit.connection.SingleConnectionFactory
     * @see org.springframework.amqp.rabbit.connection.CachingConnectionFactory
     */
    public static GenericBeanDefinition build(ConfigResponse config) {
        GenericBeanDefinition beanDefinition = null;

        /** verify again, in case not previously verified by caller. */
        if (hasConfigValues(config)) {
            String host = config.getValue(DetectorConstants.HOST).trim();
            String username = config.getValue(DetectorConstants.USERNAME).trim();
            String password = config.getValue(DetectorConstants.PASSWORD).trim();
              
            Assert.hasText(host);
            Assert.hasText(username);
            Assert.hasText(password);
 
            beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(SingleConnectionFactory.class);

            ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
            constructorArgs.addGenericArgumentValue(host);
            beanDefinition.setConstructorArgumentValues(constructorArgs);

            MutablePropertyValues props = new MutablePropertyValues();
            props.addPropertyValue(DetectorConstants.USERNAME, username);
            props.addPropertyValue(DetectorConstants.PASSWORD, password);

            
            if (config.getValue(DetectorConstants.PORT) != null) {
                props.addPropertyValue(DetectorConstants.PORT, Integer.valueOf(config.getValue(DetectorConstants.PORT)));
            }

            beanDefinition.setPropertyValues(props);
        }

        return beanDefinition;
    }

}

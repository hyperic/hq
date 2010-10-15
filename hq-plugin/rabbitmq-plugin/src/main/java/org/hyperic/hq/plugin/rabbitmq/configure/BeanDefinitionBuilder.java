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

import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.HypericBrokerAdmin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.Assert;

/**
 * BeanDefinitionBuilder
 * @author Helena Edelson
 */
public class BeanDefinitionBuilder {

    public GenericBeanDefinition build(Class beanType, ConfigResponse conf, BeanDefinition beanDef) {
        if (beanType.isAssignableFrom(SingleConnectionFactory.class)) {
            return build(conf);
        }
        else if (beanType.isAssignableFrom(HypericBrokerAdmin.class) && beanDef!= null) {
            return build(conf, beanDef);
        }
        return null;
    }

    private GenericBeanDefinition build(ConfigResponse conf) {
        ConnectionFactoryBeanDefinitionBuilder builder = new ConnectionFactoryBeanDefinitionBuilder();
        return builder.build(conf);
    }

    private GenericBeanDefinition build(ConfigResponse conf, BeanDefinition beanDef) {
        BrokerAdminBeanDefinitionBuilder builder = new BrokerAdminBeanDefinitionBuilder();
        return builder.build(conf, beanDef);
    }

    /**
     * Create a BeanDefinition for org.springframework.amqp.rabbit.connection.SingleConnectionFactory
     * programmatically to pre-initialize pending dependent beans initialized normally by Spring.
     * </p>
     * Replace SingleConnectionFactory with CachingConnectionFactory when it's ready.
     * @see org.springframework.amqp.rabbit.connection.SingleConnectionFactory
     * @see org.springframework.amqp.rabbit.connection.CachingConnectionFactory
     */
    private class ConnectionFactoryBeanDefinitionBuilder {

        private GenericBeanDefinition build(ConfigResponse config) {
            String host = config.getValue(DetectorConstants.HOST);
            String username = config.getValue(DetectorConstants.USERNAME);
            String password = config.getValue(DetectorConstants.PASSWORD);

            Assert.hasText(host, "Connection: host must not be null");
            Assert.hasText(username, "Connection: username must not be null");
            Assert.hasText(password, "Connection: password must not be null");

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(SingleConnectionFactory.class);

            ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
            constructorArgs.addGenericArgumentValue(host, "String");
            beanDefinition.setConstructorArgumentValues(constructorArgs);

            MutablePropertyValues props = new MutablePropertyValues();
            props.addPropertyValue(DetectorConstants.USERNAME, username);
            props.addPropertyValue(DetectorConstants.PASSWORD, password);

            if (config.getValue(DetectorConstants.PORT) != null) {
                props.addPropertyValue(DetectorConstants.PORT, Integer.valueOf(config.getValue(DetectorConstants.PORT)));
            }

            beanDefinition.setPropertyValues(props);

            return beanDefinition;
        }
    }

    /**
     * Build BeanDefinition for HypericBrokerAdmin
     */
    private class BrokerAdminBeanDefinitionBuilder {

        private GenericBeanDefinition build(ConfigResponse conf, BeanDefinition beanDef) {
            if (conf.getValue(DetectorConstants.AUTHENTICATION) != null) {
                String auth = conf.getValue(DetectorConstants.AUTHENTICATION);
                Assert.hasText(auth, "ErlangConnection: cookie auth must not be null");

                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(HypericBrokerAdmin.class);

                ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
                constructorArgs.addIndexedArgumentValue(0, beanDef);
                constructorArgs.addIndexedArgumentValue(1, auth);
                beanDefinition.setConstructorArgumentValues(constructorArgs);

                return beanDefinition;
            }
            return null;
        }
    }
}
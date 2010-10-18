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
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * BeanDefinitionBuilder
 * @author Helena Edelson
 */
public class BeanDefinitionBuilder {

    public GenericBeanDefinition build(Class beanType, Configuration conf, BeanDefinition beanDef) {

        if (beanType.isAssignableFrom(CachingConnectionFactory.class)) {
            return build(beanType, conf.getHostname());
        }
        else if (beanType.isAssignableFrom(Configuration.class)) {
            return build(beanType, conf);
        }
        else if (beanType.isAssignableFrom(HypericBrokerAdmin.class) && beanDef != null) {
            return build(beanType, conf.getAuthentication(), beanDef);
        }
        return null;
    }

    public GenericBeanDefinition build(Class beanType) {
        GenericBeanDefinitionBuilder builder = new GenericBeanDefinitionBuilder();
        return builder.build(beanType);
    }

    private GenericBeanDefinition build(Class beanType, Configuration conf) {
        ConfigBeanDefinitionBuilder builder = new ConfigBeanDefinitionBuilder();
        return builder.build(beanType, conf);
    }

    private GenericBeanDefinition build(Class beanType, String host) {
        ConnectionFactoryBeanDefinitionBuilder builder = new ConnectionFactoryBeanDefinitionBuilder();
        return builder.build(beanType, host);
    }

    private GenericBeanDefinition build(Class beanType, String auth, BeanDefinition beanDef) {
        BrokerAdminBeanDefinitionBuilder builder = new BrokerAdminBeanDefinitionBuilder();
        return builder.build(beanType, auth, beanDef);
    }

    /**
     * Build BeanDefinition dynamically for beans with no dependencies to inject.
     */
    private class GenericBeanDefinitionBuilder {

        private GenericBeanDefinition build(Class beanType) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanType);
            return beanDefinition;
        }
    }
    
    /**
     * Build BeanDefinition for org.springframework.amqp.rabbit.connection.CachingConnectionFactory
     * to pre-initialize all pending dependent beans in the plugin context.
     * This BeanDefinition is created after host is set and before it is validated against the broker.
     * @see org.springframework.amqp.rabbit.connection.CachingConnectionFactory
     */
    private class ConnectionFactoryBeanDefinitionBuilder {

        private GenericBeanDefinition build(Class beanType, String hostName) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanType);
            ConstructorArgumentValues.ValueHolder vh = new ConstructorArgumentValues.ValueHolder(hostName, "java.lang.String", "hostName");

            ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
            constructorArgs.addGenericArgumentValue(vh);
            beanDefinition.setConstructorArgumentValues(constructorArgs);
            return beanDefinition;
        }
    }

    /**
     * Build BeanDefinition for HypericBrokerAdmin is created after
     * an erlang cookie value is set and validated against the broker.
     */
    private class BrokerAdminBeanDefinitionBuilder {

        private GenericBeanDefinition build(Class beanType, String auth, BeanDefinition beanDef) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanType);

            ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
            constructorArgs.addIndexedArgumentValue(0, beanDef);
            constructorArgs.addIndexedArgumentValue(1, auth);
            beanDefinition.setConstructorArgumentValues(constructorArgs);

            return beanDefinition;
        }
    }

    private class ConfigBeanDefinitionBuilder {

        private GenericBeanDefinition build(Class beanType, Configuration conf) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanType);

            MutablePropertyValues props = new MutablePropertyValues();
            props.add(DetectorConstants.HOST, conf.getHostname());
            props.add(DetectorConstants.AUTHENTICATION, conf.getAuthentication());
            props.add(DetectorConstants.USERNAME, conf.getUsername());
            props.add(DetectorConstants.PASSWORD, conf.getPassword());

            beanDefinition.setPropertyValues(props);

            return beanDefinition;
        }
    }
}
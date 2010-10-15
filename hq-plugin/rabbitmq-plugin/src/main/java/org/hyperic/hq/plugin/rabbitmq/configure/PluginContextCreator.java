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
import org.hyperic.hq.plugin.rabbitmq.core.HypericBrokerAdmin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * PluginContextCreator
 * ToDo make a parent class and interface for all
 * plugin usage.
 * @author Helena Edelson
 */
public class PluginContextCreator {

    private static final Log logger = LogFactory.getLog(PluginContextCreator.class);

    private static volatile AbstractApplicationContext applicationContext;

    public static boolean isInitialized() {
        return applicationContext != null && applicationContext.isActive();
    }
    /**
     * This is only called by the Product plugin. This method is so that the caller knows nothing
     * about the ApplicationContext api itself, since in the case we only have one call.
     * @param requiredType
     * @return
     */
    public static <T> T getBean(Class<T> requiredType) {
        try {
            return applicationContext.getBean(requiredType);
        }
        catch (Throwable t) {
            logger.error(t);
        }
        return null;
    }

    /**
     * Returns the ApplicationContext.
     * @param conf
     * @param annotationConfigurations
     * @return org.springframework.context.ApplicationContext
     */
    public static AbstractApplicationContext createContext(ConfigResponse conf, Class[] annotationConfigurations) {
        return applicationContext == null ? applicationContext = doCreateApplicationContext(conf, annotationConfigurations) : applicationContext;
    }

    /**
     * Create the default ApplicationContext type.
     * @param conf
     * @param annotationConfigurations
     * @return org.springframework.context.ApplicationContext
     */
    protected static AbstractApplicationContext doCreateApplicationContext(ConfigResponse conf, Class[] annotationConfigurations) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerShutdownHook();

        BeanDefinitionBuilder beanDefinitionBuilder = new BeanDefinitionBuilder();
        GenericBeanDefinition connFactoryBeanDef = beanDefinitionBuilder.build(SingleConnectionFactory.class, conf, null);
        DynamicBeanConfigurer.registerBean(connFactoryBeanDef, (DefaultListableBeanFactory) ctx.getBeanFactory());
        GenericBeanDefinition adminBeanDef = beanDefinitionBuilder.build(HypericBrokerAdmin.class, conf, connFactoryBeanDef);
        DynamicBeanConfigurer.registerBean(adminBeanDef, (DefaultListableBeanFactory) ctx.getBeanFactory());

        ctx.register(annotationConfigurations);
        ctx.refresh();
        return ctx;
    }

}

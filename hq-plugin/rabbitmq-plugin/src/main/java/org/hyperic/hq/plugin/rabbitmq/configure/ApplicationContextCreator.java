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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;


/**
 * ApplicationContextCreator
 * @author Helena Edelson
 */
public class ApplicationContextCreator implements SmartLifecycle {

    private static Log logger = LogFactory.getLog(ApplicationContextCreator.class);

    private static AbstractApplicationContext applicationContext;

    private static AbstractApplicationContext childApplicationContext;

    /**
     * Returns the ApplicationContext.
     * @param beanDefinition
     */
    public static ApplicationContext createApplicationContext(GenericBeanDefinition beanDefinition) {
        return applicationContext == null ? applicationContext = doCreateApplicationContext(beanDefinition) : applicationContext;
    }

    /**
     * Returns the default ApplicationContext type.
     * @param beanDefinition
     * @return org.springframework.context.ApplicationContext
     */
    private static AbstractApplicationContext doCreateApplicationContext(GenericBeanDefinition beanDefinition) {
        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:/etc/*-context.xml");
        applicationContext.registerShutdownHook();
        postProcessAfterCreation(beanDefinition, applicationContext);

        return applicationContext;
    }

    /**
     * Programmatically pre-register ConnectionFactory in Spring before all dependent beans.
     * @see RabbitConfiguration
     * @param beanDefinition
     * @param applicationContext
     */ 
    private static void postProcessAfterCreation(GenericBeanDefinition beanDefinition, ConfigurableApplicationContext applicationContext) {
        if (beanDefinition == null) return;

        DynamicSpringBeanConfigurer.registerBean(beanDefinition, (DefaultListableBeanFactory) applicationContext.getBeanFactory());
        Assert.notNull(applicationContext.getBean(SingleConnectionFactory.class), "SingleConnectionFactory must not be null.");
     }

    public static AbstractApplicationContext getChildApplicationContext() {
        return childApplicationContext;
    }

    public static void setChildApplicationContext(AbstractApplicationContext childApplicationContext) {
        ApplicationContextCreator.childApplicationContext = childApplicationContext;
    }

    /** Implement  of SmartLifecycle */
    public boolean isAutoStartup() {
        return false;
    }

    public void stop(Runnable callback) {

    }

    public int getPhase() {
        return 0;
    }

    public void start() {
        logger.info("Starting " + applicationContext);
    }

    /**
     * Just in case.
     */    
    public void stop() {
        if (childApplicationContext != null && childApplicationContext.isRunning()) {
            childApplicationContext.stop();
        }
        if (applicationContext != null && isRunning()) {
            applicationContext.stop();
        }
    }

    public boolean isRunning() {
        return applicationContext.isRunning();
    }
}

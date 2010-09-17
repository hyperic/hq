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
  
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
 

/**
 * PluginBeanPostProcessor
 *
 * @author Helena Edelson
 */
@Component
public class PluginBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private AbstractApplicationContext applicationContext;
 
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException { 
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {  
        if (bean instanceof SingleConnectionFactory) {
            registerChildContextBeans(); 
        }

        return bean;
    }

    /**
     * Create child context of type that supports hot refreshes.
     * Set the parent context, then register beans with dependencies
     * that had to be programmatically created due to Hyperic
     * resource and configuration availability.
     */
    private void registerChildContextBeans() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.setParent(applicationContext);
        ctx.register(RabbitConfiguration.class);
        ctx.refresh();
        /** temporary */
        ApplicationContextCreator.setChildApplicationContext(ctx);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (AbstractApplicationContext) applicationContext;
    }
}
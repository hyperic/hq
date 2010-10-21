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
import org.hyperic.hq.product.PluginException; 
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;


/**
 * PluginContextCreator handles, in this plugin, a pretty complicated
 * use case for initialization of Spring beans within the context of
 * timing of plugin class instantiation order and what data is available when
 * in relation to that, as well as in relation to Spring AMQP api.
 * @author Helena Edelson
 */
public class PluginContextCreator {

    private static final Log logger = LogFactory.getLog(PluginContextCreator.class);

    private static volatile AbstractApplicationContext applicationContext;


    /**
     * This is only called by the Product plugin. This method is so that the caller knows nothing
     * about the ApplicationContext api itself, since in the case we only have one call.
     * @param requiredType
     * @return
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static <T> T getBean(Class<T> requiredType) throws PluginException {
        try {
            return applicationContext.getBean(requiredType);
        }
        catch (NoSuchBeanDefinitionException e) {
            throw new PluginException(e.getMessage());
        }
    }

    /**
     * Create and set the ApplicationContext.
     * @param configuration
     * @return initialized context if operation successful, else null
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static void createContext(Configuration configuration) throws PluginException {
        if (applicationContext == null) {
            applicationContext = doCreateApplicationContext(configuration);
        }
    }

    /**
     * Create the default ApplicationContext.
     * @param conf
     * @return org.springframework.context.support.AbstractApplicationContext
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    protected static AbstractApplicationContext doCreateApplicationContext(Configuration conf) throws PluginException {
        AnnotationConfigApplicationContext ctx = null;

        try {

            ctx = new AnnotationConfigApplicationContext();
            ctx.registerShutdownHook();
   
            GenericBeanDefinition configProcessorBeanDef = new BeanDefinitionBuilder().build(Configuration.class, conf);
            DynamicBeanConfigurer.registerBean(configProcessorBeanDef, (DefaultListableBeanFactory) ctx.getBeanFactory());
            
            ctx.register(PluginConfiguration.class);
            ctx.refresh();
        }
        catch (BeansException e) {
            logger.error(e.getMessage());
            ctx.close(); 
        }

        return ctx;
    }

    /**
     * Do the programmatically-created beans exist plus one from the @Configuration class.
     * Note: the context can be successfully initialized with invalid username/password for
     * the ConnectionFactory because we init with the default and alert the user post validation
     * that they need to configure these properly.
     * @return true if initialized
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static boolean isInitialized() throws PluginException {
        return applicationContext != null && getBean(Configuration.class) != null;
    }

    /**
     * Do graceful shutdown and close on demand if needed.
     */
    public static void shutdown() {
        if (applicationContext != null) applicationContext.close();
    }

}

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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.Assert;
 
import java.util.UUID;

/**
 * DynamicSpringBeanConfigurer dynamically configures and registers objects as Spring Beans
 * where programmatic BeanDefinition generation and bean registration is called for.
 * @author Helena Edelson
 */
public class DynamicSpringBeanConfigurer {

    /**
     * Create a new BeanDefinition for an anonymous bean with no dependencies.
     * Sets the bean name as camelCase of the candidate's simple name + a unique
     * identifier tag.
     * @param candidate
     * @param beanFactory
     */
    public static void createAndRegisterBean(Class candidate, DefaultListableBeanFactory beanFactory) {
        Assert.notNull(candidate, "candidate must not be null.");

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(candidate);
        registerBean(generateBeanName(candidate, true), beanDefinition, beanFactory);
    }

    /**
     * Register a pre-defined BeanDefinition with a camelCase bean name
     * of the beanDefinition's class simple name. Useful for singleton beans 
     * where the id/name must be known.
     * @param beanDefinition
     * @param beanFactory
     */
    public static void registerBean(GenericBeanDefinition beanDefinition, DefaultListableBeanFactory beanFactory) {
        registerBean(generateBeanName(beanDefinition.getBeanClass(), false), beanDefinition, beanFactory);
    }

    /**
     * Register a BeanDefinition by beanName in the Spring application context dynamically.
     * @param beanName
     * @param beanDefinition
     * @param beanFactory
     */
    public static void registerBean(String beanName, BeanDefinition beanDefinition, DefaultListableBeanFactory beanFactory) {
        Assert.hasText(beanName, "beanName must not be null.");
        Assert.notNull(beanDefinition, "beanDefinition must not be null.");
        Assert.notNull(beanFactory, "beanFactory must not be null.");

        try {
            beanFactory.registerBeanDefinition(beanName, beanDefinition);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to get ConfigurableListableBeanFactory", e);
        }
    }

    /**
     * Derive bean name from class simple name. If anonymous, add unique id tag.
     * @param candidate
     * @param isAnonymous
     * @return
     */
    private static String generateBeanName(Class candidate, boolean isAnonymous) {
        String tmp = candidate.getSimpleName();
        String replace = String.valueOf(tmp.charAt(0));
        String beanName = StringUtils.replace(tmp, replace, replace.toLowerCase());
        return isAnonymous ? new StringBuilder().append(beanName).append("-").append(UUID.randomUUID()).toString() : beanName;
    }                                                                

}

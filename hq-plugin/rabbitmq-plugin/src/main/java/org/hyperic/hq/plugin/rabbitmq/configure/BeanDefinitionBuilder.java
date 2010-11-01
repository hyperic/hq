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
import org.springframework.beans.MutablePropertyValues; 
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * BeanDefinitionBuilder
 * @author Helena Edelson
 */
public class BeanDefinitionBuilder {

    public GenericBeanDefinition build(Class beanType) {
        GenericBeanDefinitionBuilder builder = new GenericBeanDefinitionBuilder();
        return builder.build(beanType);
    }

    public GenericBeanDefinition build(Class beanType, Configuration conf) {
        ConfigurationBeanDefinitionBuilder builder = new ConfigurationBeanDefinitionBuilder();
        return builder.build(beanType, conf);
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

    private class ConfigurationBeanDefinitionBuilder {

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
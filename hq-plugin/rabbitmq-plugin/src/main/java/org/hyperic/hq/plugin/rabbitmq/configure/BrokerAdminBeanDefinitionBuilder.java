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
import org.hyperic.hq.plugin.rabbitmq.core.RabbitUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.Assert;

/**
 * BrokerAdminBeanDefinitionBuilder
 * @author Helena Edelson
 */
public class BrokerAdminBeanDefinitionBuilder {

    public static GenericBeanDefinition build(ConfigResponse conf, GenericBeanDefinition cf) {
        GenericBeanDefinition beanDefinition = null;

        String nodeCookie = null;
        if (conf.getValue(DetectorConstants.NODE_COOKIE_VALUE) != null) {
            nodeCookie = conf.getValue(DetectorConstants.NODE_COOKIE_VALUE);
        } else {
            nodeCookie = RabbitUtils.configureCookie(conf);
        }

        if (nodeCookie != null && nodeCookie.length() > 0) {
            Assert.hasText(nodeCookie);

            beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(HypericBrokerAdmin.class);

            ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
            constructorArgs.addGenericArgumentValue(cf);
            constructorArgs.addGenericArgumentValue(nodeCookie);
            beanDefinition.setConstructorArgumentValues(constructorArgs);

        }

        return beanDefinition;
    }

}

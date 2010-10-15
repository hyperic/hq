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
package org.hyperic.hq.plugin.rabbitmq.validate;

import org.hyperic.hq.plugin.rabbitmq.configure.PluginContextCreator;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitConfiguration;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin; 
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;


/**
 * ValidationTest
 * @author Helena Edelson
 */
@ContextConfiguration(loader = ValidationTestContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class ValidationTest {
     
    @Autowired
    ConfigResponse serviceConfig;

    @Test
    @ExpectedException(IllegalArgumentException.class) 
    public void noHost() throws PluginException {
        serviceConfig.setValue(DetectorConstants.HOST, null);

        PluginContextCreator.createContext(serviceConfig, new Class[]{RabbitConfiguration.class});
        RabbitGateway rabbitGateway = PluginContextCreator.getBean(RabbitGateway.class);
        assertNotNull(rabbitGateway);
    }


}

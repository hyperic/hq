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
package org.hyperic.hq.plugin.rabbitmq.detect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * RabbitServerDetectorTest
 *
 * @author Helena Edelson
 */
@Ignore("Until I setup a rabbit server for QA")
public class RabbitServerDetectorTest {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    protected static RabbitServerDetector rabbitServerDetector = new RabbitServerDetector();

    private Properties properties;

    @Before
    public void doBefore() throws PluginException {
        rabbitServerDetector.configure(createConfigResponse());
        this.properties = rabbitServerDetector.getConfig().toProperties();
        assertNotNull(properties);
    }


    @Test
    /* todo */
    public void getServerResources() throws PluginException {
        rabbitServerDetector.getServerResources(createConfigResponse());
    }

    @Test
    /* todo */
    public void pluginData() {
        PluginData pluginData = rabbitServerDetector.getPluginData();
    }

    private ConfigResponse createConfigResponse() {
        ConfigResponse config = new ConfigResponse();
        config.setValue("host", "localhost");
        config.setValue("username", "guest");
        config.setValue("password", "guest");

        config.setValue("fqdn", "hedelson.local");
        config.setValue("platform.name", "hedelson.local");
        config.setValue("platform.type", "MacOSX");

        return config;
    }

}

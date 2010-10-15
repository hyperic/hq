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

import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;


/**
 * ValidationConfiguration
 * @author Helena Edelson
 */
@ImportResource("classpath:/org/hyperic/hq/plugin/rabbitmq/validate/ValidationTest-context.xml")
public class ValidationConfiguration {

    private
    @Value("${platform.type}")
    String platformType;

    private
    @Value("${hostname}")
    String hostname;

    private
    @Value("${username}")
    String username;

    private
    @Value("${password}")
    String password;

    @Bean
    public ConfigResponse serverConfig() {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, hostname);
        conf.setValue(DetectorConstants.USERNAME, username);
        conf.setValue(DetectorConstants.PASSWORD, password);
        conf.setValue(DetectorConstants.PLATFORM_TYPE, platformType);

        return conf;
    }

    @Bean
    public PluginValidator rabbitValidator() {
        return new PluginValidator();
    }
}

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

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.util.config.ConfigResponse;

/**
 * PluginValidator
 * @author Helena Edelson
 * @author German Laullon
 */
public class ConfigurationValidator {

    private static final Log logger = LogFactory.getLog(ConfigurationValidator.class);

    /**
     * Validate the cookie.
     * @param configuration
     * @return true if the test connection was successful.
     * @throws PluginException If cookie value or host are not set
     * or if the test connection fails, throw a PluginException to alert the user.
     */
    public synchronized static boolean isValidOtpConnection(Properties configuration) throws PluginException {
        return isValidOtpConnection(new ConfigResponse(configuration));
    }

    public synchronized static boolean isValidOtpConnection(ConfigResponse configuration) throws PluginException {
        logger.debug("Validating Erlang Cookie for OtpConnection with=" + configuration);
        HypericRabbitAdmin admin = new HypericRabbitAdmin(configuration);
        return admin.getStatus();
    }
}

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

import com.ericsson.otp.erlang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;
 

/**
 * PluginValidator
 * @author Helena Edelson
 */
public class PluginValidator {

    private static final Log logger = LogFactory.getLog(PluginValidator.class);

    /**
     * Validate against the broker.
     * @param template RabbitTemplate
     * @return true if successful connection is made, false if not.
     * @throws PluginException
     */
    public static boolean isValidRabbitConnection(RabbitTemplate template) throws PluginException {
        Assert.notNull(template, "template must not be null.");
        try {
            template.convertAndSend("Hyperic Test Message"); 
            return true;
        } catch (AmqpException e) {
            return false;
        }
    }

    /**
     * Validate the cookie.
     * @param configuration
     * @return true if the test connection was successful.
     * @throws PluginException If cookie value or host are not set
     *                         or if the test connection fails, throw a PluginException to alert the user.
     */
    public static boolean isValidOtpConnection(Configuration configuration) throws PluginException {
        logger.debug("isValidOtpConnection with=" + configuration);
        Object response = null;

        if (configuration.isConfiguredOtpConnection()) {
            OtpConnection conn = null;

            try {
                OtpSelf self = new OtpSelf("rabbit-spring-monitor", configuration.getAuthentication());
                OtpPeer peer = new OtpPeer(configuration.getNodename());
                conn = self.connect(peer);
                conn.sendRPC("erlang", "date", new OtpErlangList());
                response = conn.receiveRPC() != null;
                logger.debug("Received RPC " + response);
            }
            catch (Exception e) {
                throw new PluginException("Can not connect to peer node.");
            }
            finally {
                if (conn != null) conn.close();
            }
        }

        return response != null;
    }

}

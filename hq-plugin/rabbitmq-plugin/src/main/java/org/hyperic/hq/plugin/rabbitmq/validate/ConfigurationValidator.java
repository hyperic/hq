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
import com.rabbitmq.client.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.erlang.support.converter.ErlangConversionException;
import org.springframework.util.exec.Os;

import java.io.IOException;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * PluginValidator
 * @author Helena Edelson
 * @author German Laullon
 */
public class ConfigurationValidator {

    private static final Log logger = LogFactory.getLog(ConfigurationValidator.class);

    /**
     * Validate against the broker.
     * @return true if successful connection is made, false if not.
     * @throws PluginException
     */
    public static boolean isValidUsernamePassword(Configuration c) throws PluginException {
        SingleConnectionFactory cf = null;
        Connection con = null;
        boolean valid = false;

        try {
            if (c.isConfigured()) {
                cf = new SingleConnectionFactory(c.getHostname());
                cf.setUsername(c.getUsername());
                cf.setPassword(c.getPassword());

                con = cf.createConnection();
                valid = con != null;
            }
        } catch (SocketException se) {
        	throw new PluginException("Could not connect to the RabbitMQ broker", se);
        } catch (Exception e) {
            throw new PluginException("Username/password combination were not valid to connect to the RabbitMQ broker.", e);
        }
        finally {
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (IOException e) {
                    logger.error("Error closing connection: ", e);
                }
            }

            if (cf != null) {
                cf.destroy();
                cf = null;
            }
        }
        return valid;
    }

    /**
     * Validate the cookie.
     * @param configuration
     * @return true if the test connection was successful.
     * @throws PluginException If cookie value or host are not set
     * or if the test connection fails, throw a PluginException to alert the user.
     */
    public static boolean isValidOtpConnection(Configuration configuration) throws PluginException {
        logger.debug("Validating Erlang Cookie for OtpConnection with=" + configuration);

        if (!configuration.isConfiguredOtpConnection()) {
            throw new PluginException("Plugin is not configured with the Erlang cookie. Please insure" +
                    " the Agent has permission to read the cookie");
        }

        OtpConnection conn = null;

        try {
            OtpSelf self = new OtpSelf("rabbit-monitor", configuration.getAuthentication());
            OtpPeer peer = new OtpPeer(configuration.getNodename());
            conn = self.connect(peer);
            conn.sendRPC("rabbit_mnesia", "status", new OtpErlangList());
            OtpErlangObject response = conn.receiveRPC();
            return isNodeRunning(response, configuration.getNodename());
        }
        catch (Exception e) {
            throw new PluginException("Can not connect to peer node.",e);
        }
        finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public static boolean isNodeRunning(OtpErlangObject response, String peerNodeName) throws ErlangConversionException {
        long items = ((OtpErlangList) response).elements().length;
        if (items > 0 && response instanceof OtpErlangList) {

            for (OtpErlangObject outerList : ((OtpErlangList) response).elements()) {
                if (outerList instanceof OtpErlangTuple) {
                    OtpErlangTuple entry = (OtpErlangTuple) outerList;
                    String key = entry.elementAt(0).toString();
                    if (key.equals("running_nodes") && entry.elementAt(1) instanceof OtpErlangList) {
                        OtpErlangList value = (OtpErlangList) entry.elementAt(1);
                        return value.toString().contains(peerNodeName);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Prefix could be rabbit or something like rabbit_3 vs rabbit
     * @param hostname
     * @param peerNodeName
     * @return
     */
    public static String validatePeerNodeName(String hostname, String peerNodeName) {
        logger.debug("[validatePeerNodeName] hostname='" + hostname + "' peerNodeName='" + peerNodeName + "'");
        String res = peerNodeName;
        if (Os.isFamily("windows")) {
            Pattern p = Pattern.compile("([^@]+)@");
            Matcher m = p.matcher(peerNodeName);
            String prefix = m.find() ? m.group(1) : null;
            res = prefix + "@" + hostname.toUpperCase();
        }
        logger.debug("[validatePeerNodeName] new peerNodeName='" + res + "'");
        return res;
    }
}

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
import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.product.PluginException; 

import java.io.IOException;
import java.util.Properties;

/**
 * PluginValidator
 * @author Helena Edelson
 */
public class PluginValidator {

    private static final Log logger = LogFactory.getLog(PluginValidator.class);

    /**
     * ToDo isValidConnection(getHost(props), getUsername(props), getPassword(props))
     * @param props
     * @return true if all criteria are true.
     * @throws org.hyperic.hq.product.PluginException
     */
    public static boolean isValidConfiguration(Properties props) throws PluginException {
        return isConfigured(props) && isValidAuthentication(props);
    }
  
    /**
     * Validate host, username, password against the broker.
     * @param host
     * @param username
     * @param password
     * @return true if successful connection is made, false if not.
     * @throws PluginException
     */
    public static boolean isValidConnection(String host, String username, String password) throws PluginException {
        logger.debug("isValidConnection with=" + host + ", " + username + ", " + password);

        boolean isValid = false;

        Channel channel = null;
        com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        try {
            channel = connectionFactory.newConnection().createChannel();
            isValid = channel != null;
        }
        catch (java.net.ConnectException e) {
            throw new PluginException("Connection refused to using host " + host);
        }
        catch (java.io.IOException e) {
            throw new PluginException(new StringBuilder("Unable to authenticate with the broker using username: ")
                    .append(username).append(", password: *******").append(". Please re-enter the correct username and password").toString());
        }
        /* for pending factory.setPort(0) Can't assign requested address
        catch (java.net.NoRouteToHostException e) {
        }
        */
        finally {
            if (channel != null) {
                try {
                    channel.getConnection().close();
                } catch (IOException e) {
                    logger.debug(e);
                }
            }
        }

        logger.debug("isValidConnection=" + isValid);
        return isValid;
    }

    /**
     * Validate the cookie.
     * @param props
     * @return true if the test connection was successful.
     * @throws PluginException If cookie value or host are not set
     *                         or if the test connection fails, throw a PluginException to alert the user.
     */
    public static boolean isValidAuthentication(Properties props) throws PluginException {
        logger.debug("isValidAuthentication with=" + props);
        String authentication = getAuthentication(props);
        String host = getHost(props);
        boolean isValid = false;
        OtpConnection conn = null;

        try {
            OtpSelf self = new OtpSelf("rabbit-spring-monitor", authentication);
            OtpPeer peer = new OtpPeer("rabbit@" + host);
            conn = self.connect(peer);
            conn.sendRPC("erlang", "date", new OtpErlangList());
            isValid = conn.receiveRPC() != null;
            logger.debug("isValidAuthentication=" + isValid);
        }
        catch (Exception e) {
            throw new PluginException("Can not connect to peer node.");
        }
        finally {
            if (conn != null) conn.close();
        }
        logger.debug("isValidAuthentication=" + isValid);
        return isValid;
    }

    public static boolean hasValue(String value) {
        return value != null && value.length() > 0;
    }

    public static boolean isConfigured(Properties props) throws PluginException {

        /** 2 user-entered configuration values */
        if (!hasValue(getUsername(props)) && !hasValue(getPassword(props))) {
            throw new PluginException("This resource requires a username and password for the broker.");
        }

        if (!hasValue(getAuthentication(props))) {
            throw new PluginException("Erlang cookie value is not set yet.");
        }

        if (!hasValue(getHost(props))) {
            throw new PluginException("Host name must not be null.");
        }
        return true;
    }

    public static String getAuthentication(Properties props) {
        return props.getProperty(DetectorConstants.AUTHENTICATION);
    }

    public static String getHost(Properties props) {
        return props.getProperty(DetectorConstants.HOST);
    }

    public static String getUsername(Properties props) {
        return props.getProperty(DetectorConstants.USERNAME) != null ? props.getProperty(DetectorConstants.USERNAME).trim() : null;
    }

    public static String getPassword(Properties props) {
        return props.getProperty(DetectorConstants.PASSWORD) != null ? props.getProperty(DetectorConstants.PASSWORD).trim() : null;
    }


}

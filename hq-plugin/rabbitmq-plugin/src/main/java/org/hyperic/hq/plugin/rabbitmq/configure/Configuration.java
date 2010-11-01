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
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import java.util.Properties;

/**
 * Configuration
 * @author Helena Edelson
 */
public class Configuration {

    private String nodename;

    private String hostname;

    private String virtualHost;

    private String authentication;

    private String username;

    private String password;

    private int port;

    private String defaultVirtualHost = "/";
    
    @Override
    public String toString() {
        return new StringBuilder("[nodename=").append(nodename).append(" hostname=").append(hostname)
                .append(" virtualHost=").append(virtualHost).append(" authentication=").append(authentication)
                .append(" username=").append(username).append(" password=").append(password).append("]").toString();
    }

    public boolean isDefaultVirtualHost() {
        return this.virtualHost.equalsIgnoreCase(defaultVirtualHost);
    }

    /**
     * Explicitly set the virtual host as the default
     * when we initialize for the first time in order to
     * collect virtualHosts.
     * @param doSet
     */
    public void setDefaultVirtualHost(boolean doSet) {
        if (doSet && this.virtualHost == null) {
            this.virtualHost = defaultVirtualHost;
        } 
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public boolean isMatch(Configuration comparableKey) {
        return comparableKey != null && this.getVirtualHost().equalsIgnoreCase(comparableKey.getVirtualHost())
                && this.getNodename().equalsIgnoreCase(comparableKey.getNodename());
    }

    /**
     * Call before creating ApplicationContext
     * Log which one failed.
     * ToDo Not yet handling port, ran out of time
     * @return
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public boolean isConfigured() throws PluginException {
        if (nodename == null) {
            throw new PluginException("This resource requires the node name of the broker.");
        }

        if (username == null) {
            throw new PluginException("Please configure a username and password for the broker.");
        }

        if (password == null) {
            throw new PluginException("Please configure a username and password for the broker.");
        }

        if (authentication == null) {
            throw new PluginException("Erlang cookie value is not set yet. Please insure the Agent has permission to read the Erlang cookie.");
        }

        if (hostname == null) {
            throw new PluginException("Host name must not be null.");
        }

        return true;
    }

    /**
     * Call before validating OtpConnection
     * @return true if has values
     */
    public boolean isConfiguredOtpConnection() {
        return authentication != null && nodename != null;
    }

    /**
     * Call before creating HypericBrokerAdmin
     * @return true if has values
     */
    public boolean isConfiguredConnectionFactory() {
        return username != null && password != null && hostname != null;
    }

    public String getNodename() {
        return nodename;
    }

    public void setNodename(String nodename) {
        this.nodename = nodename != null && nodename.length() > 0 ? nodename.trim() : null;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname != null && hostname.length() > 0 ? hostname.trim() : null;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication != null && authentication.length() > 0 ? authentication.trim() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null && username.length() > 0 ? username.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password != null && password.length() > 0 ? password.trim() : null;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public static Configuration toConfiguration(Properties props) {
        Configuration conf = new Configuration();
        conf.setNodename(props.getProperty(DetectorConstants.SERVER_NAME));
        conf.setVirtualHost(props.getProperty(DetectorConstants.VIRTUALHOST));
        conf.setAuthentication(props.getProperty(DetectorConstants.AUTHENTICATION));
        conf.setHostname(props.getProperty(DetectorConstants.HOST));
        conf.setUsername(props.getProperty(DetectorConstants.USERNAME));
        conf.setPassword(props.getProperty(DetectorConstants.PASSWORD));

        if (props.getProperty(DetectorConstants.PORT) != null) {
            conf.setPort(Integer.parseInt(props.getProperty(DetectorConstants.PORT)));
        }

        return conf;
    }

    public static Configuration toConfiguration(ConfigResponse configResponse) {
        Configuration conf = new Configuration();
        conf.setNodename(configResponse.getValue(DetectorConstants.SERVER_NAME));
        conf.setVirtualHost(configResponse.getValue(DetectorConstants.VIRTUALHOST));
        conf.setAuthentication(configResponse.getValue(DetectorConstants.AUTHENTICATION));
        conf.setHostname(configResponse.getValue(DetectorConstants.HOST));
        conf.setUsername(configResponse.getValue(DetectorConstants.USERNAME));
        conf.setPassword(configResponse.getValue(DetectorConstants.PASSWORD));

        if (configResponse.getValue(DetectorConstants.PORT) != null) {
            conf.setPort(Integer.parseInt(configResponse.getValue(DetectorConstants.PORT)));
        }
        return conf;
    }


}

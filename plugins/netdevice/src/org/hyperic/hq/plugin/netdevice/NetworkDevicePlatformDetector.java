/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.netdevice;

import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;

import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.util.config.ConfigResponse;

public class NetworkDevicePlatformDetector extends PlatformDetector {

    private static final String PROP_VERSION = "Version";

    private Properties props;
    private boolean autoDefaults;

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        this.props = manager.getProperties();
        this.autoDefaults =
            "true".equals(this.props.getProperty("snmp.autoDefaults"));
        if (!this.autoDefaults) {
            //command-line -DsnmpCommunity=public
            this.autoDefaults =
                this.props.getProperty(SNMPClient.PROP_COMMUNITY) != null;
        }
    }

    private SNMPSession getSession(ConfigResponse config) {
        try {
            return NetworkDeviceDetector.getSession(config);
        } catch (PluginException e) {
            getLog().error(e.getMessage(), e);
            return null;
        }
    }

    private String getString(SNMPSession session, String oid) {
        try {
            Object obj = session.getSingleValue(oid); 
            if (obj == null) {
                return null;
            }
            String value = obj.toString();
            //XXX cisco has \r\n in description, strip out?
            return value;
        } catch (SNMPException e) {
            getLog().warn("Error getting '" + oid+ "': " +
                          e.getMessage());
            return null;
        }
    }

    //allow defaults to be configure in agent.properties like so:
    //snmpCommunity.192.168.1.102=MyCommunity
    //snmpVersion.192.168.1.102=v1
    //snmpPort.192.168.1.102=1611
    private String getIpProp(String key, String ip, String defVal) {
        String propDefault = this.props.getProperty(key, defVal);
        return this.props.getProperty(key + "." + ip, propDefault);
    }
    
    public PlatformResource getPlatformResource(ConfigResponse config)
        throws PluginException {

        String platformIp = config.getValue(ProductPlugin.PROP_PLATFORM_IP);
        //for command-line -DsnmpIp=x.x.x.x usage
        platformIp =
            getIpProp(SNMPClient.PROP_IP, platformIp, platformIp);

        String defaultVersion =
            getIpProp(SNMPClient.PROP_VERSION,
                      platformIp,
                      SNMPClient.VALID_VERSIONS[1]); //v2c
        String fallbackVersion = SNMPClient.VALID_VERSIONS[0]; //v1

        PlatformResource platform =
            super.getPlatformResource(config);

        Log log = getLog();
        ConfigResponse metricConfig;
        boolean hasConfig =
            config.getValue(SNMPClient.PROP_IP) != null;

        if (hasConfig) {
            //we've already been here
            metricConfig = config;
            if (log.isDebugEnabled()) {
                log.debug("Using approved snmp config=" + metricConfig);
            }
        }
        else if (this.autoDefaults) {
            //platform was just created, attempt to auto-configure
            metricConfig = new ConfigResponse();
            metricConfig.setValue(SNMPClient.PROP_IP, platformIp);
            metricConfig.setValue(SNMPClient.PROP_VERSION, defaultVersion);
            metricConfig.setValue(SNMPClient.PROP_COMMUNITY,
                                  getIpProp(SNMPClient.PROP_COMMUNITY,
                                            platformIp,
                                            SNMPClient.DEFAULT_COMMUNITY));
            metricConfig.setValue(SNMPClient.PROP_PORT,
                                  getIpProp(SNMPClient.PROP_PORT,
                                            platformIp,
                                            SNMPClient.DEFAULT_PORT_STRING));
            metricConfig.setValue(NetworkDeviceDetector.PROP_IF_IX,
                                  getIpProp(NetworkDeviceDetector.PROP_IF_IX,
                                            platformIp,
                                            NetworkDeviceDetector.IF_DESCR));
            if (log.isDebugEnabled()) {
                log.debug("Using default snmp config=" + metricConfig);
            }
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("Need user input for snmp config=" + config);
            }
            return platform;
        }

        ConfigResponse cprops = new ConfigResponse();

        SNMPSession session;

        if ((session = getSession(metricConfig)) == null) {
            return platform;
        }

        try {
            session.getSingleValue("sysName");
        } catch (SNMPException e) {
            getLog().debug("Unable to connect using " +
                           defaultVersion + ", trying version " +
                           fallbackVersion);
            metricConfig.setValue(SNMPClient.PROP_VERSION,
                                  fallbackVersion);
            if ((session = getSession(metricConfig)) == null) {
                return platform;
            }
        }

        String[] keys =
            getCustomPropertiesSchema().getOptionNames();

        for (int i=0; i<keys.length; i++) {
            String key = keys[i];
            if (Character.isUpperCase(key.charAt(0))) {
                continue; //not a mib name
            }
            String val = getString(session, key);
            if (val == null) {
                log.debug("'" + key + "'==null");
                continue;
            }
            cprops.setValue(key, val);
        }

        if (!hasConfig) {
            //should only happen when the platform is created
            config.merge(metricConfig, false);
            platform.setProductConfig(config);
            platform.setMeasurementConfig(new ConfigResponse());
            log.debug("Setting measurement config="+metricConfig);
        }
        
        String description = getString(session, "sysDescr");
        if (description != null) {
            platform.setDescription(description);

            boolean hasVersionCprop =
                getCustomPropertiesSchema().getOption(PROP_VERSION) != null;

            if (hasVersionCprop) {
                //this works for Cisco IOS at least
                StringTokenizer tok =
                    new StringTokenizer(description, " ,");

                while (tok.hasMoreTokens()) {
                    String s = tok.nextToken();
                    if (s.equalsIgnoreCase(PROP_VERSION) &&
                        tok.hasMoreTokens())
                    {
                        String version = tok.nextToken();
                        cprops.setValue(PROP_VERSION, version);
                        break;
                    }
                }
            }
        }

        platform.setCustomProperties(cprops);

        return platform;
    }
}

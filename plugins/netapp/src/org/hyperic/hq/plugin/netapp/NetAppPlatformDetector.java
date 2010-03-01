/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.netapp;

import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.netdevice.NetworkDevicePlatformDetector;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class NetAppPlatformDetector extends NetworkDevicePlatformDetector {

    private Log log = getLog();

    public PlatformResource getPlatformResource(ConfigResponse config) throws PluginException {
        log.debug("[getPlatformResource] config=" + config);
        String platformIp = config.getValue(ProductPlugin.PROP_PLATFORM_IP);

        config.setValue(SNMPClient.PROP_IP, platformIp);
        config.setValue(SNMPClient.PROP_VERSION, SNMPClient.VALID_VERSIONS[0]);
        config.setValue(SNMPClient.PROP_COMMUNITY, SNMPClient.DEFAULT_COMMUNITY);
        config.setValue(SNMPClient.PROP_PORT, SNMPClient.DEFAULT_PORT_STRING);

        PlatformResource res = super.getPlatformResource(config);
        return res;
    }
}

/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

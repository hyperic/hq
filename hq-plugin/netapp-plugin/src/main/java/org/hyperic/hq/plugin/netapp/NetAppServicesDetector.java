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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PlatformServiceDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SNMPDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class NetAppServicesDetector extends PlatformServiceDetector {

    private Log log = getLog();

    private SNMPSession getSession(ConfigResponse config) {
        SNMPSession res = null;
        try {
            config.setValue(SNMPClient.PROP_VERSION, SNMPClient.VALID_VERSIONS[0]);
            res = new SNMPClient().getSession(config);
        } catch (SNMPException e) {
            log.error("Error getting SNMP session: " + e.getMessage(), e);
        }
        return res;
    }

    public List getServerResources(ConfigResponse config) {
        List res = new ArrayList();
        log.debug("[getServerResources] netapp:'" + config.getValue(SNMPClient.PROP_IP) + "' config=" + config);
        if (config.getValue(SNMPClient.PROP_IP) != null) {
            ServerResource server = getServer(config);
            server.setProductConfig(config);
            res.add(server);
        }
        return res;
    }

    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] netapp:'" + config.getValue("snmpIp") + "' config=" + config);
        List extServices = new ArrayList();

        try {
            extServices.addAll(SNMPDetector.discoverServices(this, config, getSession(config)));
        } catch (PluginException e) {
            log.debug("Error discoverServices netapp:'" + config.getValue("snmpIp") + "'" + e.getMessage(), e);
        }

        ServiceResource s_cifs = new ServiceResource();
        s_cifs.setType(config.getValue("platform.type") + " CIFS Server");
        s_cifs.setServiceName("CIFS Server");
        s_cifs.setProductConfig();
        s_cifs.setMeasurementConfig();
        s_cifs.setResponseTimeConfig(new ConfigResponse());
        extServices.add(s_cifs);

        ServiceResource s_nfs_v3 = new ServiceResource();
        s_nfs_v3.setType(config.getValue("platform.type") + " NFS server v3");
        s_nfs_v3.setServiceName("NFS server v3");
        s_nfs_v3.setProductConfig();
        s_nfs_v3.setMeasurementConfig();
        s_nfs_v3.setResponseTimeConfig(new ConfigResponse());
        extServices.add(s_nfs_v3);

        ServiceResource s_nfs_v2 = new ServiceResource();
        s_nfs_v2.setType(config.getValue("platform.type") + " NFS server v2");
        s_nfs_v2.setServiceName("NFS server v2");
        s_nfs_v2.setProductConfig();
        s_nfs_v2.setMeasurementConfig();
        s_nfs_v2.setResponseTimeConfig(new ConfigResponse());
        extServices.add(s_nfs_v2);

        log.debug("[discoverServices] netapp:'" + config.getValue("snmpIp") + "' -> " + extServices.size() + " services");

        return extServices;
    }
}

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

package org.hyperic.hq.plugin.system;

import java.util.ArrayList;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServiceValue;

public class NetifDetector
    extends SystemServerDetector {

    protected String getServerType() {
        return SystemPlugin.NETWORK_SERVER_NAME;
    }

    protected ArrayList getSystemServiceValues(Sigar sigar, ConfigResponse config)
        throws SigarException {
        ArrayList services = new ArrayList();

        String[] ifNames = sigar.getNetInterfaceList();

        for (int i=0; i<ifNames.length; i++) {
            String name = ifNames[i];
            NetInterfaceConfig ifconfig;

            if (name.indexOf(':') != -1) {
                continue; //filter out virtual ips
            }

            try {
                ifconfig = sigar.getNetInterfaceConfig(name);
            } catch (SigarException e) {
                getLog().debug("getNetInterfaceConfig(" + name + "): " +
                               e.getMessage(), e);
                continue;
            }

            if (NetFlags.isAnyAddress(ifconfig.getAddress())) {
                continue;
            }

            long flags = ifconfig.getFlags();

            String type;

            if ((flags & NetFlags.IFF_UP) <= 0) {
                continue;
            }

            //FreeBSD has a handful of these by default 
            if ((flags & NetFlags.IFF_POINTOPOINT) > 0) {
                continue;
            }

            if ((flags & NetFlags.IFF_LOOPBACK) > 0) {
                type = "loopback";
            }
            else {
                type = ifconfig.getType().toLowerCase();
            }

            String info = "Network Interface " + name + " (" + type + ")";

            AIServiceValue svc = 
                createSystemService(SystemPlugin.NETWORK_INTERFACE_SERVICE,
                                    getFullServiceName(info),
                                    SystemPlugin.PROP_NETIF,
                                    name);

            String desc = ifconfig.getDescription();
            if (!desc.equals(name)) {
                //XXX only the case on windows
                svc.setDescription(desc);
            }

            ConfigResponse cprops = new ConfigResponse();

            cprops.setValue("mtu", String.valueOf(ifconfig.getMtu()));

            cprops.setValue("flags", NetFlags.getIfFlagsString(flags).trim());

            String mac = ifconfig.getHwaddr();
            if (!mac.equals(NetFlags.NULL_HWADDR)) {
                cprops.setValue("mac", mac);
            }

            String[][] addrs = {
                { "address", ifconfig.getAddress() },
                { "netmask", ifconfig.getNetmask() },
                { "broadcast", ifconfig.getBroadcast() },
            };

            for (int j=0; j<addrs.length; j++) {
                String key = addrs[j][0];
                String val = addrs[j][1];
                if (NetFlags.isAnyAddress(val)) {
                    continue;
                }
                cprops.setValue(key, val);
            }

            try {
                svc.setCustomProperties(cprops.encode());
            } catch (EncodingException e) {
                getLog().error("Error encoding cprops: " + e.getMessage());
            }
            
            services.add(svc);
        }

        return services;
    }
}

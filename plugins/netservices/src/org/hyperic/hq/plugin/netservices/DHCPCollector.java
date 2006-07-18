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

package org.hyperic.hq.plugin.netservices;

import java.io.IOException;
import java.net.InetAddress;

import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class DHCPCollector extends NetServicesCollector {

    private byte[] hwaddr;
    private InetAddress address;

    private String getDefaultHwaddr() throws PluginException {
        final String errmsg =
            "Unable to determine hardware address";
        
        Sigar sigar = new Sigar();
        try {
            NetInterfaceConfig ifconfig =
                sigar.getNetInterfaceConfig();
            String hwaddr = ifconfig.getHwaddr();

            setSource(ifconfig.getName() + "/" + hwaddr);
            return hwaddr;
        } catch (SigarException e) {
            throw new PluginException(errmsg + ": " + e.getMessage());
        } finally {
            sigar.close();
        }        
    }
    
    protected void init() throws PluginException {
        super.init();

        String address = getProperty("hwaddr");
        if (address == null) {
            address = getDefaultHwaddr();
        }
        else {
            setSource(address);
        }
        this.hwaddr = DHCPClient.decodeHwaddr(address);
        
        try {
            this.address =
                InetAddress.getByName(getHostname());
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void collect() {

        DHCPClient client = null;
        
        try {
            startTime();
            client = new DHCPClient(this.address, getPort());
            client.setTimeout(getTimeoutMillis());

            DHCPClient.Packet request, response;
            
            request = client.getDiscoverPacket(this.hwaddr);

            client.send(request);

            while (true) {
                response = client.receive();
                if (response.xid == request.xid) {
                    break;
                }
            }
            endTime();

            String msg =
                "Server " + response.getSaddress() +
                " offered address: " +
                response.getYaddress();
            setInfoMessage(msg);

            setAvailability(true);
        } catch (IOException e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}

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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.hyperic.util.HostIP;
import org.hyperic.util.config.ConfigResponse;

public class SigarPlatformDetector extends PlatformDetector {

    private static final String PROP_PLATFORM_IP_IGNORE =
        ProductPlugin.PROP_PLATFORM_IP + ".ignore";
    private static final String PROP_PLATFORM_IP_DISCOVER =
        ProductPlugin.PROP_PLATFORM_IP + ".discover";

    private String fqdn;
    private String ip;
    private boolean ipDiscover;
    private PluginManager manager;
    private Map ipIgnore = new HashMap();

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        Properties props = manager.getProperties();
        this.manager = manager;
        this.fqdn =
            props.getProperty(ProductPlugin.PROP_PLATFORM_FQDN);
        this.ip =
            props.getProperty(ProductPlugin.PROP_PLATFORM_IP);
        this.ipDiscover =
            !"false".equals(props.getProperty(PROP_PLATFORM_IP_DISCOVER));

        final String prop = "platform.networkConnected";
        String networkConnected = props.getProperty(prop);
        if ("false".equals(networkConnected)) {
            this.ipDiscover = false;
            getLog().warn(prop + " is deprecated, use " +
                          PROP_PLATFORM_IP_DISCOVER + "=false instead");
        }

        //allow filter via agent.properties in case there is an address
        //collision in the inventory
        String filter = props.getProperty(PROP_PLATFORM_IP_IGNORE);
        if (filter != null) {
            StringTokenizer tok = new StringTokenizer(filter, ",");
            while (tok.hasMoreTokens()) {
                this.ipIgnore.put(tok.nextToken(), Boolean.TRUE);
            }
        }
    }

    private void setValue(ConfigResponse cprops, String key, String value) {
        if ((value == null) || (value.length() == 0)) {
            return;
        }
        cprops.setValue(key, value);
    }

    //if this platform is a VMware Guest, link to the VMware Host
    private void getPlatformVz(PlatformResource platform, Sigar sigar) {
        try {
            //dont bother trying if vmware plugin is not installed
            this.manager.getParent().getPlugin("vmware");
        } catch (PluginNotFoundException e) {
            return;
        }

        Map info = VMwareGuestInfo.getGuestInfo(sigar);
        if (info == null) {
            //we are not a Guest or guest properties are not set
            return;
        }

        String id =
            (String)info.get(ProductPlugin.PROP_PLATFORM_ID);
        String name =
            (String)info.get(ProductPlugin.PROP_PLATFORM_NAME);

        if ((id == null) || (name == null)) {
            return;
        }

        String href =
            "<a href=\"/Resource.do?eid=1:" + id + "\">" +
            name + "</a>";
        
        String description = 
            platform.getDescription() +
            " (VM Guest of " + href + ")";
        platform.setDescription(description);
    }

    /**
     * Performs all the actual platform detection.
     * @param platformConfig ConfigResponse for the existing platform, if any.
     */
    public PlatformResource getPlatformResource(ConfigResponse config) 
        throws PluginException {

        OperatingSystem os = OperatingSystem.getInstance();
        
        PlatformResource platform = new PlatformResource();
        ConfigResponse cprops = new ConfigResponse();

        platform.setPlatformTypeName(os.getName());
        platform.setDescription(os.getDescription());

        Sigar sigar = new Sigar();
        HashMap ips = new HashMap();
        String fqdn = this.fqdn;
        String ip = null;

        ips.putAll(this.ipIgnore);

        try {
            if (fqdn == null) {
                fqdn = sigar.getFQDN();
            }
            platform.setFqdn(fqdn);
            if (HostIP.isValidIP(platform.getFqdn())) {
                platform.setName(getPlatformName());
            }

            // Detect IP addresses
            String[] interfaces = sigar.getNetInterfaceList();

            if (this.ip != null) {
                getLog().info("Adding dummy ip address=" + this.ip);
                platform.addInterface(this.ip,
                                      "255.255.255.0",
                                      "00:00:00:00:00:00");
            }

            for (int i=0; i<interfaces.length; i++) {
                String name = interfaces[i];
                NetInterfaceConfig ifconfig;
                try {
                    ifconfig = sigar.getNetInterfaceConfig(name);
                } catch (SigarException e) {
                    getLog().debug("getNetInterfaceConfig(" + name + "): " +
                                   e.getMessage(), e);
                    continue;
                }

                long flags = ifconfig.getFlags();

                if ((flags & NetFlags.IFF_UP) <= 0) {
                    continue;
                }

                //FreeBSD has a handful of these by default 
                if ((flags & NetFlags.IFF_POINTOPOINT) > 0) {
                    continue;
                }

                String address = ifconfig.getAddress();

                if (address.equals(NetFlags.ANY_ADDR)) {
                    continue; //skip "0.0.0.0"
                }

                if ((flags & NetFlags.IFF_LOOPBACK) > 0) {
                    //XXX 127.0.0.1 is not useful, but keeping for now
                    //to prevent "ip set changed" in the auto-inventory portlet
                    if (!address.equals(NetFlags.LOOPBACK_ADDRESS)) {
                        continue;
                    }
                } else if (ip == null) {
                    ip = address;
                }

                if (isWin32()) {
                    //XXX MS APIs do not flag this adapter as loopback
                    //this hack should probably be in SIGAR
                    final String MS_LOOPBACK = "Microsoft Loopback Adapter";
                    if (MS_LOOPBACK.equals(ifconfig.getDescription())) {
                        continue;
                    }
                }

                if (!this.ipDiscover) {
                    if ((flags & NetFlags.IFF_LOOPBACK) <= 0) {
                        getLog().info(PROP_PLATFORM_IP_DISCOVER +
                                      "=false, skipping interface=" + name +
                                      ", ip=" + address);
                        continue;
                    }
                }

                //make extra sure there are no dups
                if (ips.get(address) != null) {
                    continue;
                }
                else {
                    ips.put(address, Boolean.TRUE);
                }

                platform.addInterface(address,
                                      ifconfig.getNetmask(),
                                      ifconfig.getHwaddr());
            }

            //allow these to fail can carry on since this is
            //cosmetic inventory info which can be filled in
            //by hand if needed.  aix for example requires
            //read permission on /dev/kmem for this info
            //but not for the info above.
            try {
                Mem mem = sigar.getMem();
                cprops.setValue("ram", mem.getRam() + " MB");
            } catch (SigarException e) {
                String msg =
                    "Error getting memory info: " +
                    e.getMessage();

                getLog().warn(msg);
                getLog().debug(msg, e);
            } 

            try {
                CpuInfo[] infos = sigar.getCpuInfoList();
                //for license restrictions
                platform.setCpuCount(new Integer(infos.length));

                String cpuSpeed = infos[0].getMhz() + " MHz";
                if (infos.length > 1) {
                    cpuSpeed =
                        infos.length + " @ " + cpuSpeed;
                }
                cprops.setValue("cpuSpeed", cpuSpeed);
            } catch (SigarException e) {
                String msg =
                    "Error getting cpu info: " +
                    e.getMessage();

                getLog().warn(msg);
                getLog().debug(msg, e);

                platform.setCpuCount(new Integer(1)); //1 at least
            }

            setValue(cprops, "ip", ip);

            try {
                NetInfo info = sigar.getNetInfo();
                setValue(cprops, "primaryDNS", info.getPrimaryDns());
                setValue(cprops, "secondaryDNS", info.getSecondaryDns());
                setValue(cprops, "defaultGateway", info.getDefaultGateway());
            } catch (SigarException e) {
                String msg =
                    "Error getting net info: " +
                    e.getMessage();

                getLog().warn(msg);
                getLog().debug(msg, e);
            }

            getPlatformVz(platform, sigar);
        } catch (SigarException se) {
            String msg = "Error retrieving system information: " +
                se.getMessage();
            throw new PluginException(msg, se);
        } finally {
            sigar.close();
        }

        cprops.setValue("arch", os.getArch());
        cprops.setValue("version", os.getVersion());
        cprops.setValue("vendor", os.getVendor());
        cprops.setValue("vendorVersion", os.getVendorVersion());

        platform.setCustomProperties(cprops);

        ConfigResponse mconfig = new ConfigResponse();
        if (isWin32()) {
            WindowsLogTrackPlugin.setDefaultConfig(mconfig);
        }
        else {
            UnixLogTrackPlugin.setDefaultConfig(mconfig);
        }

        platform.setMeasurementConfig(mconfig,
                                      LogTrackPlugin.LOGLEVEL_WARN,
                                      true);
        return platform;
    }
}

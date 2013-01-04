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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetServices;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;

public class NetServicesDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final String RPC = "RPC";

    private ServerResource getServer(ConfigResponse config) {
        ServerResource server = createServerResource("/");
        String fqdn = config.getValue(ProductPlugin.PROP_PLATFORM_FQDN);
        String type = getTypeInfo().getName();
        server.setName(fqdn + " " + type);
        server.setIdentifier(server.getName());
        server.setProductConfig();
        server.setMeasurementConfig();
        //server.setControlConfig();        
        getLog().debug("Created server=" + server.getName());
        return server;
    }
    
    public List getServerResources(ConfigResponse config) {
        List servers = new ArrayList();
        servers.add(getServer(config));
        return servers;
    }

    private ServiceResource createService(String type,
                                          String address,
                                          String port,
                                          boolean isSSL) {

        ConfigResponse config = new ConfigResponse();
        config.setValue(NetServicesCollector.PROP_HOSTNAME,
                        address);
        config.setValue(NetServicesCollector.PROP_PORT,
                        port);
        if (isSSL) {
            config.setValue(NetServicesCollector.PROP_SSL,
                            "true");
        }

        ServiceResource service = new ServiceResource();
        service.setType(type);

        String name = getPlatformName() + " " + type;
        if (isSSL) {
            name += " (SSL)";
        }

        service.setName(name);
        setProductConfig(service, config); //merge defaults
        service.setMeasurementConfig(new ConfigResponse(),
                                     LogTrackPlugin.LOGLEVEL_WARN,
                                     false);

        return service;
    }

    private String getDescription(Sigar sigar, long port) {
        long pid = -1;
        try {
            pid = sigar.getProcPort(NetFlags.CONN_TCP, port);
            if (pid == 0) {
                //XXX sigar bug
                return null;
            }
        } catch (SigarException e) {
            return null;
        }
        String description = null;
        if (pid != -1) {
            try {
                description =
                    ProcUtil.getDescription(sigar, pid);
                int len = description.length()+4;
                int max = 200 - len; //appdef limit is 200
                if (description.length() > max) {
                    int offset = description.length() - max;
                    description =
                        "..." + description.substring(offset);
                }
            } catch (SigarException e) {
                return null;
            }
        }
        if ((description == null) ||
            (description.length() == 0))
        {
            return null;
        }
        else {
            return description;
        }
    }

    //XXX lame.
    private boolean isIPv4(String address) {
        return new StringTokenizer(address, ".", true).countTokens() == 7;
    }
    
    private void discoverNFS(List services) {
        if (isWin32()) {
            return;
        }

        FileSystem[] fslist; 
        try {
            fslist = getSigar().getFileSystemList();
        } catch (SigarException e) {
            return;
        }

        for (int i=0; i<fslist.length; i++) {
            FileSystem fs = fslist[i];
            if (fs.getType() != FileSystem.TYPE_NETWORK) {
                continue;
            }
            String type = fs.getSysTypeName();
            if (!type.equals(RPCCollector.PROGRAM_NFS)) {
                continue;
            }
            String dev = fs.getDevName();
            int ix = dev.indexOf(':');
            if (ix == -1) {
                continue;
            }
            String host = dev.substring(0, ix);
            ServiceResource service = new ServiceResource();

            ConfigResponse config = new ConfigResponse();
            config.setValue(NetServicesCollector.PROP_HOSTNAME,
                            host);
            config.setValue("program", type);
            config.setValue("version", RPCCollector.RPC_VERSION);
            service.setProductConfig(config);
            service.setMeasurementConfig();
            service.setType(RPC);

            String name = getPlatformName() + " NFS mount " + dev;
            service.setName(name);
            service.setDescription("Local mount point: " + fs.getDirName());

            services.add(service);
        }
    }

    private String getDiscover(String type) {
        String key = "netservices.discover";
        if (type != null) {
            key += "." + type;
        }
        return getManager().getProperty(key);
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        ArrayList services = new ArrayList();

        if (!"false".equals(getDiscover(RPC))) {
            discoverNFS(services);
        }

        if ("true".equals(getDiscover("http"))) {
            services.add(createService("HTTP", "localhost", "80", false));
        }

        //not enabled by default, but helpful for testing.
        if (!"true".equals(getDiscover(null))) {
            return services;
        }

        String ip =
            serverConfig.getValue(ProductPlugin.PROP_PLATFORM_IP);

        ProductPluginManager ppm =
            (ProductPluginManager)getManager().getParent();
        ProductPlugin pPlugin =
            ppm.getProductPlugin("netservices");

        Sigar sigar = new Sigar();
        NetConnection[] connections;
        int flags = NetFlags.CONN_SERVER | NetFlags.CONN_TCP;
        
        try {
            connections = sigar.getNetConnectionList(flags);
        } catch (SigarException e) {
            return null;
        }

        HashMap ports = new HashMap();
        ServiceResource service;
        String description;

        for (int i=0; i<connections.length; i++) {
            NetConnection conn = connections[i];
            if (conn.getState() != NetFlags.TCP_LISTEN) {
                continue;
            }
            String address = conn.getLocalAddress();
            if (address.equals(NetFlags.ANY_ADDR_V6) ||
                !isIPv4(address))
            {
                continue;
            }

            if (NetFlags.isAnyAddress(address)) {
                address = "*";
            }
            ports.put(String.valueOf(conn.getLocalPort()), address);
        }

        TypeInfo[] types = pPlugin.getTypes();
        for (int i=0; i<types.length; i++) {
            if (types[i].getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }

            String type = types[i].getName();
            MeasurementPlugin plugin =
                ppm.getMeasurementPlugin(type);

            String[] props = {
                NetServicesCollector.PROP_PORT,
                NetServicesCollector.PROP_SSLPORT
            };

            //loop twice, 1 == default port, 2 == default ssl port
            for (int j=0; j<=1; j++) {
                String port = plugin.getTypeProperty(props[j]);
                if (port == null) {
                    continue;
                }
                boolean isSSL = j == 1;

                String address = (String)ports.remove(port);
                if (address == null) {
                    continue;
                }
                if (address.equals("*")) {
                    address = ip;
                }
                service = createService(type, address, port, isSSL);

                description =
                    getDescription(sigar, Long.parseLong(port));
                if (description != null) {
                    service.setDescription(description);
                }

                services.add(service);
            }
        }

        //if we can get a decent description, add TCP Socket
        //services for the remaining listening ports
        for (Iterator it=ports.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String portstr = (String)entry.getKey();
            String address = (String)entry.getValue();
            long port = Long.parseLong(portstr);

            StringBuffer name = new StringBuffer();
            name.append(getPlatformName()).append(" LISTEN ").
                append(address).append(':').append(port);
            
            String pname = NetServices.getTcpName(port);
            if (pname != null) {
                name.append(" (").append(pname).append(")");
            }

            description = getDescription(sigar, port);

            if ((pname == null) && (description == null)) {
                continue;
            }

            if (address.equals("*")) {
                address = ip;
            }
            service =
                createService("TCP Socket", address, portstr, false);
            service.setName(name.toString());
            service.setDescription(description);
            services.add(service);
        }

        return services; 
    }
}

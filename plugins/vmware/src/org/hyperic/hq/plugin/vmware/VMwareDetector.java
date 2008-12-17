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

package org.hyperic.hq.plugin.vmware;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMControlLibrary;
import org.hyperic.sigar.vmware.VMwareException;
import org.hyperic.sigar.vmware.VMwareServer;

public class VMwareDetector
    extends ServerDetector
    implements AutoServerDetector {
    
    static final String WIN32_EVENTLOG_DIR =
        "vmserverdRoot\\eventlog\\";
    
    static final String UNIX_EVENTLOG_DIR =
        "/var/log/vmware/";
    
    static final String PROP_VM_CONFIG = "vm.config";
    static final String PROP_VM_DISK   = "vm.disk";
    static final String PROP_VM_NIC    = "vm.nic";

    static final String VM_NAME = "VM";

    static final String VM_DISK_NAME = "VM Disk";

    static final String VM_NIC_NAME = "VM NIC";

    private static final Log log =
        LogFactory.getLog(VMwareDetector.class.getName()); 

    private static String getProductKey(String product) {
        return VMControlLibrary.REGISTRY_ROOT + "\\VMware " + product;
    }

    private static String unquote(String value) {
        if (value.charAt(0) == '"') {
            value = value.substring(1, value.length()-1);
        }
        return value;
    }

    private static class VMwareProperties extends Properties {

        private VMwareProperties() {
            super();
        }

        public synchronized Object put(Object key, Object value) {
            value = unquote((String)value);
            return super.put(key, value);
        }
    }

    private static Properties loadProperties(File file) {
        Properties props = new VMwareProperties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            props.load(is);
        } catch (IOException e) {
            log.error("Loading " + file + ": " +
                      e.getMessage(), e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }
        return props;
    }
    
    private void setCustomProperties(ServerResource server,
                                     VMwareProductInfo info) {
        if (info == null) {
            return;
        }
        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue("version", info.release);
        cprops.setValue("build", String.valueOf(info.build));
        setCustomProperties(server, cprops);
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        ServerResource server;

        if (!VMControlLibrary.isLoaded()) {
            return null;
        }

        if (isWin32()) {
            server = getWin32Server();
        }
        else {
            server = getLinuxServer();
        }

        if (server == null) {
            return null;
        }

        ArrayList servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    private boolean isESX() {
        return getTypeInfo().getName().indexOf(VM.ESX) != -1;
    }

    private VMwareProductInfo getProductInfo(Properties props) {
        VMwareProductInfo info;

        try {
            info = VMwareProductInfo.getInfo(props);
        } catch (VMwareException e) {
            log.info("Unable to get VMware product info", e);
            return null;
        }

        if (info == null) {
            log.info("Unable to get VMware product info: " +
                     "User id does not have permission to " +
                     "read VM config files?");
            return null;
        }

        if (!getTypeInfo().getName().equals(info.toString())) {
            return null;
        }

        return info;
    }

    private ServerResource getWin32Server()
        throws PluginException {

        Properties props = new Properties();
        props.setProperty(VMwareConnectParams.PROP_AUTHD_PORT,
                          VMwareConnectParams.DEFAULT_AUTHD_PORT);

        VMwareProductInfo info = getProductInfo(props);
        if (info == null) {
            return null;
        }

        String[] products = {
            "GSX Server", "Server"
        };
        String path = null;

        for (int i=0; i<products.length; i++) {
            RegistryKey key = null;
            try {
                key =
                    RegistryKey.LocalMachine.openSubKey(getProductKey(products[i]));
                path = key.getStringValue("InstallPath").trim();
                break;
            } catch (Win32Exception e) {
                continue;
            } finally {
                if (key != null) {
                    key.close();
                }
            }
        }

        if (path == null) {
            return null;
        }

        ServerResource server = createServerResource(path);
        server.setProductConfig(new ConfigResponse(props));
        server.setMeasurementConfig();
        setCustomProperties(server, info);
        return server;
    }

    private ServerResource getLinuxServer() 
        throws PluginException {

        File config = new File("/etc/vmware/config");
        if (!config.exists()) {
            return null;
        }

        Properties props = loadProperties(config);

        String path =
            props.getProperty("defaultVMPath");

        String port =
            props.getProperty(VMwareConnectParams.PROP_AUTHD_PORT);
        if (port == null) {
            //this is vmware workstation
            return null;
        }

        props.clear();

        VMwareProductInfo info = getProductInfo(props);
        if (info == null) {
            return null;
        }
        if (isESX()) {
            if ((info.major == 3) && (info.minor > 0)) {
                //only 3.0.x and lower are supported
                log.warn(info.version + " not supported, see: " +
                         "http://support.hyperic.com/display/hypcomm/VMware");
                return null;
            }
        }

        if (path == null) {
            path = "/etc/vmware";
        }

        ServerResource server = createServerResource(path);
        server.setProductConfig(new ConfigResponse(props));
        server.setMeasurementConfig();
        setCustomProperties(server, info);
        return server;
    }

    static List getDisks(VM vm) throws VMwareException {
        List disks = new ArrayList();
        String diskRes = VMwareMetrics.getResource(vm, "disk.HTL");
        StringTokenizer tok = new StringTokenizer(diskRes, ",");

        while (tok.hasMoreTokens()) {
            disks.add(tok.nextToken());
        }

        return disks;
    }

    static List getNICs(VM vm) throws VMwareException {
        List nics = new ArrayList();
        String nicRes = VMwareMetrics.getResource(vm, "net.adapters");
        StringTokenizer tok = new StringTokenizer(nicRes, ",");

        while (tok.hasMoreTokens()) {
            nics.add(tok.nextToken());
        }

        return nics;
    }

    private String serviceName(String vmName, String type) {
        return vmName + " " + type;
    }

    private String serviceName(String vmName, String type, String name) {
        return serviceName(vmName, type) + " (" + name + ")"; 
    }

    private void discoverDisks(VM vm, List services,
                               String vmName, String vmConfig)
        throws VMwareException {

        List disks = getDisks(vm);

        for (int i=0; i<disks.size(); i++) {
            String disk = (String)disks.get(i);

            ServiceResource service = new ServiceResource();
            service.setType(this, VM_DISK_NAME);
            service.setServiceName(serviceName(vmName, VM_DISK_NAME, disk));

            ConfigResponse config = new ConfigResponse();
            config.setValue(PROP_VM_CONFIG, vmConfig);
            config.setValue(PROP_VM_DISK, disk);
            service.setProductConfig(config);
            service.setMeasurementConfig();

            services.add(service);
        }
    }

    private void discoverNICs(VM vm, List services,
                              String vmName, String vmConfig)
        throws VMwareException {

        List nics = getNICs(vm);

        for (int i=0; i<nics.size(); i++) {
            String nic = (String)nics.get(i);

            ServiceResource service = new ServiceResource();
            service.setType(this, VM_NIC_NAME);
            service.setServiceName(serviceName(vmName, VM_NIC_NAME, nic));

            ConfigResponse config = new ConfigResponse();
            config.setValue(PROP_VM_CONFIG, vmConfig);
            config.setValue(PROP_VM_NIC, nic);
            service.setProductConfig(config);
            service.setMeasurementConfig();

            services.add(service);
        }
    }

    private void getGuestProps(VM vm, ConfigResponse cprops) {
        try {
            cprops.setValue("ip", vm.getGuestInfo("ip"));
        } catch (VMwareException e) {
        }
    }

    //pass these vars to the guest OS where plugin on the
    //otherside can link back to the host
    private void setGuestInfo(String name, VM vm, ConfigResponse config) {
        String[] props = {
            ProductPlugin.PROP_PLATFORM_ID, //not present w/ PluginDumper
            ProductPlugin.PROP_PLATFORM_NAME,
            ProductPlugin.PROP_PLATFORM_FQDN,
            ProductPlugin.PROP_PLATFORM_IP,
        };

        for (int i=0; i<props.length; i++) {
            String key = props[i];
            String val = config.getValue(key);

            String gkey = "hq." + key;
            //log.debug(name + ": guestinfo." + gkey + "=" + val);

            if (val == null) {
                //dont want PluginDumper to set the rest
                return;
            }
            //XXX works as root, would be good
            //to figure out howto set perms for another user
            try {
                vm.setGuestInfo(gkey, val);
            } catch (VMwareException e) {
                String msg =
                    "Cannot link guest to host: " +
                    e.getMessage();
                log.info(msg);
                break; //will be permission denied
            }
        }
    }

    protected List discoverServices(ConfigResponse serverConfig)
            throws PluginException {

        synchronized (VMwareConnectParams.LOCK) {
            return getServices(serverConfig);
        }
    }

    private String vmLogName(String name) {
        String encoded = URLEncoder.encode(name);
        return StringUtil.replace(encoded, "+", "%20");
    }

    private List getServices(ConfigResponse serverConfig,
                             VM vm, String name)
        throws VMwareException { 

        List services = new ArrayList();

        int state = vm.getExecutionState();
        String displayName = vm.getDisplayName();

        ServiceResource service = new ServiceResource();
        service.setType(this, VM_NAME);
        service.setServiceName(serviceName(displayName, VM_NAME));

        ConfigResponse config = new ConfigResponse();
        config.setValue(PROP_VM_CONFIG, name);

        String logdir =
            isWin32() ? WIN32_EVENTLOG_DIR : UNIX_EVENTLOG_DIR;
        String logfile =
            logdir + "event-" + vmLogName(name) + ".log";
        config.setValue(VMwareEventLogPlugin.PROP_FILES_SERVICE,
                        logfile);

        config.setValue(VMwareConfigTrackPlugin.PROP_FILES_SERVICE,
                        name);

        service.setProductConfig(config);

        log.debug(displayName + "=" + VM.EXECUTION_STATES[state]);

        ConfigResponse cprops = new ConfigResponse();

        String[] keys =
            getCustomPropertiesSchema(service.getType()).getOptionNames();

        //XXX use VM.getConfig if remote
        Properties props = loadProperties(new File(name));
        for (int k=0; k<keys.length; k++) {
            String value = props.getProperty(keys[k]);
            if (value != null) {
                cprops.setValue(keys[k], value);
            }
        }

        if (state == VM.EXECUTION_STATE_ON) {
            service.setMeasurementConfig();

            if (isESX()) {
                try {
                    discoverDisks(vm, services, displayName, name);
                } catch (VMwareException e) {
                    //XXX 3.0.0 bug disk.HTL not defined
                    log.error("Failed to discover VM Disks: " + e);
                }
                discoverNICs(vm, services, displayName, name);
            }

            getGuestProps(vm, cprops);

            setGuestInfo(displayName, vm, serverConfig);
        }

        service.setCustomProperties(cprops);
        service.setControlConfig();

        services.add(service);

        return services;
    }

    private List getServices(ConfigResponse serverConfig)
        throws PluginException {

        List services = new ArrayList();
        boolean isServerConnected = false;
        VMwareConnectParams params =
            new VMwareConnectParams(serverConfig.toProperties());
        VMwareServer server = new VMwareServer();

        try {
            server.connect(params);
            isServerConnected = true;

            List names = server.getRegisteredVmNames();
            log.debug("Found " + names.size() + " registered VMs");

            for (int i=0; i<names.size(); i++) {
                String name = (String)names.get(i);
                VM vm = new VM();
                boolean isVmConnected = false;
                try {
                    vm.connect(params, name);
                    isVmConnected = true;
                    services.addAll(getServices(serverConfig, vm, name));
                } catch (VMwareException e) {
                    log.error("Error discovering " + name + " services: " +
                              e.getMessage(), e);
                } finally {
                    if (isVmConnected) {
                        vm.disconnect();
                    }
                    vm.dispose();
                }
            }
        } catch (VMwareException e) {
            throw new PluginException("Error discovering VMs: " +
                                      e.getMessage(), e);
        } finally {
            params.dispose();

            if (isServerConnected) {
                server.disconnect();
            }
            server.dispose();
        }

        return services;
    }
}

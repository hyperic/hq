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

package org.hyperic.hq.product.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanListener;
import org.hyperic.hq.autoinventory.ScanManager;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.Scanner;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.scanimpl.FileScan;
import org.hyperic.hq.autoinventory.scanimpl.NullScan;
import org.hyperic.hq.autoinventory.scanimpl.WindowsRegistryScan;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.file.FileUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PluginDiscoverer implements ScanListener {

    private static final boolean IS_WIN32 = PlatformDetector.IS_WIN32;
    private AutoinventoryPluginManager pm;
    private ScanManager scanManager;
    private Log log;
    private ArrayList autoSignatures = new ArrayList();
    private ArrayList rgySignatures = new ArrayList();
    private ArrayList fileSignatures = new ArrayList();
    private Set servers;
    private PluginDumper pd;
    private boolean dumpProps = false;
    private boolean fetchMetrics = false;
    private ConfigResponse platformConfig;
    private String os;
    
    public PluginDiscoverer(PluginDumper pd) {

        this.pd = pd;
        this.log = LogFactory.getLog(PluginDiscoverer.class);
        this.pm = pd.apm;
        this.platformConfig = getPlatformConfig();
        this.scanManager = new ScanManager(this, this.log, this.pm, null); 

        this.scanManager.startup();

        this.fetchMetrics =
            PluginDumper.METHOD_METRIC.equals(this.pd.config.action);

        this.dumpProps =
            this.fetchMetrics ||
            "properties".equals(this.pd.config.action);
    }
    
    private ServerDetector getPlugin(String name)
        throws PluginNotFoundException {

        ServerDetector detector =
            (ServerDetector)this.pm.getPlatformPlugin(this.os, name);
        
        return detector;
    }

    public void scanComplete(ScanState state)
        throws AutoinventoryException, SystemException {

        this.servers = state.getAllServers(null);
    }

    private void addScanners(ScanConfiguration scanConfig,
                             ScanMethod method, List sigList,
                             ConfigResponse config) {
        
        ServerSignature[] sigs =
            (ServerSignature[])sigList.toArray(new ServerSignature[0]);
        ServerSignature[] signatures = scanConfig.getServerSignatures();
        
        signatures =
            (ServerSignature[])ArrayUtil.combine(signatures, sigs);
        
        scanConfig.setServerSignatures(signatures);
        scanConfig.setScanMethodConfig(method, config);
    }
    
    public void add(String name) {
        ServerDetector plugin;

        try {
            plugin = getPlugin(name);
        } catch (PluginNotFoundException e) {
            this.log.debug("No detector for " + name);
            return;
        }

        ServerSignature sig = plugin.getServerSignature();

        if (plugin instanceof FileServerDetector) {
            this.fileSignatures.add(sig);
        }
        
        if (plugin instanceof AutoServerDetector) {
            this.autoSignatures.add(sig);
        }
        
        if (IS_WIN32) {
            if (plugin instanceof RegistryServerDetector) {
                this.rgySignatures.add(sig);
            }
        }
    }
    
    private ConfigResponse getFileScanConfig(FileScan fileScan) {
        List options;
        ConfigResponse config = new ConfigResponse();
        Properties props = pm.getProperties();

        //if we find any property from this ConfigSchema
        //(fileScan.scanDirs, fileScan.depth, etc.)
        //then we will run a file scan.
        try {
            options = fileScan.getConfigSchema().getOptions();
        } catch (AutoinventoryException e) {
            e.printStackTrace(); //notgonnahappen
            return null;
        }

        for (int i=0; i<options.size(); i++) {
            ConfigOption option = (ConfigOption)options.get(i);
            String key = option.getName();
            String propName = "fileScan." + key;
            String value = props.getProperty(propName);
            if (value != null) {
                config.setValue(key, value);
            }
        }
        
        if (config.getKeys().size() > 0) {
            //set defaults for any not specified
            for (int i=0; i<options.size(); i++) {
                ConfigOption option = (ConfigOption)options.get(i);
                String key = option.getName();
                if (config.getValue(key) == null) {
                    config.setValue(key, option.getDefault());
                }
            }
            return config;
        }
        else {
            return null;
        }
    }

    private ConfigResponse getPlatformConfig() {
        ConfigResponse config = new ConfigResponse();
        String name = this.pd.config.plugin;

        if (name != null) {
            ProductPlugin plugin = this.pd.ppm.getProductPlugin(name);
            TypeInfo[] types = plugin.getTypes();
            for (int i=0; i<types.length; i++) {
                if (types[i] instanceof PlatformTypeInfo) {
                    PlatformTypeInfo type = (PlatformTypeInfo)types[i];
                    if (!type.isDevice()) {
                        continue;
                    }
                    config.setValue(ProductPlugin.PROP_PLATFORM_TYPE,
                                    type.getName());
                    log.debug("Set " + ProductPlugin.PROP_PLATFORM_TYPE + 
                              "=" + type.getName());
                }
            }
        }
        
        Properties props = this.pm.getProperties();
        String[][] platformProps = {
            { ProductPlugin.PROP_PLATFORM_TYPE, PluginDumper.OS },
            { ProductPlugin.PROP_PLATFORM_FQDN, "localhost" },
            { ProductPlugin.PROP_PLATFORM_NAME, "localhost" },
            { ProductPlugin.PROP_PLATFORM_IP,   "127.0.0.1" },
        };
        
        for (int i=0; i<platformProps.length; i++) {
            String key = platformProps[i][0];
            String defval = platformProps[i][1];
            String value =
                props.getProperty(key, config.getValue(key, defval));
        
            if (value != null) {
                config.setValue(key, value);
            }
        }

        this.os = config.getValue(ProductPlugin.PROP_PLATFORM_TYPE);

        return config;
    }

    public void start() {
        ScanConfiguration scanConfig = new ScanConfiguration();
        scanConfig.setConfigResponse(this.platformConfig);
        FileScan fileScan = new FileScan();
        
        ConfigResponse config = getFileScanConfig(fileScan);
        
        if (config != null) {
            this.log.debug("FileScan config=" + config);
            addScanners(scanConfig, fileScan,
                        this.fileSignatures, config);
        }
        else {
            scanConfig.setIsDefaultScan(true);
        
            addScanners(scanConfig, new NullScan(),
                        this.autoSignatures, config);
            this.log.debug("Adding Auto Scanners=" + this.autoSignatures);
        
            if (IS_WIN32) {
                this.log.debug("Adding Registry Scanners=" +
                               this.rgySignatures);
                addScanners(scanConfig, new WindowsRegistryScan(),
                            this.rgySignatures, config);
            }
        }
        
        // clear the plugin shared data, caches, etc.
        this.pm.endScan();
        
        synchronized (this.scanManager) {
            this.scanManager.queueScan(scanConfig);
        }
        
        while (this.scanManager.isScanQueued() ||
               this.scanManager.isScanRunning())
        {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }
        
        if (this.servers == null) {
            System.out.println("No servers detected");
            return;
        }
        else {
            System.out.println(this.servers.size() + " servers detected");
        }

        String discoverServices = 
            this.pm.getProperties().getProperty("discover-services", "true"); 

        runtimeScan(discoverServices.equals("true"));            
    }
    
    private void dumpConfig(ConfigResponse config,
                            ConfigResponse metricConfig,
                            ConfigResponse controlConfig,
                            ConfigResponse cprops,
                            String indent) {

        System.out.println(indent + "config...");
        System.out.println(indent + "product.." + config);
        System.out.println(indent + "metric..." + metricConfig);
        System.out.println(indent + "control.." + controlConfig);
        if (cprops != null) {
            System.out.println(indent + "cprops..." + cprops);
        }
    }
    
    private ConfigResponse decodeConfig(byte[] config) {
        if (config == null) {
            return null;
        }
        try {
            return ConfigResponse.decode(config);
        } catch (EncodingException e) {
            return null; //notgonnahappen
        }
    }

    private void dumpConfig(ConfigResponse config,
                            ConfigResponse metricConfig,
                            ConfigResponse controlConfig,
                            ConfigResponse cprops,
                            ConfigResponse rtConfig, 
                            String indent) {

        dumpConfig(config, metricConfig, controlConfig, cprops, indent);
        System.out.println(indent + "rt......." + rtConfig);
    }

    private void dumpProperties(Properties props, String type, String name) {
        if (this.fetchMetrics) {
            TypeInfo info =
                this.pd.ppm.getTypeInfo(this.os, type);
            if (info == null) {
                this.log.error("No TypeInfo found for: " + type);
                return;
            }
            //add command line props for stuff like passwords
            props.putAll(this.pd.getProperties());

            ConfigResponse config = new ConfigResponse(props);

            try {
                this.pd.fetchMetrics(info, false, config);
            } catch (PluginException e) {
                e.printStackTrace();
            }
            return;
        }

        PrintStream ps = null;
        String file = FileUtil.escape(name);
        String dir = 
            "plugin-properties" + File.separator + TypeInfo.formatName(type);
        String plugin = (String)this.pd.productTypes.get(type);

        try {
            ps = PluginDumper.openFile(dir, file + ".properties");

            ps.println("# same as '-p \"" + plugin + "\"'");
            ps.println(PluginDumper.PROP_PLUGIN+ "=" + plugin);

            ps.println("# same as '-t \"" + type + "\"'");
            ps.println(PluginDumper.PROP_TYPE + "=" + type);

            props.store(ps, name);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    private void dumpProperties(Properties props,
                                ConfigResponse pConfig,
                                ConfigResponse mConfig,
                                ConfigResponse cConfig,
                                String type,
                                String name)
    {
        if (pConfig != null) {
            props.putAll(pConfig.toProperties());
        }
        if (mConfig != null) {
            props.putAll(mConfig.toProperties());
        }
        if (cConfig != null) {
            props.putAll(cConfig.toProperties());
        }

        dumpProperties(props, type, name);
    }

    private String getDescription(String desc) {
        if (desc == null) {
            return "";
        }
        return " (" + desc + ")";
    }

    private void dumpService(Properties serverProps,
                             AIServiceValue service, String indent) {
        
        System.out.println(indent + service.getName() +
                           getDescription(service.getDescription()));
        
        ConfigResponse config =
            decodeConfig(service.getProductConfig());
        ConfigResponse metricConfig =
            decodeConfig(service.getMeasurementConfig());
        ConfigResponse controlConfig =
            decodeConfig(service.getControlConfig());
        ConfigResponse rtConfig =
            decodeConfig(service.getResponseTimeConfig());
        ConfigResponse cprops =
            decodeConfig(service.getCustomProperties());

        dumpConfig(config, metricConfig,
                   controlConfig, cprops, rtConfig, indent + " ");

        if (this.dumpProps) {
            Properties props = new Properties();
            props.putAll(serverProps);
            dumpProperties(props,
                           config, metricConfig, controlConfig,
                           service.getServiceTypeName(),
                           service.getName());
        }
    }
    
    private ConfigResponse dumpServer(AIServerValue server,
                                      Properties serverProps,
                                      ServerDetector detector) {
        
        ConfigResponse config =
            decodeConfig(server.getProductConfig());
        ConfigResponse metricConfig =
            decodeConfig(server.getMeasurementConfig());
        ConfigResponse controlConfig =
            decodeConfig(server.getControlConfig());
        ConfigResponse cprops =
            decodeConfig(server.getCustomProperties());

        System.out.println("\nServer: " + server.getName() +
                           " [" + server.getInstallPath() + "]" +
                           getDescription(server.getDescription()));
        dumpConfig(config, metricConfig, controlConfig, cprops, " ");

        if (config == null) {
            return null;
        }

        if (this.dumpProps) {
            dumpProperties(serverProps,
                           config, metricConfig, controlConfig,
                           server.getServerTypeName(),
                           server.getName());
        }

        if (detector == null) {
            return null;
        }

        if (metricConfig != null) {
            config.merge(metricConfig, true);
        }

        //merge config from command line -Dprop=value
        if (this.pd.config.plugin != null) {
            ProductPlugin pPlugin =
                this.pd.ppm.getProductPlugin(this.pd.config.plugin);
            try {
                config.merge(this.pd.getPluginConfig(pPlugin,
                                                     detector.getTypeInfo()),
                             false);
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }

        config.setValue(ProductPlugin.PROP_INSTALLPATH,
                        server.getInstallPath());

        return config;
    }

    private void runtimeScan(boolean discoverServices) {
        PlatformResource platform;
        HashMap serviceNames = new HashMap();

        try {
            platform = Scanner.detectPlatform(this.pm, this.platformConfig);
        } catch (AutoinventoryException e) {
            e.printStackTrace();
            return;
        }

        String fqdn =
            this.platformConfig.getValue(ProductPlugin.PROP_PLATFORM_FQDN);
        if ("localhost".equals(fqdn)) {
            this.platformConfig.setValue(ProductPlugin.PROP_PLATFORM_FQDN,
                                         platform.getFqdn());
        }
        ConfigResponse platformMeasurementConfig =
            decodeConfig(platform.getMeasurementConfig());
        
        boolean useDelim = (this.servers.size() > 1);
        
        for (Iterator it=this.servers.iterator(); it.hasNext();) {
            AIServerValue server = (AIServerValue)it.next();
            String typeName = server.getServerTypeName();
            String name = server.getName();
            RuntimeResourceReport report;
            ServerDetector detector;
            ConfigResponse config;
            Properties serverProps = new Properties();

            if (useDelim) {
                System.out.println("\n=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-" +
                                   "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
            }

            try {
                detector = getPlugin(typeName);
            } catch (PluginNotFoundException e) {
                detector = null;
            }

            config = dumpServer(server, serverProps, detector);

            if (!discoverServices) {
                continue;
            }

            if (detector == null) {
                continue;
            } 
            
            if (!detector.isRuntimeDiscoverySupported()) {
                continue;
            }
            
            if (config == null) {
                continue;
            }
            
            this.log.debug("Runtime discover for " + name +
                           ", config=" + config);

            config.merge(this.platformConfig, false);
            if (platformMeasurementConfig != null) {
                //snmp based platform device plugins use platform.ip
                //to auto-configure snmpIp
                config.merge(platformMeasurementConfig, false);
            }

            try {
                PluginLoader.setClassLoader(detector);                

                RuntimeDiscoverer discoverer = detector.getRuntimeDiscoverer();

                //flush old values so we dont have to run
                //PlatformDetector everytime
                platform.cleanAIServerValue();
                platform.cleanAIIpValue();
                platform.removeAllAIServerValues();
                platform.removeAllAIIpValues();

                try {
                    report =
                        discoverer.discoverResources(0, platform, config);
                } catch (PluginException e) {
                    String msg = detector.getName() + "-->" + e;
                    if (server.measurementConfigHasBeenSet()) {
                        this.log.error(msg, e);
                    }
                    else {
                        this.log.debug(msg, e);
                    }
                    continue;
                }
            } catch (NoClassDefFoundError e) {
                this.log.debug(detector.getName() + "-->" + e, e);
                continue;
            } finally {
                PluginLoader.resetClassLoader(detector);
            }
            
            System.out.print("\nRuntime Resource Report...");
            if (report == null) {
                System.out.println("none");
                continue;
            }
            System.out.print("\n");
            
            AIPlatformValue[] platforms = report.getAIPlatforms();
            if (platforms == null) {
                System.out.println("No platforms discovered");
                continue;
            }

            for (int i=0; i<platforms.length; i++) {
                System.out.println("Platform=" + platforms[i].getPlatformTypeName() +
                                   ", fqdn=" + platforms[i].getFqdn() +
                                   getDescription(platforms[i].getDescription()));

                System.out.println("config...");
                System.out.println("product.." +
                                   decodeConfig(platforms[i].getProductConfig()));
                System.out.println("metric..." +
                                   decodeConfig(platforms[i].getMeasurementConfig()));
                System.out.println("control.." +
                                   decodeConfig(platforms[i].getControlConfig()));
                System.out.println("cprops..." +
                                   decodeConfig(platforms[i].getCustomProperties()));

                AIServerValue[] servers = platforms[i].getAIServerValues();

                for (int j=0; j<servers.length; j++) {
                    if (!(servers[j] instanceof AIServerExtValue)) {
                        continue;
                    }

                    AIServerExtValue s = (AIServerExtValue)servers[j];
                    String sName;
                    if (s.getName() == null) {
                        sName = server.getName();
                    }
                    else {
                        //e.g. iPlanet, WebLogic, WebSphere discover servers
                        sName = s.getName();
                        if (!server.getName().equals(sName)) {
                            dumpServer(s, new Properties(), null);
                        }
                    }

                    if (s.getCustomProperties() != null) {
                        System.out.println("  " + sName + " cprops: " +
                                           decodeConfig(s.getCustomProperties()));
                    }

                    AIServiceValue[] services = s.getAIServiceValues();
                    if ((services == null) || (services.length == 0)) {
                        System.out.println("  [No services discovered]");
                        continue;
                    }
                    System.out.println("  " + sName + " services:");

                    for (int k=0; k<services.length; k++) {
                        String svcName = services[k].getName();
                        String prefix = ServiceResource.SERVER_NAME_PREFIX;
                        if (svcName.startsWith(prefix)) {
                            svcName = svcName.substring(prefix.length()-1);
                            svcName = sName + svcName;
                            services[k].setName(svcName);
                        }
                        if (serviceNames.get(svcName) == null) {
                            serviceNames.put(svcName, Boolean.TRUE);
                        }
                        else {
                            String msg =
                                "!!!ERROR!!! duplicate service name=" +
                                svcName;
                            System.out.println(msg);
                        }
                        dumpService(serverProps, services[k], "    ");
                    }
                }
            }
        }
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.jboss7.objects.Connector;
import org.hyperic.hq.plugin.jboss7.objects.DataSource;
import org.hyperic.hq.plugin.jboss7.objects.Deployment;
import org.hyperic.hq.plugin.jboss7.objects.WebSubsystem;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public abstract class JBossDetectorBase extends DaemonDetector implements AutoServerDetector {
    
    private final Log log = getLog();
    protected static final String USERNAME = "jboss7.user";
    protected static final String PASSWORD = "jboss7.pass";
    protected static final String HTTPS = "jboss7.https";
    protected static final String ADDR = "jboss7.addr";
    protected static final String PORT = "jboss7.port";
    protected static final String SERVER = "jboss7.server";
    protected static final String HOST = "jboss7.host";
    protected static final String CONFIG = "jboss7.conf";
    
    abstract String getPidsQuery();
    
    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList<ServerResource>();
        
        long[] pids = getPids(getPidsQuery());
        log.debug("[getServerResources] pids.length:" + pids.length);
        for (long pid : pids) {
            Map<String, String> args = parseArgs(getProcArgs(pid));
            String detectedVersion = getVersion(args);
            String expectedVersion = getTypeInfo().getVersion();
            if (expectedVersion.equals("7")) {
                expectedVersion += ".0";
            }
            boolean validVersion = detectedVersion.startsWith(expectedVersion);
            log.debug("[getServerResources] pid='" + pid + "' expectedVersion='" + expectedVersion + "' detectedVersion='" + detectedVersion + "' validVersion='" + validVersion + "'");
            if (validVersion) {
                File cfgFile = getConfigFile(args);
                String installPath;
                String AIID;
                try { // cosmetic
                    installPath = cfgFile.getParentFile().getParentFile().getCanonicalPath();
                    AIID = cfgFile.getCanonicalPath();
                } catch (IOException ex) {
                    installPath = cfgFile.getParentFile().getParentFile().getAbsolutePath();
                    AIID = cfgFile.getAbsolutePath();
                }
                ServerResource server = createServerResource(installPath);
                server.setIdentifier(AIID);
                
                ConfigResponse productConfig = getServerProductConfig(args);
                populateListeningPorts(pid, productConfig, true);
                setProductConfig(server, productConfig);
                setControlConfig(server, getServerControlConfig(args));
                server.setName(prepareServerName(server.getProductConfig()));
                servers.add(server);
            }
        }
        return servers;
    }
    
    @Override
    protected final List discoverServices(ConfigResponse config) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        log.debug("[discoverServices] config=" + config);
        if (haveServices()) {
            JBossAdminHttp admin = null;
            try {
                admin = new JBossAdminHttp(config);
            } catch (PluginException ex) {
                log.error("Error connecting to JBoss: " + ex, ex);
                return services;
            }

            // DATA SOURCES
            try {
                List<String> datasources = admin.getDatasources();
                log.debug(datasources);
                for (String ds : datasources) {
                    DataSource datasource = admin.getDatasource(ds, false, getTypeInfo().getVersion());
                    ServiceResource service = createServiceResource("Datasource");
                    service.setName(prepareServerName(config) + " Datasource " + ds);
                    
                    ConfigResponse cp = new ConfigResponse();
                    cp.setValue("jndi", datasource.getJndiName());
                    cp.setValue("driver", datasource.getDriverName());
                    
                    ConfigResponse pc = new ConfigResponse();
                    pc.setValue("name", ds);
                    
                    setProductConfig(service, pc);
                    service.setCustomProperties(cp);
                    service.setMeasurementConfig();
                    service.setControlConfig();
                    services.add(service);
                }
            } catch (PluginException ex) {
                log.error(ex, ex);
            }

            // CONECTORS
            try {
                WebSubsystem ws = admin.getWebSubsystem();
                log.debug(ws);
                for (String name : ws.getConector().keySet()) {
                    Connector connector = ws.getConector().get(name);
                    ServiceResource service = createServiceResource("Connector");
                    service.setName(prepareServerName(config) + " Connector " + name);
                    
                    ConfigResponse cp = new ConfigResponse();
                    cp.setValue("protocol", connector.getProtocol());
                    cp.setValue("scheme", connector.getScheme());
                    
                    ConfigResponse pc = new ConfigResponse();
                    pc.setValue("name", name);
                    
                    setProductConfig(service, pc);
                    service.setCustomProperties(cp);
                    service.setMeasurementConfig();
                    service.setControlConfig();
                    services.add(service);
                }
            } catch (PluginException ex) {
                log.error(ex, ex);
            }

            // deployments
            try {
                List<Deployment> deployments = admin.getDeployments();
                for (Deployment d : deployments) {
                    ServiceResource service = createServiceResource("deployment");
                    service.setName(prepareServerName(config) + " Deployment " + d.getName());
                    
                    ConfigResponse cp = new ConfigResponse();
                    cp.setValue("runtime-name", d.getRuntimeName());
                    
                    ConfigResponse pc = new ConfigResponse();
                    pc.setValue("name", d.getName());
                    
                    setProductConfig(service, pc);
                    service.setCustomProperties(cp);
                    service.setMeasurementConfig();
                    service.setControlConfig();
                    services.add(service);
                }
            } catch (PluginException ex) {
                log.error(ex, ex);
            }
        }
        return services;
    }
    
    protected String prepareServerName(ConfigResponse cfg) {
        String type = getTypeInfo().getName();
        String name = getPlatformName() + " " + type + " ";
        String host = cfg.getValue(HOST);
        String server = cfg.getValue(SERVER);
        if (host != null) {
            name += host;
            if (server != null) {
                name += " " + server;
            }
        } else {
            name += cfg.getValue(ADDR) + ":" + cfg.getValue(PORT);
        }
        return name;
    }
    
    final String parseAddress(String address, Map<String, String> args) {
        if (address.startsWith("${")) {
            address = address.substring(2, address.length() - 1);
            if (address.contains(":")) {
                String[] s = address.split(":");
                address = args.get(s[0]);
                if (address == null) {
                    address = s[1];
                }
            } else {
                address = args.get(address);
            }
        }
        return address;
    }
    
    final String getVersion(Map<String, String> args) {
        String version = "not found";
        
        String mp = args.get("mp");
        File serverModule = new File(mp, "org/jboss/as/server/main");
        if (!serverModule.isAbsolute()) {
            serverModule = new File(args.get("jboss.home.dir"), serverModule.getPath());
        }
        
        String jars[] = serverModule.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("jboss-as-server") && name.endsWith(".jar");
            }
        });
        
        if ((jars != null) && (jars.length == 1)) {
            try {
                JarFile jarFile = new JarFile(new File(serverModule, jars[0]));
                log.debug("[getVersion] jboss-as-server.jar = '" + jarFile.getName() + "'");
                Attributes attributes = jarFile.getManifest().getMainAttributes();
                jarFile.close();
                version = attributes.getValue("JBossAS-Release-Version");
            } catch (IOException e) {
                log.debug("[getVersion] Error getting JBoss version (" + e + ")", e);
            }
        } else {
            log.debug("[getVersion] serverModule=" + serverModule);
            log.debug("[getVersion] 'jboss-as-server.*.jar' not found.");
        }
        
        return version;
    }
    
    private static Map<String, String> parseArgs(String args[]) {
        Map<String, String> props = new HashMap<String, String>();
        for (int n = 0; n < args.length; n++) {
            if (args[n].startsWith("-X")) {
            } else if ((args[n].startsWith("-D") || args[n].startsWith("--")) && args[n].contains("=")) {
                String arg[] = args[n].substring(2).split("=");
                props.put(arg[0], arg[1]);
            } else if (args[n].startsWith("-") && args[n].contains("=")) {
                String arg[] = args[n].substring(1).split("=");
                props.put(arg[0], arg[1]);
            } else if (args[n].startsWith("-") && !args[n].contains("=")) {
                props.put(args[n].substring(1), args[n + 1]);
            }
        }
        return props;
    }
    
    final ConfigResponse getServerProductConfig(Map<String, String> args) {
        ConfigResponse cfg = new ConfigResponse();
        String port = null;
        String address = null;
        
        File cfgFile = getConfigFile(args);
        
        try {
            log.debug("[getProductConfig] cfgFile=" + cfgFile.getCanonicalPath());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = (Document) dBuilder.parse(cfgFile);
            
            XPathFactory factory = XPathFactory.newInstance();
            XPathExpression expr = factory.newXPath().compile(getConfigRoot() + "/management/management-interfaces/http-interface");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeList = (NodeList) result;
            
            String mgntIf = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getAttributes().getNamedItem("port") != null) {
                    port = nodeList.item(i).getAttributes().getNamedItem("port").getNodeValue();
                    mgntIf = nodeList.item(i).getAttributes().getNamedItem("interface").getNodeValue();
                }
            }
            
            if (mgntIf != null) {
                expr = factory.newXPath().compile(getConfigRoot() + "/interfaces/interface[@name='" + mgntIf + "']/inet-address");
                result = expr.evaluate(doc, XPathConstants.NODESET);
                nodeList = (NodeList) result;
                
                for (int i = 0; i < nodeList.getLength(); i++) {
                    address = nodeList.item(i).getAttributes().getNamedItem("value").getNodeValue();
                }
            }
            
            setUpExtraProductConfig(cfg, doc);
            
        } catch (Exception ex) {
            log.debug("Error discovering the jmx.url : " + ex, ex);
        }
        
        log.debug("[getProductConfig] address='" + address + "' port='" + port + "'");
        if ((address != null) && (port != null)) {
            cfg.setValue(PORT, port);
            cfg.setValue(ADDR, parseAddress(address, args));
        }
        return cfg;
    }
    
    final ConfigResponse getServerControlConfig(Map<String, String> args) {
        ConfigResponse cf = new ConfigResponse();
        String baseDir = args.get("jboss.home.dir");
        String name = args.get("jboss.domain.default.config");
        if (name == null) {
            name = args.get("server-config");
            if (name == null) {
                name = getDefaultConfigName();
            }
        }
        
        log.debug("[getServerControlConfig] baseDir = " + baseDir);
        log.debug("[getServerControlConfig] name = " + name);
        if ((name != null) && (baseDir != null)) {
            name = name.substring(0, name.lastIndexOf(".")) + (isWin32() ? ".bat" : ".sh");
            File script = new File(new File(baseDir, "bin"), name);
            cf.setValue(JBoss7Control.START_SCRIPT, script.getAbsolutePath());
            log.debug("[getServerControlConfig] script = " + script);
        }
        return cf;
    }
    
    void setUpExtraProductConfig(ConfigResponse cfg, Document doc) throws XPathException {
    }
    
    final File getConfigFile(Map<String, String> args) {
        String serverConfig = args.get("server-config");
        if (serverConfig == null) {
            serverConfig = getDefaultConfigName();
        }
        
        File cfgFile = new File(serverConfig);
        if (!cfgFile.isAbsolute()) {
            String configDir = args.get("jboss.server.config.dir");
            if (configDir == null) {
                String baseDir = args.get("jboss.server.base.dir");
                if (baseDir == null) {
                    baseDir = args.get("jboss.home.dir") + getDefaultConfigDir();
                }
                configDir = baseDir + "/configuration";
            }
            cfgFile = new File(configDir, serverConfig);
        }
        return cfgFile;
    }
    
    abstract String getConfigRoot();
    
    abstract String getDefaultConfigName();
    
    abstract String getDefaultConfigDir();
    
    abstract boolean haveServices();
    
    private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}

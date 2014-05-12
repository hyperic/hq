/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C)
 * [2004-2008], Hyperic, Inc. This file is part of HQ. HQ is free software; you
 * can redistribute it and/or modify it under the terms version 2 of the GNU
 * General Public License as published by the Free Software Foundation. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class TomcatServerDetector
    extends MxServerDetector
{

    private static final String TOMCAT_PARAMS_KEY = "\\Parameters\\Java";
    private static final String TOMCAT_SERVICE_KEY = "SOFTWARE\\Apache Software Foundation\\Procrun 2.0";

    private static final String PTQL_QUERY_WIN32 = "Pid.Service.eq=%service_name%";

    private static final String CATALINA_BASE_PROP = "-Dcatalina.base=";

    private static final String TOMCAT_DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://localhost:6969/jmxrmi";

    private static final String PTQL_CONFIG_OPTION = SigarMeasurementPlugin.PTQL_CONFIG;

    private static final String CATALINA_HOME_PROP = "-Dcatalina.home=";

    private Log log = LogFactory.getLog(TomcatServerDetector.class);

    private ServerResource getServerResource(String win32Service, List options) throws PluginException {

        if (!isWin32ServiceRunning(win32Service)) {
            log.debug(win32Service + " is not running, skipping.");
            return null;
        }
        String path;
        String[] args = (String[]) options.toArray(new String[0]);
        String catalinaBase = getCatalinaBase(args);
        if (catalinaBase == null) {
            // no catalina base found
            log.error("No Catalina Base found for service " + win32Service + ". Skipping..");
            return null;
        } else {
            File catalinaBaseDir = new File(catalinaBase);
            if (catalinaBaseDir.exists()) {
                log.debug("Successfully detected Catalina Base for service: " + catalinaBase + " options=" + options);
                path = catalinaBaseDir.getAbsolutePath();
            } else {
                log.error("Resolved catalina base " + catalinaBase +
                          " is not a valid directory. Skipping Tomcat service " + win32Service);
                return null;
            }
        }
        
        if(!isCorrectVersion(catalinaBase+"/lib/catalina.jar")){
            return null;
        }
        
        ServerResource server = createServerResource(path);
        // Set PTQL query
        ConfigResponse config = new ConfigResponse();
        config.setValue(MxUtil.PROP_JMX_URL, TOMCAT_DEFAULT_URL);
        for (int i = 0; i < args.length; i++) {
            if (configureMxURL(config, args[i])) {
                break;
            }
        }
        config.setValue(Win32ControlPlugin.PROP_SERVICENAME, win32Service);
        config.setValue(PTQL_CONFIG_OPTION, PTQL_QUERY_WIN32);
        server.setName(server.getName() + " " + win32Service);
        server.setProductConfig(config);
        server.setMeasurementConfig();        
        return server;
    }

    private String[] getServicesFromRegistry() {
        RegistryKey key = null;
        String[] services = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY);
            services = key.getSubKeyNames();
        } catch (Win32Exception e) {
            // no tomcat services installed
        } finally {
            if (key != null) {
                key.close();
            }
        }
        return services;
    }

    /**
     * Helper method to discover Tomcat server paths using the Windows registry
     */
    private Map getServerRegistryMap() {
        Map serverMap = new HashMap();

        String[] services = getServicesFromRegistry();
        // return empty map if no windows services are found
        if (services == null) {
            return serverMap;
        }

        for (int i = 0; i < services.length; i++) {
            log.debug("Detected Tomcat service " + services[i]);
            List options = new ArrayList();
            RegistryKey key = null;
            try {
                key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY + "\\" + services[i] + TOMCAT_PARAMS_KEY);
                key.getMultiStringValue("Options", options);
            } catch (Win32Exception e) {
                log.error("Failed to find Java parameters for Tomcat service " + services[i]);
                // skip current service
                continue;
            } finally {
                if (key != null) {
                    key.close();
                }
            }
            serverMap.put(services[i], options);
        }
        return serverMap;
    }

    @Override
    protected boolean isInstallTypeVersion(MxProcess process) {
       final String[] processArgs = process.getArgs();
        String catalinaHome = getCatalinaHome(processArgs);
        String catalinaBase = getCatalinaBase(processArgs);
        String bootstrapJar = getBootstrapJar(processArgs);


        
        boolean correctVersion=false;
        
        if (bootstrapJar != null) {
            // new style using bootstarp.jar meta-inf
            correctVersion = isCorrectVersion(bootstrapJar);
        } else {
            // old style
            //check catalina base first - we are using it for the process query, so it must be present
            correctVersion = isInstallTypeVersion(catalinaBase);

            if (!correctVersion) {
                //check catalina home for version file
                if (catalinaHome == null) {
                    getLog().warn("Unable to determine Tomcat version of possible Tomcat process with install path: "
                            + process.getInstallPath()
                            + ".  Could not find value of catalina.home in process system properties.  This process will be skipped.");
                    return false;
                }
                correctVersion = isInstallTypeVersion(catalinaHome);
            }
        }

        if (!correctVersion) {
            return false;
        }

        //Make sure this isn't a tc Server (if plugin present)
        Iterator keys = PluginData.getGlobalProperties().keySet().iterator();
        String extend_server = null;
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key.toUpperCase().endsWith(".EXTENDS")) {
                String val = (String) PluginData.getGlobalProperties().get(key);
                Pattern p = Pattern.compile(val);
                Matcher m = p.matcher(getTypeInfo().getName());
                boolean find = m.find();
                getLog().debug("[isInstallTypeVersion] " + key + "=" + val + " (" + getTypeInfo().getName() + ") m.find()=" + find);
                if (find) {
                    extend_server = key.substring(0, key.lastIndexOf("."));
                    final String tcServerVersionFile = getTypeProperty(extend_server, "VERSION_FILE");
                    if (tcServerVersionFile != null) {
                        File homeVersionFile = new File(catalinaHome, tcServerVersionFile);
                        File baseVersionFile = new File(catalinaBase, tcServerVersionFile);
                        if ((homeVersionFile.exists() || baseVersionFile.exists()) && findVersionFile(new File(catalinaBase), Pattern.compile("hq-common.*\\.jar")) == null) {
                            //This is a tc Server that is not the HQ server
                            getLog().debug("[isInstallTypeVersion] '" + getTypeInfo().getName() + " [" + process.getInstallPath() + "]' is a '" + extend_server + "'");
                            return false;
                        } else {
                            getLog().debug("[isInstallTypeVersion] '" + getTypeInfo().getName() + " [" + process.getInstallPath() + "]' is not a '" + extend_server + "'");
                        }
                    }
                }
            }
        }
        return true;
    }

    protected String getCatalinaHome(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(CATALINA_HOME_PROP)) {
                return   args[i].substring(CATALINA_HOME_PROP.length());
            }
        }
        return null;
    }

    protected String getProcQuery(String path) {
        String query = super.getProcessQuery();
        if (path != null) {
            query += path;
        }
        return query;
    }

    /**
     * Auto scan
     */
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = super.getServerResources(platformConfig);
        
        // if we are on windows, take a look at the registry for autodiscovery
        if (isWin32()) {
            Map registryMap = getServerRegistryMap();
            // convert registry options to server value types
            for (Iterator it = registryMap.keySet().iterator(); it.hasNext();) {
                String serviceName = (String) it.next();
                List options = (List) registryMap.get(serviceName);
                ServerResource server = getServerResource(serviceName, options);
                if (server != null) {
                    servers.add(server);
                }
            }
        }
        
        //set control config for all servers
        if (servers != null ){
            for (Object server: servers) {
               ((ServerResource)server).setControlConfig();
            }
        }
        
        return servers;
    }

    private String getBootstrapJar(String[] args) {
        String res = null;
        for (int i = 0; (i < args.length) && (res == null); i++) {
            if (args[i].equalsIgnoreCase("-classpath")) {
                String[] cp = args[i + 1].split(File.pathSeparator);
                for (int c = 0; (c < cp.length) && (res == null); c++) {
                    if(cp[c].endsWith("bootstrap.jar")){
                        res = cp[c];
                    }
                }
            }
        }
        log.debug("[getBootstrapJar] res='"+res+"'");
        return res;
    }
    
    private String getCatalinaBase(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(CATALINA_BASE_PROP)) {
                return  args[i].substring(CATALINA_BASE_PROP.length());
            }
        }
        return null;
    }

    @Override
    protected void setProductConfig(ServerResource server, ConfigResponse config, long pid) {
        populateListeningPorts(pid, config,true);
        super.setProductConfig(server, config);
    }
    
    @Override
    protected ServerResource getServerResource(MxProcess process) {
        ServerResource server = super.getServerResource(process);
        String catalinaBase = server.getInstallPath();
        
        File hq = findVersionFile(new File(catalinaBase), Pattern.compile("hq-common.*\\.jar"));
        if (hq != null) {
            server.setName(getPlatformName()+" Hyperic - Apache Tomcat " + getTypeInfo().getVersion());
            server.setIdentifier("HQ Tomcat");
        }

        return server;
    }

    /**
     * We want this ServerDetector going first to win the battle for monitoring HQ Server in an EE env
     * TODO more elegant way to do this?
     */
    @Override
    public int getScanOrder() {
       return 0;
    }

    private boolean isCorrectVersion(String versionJar) {
        boolean correctVersion = false;
        try {
            JarFile jarFile = new JarFile(versionJar);
            log.debug("[isInstallTypeVersion] versionJar='" + jarFile.getName() + "'");
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            jarFile.close();
            String tomcatVersion = attributes.getValue("Specification-Version");
            String expectedVersion = getTypeProperty("tomcatVersion");
            if(expectedVersion==null){
                expectedVersion=getTypeInfo().getVersion();
            }
            log.debug("[isInstallTypeVersion] tomcatVersion='" + tomcatVersion + "' (" + expectedVersion + ")");
            correctVersion = tomcatVersion.equals(expectedVersion);
        } catch (IOException e) {
            log.debug("Error getting Tomcat version (" + e + ")", e);
        }
        return correctVersion;
    }
    
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

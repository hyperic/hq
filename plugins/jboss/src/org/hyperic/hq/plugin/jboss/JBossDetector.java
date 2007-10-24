/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.plugin.jboss;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.naming.Context;

import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.Log4JLogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.plugin.jboss.jmx.ServerQuery;
import org.hyperic.hq.plugin.jboss.jmx.ServiceQuery;

import org.hyperic.util.config.ConfigResponse;

public class JBossDetector
    extends ServerDetector
    implements AutoServerDetector, FileServerDetector {

    private static final Log log =
        LogFactory.getLog(JBossDetector.class.getName()); 

    private static final String JBOSS_SERVICE_XML =
        "conf" + File.separator + "jboss-service.xml";

    private static final String JBOSS_MAIN = "org.jboss.Main";

    private static final String PROP_CP = "-Djava.class.path=";

    private static final String PROP_AGENT = "-javaagent:";

    //command line options that may lead us to the home directory of 
    //the JBoss instance. Listed in the order we should search them.
    //Only if none of these is specified, we should resort to
    //the run.jar detection method for detection of the paths.

    private static final String PROP_SRV_CONFIG_URL =
        "-Djboss.server.config.url=";
   
    private static final String PROP_SRV_HOME_URL =
        "-Djboss.server.home.url=";

    private static final String PROP_SRV_BASE_URL =
        "-Djboss.server.base.url=";

    private static final String EMBEDDED_TOMCAT = "jbossweb-tomcat";

    //use .sw=java to find both "java" and "javaw"
    private static final String[] PTQL_QUERIES = {
        "State.Name.sw=java,Args.*.eq=" + JBOSS_MAIN,
    };

    private static HashMap bindings = new HashMap();

    private static File getFileFromURL(String name) {
        try {
            URI uri = new URI(name);
            File file = new File(uri);
            if (file.exists()) {
                return file;
            }
        }
        catch(URISyntaxException e) {
            //e.printStackTrace();
        }
        return null;
    }

    //figure out the install root based on run.jar location in the classpath
    private static void findServerProcess(List servers, String query) {
        bindings.clear();

        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);

            String classpath = null;
            String runJar = null;
            String config = "default";
            String address = null;
            String srvHomeUrl = null;
            String srvConfigUrl = null;
            String srvBaseUrl = null;
            String configPath = null;
            String installPath = null;
            boolean mainArgs = false;

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (arg.equals("-classpath") ||
                    arg.equals("-cp"))
                {
                    classpath = args[j+1];
                    continue;
                }
                else if (arg.startsWith(PROP_CP)) {
                    classpath = arg.substring(PROP_CP.length());
                    continue;
                }
                else if (arg.startsWith(PROP_AGENT)) {
                    //.../bin/pluggable-instrumentor.jar
                    runJar = arg.substring(PROP_AGENT.length());
                    continue;
                }
                else if (arg.startsWith(PROP_SRV_CONFIG_URL)) {
                    srvConfigUrl = arg.substring(PROP_SRV_CONFIG_URL.length());
                    continue;
                }
                else if (arg.startsWith(PROP_SRV_HOME_URL)) {
                    srvHomeUrl = arg.substring(PROP_SRV_HOME_URL.length());
                    continue;
                }
                else if (arg.startsWith(PROP_SRV_BASE_URL)) {
                    srvBaseUrl = arg.substring(PROP_SRV_BASE_URL.length());
                    continue;
                }

                if (!mainArgs && arg.equals(JBOSS_MAIN)) {
                    mainArgs = true;
                    continue;
                }

                //run.sh -c serverName
                if (mainArgs && arg.startsWith("-c")) {
                    config = arg.substring(2, arg.length());
                    if (config.equals("")) {
                        config = args[j+1];
                    }
                    continue;
                }
                else if (mainArgs && arg.startsWith("-b")) {
                    address = arg.substring(2, arg.length());
                    if (address.equals("")) {
                        address = args[j+1];
                    }
                    continue;
                }
            }

            File configDir = null;
            if (srvConfigUrl != null) {
                configDir = getFileFromURL(srvConfigUrl);
                if (configDir != null) {
                    configDir = configDir.getParentFile();
                }
            }
            else if (srvHomeUrl != null) {
                configDir = getFileFromURL(srvHomeUrl);
            }
            else if ((srvBaseUrl != null) && (config != null)) {
                configDir = getFileFromURL(srvBaseUrl);
                if (configDir != null) {
                    configDir = new File(configDir, config);
                }
            }

            if ((configDir != null) && configDir.exists()) {
                configPath = configDir.getAbsolutePath();
            }

            if (classpath != null) {
                StringTokenizer tok =
                    new StringTokenizer(classpath, File.pathSeparator);

                while (tok.hasMoreTokens()) {
                    String jar = tok.nextToken();
                    if (jar.endsWith("run.jar")) {
                        runJar = jar;
                        break;
                    }
                }
            }

            if (runJar == null) {
                continue;
            }

            //e.g. runJar == /usr/local/jboss-4.0.2/bin/run.jar
            File root = new File(runJar).getParentFile();
            if ((root == null) || root.getPath().equals(".")) {
                String cwd = getProcCwd(pids[i]);
                if (cwd == null) {
                    continue;
                }
                else {
                    root = new File(cwd);
                }
            }

            root = root.getParentFile();
            if (root == null) {
                continue;
            }

            if (configPath == null) {
                configPath = getServerConfigPath(root, config);
            }

            if (address != null) {
                bindings.put(configPath, address);
            }

            installPath = root.getAbsolutePath();

            servers.add(new JBossInstance(installPath, configPath));
        }
    }

    private static String getServerConfigPath(File root, String config) {
        File configDir =
            new File(root, "server" + File.separator + config);
        return configDir.getAbsolutePath();
    }

    private static void findBrandedExe(GenericPlugin plugin, List servers) {
        String query =
            "State.Name.eq=" + plugin.getProperty("brand.exe");

        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String exe = getProcExe(pids[i]);

            if (exe == null) {
                continue;
            }

            //strip bin\brand.exe
            File root =
                new File(exe).getParentFile().getParentFile();

            String engine = plugin.getProperty("brand.dir");
            if (engine != null) {
                root = new File(root, engine);
            }

            String configPath =
                getServerConfigPath(root, "default");

            servers.add(new JBossInstance(root.getPath(), configPath));
        }
    }

    //http://www.multiplan.co.uk/software/javaservice/docs/index.html
    private static void findServiceExe(GenericPlugin plugin, List servers) {
        //XXX cant assume name will be 'jboss.exe'
        String query = "State.Name.eq=jboss";
        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];
            String exe = getProcExe(pid);

            if (exe == null) {
                log.debug("Unable to determine exe for pid=" + pid);
                continue;
            }

            //strip bin\jboss.exe
            File root = new File(exe).getParentFile();
            //XXX could dig into the registry
            //to get Parameters\Current Directory
            if (root.getName().equals("bin")) {
                root = root.getParentFile();
                log.debug("installpath derived from exe=" + exe);
            }
            else {
                String msg = exe + " outside installpath";
                String cwd = getProcCwd(pids[i]);
                if (cwd == null) {
                    log.debug(msg +
                              ", unable to determine cwd for pid=" + pid); 
                    continue;
                }
                root = new File(cwd).getParentFile();
                log.debug(msg + ", using cwd=" + cwd);
            }

            //XXX should check:
            //HKLM\SYSTEM\CurrentControlSet\Services\$service_name\Parameters
            //for -c serverName
            String configPath =
                getServerConfigPath(root, "default");

            servers.add(new JBossInstance(root.getPath(), configPath));
        }
    }

    private static List getServerProcessList(GenericPlugin plugin) {
        ArrayList servers = new ArrayList();

        for (int i=0; i<PTQL_QUERIES.length; i++) {
            findServerProcess(servers, PTQL_QUERIES[i]);
        }

        //look for jboss within brand.exe on Win32 only
        if (isWin32()) {
            findBrandedExe(plugin, servers);
            
            findServiceExe(plugin, servers);
        }

        return servers;
    }

    static String getRunningInstallPath(GenericPlugin plugin) {
        List servers = getServerProcessList(plugin);

        if (servers.size() == 0) {
            return null;
        }
        
        JBossInstance instance = 
            (JBossInstance)servers.get(0);
        return instance.getHomePath();
    }

    /**
     * Look for for the embedded Tomcat server,
     * if found drop a note of the installpath for
     * the Tomcat detector to pickup.
     */
    private void noteEmbeddedTomcat(String path) {
        File[] dirs = new File(path, "deploy").listFiles();

        for (int i=0; i<dirs.length; i++) {
            File dir = dirs[i];
            String name = dir.getName();

            if (!dir.isDirectory()) {
                continue;
            }
            
            if (!(name.startsWith(EMBEDDED_TOMCAT) ||
                  name.startsWith("jboss-web.deployer")))
            {
                continue;
            }

            Map notes = getManager().getNotes();
            List tomcats = (List)notes.get(EMBEDDED_TOMCAT);

            if (tomcats == null) {
                tomcats = new ArrayList();
                notes.put(EMBEDDED_TOMCAT, tomcats);
            }

            tomcats.add(dir);

            getLog().debug("Found embedded tomcat at: " + dir);
        }
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {

        List servers = new ArrayList();
        List paths = getServerProcessList(this);

        for (int i=0; i<paths.size(); i++) {
            String dir = 
                ((JBossInstance)paths.get(i)).getConfigPath();
            List found = getServerList(dir);
            if (found != null) {
                servers.addAll(found);
                noteEmbeddedTomcat(dir);
            }
        }

        return servers;
    }

    private String getJnpURL(String installpath, File serviceXML) {
        JBossConfig cfg = JBossConfig.getConfig(serviceXML);

        String port = cfg.getJnpPort();
        if (port == null) {
            port = "1099";
        }

        //attempt to determine address using:
        //1) -b address cmdline argument
        //2) TCP listen address for port
        //3) config file, which will likely be the default 127.0.0.1
        //   since ${jboss.bind.address} is used by default and
        //    overridden by the -b address cmdline argument
        String where;
        String address = (String)bindings.get(installpath);

        if (address == null) {
            address = getListenAddress(port);
            if ((address == null) || address.equals("localhost")) {
                address = cfg.getJnpAddress();
                where = "configuration";
            }
            else {
                where = "TCP listen table";
            }
        }
        else {
            where = "-b argument";
        }

        String url = "jnp://" + address + ":" + port;
        getLog().debug("JNP url=" + url +
                       " (address determined from " + where + ")");
        return url;
    }

    /**
     * @param installpath Example: /usr/jboss-3.2.3/server/default
     */
    public List getServerList(String installpath)
        throws PluginException {

        File configDir = new File(installpath);
        File serviceXML = new File(configDir, JBOSS_SERVICE_XML);
        File distDir = configDir.getParentFile().getParentFile();

        // jboss copies the config set into the tmp deploy dir
        if (distDir.getName().equals("deploy")) {
            return null;
        }

        String serverName = configDir.getName();

        String fullVersion = getVersion(configDir);
        
        if (fullVersion == null) {
            getLog().debug("unable to determine JBoss version in: " +
                           configDir);
            return null;
        }

        String typeVersion = fullVersion.substring(0, 3);

        if (!getTypeInfo().getVersion().equals(typeVersion)) {
            getLog().debug(configDir + " is not a " + getName());
            return null;
        }

        getLog().debug("discovered JBoss server [" + serverName + "] in " +
                       configDir);

        ConfigResponse config = new ConfigResponse();
        ConfigResponse controlConfig = new ConfigResponse();
        ConfigResponse metricConfig = new ConfigResponse();

        config.setValue(Context.PROVIDER_URL,
                        getJnpURL(installpath, serviceXML));

        //for use w/ -jar hq-product.jar or agent.properties
        Properties props = getManager().getProperties();
        String[] credProps = {
            Context.PROVIDER_URL,
            Context.SECURITY_PRINCIPAL,
            Context.SECURITY_CREDENTIALS
        };
        for (int i=0; i<credProps.length; i++) {
            String value =
                props.getProperty(credProps[i]);
            if (value != null) {
                config.setValue(credProps[i], value);
            }
        }

        String script =
            distDir + File.separator +
            JBossServerControlPlugin.getControlScript(isWin32());

        controlConfig.setValue(ServerControlPlugin.PROP_PROGRAM,
                               getCanonicalPath(script));

        controlConfig.setValue(JBossServerControlPlugin.PROP_CONFIGSET,
                               serverName);

        String logDir =
            ".." + File.separator +
            ".." + File.separator +
            ".." + File.separator + "logs";
        File brandedLogDir = new File(installpath, logDir);

        if (!brandedLogDir.exists()) {
            logDir = "log";
        }

        metricConfig.setValue(Log4JLogTrackPlugin.PROP_FILES_SERVER,
                              logDir + File.separator + "server.log");

        ServerResource server = createServerResource(installpath);

        server.setConnectProperties(new String[] {
            Context.PROVIDER_URL
        });

        server.setProductConfig(config);
        server.setMeasurementConfig(metricConfig);
        server.setControlConfig(controlConfig);

        if (JBossProductPlugin.isBrandedServer(configDir, 
                                               getProperty("brand.ear"))) {
            // Branded JBoss
            String brandName = getProperty("brand.name");
            server.setName(getPlatformName() + " " + brandName);
            server.setIdentifier(brandName);
        }
        else {
            server.setName(server.getName() + " " + serverName);
        }

        //pickup any jars found relative to this installpath
        adjustClassPath(installpath);

        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }
    
    public List getServerResources(ConfigResponse platformConfig, String path)
        throws PluginException {

        //strip conf/jboss-service.xml
        return getServerList(getParentDir(path, 2));
    }
    
    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {
    
        try {
            return discoverJBossServices(serverConfig);
        } catch (SecurityException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
    
    private List discoverJBossServices(ConfigResponse serverConfig)
        throws PluginException {

        String url = serverConfig.getValue(Context.PROVIDER_URL);
        RMIAdaptor mServer;
        
        try {
            mServer = JBossUtil.getMBeanServer(serverConfig.toProperties());
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        configure(serverConfig); //for ServerQuery to use detector.getConfig()
        ServerQuery serverQuery = new ServerQuery(this);
        serverQuery.setURL(url);
        serverQuery.getAttributes(mServer);

        serverQuery.findServices(mServer);

        List queries = serverQuery.getServiceQueries();
        getLog().debug("discovered " + queries.size() + " services");

        List services = new ArrayList();

        for (int i=0; i<queries.size(); i++) {
            ServiceQuery query = (ServiceQuery)queries.get(i);
            ServiceResource service = new ServiceResource();
            service.setName(query.getQualifiedName());
            service.setType(query.getResourceType());

            ConfigResponse config =
                new ConfigResponse(query.getResourceConfig());

            if (query.hasControl()) { 
                ConfigResponse controlConfig =
                    new ConfigResponse(query.getControlConfig());
                service.setControlConfig(controlConfig);
            }
                
            service.setProductConfig(config);
            service.setMeasurementConfig();

            config = new ConfigResponse(query.getCustomProperties());
            service.setCustomProperties(config);

            services.add(service);
        }

        //look for any cprop keys in the attributes
        ConfigResponse cprops = new ConfigResponse();
        String[] attrs = this.getCustomPropertiesSchema().getOptionNames();
        for (int i=0; i<attrs.length; i++) {
            String key = attrs[i];
            String val = serverQuery.getAttribute(key);
            if (val != null) {
                cprops.setValue(key, val);
            }
        }
        setCustomProperties(cprops);

        return services;
    }

    public static String getVersion(File installPath) {
        File file =
            new File(installPath,
                     "lib" + File.separator + "jboss-j2ee.jar");
    
        if (!file.exists()) {
            return null;
        }
    
        Attributes attributes;
        try {
            JarFile jarFile = new JarFile(file);
            attributes = jarFile.getManifest().getMainAttributes();
        } catch (IOException e) {
            return null;
        }
    
        //e.g. Implementation-Version:
        //3.0.6 Date:200301260037
        //3.2.1 (build: CVSTag=JBoss_3_2_1 date=200306201521)
        //3.2.2 (build: CVSTag=JBoss_3_2_2 date=200310182216)
        //3.2.3 (build: CVSTag=JBoss_3_2_3 date=200311301445)
        //4.0.0DR2 (build: CVSTag=JBoss_4_0_0_DR2 date=200307030107)
        String version = 
            attributes.getValue("Implementation-Version");
    
        if (version == null) {
            return null;
        }
        if (version.length() < 3) {
            return null;
        }
    
        if (!(Character.isDigit(version.charAt(0)) &&
              (version.charAt(1) == '.') &&
              Character.isDigit(version.charAt(2))))
        {
            return null;
        }
    
        return version;
    }
}

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

package org.hyperic.hq.plugin.jboss;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.naming.Context;

import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

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

    private static final String JBOSS_SERVICE_XML =
        "conf" + File.separator + "jboss-service.xml";

    private static final String JBOSS_MAIN = "org.jboss.Main";

    private static final String PROP_CP = "-Djava.class.path=";

    private static final String PROP_AGENT = "-javaagent:";

    private static final String EMBEDDED_TOMCAT = "jbossweb-tomcat";

    //use .sw=java to find both "java" and "javaw"
    private static final String[] PTQL_QUERIES = {
        "State.Name.sw=java,Args.*.eq=" + JBOSS_MAIN,
    };

    //figure out the install root based on run.jar location in the classpath
    private static void findServerProcess(List servers, String query) {
        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);

            String classpath = null;
            String runJar = null;
            String config = "default";
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

            //sanity check
            if (root != null) { 
                root = root.getParentFile();
            }

            if (root == null) {
                continue;
            }

            servers.add(getServerConfigPath(root, config));
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
            servers.add(getServerConfigPath(root, "default"));
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
        }

        return servers;
    }

    static String getRunningInstallPath(GenericPlugin plugin) {
        List servers = getServerProcessList(plugin);

        if (servers.size() == 0) {
            return null;
        }

        String path = (String)servers.get(0);

        return new File(path).getParentFile().getParent();
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

            if (!dir.isDirectory()) {
                continue;
            }
            if (!dir.getName().startsWith(EMBEDDED_TOMCAT)) {
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
            String dir = (String)paths.get(i);
            List found = getServerList(dir);
            if (found != null) {
                servers.addAll(found);
                noteEmbeddedTomcat(dir);
            }
        }

        return servers;
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

        JBossConfig cfg = JBossConfig.getConfig(serviceXML);

        String url = "jnp://127.0.0.1";
        if (cfg.getJnpPort() != null) {
            url += ":" + cfg.getJnpPort();
        }
        config.setValue(Context.PROVIDER_URL, url.toString());

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

        String logDir;
        File brandedLogDir =
            new File(installpath, "../../../logs");

        if (brandedLogDir.exists()) {
            try {
                logDir = brandedLogDir.getCanonicalPath();
            } catch (IOException e) {
                logDir = brandedLogDir.getAbsolutePath();
            }
        }
        else {
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

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

package org.hyperic.hq.plugin.servlet;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

import org.hyperic.sigar.win32.RegistryKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat40ServerDetector 
    extends ServerDetector
    implements FileServerDetector,
               RegistryServerDetector,
               AutoServerDetector {

    static final String UNIQUE_JAR = "warp.jar";
    private static final HashMap HQ_NAMES = new HashMap();

    private static final String TOMCAT_MAIN =
        "org.apache.catalina.startup.Bootstrap";

    private static final String EMBEDDED_TOMCAT = "jbossweb-tomcat";

    private static final String HQ_SERVER_TOMCAT = "HQ Tomcat 5.5";

    private static final String[] PTQL_QUERIES = {
        "State.Name.eq=java,Args.*.eq=" + TOMCAT_MAIN,
    };

    private static final String[] CONF_FILES = {
        "conf" + File.separator + "server.xml", //standard
        "META-INF" + File.separator + "jboss-service.xml", //jboss + 4.1.x
        "server.xml", //jboss + 5.5
    };

    private static Log log = LogFactory.getLog("Tomcat40ServerDetector");

    public Tomcat40ServerDetector () { 
        super();
        setName(ServletProductPlugin.NAME);
    }

    public RuntimeDiscoverer getRuntimeDiscoverer()
    {
        return new Tomcat40RuntimeADPlugin();
    }

    protected String getUniqueJar() {
        return UNIQUE_JAR;
    }

    public List getServerList(String installpath)
        throws PluginException
    {
        return getServerList(installpath, null, false);
    }

    private String getConfigPort(String installpath) {
        for (int i=0; i<CONF_FILES.length; i++) {
            File file = new File(installpath, CONF_FILES[i]);
            if (!file.exists()) {
                continue;
            }
            if (!file.canRead()) {
                log.warn(file + " exists but cannot be read, " +
                         "auto-config will be skipped");
                continue;
            }

            TomcatConfig cfg = TomcatConfig.getConfig(file);
            //keep trying because jboss w/ 5.5.sar has both
            //./server.xml and META-INF/jboss-service.xml but
            //the port is only in ./server.xml
            if ((cfg != null) && (cfg.getPort() != null)) {
                return cfg.getPort();
            }
        }
        return null;
    }

    public List getServerList(String installpath,
                              String port, boolean isEmbedded)
        throws PluginException
    {
        ServerResource server = createServerResource(installpath);
        //XXX should be more generic, done within ServerDetector
        String hqname = (String)HQ_NAMES.get(installpath);
        if (hqname != null) {
            server.setName(server.getName() + " " + hqname);
        }

        // Special case if this is the HQ embedded tomcat
        if (installpath.indexOf("hq-engine") != -1) {
            String name = getPlatformName() + " " + HQ_SERVER_TOMCAT;
            server.setName(name);
            server.setIdentifier(HQ_SERVER_TOMCAT);
        }

        if (port == null) {
            port = getConfigPort(installpath);
        }

        if (port != null) {
            ConfigResponse productConfig = new ConfigResponse();
            String address = getListenAddress(port);
            if (address == null) {
                address = "localhost";
            }
            String url = "http://" + address + ":" + port;
            productConfig.setValue(JMXRemote.PROP_JMX_URL, url);

            server.setProductConfig(productConfig);
            server.setConnectProperties(new String[] {
               JMXRemote.PROP_JMX_URL,
            });

            server.setMeasurementConfig();

            ConfigResponse controlConfig = new ConfigResponse();
            if (isWin32()) {
                String serviceName;
                String version = getTypeInfo().getVersion();
                if (version.startsWith("4.")) {
                    serviceName = "Apache Tomcat " + version;
                }
                else {
                    serviceName = "Tomcat5";
                }
                controlConfig.setValue(Win32ControlPlugin.PROP_SERVICENAME,
                                       serviceName);                
            }
            else {
                String startupScript = installpath +
                File.separator + "bin" + File.separator + "catalina.sh";

                controlConfig.setValue(ServletEngineControlPlugin.
                                       PROP_STARTUP_SCRIPT,
                                       startupScript);
            }
            if (!isEmbedded) {
                server.setControlConfig(controlConfig);
            }
        }

        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    private void getJBossEmbeddedServers(List servers)
        throws PluginException {

        List embedded =
            (List)getManager().getNotes().get(EMBEDDED_TOMCAT);

        if (embedded == null) {
            return;
        }

        for (int i=0; i<embedded.size(); i++) {
            Map config = (Map)embedded.get(i);
            final String prop = ServletProductPlugin.PROP_INSTALLPATH;
            String installpath = (String)config.get(prop);
            if (installpath == null) {
                log.error(config + " missing " + prop);
                continue;
            }
            File dir = new File(installpath);
            String port = (String)config.get("port");

            if (!ServletProductPlugin.isJBossEmbeddedVersion(getTypeInfo(),
                                                             dir.getName())) {
                continue;
            }

            log.debug("Adding JBoss embedded server " + getName()
                      + " at " + dir);

            List dirs = getServerList(dir.getAbsolutePath(), port, true);
            if (dirs != null) {
                servers.addAll(dirs);
            }
        }
    }

    private static void findServerProcess(List servers, String query,
                                          String uniqueJar) {
        final String baseProp = "-Dcatalina.base=";
        final String homeProp = "-Dcatalina.home=";
        final String nameProp = "-Dhq.name=";

        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);

            String base = null;
            String home = null;
            String name = null;

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (arg.startsWith(baseProp)) {
                    base = arg.substring(baseProp.length(),
                                         arg.length());
                }
                else if (arg.startsWith(homeProp)) {
                    home = arg.substring(homeProp.length(),
                                         arg.length());
                }
                else if (arg.startsWith(nameProp)) {
                    name = arg.substring(nameProp.length(),
                                         arg.length());
                }
            }

            //in standard tomcat installs, base == home.
            //in Covalent ERS for example they are not the same,
            //base is where the server conf is,
            //home is where the binaries are.
            if (base == null) {
                continue;
            }

            if (home == null) {
                home = base;
            }

            File baseDir = new File(base);
            if (baseDir.isAbsolute()) {
                base = getCanonicalPath(base);
                home = getCanonicalPath(home);
            }
            else {
                //if catalina.base is not absolute
                //it will be relative to the process
                //current working directory.
                String cwd = getProcCwd(pids[i]);
                if (cwd == null) {
                    continue;
                }
                base = getCanonicalPath(cwd + File.separator + base);
                home = getCanonicalPath(cwd + File.separator + home);
            }

            File serverLib =
                new File(home, "server" + File.separator + "lib");

            if (!serverLib.canRead()) {
                // XXX: Future enhancement, could check the install directory
                // from the PTQL query for -4.0|-4.1|-5.0|-5.5
                log.debug("Unable to read from directory " +
                          serverLib);
                continue;
            }

            if (serverLib.exists()) {
                if (!new File(serverLib, uniqueJar).exists()) {
                    continue;
                }

                servers.add(base);
                if (name != null) {
                    HQ_NAMES.put(base, name);
                }
            }
        }
    }

    private static List getServerProcessList(String uniqueJar) {
        ArrayList servers = new ArrayList();

        for (int i=0; i<PTQL_QUERIES.length; i++) {
            findServerProcess(servers, PTQL_QUERIES[i], uniqueJar);
        }

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException
    {
        List servers = new ArrayList();

        getJBossEmbeddedServers(servers);

        List paths = getServerProcessList(getUniqueJar());

        for (int i=0; i<paths.size(); i++) {
            String dir = (String)paths.get(i);
            List found = getServerList(dir);
            if (found != null) {
                servers.addAll(found);
            }
        }

        return servers;
    }
    
    public List getServerResources(ConfigResponse platformConfig, String path) 
        throws PluginException
    {
        int levels;
        if (path.endsWith(".xml")) {
            //strip conf/server.xml or META-INF/jboss-service.xml
            levels = 2;
        }
        else if (path.endsWith(".jar")) {
            //strip server/lib/*.jar
            levels = 3;
        }
        else {
            log.warn("Unexpected file from scanner: " + path);
            return null;
        }

        return getServerList(getParentDir(path, levels));
    }

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException
    {
        return getServerList(path);
    }

    public List getRegistryScanKeys() {
        ArrayList lst = new ArrayList();
        lst.add("SOFTWARE\\Apache\\Apache Tomcat 4.0");
        return lst;
    }

    public static void main(String[] args) {
        String[][] jars = {
            { "4.0", Tomcat40ServerDetector.UNIQUE_JAR },
            { "4.1", Tomcat41ServerDetector.UNIQUE_JAR },
            { "5.0", Tomcat50ServerDetector.UNIQUE_JAR },
        };

        for (int i=0; i<jars.length; i++) {
            String version = jars[i][0];
            String jar = jars[i][1];

            List paths = getServerProcessList(jar);

            for (int j=0; j<paths.size(); j++) {
                System.out.println(version + "==>" + paths.get(j));
            }
        }
    }
}

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
package org.hyperic.hq.plugin.weblogic;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.weblogic.jmx.ApplicationQuery;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.plugin.weblogic.jmx.NodeManagerQuery;
import org.hyperic.hq.plugin.weblogic.jmx.ServerQuery;
import org.hyperic.hq.plugin.weblogic.jmx.ServiceQuery;
import org.hyperic.hq.plugin.weblogic.jmx.WeblogicDiscover;
import org.hyperic.hq.plugin.weblogic.jmx.WeblogicDiscoverException;
import org.hyperic.hq.plugin.weblogic.jmx.WeblogicQuery;
import org.hyperic.hq.plugin.weblogic.jmx.WeblogicRuntimeDiscoverer;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.SigarException;

public abstract class WeblogicDetector extends ServerDetector implements AutoServerDetector {

    private static final String PTQL_QUERY =
            "State.Name.eq=java,Args.-1.eq=weblogic.Server";
    private static final String SCRIPT_EXT =
            isWin32() ? ".cmd" : ".sh";
    private static final String ADMIN_START =
            "startWebLogic" + SCRIPT_EXT;
    public static final String NODE_START = "bin/startManagedWebLogic" + SCRIPT_EXT;
    static final String PROP_MX_SERVER =
            "-Dweblogic.management.server";
    private static final Log log = LogFactory.getLog(WeblogicDetector.class);

    public WeblogicDetector() {
        super();
        setName(WeblogicProductPlugin.SERVER_NAME);
    }

    @Override
    public RuntimeDiscoverer getRuntimeDiscoverer() {
        if(WeblogicProductPlugin.NEW_DISCOVERY){
            return super.getRuntimeDiscoverer();
        }
        return new WeblogicRuntimeDiscoverer(this);
    }

    //just here to override protected access.
    void adjustWeblogicClassPath(String installpath) {
        adjustClassPath(installpath);
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        getLog().debug("[getServerResources] platformConfig=" + platformConfig);
        List servers = new ArrayList();
        List<WLSProcWithPid> procs = getServerProcessList();
        List s;

        for (WLSProcWithPid proc : procs) {
            try {
                s = discoverServer(proc);
                if (s != null) {
                    servers.addAll(s);
                }
            } catch (SigarException e) {
                getLog().debug(e.getMessage(), e);
                throw new PluginException(e);
            } catch (PluginException ex) {
                getLog().debug(ex.getMessage(), ex);
                throw ex;
            }
        }
        return servers;
    }

    private List discoverServer(WLSProcWithPid proc) throws PluginException, SigarException {

        getLog().debug("Looking at: " + proc.getPath());

        File installDir = new File(proc.getPath());
        File configXML = new File(proc.getPath(), "config/config.xml");

        if (!configXML.exists()) {  //6.1
            configXML = new File(proc.getPath(), "config.xml");
        }

        WeblogicConfig cfg = new WeblogicConfig();

        try {
            cfg.read(configXML);
        } catch (IOException e) {
            throw new PluginException("Failed to read " + configXML + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PluginException("Failed to parse " + configXML + ": " + e.getMessage(), e);
        }

        WeblogicConfig.Server srvConfig = cfg.getServer(proc.getName());
        getLog().debug("srvConfig=" + srvConfig);

        if (srvConfig == null) {
            throw new PluginException("server '" + proc.getName() + "' not found in " + configXML);
        }

        if (getName().indexOf(srvConfig.getVersion()) < 0) {
            getLog().debug("server '" + proc.getName() + " is not a " + getName());
            return null;
        }

        ConfigResponse productConfig =
                new ConfigResponse(srvConfig.getProperties());

        String[] dirs = {
            proc.getName(), //8.1
            "logs", //9.1
        };

        File wlsLog = null;

        for (int i = 0; i < dirs.length; i++) {
            wlsLog =
                    new File(installDir,
                    dirs[i] + File.separator + proc.getName() + ".log");
            if (wlsLog.exists()) {
                break;
            }
        }

        productConfig.setValue(WeblogicLogFileTrackPlugin.PROP_FILES_SERVER,
                wlsLog.toString());

        ConfigResponse controlConfig = new ConfigResponse();
        File script = new File(installDir, "../../" + ADMIN_START);
        try {
            controlConfig.setValue(ServerControlPlugin.PROP_PROGRAM, script.getCanonicalPath());
        } catch (IOException ex) {
            controlConfig.setValue(ServerControlPlugin.PROP_PROGRAM, script.getPath());
            getLog().debug(ex);
        }

        boolean hasCreds = false;
        //for use w/ -jar hq-product.jar or agent.properties
        Properties props = getManager().getProperties();
        String[] credProps = {
            WeblogicMetric.PROP_ADMIN_USERNAME,
            WeblogicMetric.PROP_ADMIN_PASSWORD,
            WeblogicMetric.PROP_ADMIN_URL,
            WeblogicMetric.PROP_JVM,};

        for (int i = 0; i < credProps.length; i++) {
            String key = credProps[i];
            //try both ${domain}.admin.url and admin.url
            String val =
                    props.getProperty(srvConfig.domain + "." + key,
                    props.getProperty(key));
            if (val != null) {
                productConfig.setValue(key, val);
                hasCreds = true;
            } else {
                hasCreds = false;
            }
        }
        populateListeningPorts(proc.getPid(), productConfig, true);
        if (getLog().isDebugEnabled()) {
            getLog().debug(getName() + " config: " + productConfig);
        }

        String installpath = getCanonicalPath(installDir.getPath() + File.separator + "servers" + File.separator + srvConfig.name);
        log.debug("[discoverServer] installDir=" + installDir);
        log.debug("[discoverServer] installpath=" + installpath);
        if (!new File(installpath).exists()) {
            installpath = getCanonicalPath(installDir.getPath());
        }

        List servers = new ArrayList();
        ServerResource server = createServerResource(installpath);

        String name = getTypeInfo().getName()
                + " " + srvConfig.domain + " " + srvConfig.name;
		if (WeblogicProductPlugin.usePlatformName && WeblogicProductPlugin.NEW_DISCOVERY) {
		    name = getPlatformName() + " " + name;
		}

        server.setName(name);
        setIdentifier(server,name);
        log.debug("[discoverServer] identifier=" + server.getIdentifier());
        
        setProductConfig(server, productConfig);
        setControlConfig(server, controlConfig);
        //force user to configure by not setting measurement config
        //since we dont discover username or password.
        if (hasCreds) {
            server.setMeasurementConfig();
        }

        servers.add(server);
        installpath = getInstallRoot(installpath);
        if (installpath != null) {
            //handle the case where agent is started before WLS
            adjustClassPath(installpath);
        }

        return servers;
    }

    private static boolean isAdminDir(String path) {
        if (path.startsWith("/")) {
            File config =
                    new File(path, "config.xml.booted");
            return config.exists();
        } else {
            return false;
        }
    }

    private List<WLSProcWithPid> getServerProcessList() {
        ArrayList<WLSProcWithPid> servers = new ArrayList<WLSProcWithPid>();

        long[] pids = getPids(PTQL_QUERY);

        for (int i = 0; i < pids.length; i++) {
            getLog().debug("pid = '" + pids[i] + "'");
            String cwd = null;
            String name = null;

            String[] args = getProcArgs(pids[i]);
            getLog().debug("[" + pids[i] + "] args = " + Arrays.asList(args));
            if (isValidProc(args)) {
                getLog().debug("[" + pids[i] + "] is valid");
                try {
                    cwd = getSigar().getProcExe(pids[i]).getCwd();
                } catch (SigarException e) {
                    getLog().debug("[" + pids[i] + "] Error getting process info, reason: '" + e.getMessage() + "'");
                }
                for (int j = 0; j < args.length; j++) {
                    String arg = args[j];
                    if (arg.startsWith("-Dweblogic.Name")) {
                        name = arg.substring(arg.indexOf("=") + 1).trim();
                    } else if ((cwd == null) && arg.startsWith("-D")) {
                        int ix = arg.indexOf("=");
                        if (ix != -1) {
                            String path = arg.substring(ix + 1).trim();
                            if (isAdminDir(path)) {
                                cwd = path;
                            }
                        }
                    }
                }

                getLog().debug("[" + pids[i] + "] cwd = '" + cwd + "' name = '" + name + "'");
                if (cwd != null) {
                    servers.add(new WLSProcWithPid(cwd, name,pids[i]));
                }
            } else {
                getLog().debug("[" + pids[i] + "] is not valid");
            }
        }

        return servers;
    }

    public abstract boolean isValidProc(String[] args);

    private static String getInstallRoot(String installpath) {
        final String jar =
                "server" + File.separator
                + "lib" + File.separator
                + "weblogic.jar";

        File dir = new File(installpath);
        while (dir != null) {
            if (new File(dir, jar).exists()) {
                return dir.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }

        return null;
    }

    public static String getRunningInstallPath() {
        String installpath = null;
        long[] pids = getPids(PTQL_QUERY);

        for (int i = 0; i < pids.length; i++) {
            String[] args = getProcArgs(pids[i]);

            for (int j = 1; j < args.length; j++) {
                String arg = args[j];

                if (arg.startsWith("-D")) {
                    //e.g. -Dplatform.home=$PWD
                    int ix = arg.indexOf("=");
                    if (ix == -1) {
                        continue;
                    }
                    //e.g. 6.1 petstore is  -Djava.security.policy==...
                    if (arg.startsWith("=")) {
                        arg = arg.substring(1, arg.length());
                    }

                    arg = arg.substring(ix + 1).trim();
                    if (!new File(arg).exists()) {
                        continue;
                    }
                } else {
                    continue;
                }

                installpath = getInstallRoot(arg);
                if (installpath != null) {
                    log.debug(WeblogicProductPlugin.PROP_INSTALLPATH + "="
                            + installpath + " (derived from " + args[j] + ")");
                    break;
                }
            }
        }

        return installpath;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        getLog().debug("[discoverServices] config=" + config);
        List services = new ArrayList();
        List aServices = new ArrayList();
        try {
            WeblogicDiscover discover = new WeblogicDiscover(getTypeInfo().getVersion(), config.toProperties());
            MBeanServer mServer = discover.getMBeanServer();
            discover.init(mServer);
            NodeManagerQuery nodemgrQuery = new NodeManagerQuery();
            ServerQuery serverQuery = new ServerQuery();
            serverQuery.setDiscover(discover);
            serverQuery.setName(config.getValue("server"));
            ArrayList servers = new ArrayList();
            discover.find(mServer, serverQuery, servers);
            WeblogicQuery[] serviceQueries = discover.getServiceQueries();

            for (int j = 0; j < serviceQueries.length; j++) {
                WeblogicQuery serviceQuery = serviceQueries[j];

                serviceQuery.setParent(serverQuery);
                serviceQuery.setVersion(serverQuery.getVersion());

                discover.find(mServer, serviceQuery, services);
            }

            for (int k = 0; k < services.size(); k++) {
                boolean valid = true;
                ServiceQuery service = (ServiceQuery) services.get(k);
                if (service instanceof ApplicationQuery) {
                    valid = ((ApplicationQuery) service).isEAR();
                }
                if (valid) {
                    aServices.add(generateService(service));

                } else {
                    log.debug("skipped service:" + service.getName());
                }
            }


        } catch (WeblogicDiscoverException ex) {
            getLog().debug(ex.getMessage(), ex);
        }
        return aServices;
    }

    public static ServiceResource generateService(ServiceQuery service) throws PluginException {
        ServiceResource aiservice = new ServiceResource();

        ConfigResponse productConfig = new ConfigResponse(service.getResourceConfig());
        ConfigResponse metricConfig = new ConfigResponse();
        ConfigResponse cprops = new ConfigResponse(service.getCustomProperties());

        String notes = service.getDescription();
        if (notes != null) {
            aiservice.setDescription(notes);
        }

        aiservice.setType(service.getResourceName());

        String name = service.getResourceFullName();
//        if (usePlatformName) {
//            name = GenericPlugin.getPlatformName() + " " + name;
//        }
        if (name.length() >= 200) {
            // make sure we dont exceed service name limit
            name = name.substring(0, 199);
        }
        aiservice.setName(name);

        if (service.hasControl() && !service.isServer61()) {
            ConfigResponse controlConfig = new ConfigResponse(service.getControlConfig());
            aiservice.setControlConfig(controlConfig);
        }

        aiservice.setProductConfig(productConfig);
        aiservice.setMeasurementConfig(metricConfig);
        aiservice.setCustomProperties(cprops);

        if (service.hasResponseTime()) {
            ConfigResponse rtConfig = new ConfigResponse(service.getResponseTimeConfig());
            aiservice.setResponseTimeConfig(rtConfig);
        }

        log.debug("discovered service: " + aiservice.getName());

        return aiservice;
    }

    abstract void setIdentifier(ServerResource server, String name);

    public class WLSProc {

        private String path;
        private String name;

        public WLSProc(String path, String name) {
            this.path = path;
            this.name = name;
        }

        /**
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * @param path the path to set
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }
    }
    
    protected class WLSProcWithPid extends WLSProc {
        public WLSProcWithPid(String path, String name, long pid) {
            super(path, name);
            this.pid = pid;
        }

        public long getPid() {
            return pid;
        }

        public void setPid(long pid) {
            this.pid = pid;
        }

        protected long pid;
        
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

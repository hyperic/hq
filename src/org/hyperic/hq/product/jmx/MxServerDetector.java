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

package org.hyperic.hq.product.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class MxServerDetector
    extends DaemonDetector
    implements AutoServerDetector
{
    private static final Log log = LogFactory.getLog(MxServerDetector.class);
    static final String PROP_SERVICE_NAME = "name";
    public static final String PROC_MAIN_CLASS    = "PROC_MAIN_CLASS";
    public static final String PROC_HOME_PROPERTY = "PROC_HOME_PROPERTY";
    public static final String PROC_HOME_ENV      = "PROC_HOME_ENV";
    public static final String PROP_PROCESS_QUERY = "process.query";
    protected static final String PROC_JAVA = "State.Name.sw=java";
    protected static final String SUN_JMX_REMOTE =
        "-Dcom.sun.management.jmxremote";
    protected static final String SUN_JMX_PORT = 
        SUN_JMX_REMOTE + ".port=";

    protected static String getMxURL(String port) {
        return
        "service:jmx:rmi:///jndi/rmi://localhost:" +
        port + "/jmxrmi";
    }

    protected String parseMxPort(String arg) {
        if (!arg.startsWith(SUN_JMX_PORT)) {
            return null;
        }
        return arg.substring(SUN_JMX_PORT.length());
    }

    protected boolean configureMxURL(ConfigResponse config, String arg) {
        final String prop = SUN_JMX_REMOTE + "=";
        if (arg.startsWith(prop)) {
            String url = arg.substring(prop.length());
            if (url.startsWith(MxUtil.PTQL_PREFIX)) {
                //local access enabled via:
                //-Dcom.sun.management.jmxremote=ptql:State.Name.eq=java,...
                config.setValue(MxUtil.PROP_JMX_URL, url);
                return true;
            }
        }

        String port = parseMxPort(arg);
        if (port == null) {
            return false;
        }

        String url = getMxURL(port);
        //remote access enabled via:
        //-Dcom.sun.management.jmxremote.port=xxxx
        config.setValue(MxUtil.PROP_JMX_URL, url);

        return true;
    }

    protected boolean configureLocalMxURL(ConfigResponse config,
                                          String arg, String query) {
        if ((query == null) || !arg.equals(SUN_JMX_REMOTE)) {
            return false;
        }

        try {
            //verify local url access is supported by this JVM
            //and we have the appropriate permissions
            MxUtil.getUrlFromPid(query);
            config.setValue(MxUtil.PROP_JMX_URL,
                            MxUtil.PTQL_PREFIX + query);
            return true;
        } catch (Exception e) {
            log.debug(e.getMessage());
            return false;
        }
    }

    protected String getProcMainClass() {
        return getTypeProperty(PROC_MAIN_CLASS);
    }

    protected String getProcHomeProperty() {
        return getTypeProperty(PROC_HOME_PROPERTY);
    }

    protected String getProcHomeEnv() {
        return getTypeProperty(PROC_HOME_ENV);
    }

    private String getProcHomeEnv(long pid) {
        String key = getProcHomeEnv();
        if (key == null) {
            return null;
        }

        try {
            String val = getSigar().getProcEnv(pid, key);
            return val;
        } catch (SigarException e) {
            return null;
        }
    }

    protected String getProcQuery() {
        StringBuffer query = new StringBuffer();
        String mainClass = getProcMainClass(); 
        query.append(PROC_JAVA);

        if (mainClass == null) {
            String homeProp = getProcHomeProperty();
            if (homeProp == null) {
                String msg =
                    "No " + PROC_MAIN_CLASS + " or " +
                    PROC_HOME_PROPERTY + " defined";
                throw new IllegalStateException(msg);
            }
            query.append(",Args.*.sw=-D" + homeProp + "=");
        }
        else {
            query.append(",Args.*.eq=" + mainClass);
        }

        return query.toString();
    }

    protected class MxProcess {
        long _pid;
        String _installpath;
        String[] _args;
        String _url;

        protected MxProcess(long pid,
                            String[] args,
                            String installpath) {
            _pid = pid;
            _args = args;
            _installpath = installpath;
        }

        public long getPid() {
            return _pid;
        }

        public String getInstallPath() {
            return _installpath;
        }

        public String[] getArgs() {
            return _args;
        }

        public String getURL() {
            return _url;
        }

        public void setURL(String url) {
            _url = url;
        }
    }

    protected List getServerProcessList()
    {
        List procs = new ArrayList();
        long[] pids = getPids(getProcQuery());
        log.debug(getProcQuery() + " matched " + pids.length + " processes");

        String homeProp = "-D" + getProcHomeProperty() + "=";

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];
            //need to find installpath for each match
            //-Dfoo.home arg, FOO_HOME env var or cwd
            String[] args = getProcArgs(pid);
            String path = null;

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (arg.startsWith(homeProp)) {
                    path = arg.substring(homeProp.length());
                    break;
                }
            }

            if (path == null) {
                path = getProcHomeEnv(pid);
            }
            if (path == null) {
                path = getProcCwd(pid);
            }

            if (path != null) {
                MxProcess process =
                    new MxProcess(pid,
                                  args,
                                  path);        
                procs.add(process);
            }
        }

        return procs;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        setPlatformConfig(platformConfig);

        List servers = new ArrayList();
        List procs = getServerProcessList();

        for (int i=0; i<procs.size(); i++) {
            MxProcess process = (MxProcess)procs.get(i);
            String dir = process.getInstallPath();

            if (!isInstallTypeVersion(dir)) {
                continue;
            }
            String query =
                PROC_JAVA + ",Args.*.eq=-D" +
                getProcHomeProperty() + "=" + dir;

            // Create the server resource
            ServerResource server = newServerResource(dir);
            adjustClassPath(dir);

            ConfigResponse config = new ConfigResponse();
            ConfigSchema schema =
                getConfigSchema(getTypeInfo().getName(),
                                ProductPlugin.CFGTYPE_IDX_PRODUCT);

            if (schema != null) {
                ConfigOption option =
                    schema.getOption(PROP_PROCESS_QUERY);

                if (option != null) {
                    // Configure process.query
                    config.setValue(option.getName(), query);
                }
            }

            if (process.getURL() != null) {
                config.setValue(MxUtil.PROP_JMX_URL,
                                process.getURL());
            }
            else {
                String[] args = process.getArgs();
                for (int j=0; j<args.length; j++) {
                    if (configureMxURL(config, args[j])) {
                        break;
                    }
                    else if (configureLocalMxURL(config, args[j], query)) {
                        //continue as .port might come later
                    }
                }
            }

            // default anything not auto-configured
            setProductConfig(server, config);
            discoverServerConfig(server, process.getPid());

            server.setMeasurementConfig();
            servers.add(server);
        }

        return servers;
    }

    protected List discoverMxServices(MBeanServerConnection mServer,
                                      ConfigResponse serverConfig)
        throws PluginException {

        String url = serverConfig.getValue(MxUtil.PROP_JMX_URL);

        configure(serverConfig); //for MxServerQuery to use detector.getConfig()
        MxServerQuery serverQuery = new MxServerQuery(this);
        String objName = getTypeProperty(MxQuery.PROP_OBJECT_NAME);

        if (objName != null) {
            try {
                objName = Metric.translate(objName, serverConfig);
                serverQuery.setObjectName(new ObjectName(objName));
            } catch (MalformedObjectNameException e) {
                throw new PluginException(objName, e);
            }
        }

        serverQuery.setURL(url);
        serverQuery.getAttributes(mServer);

        serverQuery.findServices(mServer);

        List queries = serverQuery.getServiceQueries();
        getLog().debug("discovered " + queries.size() + " services");

        List services = new ArrayList();

        for (int i=0; i<queries.size(); i++) {
            MxServiceQuery query = (MxServiceQuery)queries.get(i);
            ServiceResource service = new ServiceResource();
            ConfigResponse config =
                new ConfigResponse(query.getResourceConfig());
            ConfigResponse cprops =
                new ConfigResponse(query.getCustomProperties());
            
            service.setType(query.getResourceType());

            String name =
                formatAutoInventoryName(service.getType(),
                                     serverConfig,
                                     config,
                                     cprops);

            if (name == null) {
                //prefix w/ server name
                name = ServiceResource.SERVER_NAME_PREFIX;

                String queryName = query.getName();
                if ((queryName != null) && (queryName.length() != 0)) {
                    name += query.getName() + " ";
                }

                name += query.getServiceResourceType();
            }

            service.setName(name);

            if (query.hasControl()) { 
                ConfigResponse controlConfig =
                    new ConfigResponse(query.getControlConfig());
                service.setControlConfig(controlConfig);
            }
            
            service.setProductConfig(config);
            service.setMeasurementConfig();

            service.setCustomProperties(cprops);

            services.add(service);
        }

        setCustomProperties(new ConfigResponse(serverQuery.getCustomProperties()));

        return services;
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        JMXConnector connector;
        MBeanServerConnection mServer;
    
        try {
            connector = MxUtil.getMBeanConnector(serverConfig.toProperties());
            mServer = connector.getMBeanServerConnection();
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        try {
            return discoverMxServices(mServer, serverConfig);
        } finally {
            try {
                connector.close();
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        }
    }
}

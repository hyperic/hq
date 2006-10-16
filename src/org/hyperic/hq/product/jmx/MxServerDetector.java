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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class MxServerDetector
    extends DaemonDetector
    implements AutoServerDetector
{
    static final String PROP_SERVICE_NAME = "name";
    private static final String PROC_MAIN_CLASS    = "PROC_MAIN_CLASS";
    private static final String PROC_HOME_PROPERTY = "PROC_HOME_PROPERTY";
    private static final String PROP_PROCESS_QUERY = "process.query";
    protected static final String PROC_JAVA = "State.Name.sw=java";
    protected static final String SUN_JMX_PORT = 
        "-Dcom.sun.management.jmxremote.port=";

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
        String port = parseMxPort(arg);
        if (port == null) {
            return false;
        }

        String url = getMxURL(port);
        config.setValue(MxUtil.PROP_JMX_URL, url);

        return true;
    }

    protected String getProcMainClass() {
        return getTypeProperty(PROC_MAIN_CLASS);
    }

    protected String getProcHomeProperty() {
        return getTypeProperty(PROC_HOME_PROPERTY);
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
        String homeProp = "-D" + getProcHomeProperty() + "=";

        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);
            
            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (arg.startsWith(homeProp)) {
                    MxProcess process =
                        new MxProcess(pids[i],
                                      args,
                                      arg.substring(homeProp.length()));        
                    procs.add(process);
                }
            }
        }

        return procs;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        List servers = new ArrayList();
        List procs = getServerProcessList();
        String versionFile = getTypeProperty("VERSION_FILE");

        for (int i=0; i<procs.size(); i++) {
            MxProcess process = (MxProcess)procs.get(i);
            String dir = process.getInstallPath();

            //optional use a file to determine correct type version
            if (versionFile != null) {
                File file = new File(dir, versionFile);
                if (!file.exists()) {
                    getLog().debug(file + " does not exist, skipping");
                    continue;
                }
            }

            // Create the server resource
            ServerResource server = createServerResource(dir);
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
                    String query =
                        PROC_JAVA + ",Args.*.eq=-D" +
                        getProcHomeProperty() + "=" + dir;
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
                }
            }

            // default anything not auto-configured
            setProductConfig(server, config);

            String name =
                formatAutoInventoryName(server.getType(),
                                     platformConfig,
                                     server.getProductConfig(),
                                     new ConfigResponse());

            if (name != null) {
                server.setName(name);
            }

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

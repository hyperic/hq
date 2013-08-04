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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class MxServerDetector
    extends DaemonDetector
    implements AutoServerDetector
{
    private static final String SUN_REMOTE_AUTHENTICATION_FALSE = "com.sun.management.jmxremote.authenticate=false";
    private static final String TEMPLATE_PROPERTY = "template";
    private static final String CONTROL_CLASS_PROPERTY = "control-class";
    private static final String MEASUREMENT_CLASS_PROPERTY = "measurement-class";
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
    
    private ServiceTypeFactory serviceTypeFactory = new ServiceTypeFactory();
   

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
    
    /**
     * First checks if the ptql query is specified in the process properties, 
     * if not, then it checks to see if the port value is specified, to generate the service URL.
     * @return True if configured, otherwise false.
     */
    protected boolean configureMxURL(ConfigResponse config, String arg){
        final String prop = SUN_JMX_REMOTE + "=";
        if (arg.startsWith(prop)){
            String subString = arg.substring(prop.length());
            if (subString.startsWith(MxUtil.PTQL_PREFIX)) {
                log.debug("Found jmx ptql query for local pid connection: " + subString);
                // local access enabled via:
                // -Dcom.sun.management.jmxremote=ptql:State.Name.eq=java,...
                config.setValue(MxUtil.PROP_JMX_URL, subString);
                return true;
            }
        } 
        String port;
        if ((port = parseMxPort(arg)) != null) {
            String serviceUrl = getMxURL(port);
            log.debug("Found jmx port, creating service url:" + serviceUrl);
            // remote access enabled via:
            // -Dcom.sun.management.jmxremote.port=xxxx
            config.setValue(MxUtil.PROP_JMX_URL, serviceUrl);
            // for use in name %jmx.port% template
            config.setValue(MxUtil.PROP_JMX_PORT, port);
            return true;
        } 
        return false;
    }
    
    /**
     * Goes through several checks in order to find the jmx url in the configuration.
     * For each process argument it will check the following:
     * 1) Does it use the ptql query option to detect the process (i.e -Dcom.sun.management.jmxremote=ptql:State.Name...).
     * 2) Does it match -Dcom.sun.management.jmxremote.port=<port>.
     * 3) Did the user configure the jmx.url in the ui(optionally the jmx.username and jmx.password).
     * 4) Does it match the ptql query generated using the install path.
     * 5) Otherwise it returns false.
     * 
     * @param config
     * @param args
     * @param processQuery
     * @return True if the url was set, otherwise false.
     */
    protected boolean findAndSetURL(ConfigResponse config, List<String> args, String processQuery) {
        boolean authenticationNotRequired = args.contains(SUN_REMOTE_AUTHENTICATION_FALSE);
        for (String arg : args) {
            if (!configureMxURL(config, arg)) {
                if (!configureUserSpecifiedMxUrl(config, arg, authenticationNotRequired)) {
                    if (!configureLocalMxURL(config, arg, processQuery)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
        
    private boolean configureUserSpecifiedMxUrl(ConfigResponse config, String arg,
                                                boolean authenticationNotRequired) {
        String mxUrl = config.getValue(MxUtil.PROP_JMX_URL);
        boolean urlSet = false;
        if (mxUrl != null) {
            log.debug("Using jmx.url specified in the configuration: " + mxUrl);
            urlSet = true;
            config.setValue(MxUtil.PROP_JMX_URL, mxUrl);
            if (!authenticationNotRequired) {
                String username = config.getValue(MxUtil.PROP_JMX_USERNAME);
                String password = config.getValue(MxUtil.PROP_JMX_PASSWORD);
                if (username != null && password != null) {
                    log.debug("Found username and password for JMX auth.");
                    // only set these values if both exist.
                    config.setValue(MxUtil.PROP_JMX_USERNAME, username);
                    config.setValue(MxUtil.PROP_JMX_PASSWORD, password);
                } else {
                    log.debug("No username and/or password was specified for JMX connection.");
                }
            }
        }
        return urlSet;
    }
    

    protected boolean configureLocalMxURL(ConfigResponse config, String arg, String query) {
        String mxUrl = null;
        boolean urlSet = false;

        try {
            //verify local url access is supported by this JVM
            //and we have the appropriate permissions
            // MxUtil will throw an exception if it doesn't get the pid url
            MxUtil.getUrlFromPid(query);
            mxUrl = MxUtil.PTQL_PREFIX + query;
            config.setValue(MxUtil.PROP_JMX_URL, mxUrl);
            urlSet = true;
            log.debug("Using the local pid to create jmx url: " + mxUrl);
        } catch (Exception e) {
            log.debug("Cannot configure jmx.url using local pid: " +
                      e.getMessage(),e);
        }
        return urlSet;
        
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
        return getProcQuery(null);
    }

    private boolean isMatch(String val) {
        return val.indexOf('=') != -1;
    }

    protected String getProcQuery(String path) {
        StringBuffer query = new StringBuffer();
        String mainClass = getProcMainClass(); 
        query.append(PROC_JAVA);

        if (mainClass != null) {
            query.append(",Args.*.eq=" + mainClass);
        }

        String homeProp = getProcHomeProperty();
        if (homeProp != null) {
            boolean isMatch = isMatch(homeProp);
            if (path == null) {
                query.append(",Args.*.");
                if (isMatch) {
                    query.append("re=-D" + homeProp);
                }
                else {
                    query.append("sw=-D" + homeProp + "=");
                }
            }
            else {
                if (isMatch) {
                    int ix = homeProp.indexOf('=');
                    if (ix != -1) {
                        homeProp = homeProp.substring(0, ix);
                    }
                }
                //expand to exact match if given path
                query.append(",Args.*.eq=-D" + homeProp + "=" + path);
            }
        }
        
        if ((homeProp == null) && (mainClass == null)) {
            String msg =
                "No " + PROC_MAIN_CLASS + " or " +
                PROC_HOME_PROPERTY + " defined";
            throw new IllegalStateException(msg);
        }
        if (log.isDebugEnabled()) log.debug("using ptql query=" + query);
        return query.toString();
    }

    public static class MxProcess {
        long _pid;
        String _installpath;
        String[] _args;
        String _url;

        public MxProcess(long pid,
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

    private boolean matches(String source, String regex) {
        return Pattern.compile(regex).matcher(source).find();
    }

    protected List getServerProcessList() {
        List procs = new ArrayList();
        String query = getProcQuery();
        long[] pids = getPids(query);
        if (log.isDebugEnabled()) log.debug("ptql=" + query + " matched pids=" + Arrays.asList(pids));
 
        String homeProp = getProcHomeProperty();
        final boolean isMatch = isMatch(homeProp);
        if (isMatch) {
            homeProp = "-D" +  homeProp;
        } else {
            homeProp = "-D" +  homeProp + "=";
        }

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];
            //need to find installpath for each match
            //-Dfoo.home arg, FOO_HOME env var or cwd
            String[] args = getProcArgs(pid);
            String path = null;

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (isMatch) {
                    if (matches(arg, homeProp)) {
                        int ix = arg.indexOf('=');
                        if (ix != -1) {
                            path = arg.substring(ix+1);
                            break;
                        }
                    }
                }
                else if (arg.startsWith(homeProp)) {
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
    
    protected boolean isInstallTypeVersion(MxProcess process) {
        String dir = process.getInstallPath();
        return isInstallTypeVersion(dir);
    }
    
    protected void setProductConfig(ServerResource server, ConfigResponse config, long pid) {
        super.setProductConfig(server, config);
    }
    
    protected  ServerResource getServerResource(MxProcess process) {
        String dir = process.getInstallPath();
        //set process.query using the same query used to find the process,
        //with PROC_HOME_DIR (if defined) expanded to match dir
        String query = getProcQuery(dir);

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
		
		try {
			setJmxUrl(process,config);
		} catch (MxRuntimeException e){
			if (log.isDebugEnabled()){
				log.debug(e.getMessage(), e);
			} else {
				log.info(e.getMessage());
			}
		}

        // default anything not auto-configured
        setProductConfig(server, config,process.getPid());
        discoverServerConfig(server, process.getPid());

        server.setMeasurementConfig();
        return server;
    }
    
    /**
     * Sets the JMX url.
     * First checks whether the process supplies the jmx.url. Then does some searching for the url.
     * @param process
     * @param config
     * @throws MxRuntimeException If there is no jmx.url found.
     */
    protected void setJmxUrl(MxProcess process, ConfigResponse config) {
        boolean urlSet = false;
        String processQuery = getProcQuery(process.getInstallPath());
        if (process.getURL() != null) {
            log.debug("Found jmx.url in the process args: " + process.getURL());
            // check if jmx.url was found in the process props.
            config.setValue(MxUtil.PROP_JMX_URL, process.getURL());
            urlSet = true;
        } else {
            // check for url in other places
            List<String> args = Arrays.asList(process.getArgs());
            urlSet = findAndSetURL(config, args, processQuery);
        }
        if (!urlSet) {
            throw new MxRuntimeException(
                "Unable to find the jmx.url in configuration properties: " + config);
        }
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        setPlatformConfig(platformConfig);

        List servers = new ArrayList();
        List procs = getServerProcessList();

        for (int i=0; i<procs.size(); i++) {
            MxProcess process = (MxProcess)procs.get(i);
            
            if (!isInstallTypeVersion(process)) {
                continue;
            }
            servers.add(getServerResource(process));
        }

        return servers;
    }

    protected List discoverMxServices(MBeanServerConnection mServer,
                                      ConfigResponse serverConfig)
        throws PluginException {

        String url = serverConfig.getValue(MxUtil.PROP_JMX_URL);
        log.debug("[discoverMxServices] url="+url);

        configure(serverConfig); //for MxServerQuery to use detector.getConfig()
        MxServerQuery serverQuery = new MxServerQuery(this);
        String objName = getTypeProperty(MxQuery.PROP_OBJECT_NAME);
        log.debug("[discoverMxServices] objName="+objName);

        if (objName != null) {
            try {
                objName = Metric.translate(objName, serverConfig);
                log.debug("[discoverMxServices] objName="+objName);
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
        log.debug("[discoverServices] serverConfig="+serverConfig);

        JMXConnector connector = null;
        MBeanServerConnection mServer;
    
        try {
            connector = MxUtil.getCachedMBeanConnector(serverConfig.toProperties());
            mServer = connector.getMBeanServerConnection();
        } catch (Exception e) {
            MxUtil.close(connector);
            throw new PluginException(e.getMessage(), e);
        }

        try {
            return discoverMxServices(mServer, serverConfig);
        } finally {
            MxUtil.close(connector);
        }
    }
    
   
    
    public Set discoverServiceTypes(ConfigResponse serverConfig)
	throws PluginException {
    	 JMXConnector connector = null;
         MBeanServerConnection mServer;
         Set serviceTypes = new HashSet();
         
         //plugins need to define these properties at the plugin level to discover dynamic service types
         if(getProductPlugin().getPluginData()
			.getProperty(MEASUREMENT_CLASS_PROPERTY) == null || getProductPlugin().getPluginData()
			.getProperty(CONTROL_CLASS_PROPERTY) == null || getProductPlugin().getPluginData()
			.getProperty(TEMPLATE_PROPERTY) == null) {
        	 return serviceTypes;
         }
         
         try {
            connector = MxUtil.getCachedMBeanConnector(serverConfig.toProperties());
            mServer = connector.getMBeanServerConnection();
         } catch (Exception e) {
             MxUtil.close(connector);
             throw new PluginException(e.getMessage(), e);
         } 

    	try {
			final Set objectNames = mServer.queryNames(new ObjectName(MBeanUtil.DYNAMIC_SERVICE_DOMAIN + ":*"), null);
			serviceTypes = serviceTypeFactory.create(getProductPlugin(), (ServerTypeInfo)getTypeInfo(), mServer, objectNames);
		} catch (Exception e) {
			 throw new PluginException(e.getMessage(), e);
		} finally {
		    MxUtil.close(connector);
        }
		return serviceTypes;
    }

}

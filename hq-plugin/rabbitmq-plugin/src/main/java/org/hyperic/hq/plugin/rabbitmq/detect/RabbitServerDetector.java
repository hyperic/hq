/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.detect;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.autoinventory.agent.client.AICommandsUtils;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitTransientResourceManager;
import org.hyperic.hq.plugin.rabbitmq.manage.TransientResourceManager;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;

/**
 * RabbitServerDetector
 * @author Helena Edelson
 * @author German Laullon
 * @author Patrick Nguyen
 */
public class RabbitServerDetector extends ServerDetector implements AutoServerDetector {

    private static final Log logger = LogFactory.getLog(RabbitServerDetector.class);
    private final static String PTQL_QUERY = "State.Name.re=[beam|erl],Args.*.eq=-sname";
    private static Map<String, String> signatures = new HashMap();

    /**
     * @param platformConfig
     * @return
     * @throws PluginException
     */
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        logger.debug("[getServerResources] platformConfig=" + platformConfig);

        List<ServerResource> resources = new ArrayList<ServerResource>();
        long[] pids = getPids(PTQL_QUERY);
        logger.debug("[getServerResources] pids.length=" + pids.length);

        if (pids.length > 0) {
            List<String> nodes = new ArrayList<String>();

            for (long nodePid : pids) {
                final String nodeArgs[] = getProcArgs(nodePid);
                final String nodePath = getNodePath(nodeArgs);
                final String nodeName = getServerName(nodeArgs);
                
                if (nodePath != null && !nodes.contains(nodePath)) {
                    nodes.add(nodePath);

                    ServerResource server = doCreateServerResource(nodeName, nodePath, nodePid, nodeArgs);
                    if (server != null) {
                        resources.add(server);

                        if (logger.isDebugEnabled()) {
                            StringBuilder sb = new StringBuilder("Discovered ").append(server.getName()).append(" productConfig=").append(server.getProductConfig()).append(" customProps=").append(server.getCustomProperties());
                            logger.debug(sb.toString());
                        }

                        String new_signature = generateSignature(server);
                        String node = server.getProductConfig().getValue(DetectorConstants.NODE);
                        String signature = signatures.get(node);
                        if (!new_signature.equalsIgnoreCase(signature)) {
                            if (signature != null) {
                                runAutoDiscovery(server.getProductConfig());
                            }
                            signatures.put(node, new_signature);
                        }
                    }
                }
            }
        }

        return resources;
    }

    public void runAutoDiscovery(ConfigResponse cf) {
        logger.debug("[runAutoDiscovery] >> start");
        try {
            AgentRemoteValue configARV = AICommandsUtils.createArgForRuntimeDiscoveryConfig(0, 0, "RabbitMQ", null, cf);
            logger.debug("[runAutoDiscovery] configARV=" + configARV);
            AgentCommand ac = new AgentCommand(1, 1, "autoinv:pushRuntimeDiscoveryConfig", configARV);
            AgentDaemon.getMainInstance().getCommandDispatcher().processRequest(ac, null, null);
            logger.debug("[runAutoDiscovery] << OK");
        } catch (Exception ex) {
            logger.debug("[runAutoDiscovery]" + ex.getMessage(), ex);
        }
    }

    /**
     * Creates ServiceResources from RabbitMQ processes
     * as well as Queues, Exchanges, etc.
     * @param config Configuration of the parent server resource.
     * @return
     * @throws PluginException
     */
    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        logger.debug("[discoverServices] config=" + config);
        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        List<ServiceResource> rabbitResources = createRabbitResources(config);
        if (rabbitResources != null && rabbitResources.size() > 0) {
            serviceResources.addAll(rabbitResources);
            syncServices(config, rabbitResources);
        }
        return serviceResources;
    }

    private void syncServices(ConfigResponse serviceConfig, List<ServiceResource> rabbitResources) {
        boolean autoSync = serviceConfig.getValue(DetectorConstants.AUTO_SYNC, "false").equals("true");
        logger.debug("[syncServices] autoSync=" + autoSync + " " + rabbitResources.size() + " resources");
        if (autoSync) {
            try {
                Properties props = new Properties();
                props.putAll(serviceConfig.toProperties());
                props.putAll(getManager().getProperties());

                TransientResourceManager manager = new RabbitTransientResourceManager(props);
                manager.syncServices(rabbitResources);
            } catch (Throwable e) {
                logger.debug("Could not sync transient services: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create RabbitMQ-specific resources to add to inventory.
     * @param serviceConfig
     * @return
     * @throws PluginException
     */
    public List<ServiceResource> createRabbitResources(ConfigResponse serviceConfig) throws PluginException {
        List<ServiceResource> rabbitResources = null;
        List<RabbitObject> rabbitObjectss = new ArrayList();

        if (getLog().isDebugEnabled()) {
            getLog().debug("[createRabbitResources] serviceConfig=" + serviceConfig);
        }

        String node = serviceConfig.getValue(DetectorConstants.SERVER_NAME);
        boolean noDurable = serviceConfig.getValue(DetectorConstants.NO_DURABLE, "false").equals("true");

        HypericRabbitAdmin admin = new HypericRabbitAdmin(serviceConfig);
        try {
            rabbitObjectss.addAll(admin.getConnections());
        } catch (PluginException ex) {
            logger.debug("[createRabbitResources] error with Connections: "+ex.getMessage(), ex);
        }

        try {
            rabbitObjectss.addAll(admin.getChannels());
        } catch (PluginException ex) {
            logger.debug("[createRabbitResources] error with Channels: "+ex.getMessage(), ex);
        }

        try {
            List<RabbitVirtualHost> vhosts = admin.getVirtualHosts();
            for (RabbitVirtualHost vhost : vhosts) {
                try {
                    rabbitObjectss.addAll(admin.getQueues(vhost));
                } catch (PluginException ex) {
                    logger.debug("[createRabbitResources] error with Queues on "+vhost+": "+ex.getMessage(), ex);
                }
                try {
                    rabbitObjectss.addAll(admin.getExchanges(vhost));
                } catch (PluginException ex) {
                    logger.debug("[createRabbitResources] error with Exchanges on "+vhost+": "+ex.getMessage(), ex);
                }
            }
            rabbitObjectss.addAll(vhosts);
        } catch (PluginException ex) {
            logger.debug(ex, ex);
        }

        rabbitResources = doCreateServiceResources(rabbitObjectss, node, noDurable);
        return rabbitResources;
    }

    /**
     * For each AMQP type we auto-detect, create ServiceResources that
     * are mostly non-specific to each type. We do some handling that is
     * type-specific if necessary.
     * @param rabbitObjects
     * @param rabbitType
     * @param vHost
     * @return
     */
    private List<ServiceResource> doCreateServiceResources(List<RabbitObject> rabbitObjects, String node, boolean noDurable) {
        List<ServiceResource> serviceResources = null;

        if (rabbitObjects != null) {
            serviceResources = new ArrayList<ServiceResource>();

            for (RabbitObject obj : rabbitObjects) {
                if (obj.isDurable() || noDurable) {
                    ServiceResource service = createServiceResource(obj.getServiceType());
                    service.setName(node + " " + obj.getServiceName());
                    setProductConfig(service, obj.getProductConfig());
                    service.setCustomProperties(obj.getCustomProperties());
                    service.setMeasurementConfig();
                    serviceResources.add(service);
                }
            }
        }

        return serviceResources;
    }

    /**
     * Configure a ServerResource
     * @param nodeName
     * @param nodePath
     * @param nodeArgs
     * @param nodePid
     * @return
     */
    private ServerResource doCreateServerResource(String nodeName, String nodePath, long nodePid, String[] nodeArgs) throws PluginException {
        logger.debug("doCreateServerResource");

        ServerResource node = createServerResource(nodePath);
        node.setIdentifier(nodePath);
        node.setName(getPlatformName() + " " + getTypeInfo().getName() + " Node " + nodeName);
        node.setDescription(getTypeInfo().getName() + " Node " + nodePid);


        ConfigResponse conf = new ConfigResponse();
        for (int n = 0; n < nodeArgs.length; n++) {
            if (nodeArgs[n].equalsIgnoreCase("-rabbit_mochiweb") && nodeArgs[n + 1].equalsIgnoreCase("port")) {
                conf.setValue("port", nodeArgs[n + 2]);
            }
        }
        conf.setValue(DetectorConstants.SERVER_NAME, nodeName);
		
        populateListeningPorts(nodePid , conf, true);
        
        logger.debug("ProductConfig[" + conf + "]");

//        ConfigResponse custom = createCustomConfig(nodeName, nodePath, nodePid, nodeArgs);
//        if (custom != null) {
//            node.setCustomProperties(custom);
//        }

        ConfigResponse log = createLogConfig(nodeArgs);
        if (log != null) {
            setMeasurementConfig(node, log);
        }

        setProductConfig(node, conf);

        return node;
    }

    /**
     * -kernel error_logger {file,"/path/to/rabbitnode@localhost.log"}
     * @param nodeArgs
     * @return
     */
    private ConfigResponse createLogConfig(String[] nodeArgs) {
        Pattern p = Pattern.compile("[{]file,\\s*\"([^\"]+)\"}");

        ConfigResponse logConfig = null;

        for (int n = 0; n < nodeArgs.length; n++) {
            if (nodeArgs[n].equalsIgnoreCase("-kernel") && nodeArgs[n + 1].equalsIgnoreCase("error_logger") && nodeArgs[n + 2].startsWith("{file,")) {
                Matcher m = p.matcher(nodeArgs[n + 2]);
                if (m.find()) {
                    File log = new File(m.group(1));
                    if (log.exists() && log.canRead()) {
                        logConfig = new ConfigResponse();
                        logConfig.setValue(DetectorConstants.SERVER_LOG_TRACK_ENABLE, true);
                        logConfig.setValue(DetectorConstants.SERVER_LOG_TRACK_FILES, log.getAbsolutePath());
                    }
                }
            }
        }

        return logConfig;
    }

    /**
     * Create the server name
     * @param args
     * @return rabbit@host
     */
    private String getServerName(String[] args) {
        String name = null;
        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase(DetectorConstants.SNAME)) {
                name = args[n + 1];
            }
        }
        if ((name != null) && (!name.contains("@"))) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                String hostname = addr.getHostName();
                String old_name = name;
                name += "@" + hostname;
                name = name.substring(0, name.indexOf("."));
                logger.debug(DetectorConstants.SNAME + "=" + old_name + " -> " + name);
            } catch (UnknownHostException ex) {
                name = null;
                logger.debug(ex.getMessage(), ex);
            }
        }
        return name;
    }

    /**
     * Parse -mnesia dir "path/to/mnesia/rabbit_nodename@hostname" to get
     * The current node's path.
     * @param args node PID args
     * @return
     */
    private String getNodePath(String[] args) {
        String mpath = null;

        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase(DetectorConstants.MNESIA) && args[n + 1].equalsIgnoreCase(DetectorConstants.DIR)) {
                mpath = args[n + 2];

                if (mpath.startsWith("\"")) {
                    mpath = mpath.substring(1);
                }
                if (mpath.endsWith("\"")) {
                    mpath = mpath.substring(0, mpath.length() - 1);
                }
                logger.debug("mnesia " + args[n] + " " + args[n + 1] + " " + args[n + 2]);
            }
        }
        return mpath;
    }

    private String generateSignature(ServerResource server) {
        boolean disable = "true".equalsIgnoreCase(getManager().getProperty("rabbitmq.disable.runtimeScan"));
        logger.debug("[generateSignature] rabbitmq.disable.runtimeScan="+disable);
        if (disable) {
            return "";
        }

        List<RabbitObject> objs = new ArrayList();
        try {
            HypericRabbitAdmin admin = new HypericRabbitAdmin(server.getProductConfig());
            objs.addAll(admin.getChannels());
            objs.addAll(admin.getConnections());
            List<RabbitVirtualHost> vhs = admin.getVirtualHosts();
            for (RabbitVirtualHost vh : vhs) {
                objs.addAll(admin.getQueues(vh));
                objs.addAll(admin.getExchanges(vh));
            }
        } catch (PluginException e) {
            logger.debug(e.getMessage(), e);
            objs.clear();
        }

        List<String> names = new ArrayList();
        for (RabbitObject obj : objs) {
            names.add(obj.getServiceName());
        }
        Collections.sort(names);
        return names.toString();
    }
    
    private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            logger.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            logger.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            logger.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.collect.*;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.*;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.util.Assert;

/**
 * RabbitServerDetector
 * @author Helena Edelson
 */
public class RabbitServerDetector extends ServerDetector implements AutoServerDetector {

    private static final Log logger = LogFactory.getLog(RabbitServerDetector.class);

    private final static String PTQL_QUERY = "State.Name.sw=beam,Args.*.eq=-sname";

    /**
     * @param serverConfig
     * @return
     * @throws PluginException
     */
    public List getServerResources(ConfigResponse serverConfig) throws PluginException {
        configure(serverConfig);

        /** ToDo does the caller handle NullPointer's */
        List<ServerResource> resources = new ArrayList<ServerResource>();

        long[] pids = getPids(PTQL_QUERY);

        if (pids.length > 0) {

            List<String> nodes = new ArrayList<String>();

            /** Each node has/is a unique PID */
            for (long nodePid : pids) {

                /** Each Node in Broker */
                final String nodeArgs[] = getProcArgs(nodePid);

                final String nodePath = getNodePath(nodeArgs);

                final String nodeName = getServerName(nodeArgs);

                logger.debug("node.pid=" + nodePid + " node.args=" + Arrays.toString(nodeArgs) + " server.name=" + nodeName);

                if (nodePath != null && !nodes.contains(nodePath)) {
                    nodes.add(nodePath);

                    ServerResource server = doCreateServerResource(nodeName, nodePath, nodePid, nodeArgs);

                    if (server != null) {
                        logger.debug("Created server=" + server.getName());
                        resources.add(server);
                    }
                }
            }
        }

        return resources;
    }

    /**
     * Creates ServiceResources from RabbitMQ processes
     * as well as Queues, Exchanges, etc.
     * @param serviceConfig Configuration of the parent server resource.
     * @return
     * @throws PluginException
     */
    @Override
    protected List discoverServices(ConfigResponse serviceConfig) throws PluginException {
        logger.debug("discoverServices [" + serviceConfig + "]");

        /** For the moment this is how we have to do this. */
        if (serviceConfig.getValue(DetectorConstants.NODE_COOKIE_VALUE) == null) {
            /** Letting throw a potential OtpAuthException so the user can see permissions issue */
            String auth = ErlangCookieHandler.configureCookie(serviceConfig);
            Assert.notNull(auth, "Cookie value for node must not be null");

            /** Since we can never connect without the cookie on any test environment: this is what we have to
             * do for the moment and will modify as soon as possible. */
            if (auth != null && auth.length() > 0) {
                serviceConfig.setValue(DetectorConstants.NODE_COOKIE_VALUE, auth);
            }
        }

        if (serviceConfig.getValue(DetectorConstants.SERVER_NAME) != null) {
            String nodeName = serviceConfig.getValue(DetectorConstants.SERVER_NAME);
            if (nodeName != null) {
                String hostName = getHostFromNode(nodeName);
                if (hostName != null) {
                    serviceConfig.setValue(DetectorConstants.HOST, hostName);
                }
            }
        }

        configure(serviceConfig);

        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        /** configure node process */
        /*List<ServiceResource> processes = createProcessServiceResources(serviceConfig);
        if (processes != null) {
            serviceResources.addAll(processes);
            logger.debug("discoverServices detected " + processes.size() + " processes");
        }*/

        /** get rabbit  services */
        List<ServiceResource> rabbitResources = null;
        try {
            rabbitResources = createRabbitResources(serviceConfig);
        }
        catch (Exception e) {
            logger.error(e);
        }

        if (rabbitResources != null && rabbitResources.size() >= 0) {
            serviceResources.addAll(rabbitResources);
        }

        return serviceResources;
    }

    /**
     * Create RabbitMQ-specific resources to add to inventory.
     * @param serviceConfig
     * @return
     * @throws PluginException
     */
    public List<ServiceResource> createRabbitResources(ConfigResponse serviceConfig) throws PluginException {
        logger.debug("createRabbitResources [" + serviceConfig + "]");
        List<ServiceResource> rabbitResources = null;

        String nodeName = serviceConfig.getValue(DetectorConstants.SERVER_NAME);

        if (RabbitProductPlugin.getRabbitGateway() == null) {
            logger.debug("Initializing gateway");
            RabbitProductPlugin.initializeGateway(serviceConfig);
        }

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();

        try {
            if (rabbitGateway != null) {
                List<String> virtualHosts = rabbitGateway.getVirtualHosts();

                if (virtualHosts != null) {
                    rabbitResources = new ArrayList<ServiceResource>();

                    for (String virtualHost : virtualHosts) {
                        ServiceResource vHost = createServiceResource(DetectorConstants.VIRTUAL_HOST);
                        vHost.setName(new StringBuilder().append(getTypeInfo().getName())
                                .append(" Node: ").append(nodeName).append(" ").append(DetectorConstants.VIRTUAL_HOST).append(": ").append(virtualHost).toString());

                        List<ServiceResource> queues = createQueueServiceResources(rabbitGateway, nodeName, virtualHost);
                        if (queues != null) rabbitResources.addAll(queues);

                        List<ServiceResource> connections = createConnectionServiceResources(rabbitGateway, nodeName, virtualHost);
                        if (connections != null) rabbitResources.addAll(connections);

                        List<ServiceResource> channels = createChannelServiceResources(rabbitGateway, nodeName, virtualHost);
                        if (channels != null) rabbitResources.addAll(channels);

                        List<ServiceResource> exchanges = createExchangeServiceResources(rabbitGateway, nodeName, virtualHost);
                        if (exchanges != null) rabbitResources.addAll(exchanges);
                    }
                }
            }
            if (rabbitResources != null) {
                logger.debug("Created " + rabbitResources.size() + " rabbit services");
            }
        } catch (Exception e) {

        }
        return rabbitResources;
    }


    /**
     * Creates service resources from processes.
     * @return
     */
    List<ServiceResource> createProcessServiceResources(ConfigResponse config) {
        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        long[] pids = getPids(Metric.translate(config.getValue(DetectorConstants.PROCESS_QUERY), config));

        StringBuilder id = new StringBuilder(DetectorConstants.PROCESS).append(" ");

        for (long nodePid : pids) {
            String nodeArgs[] = getProcArgs(nodePid);
            String sName = getServerName(nodeArgs);
            logger.debug("Creating process: pid=" + nodePid + " args=" + Arrays.toString(nodeArgs) + " servername=" + sName);

            id.append(nodePid).append(":").append(sName);

            ServiceResource processResource = createServiceResource(DetectorConstants.PROCESS);
            processResource.setName(new StringBuilder().append(getTypeInfo().getName()).append(" ").
                    append(DetectorConstants.PROCESS).append(" ").append(sName).toString());

            ConfigResponse productConfig = new ConfigResponse();
            productConfig.setValue(DetectorConstants.PROCESS_NAME, sName);
            processResource.setProductConfig(productConfig);

            for (int n = 0; n < nodeArgs.length; n++) {

                /** -kernel error_logger {file,"/path/to/rabbitnode@localhost.log"} */
                if (nodeArgs[n].equalsIgnoreCase("-kernel") && nodeArgs[n + 1].equalsIgnoreCase("error_logger") && nodeArgs[n + 2].startsWith("{file,")) {

                    Pattern p = Pattern.compile("[{]file,\\s*\"([^\"]+)\"}");
                    Matcher m = p.matcher(nodeArgs[n + 2]);
                    String logPath = m.find() ? m.group(1) : null;

                    if (logPath != null) {
                        File logFile = new File(logPath);
                        if (logFile.exists()) {
                            logger.debug("Log file exists at " + logFile.getAbsolutePath());
                            ConfigResponse c = new ConfigResponse();
                            c.setValue(DetectorConstants.SERVICE_LOG_TRACK_ENABLE, true);
                            c.setValue(DetectorConstants.SERVICE_LOG_TRACK_FILES, logFile.getAbsolutePath());
                            setMeasurementConfig(processResource, c);
                        }
                    }
                }
            }

            if (processResource != null) {
                serviceResources.add(processResource);
            }
        }

        logger.debug("createProcessServiceResources detected " + serviceResources.size() + " process serviceResources");

        return serviceResources;
    }

    /**
     * Create ServiceResources for auto-detected Queues
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createQueueServiceResources(RabbitGateway rabbitGateway, String nodeName, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<QueueInfo> queues = rabbitGateway.getQueues(vHost);
        if (queues != null) {
            serviceResources = doCreateServiceResources(queues, DetectorConstants.QUEUE, nodeName, vHost);
        }

        return serviceResources;
    }

    /**
     * Create ServiceResources for auto-detected Connections
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createConnectionServiceResources(RabbitGateway rabbitGateway, String nodeName, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<HypericConnection> connections = rabbitGateway.getConnections(vHost);
        if (connections != null) {
            serviceResources = doCreateServiceResources(connections, DetectorConstants.CONNECTION, nodeName, vHost);
        }

        return serviceResources;
    }

    /**
     * Create ServiceResources for auto-detected Channels
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createChannelServiceResources(RabbitGateway rabbitGateway, String nodeName, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<HypericChannel> channels = rabbitGateway.getChannels(vHost);
        if (channels != null) {
            serviceResources = doCreateServiceResources(channels, DetectorConstants.CHANNEL, nodeName, vHost);
        }

        return serviceResources;
    }

    /**
     * Create ServiceResources for auto-detected Exchanges
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createExchangeServiceResources(RabbitGateway rabbitGateway, String nodeName, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;

        try {
            List<Exchange> exchanges = rabbitGateway.getExchanges(vHost);
            if (exchanges != null) {
                serviceResources = doCreateServiceResources(exchanges, DetectorConstants.EXCHANGE, nodeName, vHost);
            }
        }
        catch (Exception e) {
            logger.error(e);
        }

        return serviceResources;
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
    private List<ServiceResource> doCreateServiceResources(List rabbitObjects, String rabbitType, String nodeName, String vHost) {
        List<ServiceResource> serviceResources = null;

        if (rabbitObjects != null) {
            serviceResources = new ArrayList<ServiceResource>();

            for (Object obj : rabbitObjects) {

                ServiceResource resource = createServiceResource(rabbitType);
                String name = null;

                if (obj instanceof QueueInfo) {
                    name = ((QueueInfo) obj).getName();
                    resource.setCustomProperties(QueueCollector.getAttributes((QueueInfo) obj));
                } else if (obj instanceof HypericConnection) {
                    name = ((HypericConnection) obj).getPid();
                    resource.setCustomProperties(ConnectionCollector.getAttributes((HypericConnection) obj));
                } else if (obj instanceof Exchange) {
                    name = ((Exchange) obj).getName();
                    resource.setCustomProperties(ExchangeCollector.getAttributes((Exchange) obj));
                } else if (obj instanceof HypericChannel) {
                    name = ((HypericChannel) obj).getPid();
                    resource.setCustomProperties(ChannelCollector.getAttributes((HypericChannel) obj));
                }

                StringBuilder desc = new StringBuilder(rabbitType).append(":").append(name).append(" on ").append(vHost);

                ConfigResponse configResponse = new ConfigResponse();
                configResponse.setValue(DetectorConstants.VIRTUAL_HOST.toLowerCase(), vHost);
                configResponse.setValue(DetectorConstants.NAME, name);

                resource.setName(desc.toString());
                resource.setProductConfig(configResponse);
                resource.setDescription(desc.toString());
                setMeasurementConfig(resource, configResponse);

                if (resource != null) serviceResources.add(resource);
            }
        }

        if (serviceResources != null)
            logger.debug(new StringBuilder("Created ").append(serviceResources.size()).append(" ").append(rabbitType).append(" serviceResources"));

        return serviceResources;
    }

    /**
     * Configures a ServiceResource
     * @param nodeName
     * @param nodePath
     * @param nodeArgs
     * @param nodePid
     * @return
     */
    private ServerResource doCreateServerResource(String nodeName, String nodePath, long nodePid, String[] nodeArgs) {
        Assert.hasText(nodeName);
        Assert.hasText(nodePath);

        ServerResource node = createServerResource(nodePath);
        node.setName(new StringBuilder(getPlatformName()).append(" ").append(getTypeInfo().getName()).append(" ")
                .append(DetectorConstants.NODE).append(" ").append(nodeName).toString());
        node.setIdentifier(nodePath);
        node.setDescription(new StringBuilder(getTypeInfo().getName()).append(" ").append(DetectorConstants.NODE).toString());

        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.SERVER_NAME, nodeName);
        conf.setValue(DetectorConstants.SERVER_PATH, nodePath);
        setProductConfig(node, conf);

        ConfigResponse custom = createCustomConfig(nodeName,nodePath, nodePid, nodeArgs);
        if (custom != null) node.setCustomProperties(custom);

        ConfigResponse log = createLogConfig(nodeArgs);
        if (log != null) setMeasurementConfig(node, log);

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
                        logConfig.setValue(DetectorConstants.SERVICE_LOG_TRACK_ENABLE, true);
                        logConfig.setValue(DetectorConstants.SERVICE_LOG_TRACK_FILES, log.getAbsolutePath());
                    }
                }
            }
        }

        return logConfig;
    }

    /**
     * Create ConfigResponse for custom node properties to display.
     * @param nodeName
     * @param nodePath
     * @param nodePid
     * @param nodeArgs
     * @return
     */
    private ConfigResponse createCustomConfig(String nodeName, String nodePath, long nodePid, String[] nodeArgs) {
        ConfigResponse custom = new ConfigResponse();
        custom.setValue(DetectorConstants.NODE_NAME, nodeName);
        custom.setValue(DetectorConstants.NODE_PATH, nodePath);
        custom.setValue(DetectorConstants.NODE_PID, nodePid);

        for (int n = 0; n < nodeArgs.length; n++) {
            if (nodeArgs[n].contains("beam")) {
                custom.setValue(DetectorConstants.ERLANG_PROCESS, nodeArgs[n]);
            }
            if (nodeArgs[n].contains("boot")) {
                custom.setValue(DetectorConstants.RABBIT_BOOT, nodeArgs[n + 1]);
            }
        }

        return custom;
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
                logger.debug("mnesia " + args[n] + " " + args[n + 1] + " " + args[n + 2]);
                mpath = args[n + 2];

                if (mpath.startsWith("\"")) {
                    mpath = mpath.substring(1);
                }
                if (mpath.endsWith("\"")) {
                    mpath = mpath.substring(0, mpath.length() - 1);
                }
            }
        }
        return mpath;
    }

    private String getHostFromNode(String nodeName) {
        if (nodeName != null) {
            Pattern p = Pattern.compile("@([^\\s.]+)");
            Matcher m = p.matcher(nodeName);
            return (m.find()) ? m.group(1) : null;
        }
        return null;
    }

}

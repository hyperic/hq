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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.collect.*;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
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

        List<ServerResource> resources = new ArrayList<ServerResource>();
        long[] pids = getPids(PTQL_QUERY);

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
                            StringBuilder sb = new StringBuilder("Discovered ").append(server.getName()).append(" productConfig=")
                                    .append(server.getProductConfig()).append(" customProps=").append(server.getCustomProperties());
                            logger.debug(sb.toString());
                        }
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
        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        configure(serviceConfig);
 
        List<ServiceResource> rabbitResources = createRabbitResources(serviceConfig);
        if (rabbitResources != null && rabbitResources.size() > 0) {
            serviceResources.addAll(rabbitResources);
            logger.debug("Detected " + rabbitResources.size() + " Rabbit resources");
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
        List<ServiceResource> rabbitResources = null;

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway(Configuration.toConfiguration(serviceConfig));

        if (rabbitGateway != null) {
            List<String> virtualHosts = rabbitGateway.getVirtualHosts();

            if (virtualHosts != null) {
                String nodeName = serviceConfig.getValue(DetectorConstants.SERVER_NAME);
                rabbitResources = new ArrayList<ServiceResource>();

                for (String virtualHost : virtualHosts) {
                    Configuration configuration = Configuration.toConfiguration(serviceConfig);
                    configuration.setVirtualHost(virtualHost);

                    RabbitGateway gateway = new RabbitBrokerGateway(configuration); 

                    ServiceResource vHost = createServiceResource(DetectorConstants.VIRTUAL_HOST);
                    vHost.setName(new StringBuilder().append(getTypeInfo().getName())
                            .append(" Node: ").append(nodeName).append(" ").append(DetectorConstants.VIRTUAL_HOST).append(": ").append(virtualHost).toString());
                    //rabbitResources.add(vHost);

                    List<ServiceResource> queues = createQueueServiceResources(gateway, virtualHost);
                    if (queues != null && queues.size() > 0) rabbitResources.addAll(queues);

                    List<ServiceResource> connections = createConnectionServiceResources(gateway, virtualHost);
                    if (connections != null) rabbitResources.addAll(connections);

                    List<ServiceResource> channels = createChannelServiceResources(gateway, virtualHost);
                    if (channels != null) rabbitResources.addAll(channels);

                    List<ServiceResource> exchanges = createExchangeServiceResources(gateway, virtualHost);
                    if (exchanges != null) rabbitResources.addAll(exchanges);
                }

                if (rabbitResources != null) {
                    logger.debug("Created " + rabbitResources.size() + " rabbit services");
                }
            }
        }

        return rabbitResources;
    }

    /**
     * Create ServiceResources for auto-detected Queues
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createQueueServiceResources(RabbitGateway rabbitGateway, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<QueueInfo> queues = rabbitGateway.getQueues();
        if (queues != null) {
            serviceResources = doCreateServiceResources(queues, DetectorConstants.QUEUE, vHost);
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
    protected List<ServiceResource> createConnectionServiceResources(RabbitGateway rabbitGateway, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<RabbitConnection> connections = rabbitGateway.getConnections();
        if (connections != null) {
            serviceResources = doCreateServiceResources(connections, DetectorConstants.CONNECTION, vHost);
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
    protected List<ServiceResource> createChannelServiceResources(RabbitGateway rabbitGateway, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<RabbitChannel> channels = rabbitGateway.getChannels();
        if (channels != null) {
            serviceResources = doCreateServiceResources(channels, DetectorConstants.CHANNEL, vHost);
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
    protected List<ServiceResource> createExchangeServiceResources(RabbitGateway rabbitGateway, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<Exchange> exchanges = rabbitGateway.getExchanges();
        if (exchanges != null) {
            serviceResources = doCreateServiceResources(exchanges, DetectorConstants.EXCHANGE, vHost);
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
    private List<ServiceResource> doCreateServiceResources(List rabbitObjects, String rabbitType, String vHost) {
        List<ServiceResource> serviceResources = null;

        if (rabbitObjects != null) {
            serviceResources = new ArrayList<ServiceResource>();

            IdentityBuilder builder = new ObjectIdentityBuilder();

            for (Object obj : rabbitObjects) {
                ServiceResource service = createServiceResource(rabbitType);
                String name = builder.buildIdentity(obj, vHost);

                if (obj instanceof QueueInfo) {
                    service.setCustomProperties(QueueCollector.getAttributes((QueueInfo) obj));
                } else if (obj instanceof RabbitConnection) {
                    service.setCustomProperties(ConnectionCollector.getAttributes((RabbitConnection) obj));
                } else if (obj instanceof Exchange) {
                    service.setCustomProperties(ExchangeCollector.getAttributes((Exchange) obj));
                } else if (obj instanceof RabbitChannel) {
                    service.setCustomProperties(ChannelCollector.getAttributes((RabbitChannel) obj));
                }

                ConfigResponse configResponse = new ConfigResponse();
                configResponse.setValue(DetectorConstants.VIRTUAL_HOST.toLowerCase(), vHost);
                configResponse.setValue(DetectorConstants.NAME, name);

                service.setName(name);
                service.setDescription(name);
                service.setProductConfig(configResponse);
                setMeasurementConfig(service, configResponse);

                if (service != null) serviceResources.add(service);
            }
        }

        if (serviceResources != null)
            logger.debug(new StringBuilder("Created ").append(serviceResources.size()).append(" ").append(rabbitType).append(" serviceResources"));

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
        Assert.hasText(nodeName);
        Assert.hasText(nodePath);

        ServerResource node = createServerResource(nodePath);
        node.setIdentifier(nodePath);
        node.setName(new StringBuilder(getPlatformName()).append(" ").append(getTypeInfo().getName()).append(" ")
                .append(DetectorConstants.NODE).append(" ").append(nodeName).toString());
        node.setDescription(new StringBuilder(getTypeInfo().getName()).append(" ").append(DetectorConstants.NODE)
                .append(" ").append(nodePid).toString());

        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.SERVER_NAME, nodeName);
        conf.setValue(DetectorConstants.SERVER_PATH, nodePath);
        conf.setValue(DetectorConstants.NODE_PID, nodePid);

        String auth = ErlangCookieHandler.configureCookie(conf);
        if (auth != null) {
            conf.setValue(DetectorConstants.AUTHENTICATION, auth);
        }

        String hostName = nodeName != null ? getHostFromNode(nodeName) : null;
        if (hostName != null) {
            conf.setValue(DetectorConstants.HOST, hostName);
        }

        logger.debug("ProductConfig[" + conf + "]");

        ConfigResponse custom = createCustomConfig(nodeName, nodePath, nodePid, nodeArgs);
        if (custom != null) node.setCustomProperties(custom);

        ConfigResponse log = createLogConfig(nodeArgs);
        if (log != null)

            setMeasurementConfig(node, log);

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
        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase(DetectorConstants.SNAME)) {
                return args[n + 1];
            }
        }
        return null;
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

    private String getHostFromNode(String nodeName) {
        if (nodeName != null && nodeName.length() > 0) {
            Pattern p = Pattern.compile("@([^\\s.]+)");
            Matcher m = p.matcher(nodeName);
            return (m.find()) ? m.group(1) : null;
        }
        return null;
    }

}

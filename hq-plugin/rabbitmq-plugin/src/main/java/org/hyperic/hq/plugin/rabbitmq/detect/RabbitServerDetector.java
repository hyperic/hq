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
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.collect.*;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitTransientResourceManager;
import org.hyperic.hq.plugin.rabbitmq.manage.TransientResourceManager;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.Sigar;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.util.Assert;

/**
 * RabbitServerDetector
 * @author Helena Edelson
 * @author German Laullon
 * @author Patrick Nguyen
 */
public class RabbitServerDetector extends ServerDetector implements AutoServerDetector {

    private static final Log logger = LogFactory.getLog(RabbitServerDetector.class);

    private final static String PTQL_QUERY = "State.Name.re=[beam|erl],Args.*.eq=-sname";


    /**
     * @param serverConfig
     * @return
     * @throws PluginException
     */
    public List getServerResources(ConfigResponse serverConfig) throws PluginException {
        configure(serverConfig);


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
        }   

        syncServices(serviceConfig, rabbitResources);

        return serviceResources;
    }

    private void syncServices(ConfigResponse serviceConfig, List<ServiceResource> rabbitResources) {

        try {
            Properties props = new Properties();
            props.putAll(serviceConfig.toProperties());
            props.putAll(getManager().getProperties());

            TransientResourceManager manager = new RabbitTransientResourceManager(props);
            manager.syncServices(rabbitResources);

        } catch (Throwable e) {
            logger.info("Could not sync transient services: " + e.getMessage(), e);
        }
    }

    /**
     * Create RabbitMQ-specific resources to add to inventory.
     * @param serviceConfig
     * @return
     * @throws PluginException
     */
    public List<ServiceResource> createRabbitResources(ConfigResponse serviceConfig) {
        List<ServiceResource> rabbitResources = null;

        Configuration configuration = Configuration.toConfiguration(serviceConfig);
        if (getLog().isDebugEnabled()) {
            getLog().debug("[createRabbitResources] serviceConfig=" + serviceConfig);
            getLog().debug("[createRabbitResources] configuration=" + configuration);
        }

        try{
           if (isConfiguredAndValid(configuration)) {
            	rabbitResources = new ArrayList<ServiceResource>();
                RabbitConfigurationManager cm = new RabbitConfigurationManager(configuration);
                Map<String, HypericRabbitAdmin> admins = cm.getVirtualHostsForNode();

                if (admins != null) {
                    for (Map.Entry entry : admins.entrySet()) {
                        HypericRabbitAdmin rabbitAdmin = (HypericRabbitAdmin) entry.getValue();

                        List<ServiceResource> resources = createResourcesPerVirtualHost(rabbitAdmin);
                        if (resources != null) {
                            rabbitResources.addAll(resources);
                        }
                    }
                }
            }
        }catch (RuntimeException ex){
            logger.debug(ex,ex);
        }catch (PluginException ex){
            logger.debug(ex,ex);
        }
        return rabbitResources;
    }

    /**
     * 
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createResourcesPerVirtualHost(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        List<ServiceResource> rabbitResources = new ArrayList<ServiceResource>();

        if (rabbitAdmin != null) {

            List<ServiceResource> queues = createQueueServiceResources(rabbitAdmin);
            if (queues != null && queues.size() > 0) rabbitResources.addAll(queues);

            List<ServiceResource> exchanges = createExchangeServiceResources(rabbitAdmin);
            if (exchanges != null) rabbitResources.addAll(exchanges);

            List<ServiceResource> connections = createConnectionServiceResources(rabbitAdmin);
            if (connections != null) rabbitResources.addAll(connections);

            List<ServiceResource> channels = createChannelServiceResources(rabbitAdmin);
            if (channels != null) rabbitResources.addAll(channels);

            List<ServiceResource> vHosts = createVirtualHostResources(rabbitAdmin);
            if (vHosts != null) rabbitResources.addAll(vHosts);
        }

        return rabbitResources;
    }

    /**
     * Create ServiceResources for auto-detected Queues
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createQueueServiceResources(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        List<QueueInfo> queues = rabbitAdmin.getQueues();

        return queues != null ? doCreateServiceResources(queues, AMQPTypes.QUEUE,
                    rabbitAdmin.getPeerNodeName(), rabbitAdmin.getVirtualHost()) : null;
    }

    /**
     * Create ServiceResources for auto-detected Connections
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createVirtualHostResources(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        RabbitVirtualHost virtualHost = rabbitAdmin.buildRabbitVirtualHost();
        List<RabbitVirtualHost> virtualHostList = null;
        if (virtualHost != null) {
            virtualHostList = new ArrayList<RabbitVirtualHost>();
            virtualHostList.add(virtualHost);
        }

        return virtualHostList != null ? doCreateServiceResources(virtualHostList, AMQPTypes.VIRTUAL_HOST,
                rabbitAdmin.getPeerNodeName(), rabbitAdmin.getVirtualHost()) : null;
    }

    /**
     * Create ServiceResources for auto-detected Connections
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createConnectionServiceResources(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        List<RabbitConnection> connections = rabbitAdmin.getConnections();

        return connections != null ? doCreateServiceResources(connections, AMQPTypes.CONNECTION,
                    rabbitAdmin.getPeerNodeName(), rabbitAdmin.getVirtualHost()) : null;
    }

    /**
     * Create ServiceResources for auto-detected Channels
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createChannelServiceResources(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        List<RabbitChannel> channels = rabbitAdmin.getChannels();

        return channels != null ? doCreateServiceResources(channels, AMQPTypes.CHANNEL,
                    rabbitAdmin.getPeerNodeName(), rabbitAdmin.getVirtualHost()) : null;

    }

    /**
     * Create ServiceResources for auto-detected Exchanges
     * @param rabbitAdmin
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createExchangeServiceResources(HypericRabbitAdmin rabbitAdmin) throws PluginException {
        List<Exchange> exchanges = rabbitAdmin.getExchanges();

        return exchanges != null ? doCreateServiceResources(exchanges, AMQPTypes.EXCHANGE,
                    rabbitAdmin.getPeerNodeName(), rabbitAdmin.getVirtualHost()) : null;
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
    private List<ServiceResource> doCreateServiceResources(List rabbitObjects, String rabbitType, String node, String vHost) {
        List<ServiceResource> serviceResources = null;

        if (rabbitObjects != null) {
            serviceResources = new ArrayList<ServiceResource>();

            IdentityBuilder builder = new ObjectIdentityBuilder();

            for (Object obj : rabbitObjects) {
                ServiceResource service = createServiceResource(rabbitType);
                String name = builder.buildIdentity(obj, vHost); // the hq inventory name

                ConfigResponse c = new ConfigResponse();
                c.setValue(MetricConstants.NODE, node);
                c.setValue(MetricConstants.VIRTUALHOST, vHost);

                if (obj instanceof QueueInfo) {
                    QueueInfo queue = (QueueInfo) obj;
                    c.setValue(MetricConstants.QUEUE, queue.getName());
                    service.setCustomProperties(QueueCollector.getAttributes(queue));
                } else if (obj instanceof RabbitConnection) {
                    RabbitConnection conn = (RabbitConnection) obj;
                    c.setValue(MetricConstants.CONNECTION, conn.getPid());
                    service.setCustomProperties(ConnectionCollector.getAttributes(conn));
                } else if (obj instanceof Exchange) {
                    Exchange exchange = (Exchange) obj;
                    if (exchange.getName() == null) {
                        name = AMQPTypes.DEFAULT_EXCHANGE_NAME;
                    } 
                    c.setValue(MetricConstants.EXCHANGE, exchange.getName() == null ?
                            AMQPTypes.DEFAULT_EXCHANGE_NAME : exchange.getName());
                    service.setCustomProperties(ExchangeCollector.getAttributes((Exchange) obj));
                } else if (obj instanceof RabbitChannel) {
                    RabbitChannel channel = (RabbitChannel) obj;
                    c.setValue(MetricConstants.CHANNEL, channel.getPid());
                    service.setCustomProperties(ChannelCollector.getAttributes(channel));
                } else if (obj instanceof RabbitVirtualHost) {
                    RabbitVirtualHost vh = (RabbitVirtualHost) obj;
                    c.setValue(MetricConstants.VIRTUAL_HOST, vh.getName());
                    service.setCustomProperties(VirtualHostCollector.getAttributes(vh));
                }

                service.setName(node+" "+name);
                service.setDescription(name);
                service.setProductConfig(c);
                setMeasurementConfig(service, c);

                if (service != null) serviceResources.add(service);
            }
        }

        if (serviceResources != null)
            logger.debug(new StringBuilder("Detected ").append(serviceResources.size()).append(" ").append(rabbitType).append(" resources"));

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
        node.setName(getPlatformName()+" "+getTypeInfo().getName()+" Node "+nodeName);
        node.setDescription(getTypeInfo().getName()+" Node "+nodePid);

        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.SERVER_NAME, nodeName);
        conf.setValue(DetectorConstants.SERVER_PATH, nodePath);
        conf.setValue(DetectorConstants.NODE_PID, nodePid);

        final String home = getProcessHome(nodePid);
        final String auth = ErlangCookieHandler.configureCookie(home);
        if (auth != null) {
            conf.setValue(DetectorConstants.AUTHENTICATION, auth);
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

    /**
     * based on https://github.com/erlang/otp/blob/dev/lib/erl_interface/src/connect/ei_connect.c
     * @param nodePid
     * @return
     */
    private String getProcessHome(long nodePid) {
        Sigar sigar = new Sigar();
        String home = null;
        try {
            if (isWin32()) {
                String homedrive = sigar.getProcEnv(nodePid, "HOMEDRIVE");
                String homepath = sigar.getProcEnv(nodePid, "HOMEPATH");
                if ((homedrive != null) && (homepath != null)) {
                    home = new File(homedrive, homepath).getAbsolutePath();
                }
                if (home == null) {
                    home = sigar.getProcEnv(nodePid, "windir");
                    logger.debug("[getProcessHome] home==null -> windir");
                }
            } else {
                home = sigar.getProcEnv(nodePid, "HOME");
            }
            logger.debug("[getProcessHome] home=" + home);
        } catch (Exception ex) {
            logger.debug("[getProcessHome] error=" + ex.getMessage(), ex);
        }
        return home;
    }
    
    private boolean isConfiguredAndValid(Configuration configuration) 
    	throws PluginException {

        return configuration.isConfigured()
        			&& RabbitProductPlugin.isValidUsernamePassword(configuration)
        			&& RabbitProductPlugin.isValidOtpConnection(configuration);
    }

}

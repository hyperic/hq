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
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.*;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.erlang.core.Application;

/**
 * RabbitServerDetector
 * @author Helena Edelson
 */
public class RabbitServerDetector extends ServerDetector implements AutoServerDetector {

    private static final Log logger = LogFactory.getLog(RabbitServerDetector.class);

    private final static String PTQL_QUERY = "State.Name.sw=beam,Args.*.eq=-sname";

    /**
     * @param platformConfig
     * @return
     * @throws PluginException
     */
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        configure(platformConfig);

        long[] pids = getPids(PTQL_QUERY);

        List<ServerResource> resources = null;

        List<String> paths = buildPaths(pids);

        if (paths != null) {
            resources = new ArrayList<ServerResource>();

            for (String path : paths) {
                for (long pid : pids) {

                    final String args[] = getProcArgs(pid);
                    final String node = getServerName(args);

                    ServerResource server = doCreateServerResource(node, path);
                    logger.debug("Created server=" + server.getName());
                    if (server != null) {
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
        configure(serviceConfig);

        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        /** get rabbit  services */
        List<ServiceResource> rabbitResources = createRabbitResources(serviceConfig);
        if (rabbitResources != null && rabbitResources.size() >= 0) {
            serviceResources.addAll(rabbitResources);
        }

        /** configure service processes */
        List<ServiceResource> processes = createProcessServiceResources(serviceConfig);
        if (processes != null) {
            serviceResources.addAll(processes);
            logger.debug("discoverServices detected " + processes.size() + " processes");
        }

        return serviceResources;
    }

    /**
     * Creates service resources from processes.
     * @return
     */
    List<ServiceResource> createProcessServiceResources(ConfigResponse config) {
        List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();

        long[] pids = getPids(Metric.translate(config.getValue(DetectorConstants.PROCESS_QUERY), config));

        StringBuilder id = new StringBuilder().append(getTypeInfo().getName()).append(" ").append(DetectorConstants.PROCESS);

        for (long pid : pids) {
            String args[] = getProcArgs(pid);
            String sName = getServerName(args);
            id.append(" ").append(sName);

            ServiceResource processResource = createServiceResource(DetectorConstants.PROCESS);
            processResource.setName(new StringBuilder().append(getTypeInfo().getName()).append(" ").
                    append(DetectorConstants.PROCESS).append(" ").append(sName).toString());

            ConfigResponse productConfig = new ConfigResponse();
            productConfig.setValue(DetectorConstants.PROCESS_NAME, sName);
            processResource.setProductConfig(productConfig);

            for (int n = 0; n < args.length; n++) {

                /** -kernel error_logger {file,"/path/to/rabbit@localhost.log"} */
                if (args[n].equalsIgnoreCase("-kernel") && args[n + 1].equalsIgnoreCase("error_logger") && args[n + 2].startsWith("{file,")) {

                    Pattern p = Pattern.compile("[{]file,\\s*\"([^\"]+)\"}");
                    Matcher m = p.matcher(args[n + 2]);
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
     * Create RabbitMQ-specific resources to add to inventory.
     * @param serviceConfig
     * @return
     * @throws PluginException
     */
    public List<ServiceResource> createRabbitResources(ConfigResponse serviceConfig) throws PluginException {
        List<ServiceResource> rabbitResources = null;

        if (RabbitProductPlugin.getRabbitGateway() == null) {
            RabbitProductPlugin.initializeGateway(serviceConfig);
        }

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();

        if (rabbitGateway != null) {
            List<String> virtualHosts = rabbitGateway.getVirtualHosts();

            if (virtualHosts != null) {
                rabbitResources = new ArrayList<ServiceResource>();

                for (String virtualHost : virtualHosts) {
                    ServiceResource vHostServiceResource = createServiceResource(DetectorConstants.VHOST);
                    vHostServiceResource.setName(new StringBuilder().append(getTypeInfo().getName())
                            .append(" ").append(DetectorConstants.VHOST).append(" ").append(virtualHost).toString());

                    List<ServiceResource> queues = createQueueServiceResources(rabbitGateway, virtualHost);
                    if (queues != null) rabbitResources.addAll(queues);

                    List<ServiceResource> connections = createConnectionServiceResources(rabbitGateway, virtualHost);
                    if (connections != null) rabbitResources.addAll(connections);

                    List<ServiceResource> channels = createChannelServiceResources(rabbitGateway, virtualHost);
                    if (channels != null) rabbitResources.addAll(channels);

                    List<ServiceResource> exchanges = createExchangeServiceResources(rabbitGateway, virtualHost);
                    if (exchanges != null) rabbitResources.addAll(exchanges);

                    List<ServiceResource> runningApps = createAppServiceResources(rabbitGateway, virtualHost);
                    if (runningApps != null) rabbitResources.addAll(runningApps);

                    List<ServiceResource> users = createUserServiceResources(rabbitGateway, virtualHost);
                    if (users != null) rabbitResources.addAll(users);
                }
            }
        }

        return rabbitResources;
    }

    protected List<ServiceResource> createUserServiceResources(RabbitGateway rabbitGateway, String virtualHost) {
        List<ServiceResource> serviceResources = null;
        List<String> users = rabbitGateway.getUsers();
        if (users != null) {
            serviceResources = doCreateServiceResources(users, DetectorConstants.USER, virtualHost);
        }

        return serviceResources;
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
        List<Connection> connections = rabbitGateway.getConnections();
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
        List<Channel> channels = rabbitGateway.getChannels();
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

        try {
            List<Exchange> exchanges = rabbitGateway.getExchanges();
            if (exchanges != null) {
                serviceResources = doCreateServiceResources(exchanges, DetectorConstants.EXCHANGE, vHost);
            }
        }
        catch (Exception e) {
            logger.error(e);
        }

        return serviceResources;
    }

    /**
     * Create ServiceResources for auto-detected Applications
     * @param rabbitGateway
     * @param vHost
     * @return
     * @throws PluginException
     */
    protected List<ServiceResource> createAppServiceResources(RabbitGateway rabbitGateway, String vHost) throws PluginException {
        List<ServiceResource> serviceResources = null;
        List<Application> runningApps = rabbitGateway.getRunningApplications();
        if (runningApps != null) {
            serviceResources = doCreateServiceResources(runningApps, DetectorConstants.BROKER_APP, vHost);
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

            for (Object obj : rabbitObjects) {

                ServiceResource resource = createServiceResource(rabbitType);
                String name = null;

                if (obj instanceof QueueInfo) {
                    name = ((QueueInfo) obj).getName();
                    resource.setCustomProperties(RabbitQueueCollector.getAttributes((QueueInfo) obj));
                }
                else if (obj instanceof Connection) {
                    name = ((Connection) obj).getPid();
                    resource.setCustomProperties(RabbitConnectionCollector.getAttributes((Connection) obj));
                }
                else if (obj instanceof Exchange) {
                    name = ((Exchange) obj).getName();
                    resource.setCustomProperties(RabbitExchangeCollector.getAttributes((Exchange) obj));
                }
                else if (obj instanceof Application) {
                    name = ((Application) obj).getDescription();
                    resource.setCustomProperties(BrokerAppCollector.getAttributes((Application) obj));
                }
                else if (obj instanceof Channel) {
                    name = ((Channel) obj).getPid();
                    resource.setCustomProperties(RabbitChannelCollector.getAttributes((Channel) obj));
                }
                else if (obj instanceof String) {
                    name = (String) obj;
                }

                ConfigResponse configResponse = new ConfigResponse();
                configResponse.setValue(DetectorConstants.VHOST.toLowerCase(), vHost);
                configResponse.setValue(DetectorConstants.NAME, name);

                resource.setName(new StringBuilder().append("RabbitMQ ").append(rabbitType).append(" ").append(name).toString());
                resource.setProductConfig(configResponse);
                resource.setDescription(new StringBuilder(rabbitType).append(" ").append(name).toString());
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
     * @param node
     * @param path
     * @return
     */
    private ServerResource doCreateServerResource(String node, String path) {
        ServerResource server = createServerResource(path);
        server.setName(new StringBuilder(getPlatformName()).append(" ").append(getTypeInfo().getName()).append(" ").append(node).toString());
        server.setIdentifier(node);
        server.setDescription("Erlang Node " + node);

        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.SERVER_NAME, node);
        conf.setValue(DetectorConstants.SERVER_PATH, path);

        String rabbitHome = System.getenv(DetectorConstants.RABBITMQ_HOME);
        if (rabbitHome != null) {
            conf.setValue(DetectorConstants.RABBITMQ_HOME, rabbitHome);
        }

        setProductConfig(server, conf);
        setMeasurementConfig(server, new ConfigResponse());

        return server;
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
     * Parses -mnesia dir "path/to/mnesia/rabbit@localhost" to get to
     * RABBITMQ_HOME which we can not rely on to be an env var that is set.
     * A totally scary method at the moment...
     * @param args
     * @return
     */
    private String getServerDir(String[] args) {
        String mpath = null;

        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase(DetectorConstants.MNESIA) && args[n + 1].equalsIgnoreCase(DetectorConstants.DIR)) {
                mpath = args[n + 2];

                /*if (mpath.startsWith("\\")) {
                    mpath = mpath.substring(1, mpath.length() - 1);
                }*/

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

    /**
     * Build server paths from pids
     * @param pids
     * @return
     */
    private List<String> buildPaths(long[] pids) {
        List<String> paths = new ArrayList<String>();

        for (long pid : pids) {
            String args[] = getProcArgs(pid);
            String path = getServerDir(args);

            if (path != null) {
                path = new File(path).getParent();
                if (!paths.contains(path)) {
                    paths.add(path);
                }
            }
        }

        return paths;
    }

}

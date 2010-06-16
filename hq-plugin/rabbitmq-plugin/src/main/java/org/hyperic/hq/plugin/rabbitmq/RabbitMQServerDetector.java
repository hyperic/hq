/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class RabbitMQServerDetector extends ServerDetector implements AutoServerDetector {

    public static final String SERVERNAME = "server.name";
    Log log = getLog();
    private final static String PTQL_QUERY = "State.Name.eq=beam,Args.*.eq=-sname";

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        List res = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);
        for (long pid : pids) {
            String args[] = getProcArgs(pid);
            String serverName = getServerName(args);
            if (RabbitMQUtils.getServerVersion(serverName).startsWith(getTypeInfo().getVersion())) {
                log.debug("ok " + serverName);
                ServerResource server = createServerResource(getServerDir(args));
                server.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + serverName);

                ConfigResponse conf = new ConfigResponse();
                conf.setValue(SERVERNAME, serverName);

                setProductConfig(server, conf);
                setMeasurementConfig(server, new ConfigResponse());

                res.add(server);
            }
        }
        return res;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List<String> vHosts = RabbitMQUtils.getVHost(config.getValue(SERVERNAME));
        for (String vHost : vHosts) {
            RabbitMQUtils.getQueues(config.getValue(SERVERNAME),vHost);
        }
        return super.discoverServices(config);
    }

    private String getServerName(String[] args) {
        String res = null;
        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase("-sname")) {
                res = args[n + 1];
            }
        }
        return res;
    }

    private String getServerDir(String[] args) {
        String res = null;
        for (int n = 0; n < args.length; n++) {
            if (args[n].equalsIgnoreCase("-mnesia") && args[n + 1].equalsIgnoreCase("dir")) {
                res = args[n + 2];
            }
        }
        return res;
    }
}

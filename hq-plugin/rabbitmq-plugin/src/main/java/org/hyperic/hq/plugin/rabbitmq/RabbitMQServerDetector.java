/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.rabbitmq.objs.Queue;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
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
        List<String> paths = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        for (long pid : pids) {
            String args[] = getProcArgs(pid);
            String path = getServerDir(args);
            path = new File(path).getParent();
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }

        for (String path : paths) {
            String serverName = "";
            for (long pid : pids) {
                String args[] = getProcArgs(pid);
                if (getServerDir(args).startsWith(path)) {
                    serverName += getServerName(args) + ",";
                }
            }
            log.debug("path='" + path + "' ='" + serverName + "'");
            if (RabbitMQUtils.getServerVersion(serverName).startsWith(getTypeInfo().getVersion())) {
                log.debug("ok " + serverName);
                ServerResource server = createServerResource(path);
                server.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + path);

                ConfigResponse conf = new ConfigResponse();
                conf.setValue(SERVERNAME, serverName);
                conf.setValue("server.path", path);

                setProductConfig(server, conf);
                setMeasurementConfig(server, new ConfigResponse());

                res.add(server);
            }
        }

        String dummy = getManagerProperty("rabbitmq.dummy");
        log.debug("[getServerResources] rabbitmq.dummy=" + dummy);
        if ("one".equalsIgnoreCase(dummy)) {
            new Dummy().send();
        } else if ("thread".equalsIgnoreCase(dummy)) {
            new Thread(new Dummy()).start();
        }

        return res;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        String serverName = config.getValue(SERVERNAME);
        List<ServiceResource> res = new ArrayList();
        List<String> vHosts = RabbitMQUtils.getVHost(serverName);
        for (String vHost : vHosts) {
            ServiceResource svh = createServiceResource("VHost");
            svh.setName(getTypeInfo().getName() + " VHost " + vHost);
            //res.add(svh);
            List<Queue> queues = RabbitMQUtils.getQueues(serverName, vHost);
            for (Queue queue : queues) {
                ServiceResource q = createServiceResource("Queue");
                q.setName(getTypeInfo().getName() + " Queue " + queue.getFullName());
                ConfigResponse c = new ConfigResponse();
                c.setValue("vhost", queue.getVHost());
                c.setValue("name", queue.getName());
                q.setProductConfig(c);
                setMeasurementConfig(q, c);
                res.add(q);
            }
        }

        long[] pids = getPids(Metric.translate(config.getValue("process.query"), config));
        if (pids.length > 1) {
            for (long pid : pids) {
                String args[] = getProcArgs(pid);
                String name = getServerName(args);
                ServiceResource p = createServiceResource("Proccess");
                ConfigResponse c = new ConfigResponse();
                p.setName(getTypeInfo().getName() + " Proccess " + name);
                c.setValue("proccess.name", name);
                p.setProductConfig(c);
                setMeasurementConfig(p, new ConfigResponse());
                setMeasurementConfig(p, new ConfigResponse());
                res.add(p);
            }
        }
        return res;
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
                if (res.startsWith("\"")) {
                    res = res.substring(1, res.length() - 2);
                }
            }
        }
        return res;
    }
}

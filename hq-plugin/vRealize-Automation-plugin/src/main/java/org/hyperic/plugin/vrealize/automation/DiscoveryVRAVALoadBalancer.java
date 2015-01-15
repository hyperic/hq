/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAVALoadBalancer extends Discovery implements AutoServerDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVRAVALoadBalancer.class);

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList<ServerResource>();

//        Properties props = VRAUtils.configFile();
//        String cspHost = props.getProperty("csp.host");
//
//        log.debug("[getServerResources] csp.host=" + cspHost);
//
//        if (cspHost != null) {
//            ServerResource server = createServerResource(cspHost);
//            server.setName(cspHost + " " + getTypeInfo().getName());
//            ConfigResponse c = new ConfigResponse();
//            c.setValue("hostname", cspHost);
//            setProductConfig(server, c);
//            servers.add(server);
//        }

        return servers;
    }

}

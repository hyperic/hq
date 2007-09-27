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

package org.hyperic.hq.plugin.nagios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

public class NagiosDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final String PLUGIN_NAME = "Plugin";
    
    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        ArrayList servers = new ArrayList();

        //for the moment for -D command line test and agent.properties
        String installpath =
            getManager().getProperty("nagios.installpath",
                                     "/usr/local/nagios");
        if (!new File(installpath).exists()) {
            return null;
        }

        ServerResource server = createServerResource(installpath);

        ConfigResponse config = new ConfigResponse();
        config.setValue("nagios.cfg",
                        installpath + "/etc/nagios.cfg");
        server.setProductConfig(config);

        server.setMeasurementConfig();

        servers.add(server);
        return servers;
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        String file = serverConfig.getValue("nagios.cfg");
        if (!new File(file).isAbsolute()) {
            file =
                serverConfig.getValue(ProductPlugin.PROP_INSTALLPATH) +
                File.separator + file;
        }

        NagiosConfig parser = new NagiosConfig(file);
        List configs;
        try {
            configs = parser.parse();
        } catch (IOException e) {
            String msg =
                "Error parsing " + file + ": " +
                e.getMessage();
            throw new PluginException(msg, e);
        }

        int size = configs.size();
        List services = new ArrayList(size);

        for (int i=0; i<size; i++) {
            NagiosConfig.Service checker =
                (NagiosConfig.Service)configs.get(i);

            ServiceResource service =
                createServiceResource(PLUGIN_NAME);
            service.setServiceName(PLUGIN_NAME + " " + checker.name);

            ConfigResponse config = new ConfigResponse();
            config.setValue("path", checker.cmd);
            config.setValue("args", checker.args);

            service.setProductConfig(config);

            ConfigResponse metricConfig = new ConfigResponse();
            //XXX does not work (PR 9882)
            //LogTrackPlugin.setEnabled(metricConfig,
            //                          TypeInfo.TYPE_SERVICE,
            //                          LogTrackPlugin.LOGLEVEL_WARN);
            service.setMeasurementConfig(metricConfig);

            services.add(service);
        }

        return services;
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.jboss7.objects.Connector;
import org.hyperic.hq.plugin.jboss7.objects.Deployment;
import org.hyperic.hq.plugin.jboss7.objects.WebSubsystem;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

public class JBossStandaloneDetector extends JBossDetectorBase {

    private final Log log = getLog();

    @Override
    protected List discoverServices(ConfigResponse config) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        log.debug("[discoverServices] config=" + config);

        JBossAdminHttp admin = null;
        try {
            admin = new JBossAdminHttp(config);
        } catch (PluginException ex) {
            log.error(ex, ex);
        }

        // DATA SOURCES
        try {
            List<String> datasources = admin.getDatasources();
            log.debug(datasources);
            for (String ds : datasources) {
                Map<String, String> datasource = admin.getDatasource(ds, false);
                ServiceResource service = createServiceResource("Datasource");
                service.setName("XXXX Datasource " + ds);

                ConfigResponse cp = new ConfigResponse();
                cp.setValue("jndi", datasource.get("jndi-name"));
                cp.setValue("driver", datasource.get("driver-name"));

                ConfigResponse pc = new ConfigResponse();
                pc.setValue("name", ds);

                setProductConfig(service, pc);
                service.setCustomProperties(cp);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        } catch (PluginException ex) {
            log.error(ex, ex);
        }

        // CONECTORS
        try {
            WebSubsystem ws = admin.getWebSubsystem();
            log.debug(ws);
            for (String name : ws.getConector().keySet()) {
                Connector connector = ws.getConector().get(name);
                ServiceResource service = createServiceResource("Connector");
                service.setName("XXXX Connector " + name);

                ConfigResponse cp = new ConfigResponse();
                cp.setValue("protocol", connector.getProtocol());
                cp.setValue("scheme", connector.getScheme());

                ConfigResponse pc = new ConfigResponse();
                pc.setValue("name", name);

                setProductConfig(service, pc);
                service.setCustomProperties(cp);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        } catch (PluginException ex) {
            log.error(ex, ex);
        }

        // deployments
        try {
            List<Deployment> deployments = admin.getDeployments();
            for(Deployment d : deployments){
                ServiceResource service = createServiceResource("deployment");
                service.setName("XXXX Deployment " + d.getName());

                ConfigResponse cp = new ConfigResponse();
                cp.setValue("runtime-name", d.getRuntimeName());

                ConfigResponse pc = new ConfigResponse();
                pc.setValue("name", d.getName());

                setProductConfig(service, pc);
                service.setCustomProperties(cp);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        } catch (PluginException ex) {
            log.error(ex, ex);
        }

        return services;
    }

    @Override
    String getPidsQuery() {
        return "State.Name.sw=java,Args.*.eq=org.jboss.as.standalone";
    }

    @Override
    String getConfigRoot() {
        return "//server";
    }

    @Override
    String getDefaultConfigName() {
        return "standalone.xml";
    }

    @Override
    String getDefaultConfigDir() {
        return "/standalone";
    }
}

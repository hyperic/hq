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

package org.hyperic.hq.plugin.websphere.ejs;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.websphere.pmi.PmiException;
import com.ibm.websphere.pmi.client.CpdCollection;
import com.ibm.websphere.pmi.client.PerfDescriptor;
import com.ibm.websphere.pmi.client.PmiClient;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.hq.plugin.websphere.WebspherePMI;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

import org.hyperic.hq.plugin.websphere.wscp.AppServerCommand;
import org.hyperic.hq.plugin.websphere.wscp.ApplicationCommand;
import org.hyperic.hq.plugin.websphere.wscp.DataSourceCommand;
import org.hyperic.hq.plugin.websphere.wscp.EjbCommand;
import org.hyperic.hq.plugin.websphere.wscp.ThreadPoolCommand;
import org.hyperic.hq.plugin.websphere.wscp.WebappCommand;
import org.hyperic.hq.plugin.websphere.wscp.WebsphereCommand;

/**
 * WebSphere AE 4.0 Application server and service discovery.
 */
public class EjsWebsphereRuntimeDiscoverer {

    private Log log = LogFactory.getLog("EjsWebsphereRuntimeDiscoverer");

    private static HashMap types = new HashMap();

    static {
        String appServerType = 
            WebsphereProductPlugin.SERVER_NAME_AE +
            " " + WebsphereProductPlugin.VERSION_40;

        types.put(AppServerCommand.OBJECT_NAME,
                  appServerType);

        types.put(ApplicationCommand.OBJECT_NAME,
                  appServerType + " " +
                  WebsphereProductPlugin.APP_NAME);

        types.put(WebappCommand.OBJECT_NAME,
                  appServerType + " " +
                  WebsphereProductPlugin.WEBAPP_NAME);

        types.put(EjbCommand.OBJECT_NAME,
                  appServerType + " " +
                  WebsphereProductPlugin.EJB_NAME);

        types.put(DataSourceCommand.OBJECT_NAME,
                  appServerType + " " +
                  WebsphereProductPlugin.CONNPOOL_NAME);

        types.put(ThreadPoolCommand.OBJECT_NAME,
                  appServerType + " " +
                  WebsphereProductPlugin.THRPOOL_NAME);
    }

    private String getTypeName(WebsphereCommand cmd) {
        return (String)types.get(cmd.getObjectFullName());
    }

    class AppServerResource {
        AppServerCommand server;
        ArrayList services = new ArrayList();
    }

    public List discoverServers(ConfigResponse config) 
        throws PluginException {

        String installpath =
            config.getValue(ProductPlugin.PROP_INSTALLPATH);

        this.log.debug("discover using: " + config);

        PmiClient pmiclient = null;
        boolean enablePMI = WebsphereProductPlugin.enablePMI(config);

        try {
            pmiclient =
                WebspherePMI.getPmiClient(config.toProperties(),
                                          WebsphereProductPlugin.VERSION_AE);
        } catch (RemoteException e) {
            this.log.warn("Unable to get PMI client", e);
        }

        List aiservers = new ArrayList();

        String adminHost =
            config.getValue(WebsphereProductPlugin.PROP_ADMIN_HOST);

        String adminPort =
            config.getValue(WebsphereProductPlugin.PROP_ADMIN_PORT);

        String node =
            config.getValue(WebsphereProductPlugin.PROP_SERVER_NODE);

        WebsphereRemote remote;

        remote = WebsphereRemote.getInstance(adminHost, adminPort, false);

        WebsphereCommand[] objects = remote.discover(node);

        ArrayList servers = new ArrayList();

        AppServerResource res = null;

        for (int i=0; i<objects.length; i++) {
            if (objects[i] instanceof AppServerCommand) {
                res = new AppServerResource();
                res.server = (AppServerCommand)objects[i];
                servers.add(res);
            }
            else {
                res.services.add(objects[i]);
            }
        }

        for (int i=0; i<servers.size(); i++) {
            res = (AppServerResource)servers.get(i);
            AppServerCommand server = res.server;
            ArrayList services = res.services;
            ServerResource aServer = new ServerResource();

            aServer.setInstallPath(installpath);
            aServer.setIdentifier(server.getFullName());

            ConfigResponse productConfig =
                new ConfigResponse(server.getProperties());

            productConfig.merge(config, false);

            ConfigResponse metricConfig =
                new ConfigResponse(server.getMetricProperties());

            ConfigResponse controlConfig = new ConfigResponse();

            aServer.setProductConfig(productConfig);
            aServer.setMeasurementConfig(metricConfig);
            aServer.setControlConfig(controlConfig);

            String srvType = getTypeName(server);
            String srvName = server.getLeafName();
            String srvId   = node + " " + srvName;

            aServer.setType(srvType);
            aServer.setName(srvType + " " + srvId);

            this.log.debug("discovered server: " + aServer.getName());

            if (enablePMI) {
                try {
                    WebspherePMI.enablePMI(pmiclient, node, srvName);
                    this.log.debug("PMI enabled for " + node + "/" + srvName);
                } catch (PluginException e) {
                    this.log.warn("Failed to enable PMI for " + srvName, e);
                }
            }

            ArrayList containedServices =
                discoverThreadPools(pmiclient, node, srvName);

            if (containedServices.size() > 0) {
                containedServices.addAll(services);
            }
            else {
                containedServices = services;
            }

            for (int j=0; j<containedServices.size(); j++) {
                WebsphereCommand service =
                    (WebsphereCommand)containedServices.get(j);
                ServiceResource aiservice = new ServiceResource();

                aiservice.setType(getTypeName(service));
                aiservice.setName(srvId + " " +
                                  service.getLeafName() + " " +
                                  service.getObjectFullName());

                this.log.debug("discovered service: " + aiservice.getName());

                productConfig = new ConfigResponse(service.getProperties());

                metricConfig =
                    new ConfigResponse(service.getMetricProperties());

                aiservice.setProductConfig(productConfig);
                aiservice.setMeasurementConfig(metricConfig);
                if (service.hasControl()) {
                    aiservice.setControlConfig(controlConfig);
                }

                aServer.addService(aiservice);
            }

            aiservers.add(aServer);
        }

        return aiservers;
    }

    private ArrayList discoverThreadPools(PmiClient pmiclient,
                                          String node,
                                          String server) {
        ArrayList pools = new ArrayList();

        if (pmiclient == null) {
            return pools;
        }

        PerfDescriptor serverDescriptor =
            PmiClient.createPerfDescriptor(node + "/" + server);

        PerfDescriptor threadPool =
            PmiClient.createPerfDescriptor(serverDescriptor,
                                           "threadPoolModule");

        CpdCollection cpd;

        try {
            cpd = pmiclient.get(threadPool, true);
        } catch (PmiException e) {
            this.log.error(e.getMessage(), e);
            return pools;
        }

        if (cpd == null) {
            return pools;
        }

        CpdCollection submodules[] = cpd.subcollections();

        if (submodules == null) {
            return pools;
        }

        for (int i=0; i<submodules.length; i++) {
            String name = submodules[i].getDescriptor().getName();
            pools.add(new ThreadPoolCommand(name));
        }

        return pools;
    }
}


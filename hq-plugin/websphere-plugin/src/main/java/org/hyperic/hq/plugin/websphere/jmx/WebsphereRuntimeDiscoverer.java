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
package org.hyperic.hq.plugin.websphere.jmx;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.websphere.WebSphereProcess;
import org.hyperic.hq.plugin.websphere.WebsphereDetector;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;
import org.hyperic.hq.plugin.websphere.WebsphereUtil;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginUpdater;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.jmx.ServiceTypeFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * WebSphere Application server and service discovery.
 */
public class WebsphereRuntimeDiscoverer {

    private Log log =
            LogFactory.getLog(WebsphereRuntimeDiscoverer.class.getName());
    private ServiceTypeFactory serviceTypeFactory = new ServiceTypeFactory();
    private String version;
    private WebsphereDetector serverDetector;
    static final WebSphereQuery[] serviceQueries = {
        new JDBCProviderQuery(),
        new ThreadPoolQuery(),
        new ApplicationQuery(),};
    static final WebSphereQuery[] moduleQueries = {
        new EJBModuleQuery(),
        new WebModuleQuery(),};
    private PluginUpdater pluginUpdater = new PluginUpdater();

    public WebsphereRuntimeDiscoverer(String version, WebsphereDetector serverDetector) {
        this.version = version;
        this.serverDetector = serverDetector;
    }

    private List discover(AdminClient mServer,
            String domain,
            WebSphereQuery query)
            throws PluginException {

        List res = new ArrayList();
        boolean isApp = query instanceof ApplicationQuery;

        ObjectName scope;
        try {
            scope = new ObjectName(domain + ":"
                    + query.getScope() + ",*");
        } catch (MalformedObjectNameException e) {
            throw new PluginException(e.getMessage(), e);
        }

        Set beans;

        try {
            query.setMBeanServer(mServer);
            beans = mServer.queryNames(scope, null);
        } catch (ConnectorException e) {
            throw new PluginException(e.getMessage(), e);
        }

        for (Iterator it = beans.iterator(); it.hasNext();) {
            ObjectName obj = (ObjectName) it.next();
            if (!query.apply(obj)) {
                continue;
            }
            WebSphereQuery type = query.cloneInstance();
            type.setName(obj.getKeyProperty("name"));
            type.setObjectName(obj);
            type.getAttributes(mServer, obj);
            res.add(type);

            if (isApp) {
                for (int i = 0; i < moduleQueries.length; i++) {
                    WebSphereQuery moduleQuery = moduleQueries[i];
                    moduleQuery.setParent(type);
                    res.addAll(discover(mServer, domain, moduleQuery));
                }
            }
        }
        return res;
    }

    public List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List res = new ArrayList();
        try {
            AdminClient mServer = WebsphereUtil.getMBeanServer(config.toProperties());
            String domain = mServer.getDomainName();
            WebSphereProcess proc = new WebSphereProcess(config);

            NodeQuery nodeQuery = new NodeQuery();
            nodeQuery.setName(proc.getNode());
            nodeQuery.setCell(proc.getCell());
            nodeQuery.setVersion(this.version);

            AppServerQuery serverQuery = new AppServerQuery();
            serverQuery.setParent(nodeQuery);
            serverQuery.installpath = proc.getServerRoot();

            List servers = discover(mServer, domain, serverQuery);
            for (int i = 0; i < servers.size(); i++) {
                WebSphereQuery server = (WebSphereQuery) servers.get(i);
                if (server.getName().equals(proc.getServer())) {
                    res = discoverServices(mServer, server, proc);
                }
            }
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (ConnectorException e) {
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        return res;
    }

    private List discoverServices(AdminClient mServer, WebSphereQuery server, WebSphereProcess proc) throws ConnectorException, PluginException {
        ConfigResponse productConfig;
        ConfigResponse metricConfig;
        ConfigResponse controlConfig = new ConfigResponse();
        ConfigResponse cprops;
        String profile = proc.getServerRoot().substring(proc.getServerRoot().lastIndexOf("/") + 1);

        String domain = mServer.getDomainName();
        List res = new ArrayList();
        List services = new ArrayList();
        for (int j = 0; j < serviceQueries.length; j++) {
            WebSphereQuery serviceQuery = serviceQueries[j];
            serviceQuery.setParent(server);
            services.addAll(discover(mServer, domain, serviceQuery));
        }

        for (int k = 0; k < services.size(); k++) {
            WebSphereQuery service = (WebSphereQuery) services.get(k);
            ServiceResource aiservice = new ServiceResource();

            String svcType = service.getResourceName();
            aiservice.setType(svcType);
            aiservice.setName(svcType + " " + profile + " " + proc.getCell() + " " + service.getFullName());

            this.log.debug("discovered service: " + aiservice.getName());

            productConfig = new ConfigResponse(service.getProperties());
            productConfig.setValue("server.name", proc.getServer());

            metricConfig = new ConfigResponse(service.getMetricProperties());

            aiservice.setProductConfig(productConfig);
            aiservice.setMeasurementConfig(metricConfig);
            if (service.hasControl()) {
                aiservice.setControlConfig(controlConfig);
            }

            cprops = new ConfigResponse(service.getCustomProperties());

            aiservice.setCustomProperties(cprops);
            res.add(aiservice);
        }
        return res;
    }

    private boolean hasValidCredentials(AdminClient mServer,
            String domain,
            String node,
            String server) {
        String name =
                domain + ":"
                + "process=" + server + ","
                + "node=" + node + ","
                + "type=Perf,name=PerfMBean,*";

        //try something that'll fail if global security is enabled.
        //don't want to report the servers/services until credentials
        //have been given to this admin instance.
        final String method = "getInstrumentationLevelString";
        try {
            Object level =
                    WebsphereUtil.invoke(mServer, name, method,
                    new Object[0], new String[0]);
            log.debug(name + ": level=" + level);
            return true;
        } catch (Exception e) {
            this.log.error("Unable to determine PMI level for '" + name + "': " + e.getMessage());
            return false;
        }
    }

    public List discoverServers(ConfigResponse config)
            throws PluginException {

        List aiservers = new ArrayList();
        AdminClient mServer;
        String domain;

        WebSphereProcess parentProc = new WebSphereProcess(config);

        this.log.debug("discover using: " + config);

        try {
            mServer = WebsphereUtil.getMBeanServer(config.toProperties());
            domain = mServer.getDomainName();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }
            return aiservers;
        }

        String installpath =
                config.getValue(ProductPlugin.PROP_INSTALLPATH);

        String node =
                config.getValue(WebsphereProductPlugin.PROP_SERVER_NODE);

        NodeQuery nodeQuery = new NodeQuery();
        nodeQuery.setName(node);
        nodeQuery.setVersion(this.version);

        AppServerQuery serverQuery = new AppServerQuery();
        serverQuery.setParent(nodeQuery);
        serverQuery.installpath = installpath;


        List servers = discover(mServer, domain, serverQuery);

        if (servers.size() == 0) {
            //likely invalid node name
            log.warn("No servers discovered for node: " + node);
        }

        for (int i = 0; i < servers.size(); i++) {
            serverQuery = (AppServerQuery) servers.get(i);
            String srvName = serverQuery.getName();

            ConfigResponse productConfig = new ConfigResponse(serverQuery.getProperties());

            WebSphereProcess proc = new WebSphereProcess(config);
            proc.setServer(srvName);
            log.debug("[discoverServers] --> config=" + config);
            log.debug("[discoverServers] --> proc=" + proc);

            productConfig.merge(config, false);
            productConfig.setValue(WebsphereProductPlugin.PROP_SERVER_NAME, serverQuery.getName());
            productConfig.setValue(WebsphereProductPlugin.PROP_ADMIN_HOST, serverDetector.getAdminHost(proc));
            productConfig.setValue(WebsphereProductPlugin.PROP_ADMIN_PORT, serverDetector.getAdminPort(proc));
            log.debug("[discoverServers] --> pc=" + productConfig);

            ServerResource server = new ServerResource();
            aiservers.add(server);

            server.setInstallPath(installpath);
            server.setIdentifier(proc.getIdentifier());

            String srvType = serverQuery.getResourceName();
            if (serverQuery.getName().equals(parentProc.getServer())) {
                srvType = WebsphereProductPlugin.SERVER_NAME + " Admin " + serverQuery.getVersion();
            }
            server.setType(srvType);
            server.setName(WebsphereDetector.getPlatformName() + " " + srvType + " " + proc.getServerName());

            this.log.debug("discovered server: " + server.getName());

            server.setProductConfig(productConfig);
            server.setMeasurementConfig(new ConfigResponse(serverQuery.getMetricProperties()));
            server.setControlConfig(serverDetector.getControlConfig(proc));
            server.setCustomProperties(new ConfigResponse(serverQuery.getCustomProperties()));

            ArrayList services = new ArrayList();

            if (!hasValidCredentials(mServer, domain, node, srvName)) {
                continue;
            }

            List res;
            try {
                res = discoverServices(mServer, serverQuery, proc);
            } catch (ConnectorException e) {
                throw new PluginException(e.getMessage(), e);
            }
            for (int n = 0; n < res.size(); n++) {
                server.addService((ServiceResource) res.get(n));
            }
        }

        return aiservers;
    }
}

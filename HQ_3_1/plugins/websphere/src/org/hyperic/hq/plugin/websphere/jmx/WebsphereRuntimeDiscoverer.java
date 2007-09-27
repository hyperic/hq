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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.hq.plugin.websphere.WebsphereUtil;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

/**
 * WebSphere Application server and service discovery.
 */
public class WebsphereRuntimeDiscoverer {

    private Log log =
        LogFactory.getLog(WebsphereRuntimeDiscoverer.class.getName());
    private String version;

    static final WebSphereQuery[] serviceQueries = {
        new JDBCProviderQuery(),
        new ThreadPoolQuery(),
        new ApplicationQuery(),
    };

    static final WebSphereQuery[] moduleQueries = {
        new EJBModuleQuery(),
        new WebModuleQuery(),
    };

    public WebsphereRuntimeDiscoverer(String version) {
        this.version = version;
    }

    public void discover(AdminClient mServer,
                         String domain,
                         WebSphereQuery query,
                         ArrayList types)
        throws PluginException {

        boolean isApp = query instanceof ApplicationQuery;

        ObjectName scope;
        try {
            scope = new ObjectName(domain + ":" +
                                   query.getScope() + ",*");
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

        for (Iterator it = beans.iterator();
             it.hasNext();) 
        {
            ObjectName obj = (ObjectName)it.next();
            WebSphereQuery type = query.cloneInstance();
            type.setName(obj.getKeyProperty("name"));
            type.setObjectName(obj);
            type.getAttributes(mServer, obj);
            types.add(type);

            if (isApp) {
                for (int i=0; i<moduleQueries.length; i++) {
                    WebSphereQuery moduleQuery = moduleQueries[i];
                    moduleQuery.setParent(type);
                    discover(mServer, domain, moduleQuery, types);
                }
            }
        }
    }

    private boolean hasValidCredentials(AdminClient mServer,
                                        String domain,
                                        String node,
                                        String server) {
        String name =
            domain + ":" +
            "process=" + server + "," +
            "node=" + node + "," +
            "type=Perf,name=PerfMBean,*";

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
            this.log.error("Unable to determine PMI level for '" +
                           name + "': " + e.getMessage());
            return false;
        }
    }

    public List discoverServers(ConfigResponse config)
        throws PluginException {

        List aiservers = new ArrayList();
        AdminClient mServer;
        String domain;

        this.log.debug("discover using: " + config);

        try {
            mServer = WebsphereUtil.getMBeanServer(config.toProperties());
            domain = mServer.getDomainName();
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (ConnectorException e) {
            throw new PluginException(e.getMessage(), e);
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

        ArrayList servers = new ArrayList();

        discover(mServer, domain, serverQuery, servers);

        if (servers.size() == 0) {
            //likely invalid node name
            log.warn("No servers discovered for node: " + node);
        }

        for (int i=0; i<servers.size(); i++) {
            ArrayList services = new ArrayList();
            serverQuery = (AppServerQuery)servers.get(i);
            String srvName = serverQuery.getName();

            if (!hasValidCredentials(mServer, domain, node, srvName)) {
                continue;
            }

            for (int j=0; j<serviceQueries.length; j++) {
                WebSphereQuery serviceQuery = serviceQueries[j];

                serviceQuery.setParent(serverQuery);

                discover(mServer, domain, serviceQuery, services);
            }

            ServerResource server = new ServerResource();
            aiservers.add(server);
            
            server.setInstallPath(installpath);
            server.setIdentifier(serverQuery.getFullName());

            String srvType = serverQuery.getResourceName();
            server.setType(srvType);
            server.setName(srvType + " " + serverQuery.getFullName());

            this.log.debug("discovered server: " + server.getName());

            ConfigResponse productConfig =
                new ConfigResponse(serverQuery.getProperties());

            productConfig.merge(config, false);

            ConfigResponse metricConfig =
                new ConfigResponse(serverQuery.getMetricProperties());

            //defaults are fine
            ConfigResponse controlConfig = new ConfigResponse();
            
            // this doesnt get set unless its a webapp and its configured 
            // for RT
            ConfigResponse rtConfig = null;

            ConfigResponse cprops = 
                new ConfigResponse(serverQuery.getCustomProperties());
            server.setProductConfig(productConfig);
            server.setMeasurementConfig(metricConfig);
            server.setControlConfig(controlConfig);
            server.setCustomProperties(cprops);

            for (int k=0; k<services.size(); k++) {
                WebSphereQuery service = (WebSphereQuery)services.get(k);
                ServiceResource aiservice = new ServiceResource();

                String svcType = service.getResourceName();
                aiservice.setType(svcType);
                aiservice.setName(svcType + " " + service.getFullName());

                this.log.debug("discovered service: " + aiservice.getName());

                productConfig = new ConfigResponse(service.getProperties());

                metricConfig = new ConfigResponse(service.getMetricProperties());
                // ResponseTime auto config
                if(service instanceof WebModuleQuery) {
                    rtConfig = ((WebModuleQuery)service).getRtConfigResponse();
                }

                aiservice.setProductConfig(productConfig);
                aiservice.setMeasurementConfig(metricConfig);
                if (service.hasControl()) {
                    aiservice.setControlConfig(controlConfig);
                }
                // only autoconfigure RT if there is a configresponse
                // and its got something in it
                if (rtConfig != null && 
                    rtConfig.size() > 0)
                {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("AutoConfiguring ResponseTime for " +
                                aiservice.getName() + ": " +
                                rtConfig.toProperties());
                    }
                    aiservice.setResponseTimeConfig(rtConfig);
                }
                else {
                    this.log.debug("Skipping response time " +
                            "autoconfiguration for: " +
                            aiservice.getName());
                }

                cprops =
                    new ConfigResponse(service.getCustomProperties());
                aiservice.setCustomProperties(cprops);
                server.addService(aiservice);
            }
        }

        return aiservers;
    }
}


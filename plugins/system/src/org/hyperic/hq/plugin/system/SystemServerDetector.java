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

package org.hyperic.hq.plugin.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;

import org.hyperic.hq.autoinventory.ServerSignature;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class SystemServerDetector
    extends ServerDetector
    implements AutoServerDetector, RuntimeDiscoverer
{
    private String name = null;
    private String autoinventoryIdentifier = null;
    private String fqdn;
    private String platformType;
    protected Properties props;
    
    protected static Log log = LogFactory.getLog("SystemServerDetector");

    public SystemServerDetector() {
        super();
    }

    protected abstract String getServerType();

    protected abstract ArrayList getSystemServiceValues(Sigar sigar, ConfigResponse config)
        throws SigarException;

    public void init(PluginManager manager)
            throws PluginException {

        super.init(manager);
        this.props = manager.getProperties();
    }

    private String getServerName() {
        if (this.name == null) {
            this.name =
                getPlatformName() + " " + this.platformType;
        }
        
        return this.name;
    }

    // Backwards compat for current customers.
    private String getAutoinventoryIdentifier() {
        if (this.autoinventoryIdentifier == null) {
            this.autoinventoryIdentifier =
                this.fqdn + " " + this.platformType;
        }

        return this.autoinventoryIdentifier;
    }

    public ServerSignature getServerSignature() {
        return new ServerSignature(getServerType(),
                                   new String[0],
                                   new String[0], 
                                   new String[0]);
    }

    public void configure(ConfigResponse config)
        throws PluginException {
        
        super.configure(config);

        this.fqdn =
            config.getValue(SystemPlugin.PROP_PLATFORM_FQDN);
        this.platformType =
            config.getValue(SystemPlugin.PROP_PLATFORM_TYPE);
    }
    
    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {

        ArrayList servers = new ArrayList();

        configure(platformConfig);

        servers.add(getSystemServerValue());

        return servers;
    }

    public RuntimeDiscoverer getRuntimeDiscoverer() {
        return this;
    }

    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config) 
        throws PluginException {

        RuntimeResourceReport rrr;
        AIServerExtValue server;
        AIServiceValue[] services;

        configure(config);
        services = getSystemServiceValues(config);
        if (services == null) {
            return null;
        }

        server = getSystemServerValue();

        server.setId(new Integer(serverId));

        for (int i=0; i<services.length; i++) {
            services[i].setServerId(serverId);
        }

        String prop = "system.services.discover";
        boolean reportServices = 
            !"false".equals(getManagerProperty(prop));

        if (services.length != 0) {
            //system.CPU.discover=false
            String type = services[0].getServiceTypeName();
            prop = "system." + type + ".discover";
            if ("false".equals(getManagerProperty(prop))) {
                reportServices = false;
            }
            if (!reportServices) {
                log.info("Ignoring " + type + " services.");
            }
        }

        if (reportServices) {
            server.setAIServiceValues(services);
        }

        aiplatform.addAIServerValue(server);

        rrr = new RuntimeResourceReport(serverId);
        rrr.addAIPlatform(aiplatform);

        return rrr;
    }

    private AIServerExtValue getSystemServerValue() {
        AIServerExtValue srv = new AIServerExtValue();

        String name = getFullServiceName(getServerType());
        String ident = getFullAutoinventoryIdentifier(getServerType());

        srv.setName(name);
        srv.setServerTypeName(getServerType());
        srv.setInstallPath("/");
        srv.setAutoinventoryIdentifier(ident);
        srv.setCTime(new Long(System.currentTimeMillis()));

        srv.setProductConfig(ConfigResponse.EMPTY_CONFIG);
        srv.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
        srv.setControlConfig(ConfigResponse.EMPTY_CONFIG);

        return srv;
    }

    protected AIServiceValue createSystemService(String type, String name) {
        Long now = new Long(System.currentTimeMillis());

        AIServiceValue svc = new AIServiceValue();
        svc.setServiceTypeName(type);
        svc.setName(name);
        svc.setCTime(now);
        svc.setMTime(now);

        return svc;
    }
    
    protected AIServiceValue createSystemService(String type,
                                                 String name,
                                                 String propName,
                                                 String propValue) {

        AIServiceValue svc = createSystemService(type, name);

        log.debug("discovered " + name + " [" + type + "]");

        try {
            ConfigResponse productConfig = new ConfigResponse();
            productConfig.setValue(propName, propValue);
            svc.setProductConfig(productConfig.encode());
            svc.setMeasurementConfig(new ConfigResponse().encode());
        } catch (Exception e) {
            log.error("Unable to encode config");
        }

        return svc;
    }

    protected String getFullServiceName(String name) {
        return getServerName() + " " + name;
    }

    protected String getFullAutoinventoryIdentifier(String name) {
        return getAutoinventoryIdentifier() + " " + name;
    }

    protected AIServiceValue[] getSystemServiceValues(ConfigResponse config) {
        ArrayList services;
        Sigar sigar = new Sigar();

        try {
            services = getSystemServiceValues(sigar, config);
        } catch (SigarException e) {
            log.error(e.getMessage(), e);
            return new AIServiceValue[0];
        } finally {
            sigar.close();
        }

        if (services == null) {
            return null;
        }

        return (AIServiceValue[])services.toArray(new AIServiceValue[0]);
    }
}

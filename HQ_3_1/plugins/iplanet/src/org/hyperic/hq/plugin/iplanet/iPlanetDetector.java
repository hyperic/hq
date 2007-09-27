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

package org.hyperic.hq.plugin.iplanet;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

import org.hyperic.util.config.ConfigResponse;

/**
 * Handles iPlanet server detection.
 */
public abstract class iPlanetDetector
    extends ServerDetector
    implements FileServerDetector {

    static final String SERVER_NAME      = "iPlanet";
    static final String VHOST_NAME       = "VHost";
    static final String THREAD_POOL_NAME = "Thread Pool";
    
    static final String PROP_SERVER_ID  = "server.id";
    static final String PROP_THRPOOL_ID = "thrpool.id";

    static final String DEFAULT_ADMIN_ID = "https-admserv";

    protected abstract boolean isVersionConfigDir(File dir);

    protected abstract String getServerNameColumn();
    protected abstract String getServerPortColumn();
    protected abstract String getServerIdPrefix();

    protected List getServerList(String installpath) {
        ServerResource server = createServerResource(installpath);
        
        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain a List of 
     * AIServerValues, one for each server found.
     */
    public List getServerResources(ConfigResponse platformConfig, String path) throws PluginException {

        File filePath = new File(path).getParentFile().getParentFile();

        if ( !filePath.exists() ) {
            throw new PluginException("Error detecting iPlanet "
                                      + "server in: " + path);
        }

        if (!isVersionConfigDir(new File(filePath, "config"))) {
            return null;
        }

        return getServerList(filePath.toString());
    }

    protected ConfigResponse getControlConfig(String installpath) {
        ConfigResponse config = new ConfigResponse();

        config.setValue(iPlanetControlPlugin.PROP_PROGRAM, 
                        installpath + "/start");
        config.setValue(iPlanetControlPlugin.PROP_PIDFILE, 
                        installpath + "/logs/pid");
        config.setValue(iPlanetControlPlugin.PROP_TIMEOUT, "120");
        
        return config;
    }

    protected List discoverServers(ConfigResponse config)
            throws PluginException {

        SNMPClient client = new SNMPClient();
        SNMPSession session;

        try {
            session = client.getSession(config);
        } catch (SNMPException e) {
            throw new PluginException("Error getting SNMP session: " + e, e);
        }
        
        return discoverServers(session, config);
    }

    protected List discoverServers(SNMPSession session,
                                   ConfigResponse serverConfig) 
        throws PluginException {

        List names, ports = new ArrayList();

        try {
            names = session.getColumn(getServerNameColumn());
        } catch (SNMPException e) {
            throw new PluginException("Error getting SNMP column: " + 
                                      getServerNameColumn() + ": " + e, e);
        }

        try {
            //grab the first port for each iwsInstanceId
            List listenPorts = session.getColumn(getServerPortColumn());

            for (int i=0; i<listenPorts.size(); i++) {
                SNMPValue val = (SNMPValue)listenPorts.get(i);
                String oid = val.getOID();
                int ix = oid.lastIndexOf(".") + 1;
                if (!oid.substring(ix).equals("1")) {
                    continue;
                }
                ports.add(val.toString());
            }
        } catch (SNMPException e) {
            getLog().warn("Error getting SNMP column: " + 
                          getServerPortColumn() + ":" + e.getMessage(), e);
            ports = null;
        }

        if (ports.size() == 0) {
            getLog().warn("no data found in column=" + getServerPortColumn());
            ports = null;
        }

        List servers = new ArrayList(names.size());
        String prefix = getServerIdPrefix();
        String adminPath = 
            serverConfig.getValue(ProductPlugin.PROP_INSTALLPATH);

        for (int i=0; i<names.size(); i++) {
            String name = names.get(i).toString();
            String id = prefix + name;

            String installpath = 
                new File(new File(adminPath).getParentFile(),
                         id).getAbsolutePath();

            ServerResource server = createServerResource(installpath);
            servers.add(server);
            server.setType(getServerType());
            server.setName(getPlatformName() + " " + 
                           getServerType() + " " + name);

            if (ports == null) {
                continue;
            }

            String portName = null;

            if (i+1 > ports.size()) {
                getLog().info(name +
                              " is not running, unable to configure port");
                portName = "80"; //default
            }
            else {
                portName = ports.get(i).toString();
            }

            ConfigResponse productConfig = new ConfigResponse();
            ConfigResponse metricConfig = new ConfigResponse();

            //merge snmp props
            for (Iterator it=serverConfig.getKeys().iterator(); it.hasNext();) {
                String key = (String)it.next();
                if (key.startsWith("snmp")) {
                    metricConfig.setValue(key, serverConfig.getValue(key));
                }
            }

            serverConfig.merge(productConfig, false);
            productConfig.setValue(Collector.PROP_PORT, portName);
            productConfig.setValue(Collector.PROP_PROTOCOL,
                                   getConnectionProtocol(portName));
            productConfig.setValue(iPlanetDetector.PROP_SERVER_ID, id);

            server.setProductConfig(productConfig);
            server.setMeasurementConfig(metricConfig);
            server.setControlConfig(getControlConfig(installpath));
        }
        
        return servers;
    }

    protected String getServerType() {
        return SERVER_NAME + " " + getTypeInfo().getVersion();
    }
}

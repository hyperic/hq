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
import java.util.List;
import java.util.Properties;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

import org.hyperic.util.config.ConfigResponse;

public class iPlanet6Detector
    extends iPlanetDetector
    implements AutoServerDetector {

    private static final String[] PTQL_QUERIES = {
        "State.Name.eq=uxwdog",        //6.0
        "State.Name.eq=webservd-wdog", //6.1
    };

    private static final String VHOST_COLUMN       = "iwsVsId";
    private static final String THREAD_POOL_COLUMN = "iwsThreadPoolId";

    protected String getServerNameColumn() {
        return "iwsInstanceId";
    }

    protected String getServerPortColumn() {
        return "iwsListenPort";
    }

    protected String getServerIdPrefix() {
        return "";
    }
    
    protected boolean isVersionConfigDir(File dir) {
        //this file does not exist in 4.1 installations
        return new File(dir, "server.dtd").exists();
    }

    /**
     * Find installpath of the Admin server.
     */
    private static String findAdminServerProcess(long[] pids) {
        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);
            String configDir = null;

            for (int j=0; j<args.length; j++) {
                if (args[j].equals("-d")) {
                    configDir = args[j+1];
                    break;
                }
            }

            if (configDir == null) {
                continue;
            }

            //example: /usr/local/iplanet-6.0/servers/https-admserv/config
            File dir = new File(configDir).getParentFile();
            if (dir.getName().equals(DEFAULT_ADMIN_ID)) {
                return dir.toString();
            }
        }

        return null;
    }

    static String findServerProcess() {
        for (int i=0; i<PTQL_QUERIES.length; i++) {
            long[] pids = getPids(PTQL_QUERIES[i]);
            String path = findAdminServerProcess(pids);
            if (path != null) {
                return path;
            }
        }

        String idefault = "/opt/SUNWwbsvr/https-admserv";
        if (new File(idefault).exists()) {
            return idefault;
        }

        return null;
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        String path;

        if ((path = findServerProcess()) == null) {
            return null;
        }
        ServerResource server = createServerResource(path);

        Properties config = new Properties();
        config.setProperty(SNMPClient.PROP_VERSION,
                           SNMPClient.VALID_VERSIONS[0]); //v1

        setMeasurementConfig(server, new ConfigResponse(config));
        
        config = new Properties();
        config.setProperty(Collector.PROP_PORT, "8888");
        config.setProperty(PROP_SERVER_ID, DEFAULT_ADMIN_ID);
        config.setProperty(Collector.PROP_PROTOCOL,
                           Collector.PROTOCOL_HTTP);
        setProductConfig(server, new ConfigResponse(config));
        setControlConfig(server, getControlConfig(path));

        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    protected List discoverServers(SNMPSession session,
                                   ConfigResponse serverConfig) 
        throws PluginException {

        List servers = super.discoverServers(session, serverConfig);

        discoverServices(session, servers,
                         VHOST_COLUMN,
                         VHOST_NAME, 1);

        discoverServices(session, servers,
                         THREAD_POOL_COLUMN,
                         THREAD_POOL_NAME, 2);

        return servers;
    }

    private void discoverServices(SNMPSession session,
                                  List servers,
                                  String column,
                                  String type,
                                  int suffix)
        throws PluginException {

        List names;
        try {
            names = session.getColumn(column);
        } catch (SNMPException e) {
            throw new PluginException("Error getting SNMP column: " +
                                      column + ":" + e, e);
        }

        for (int i=0; i<names.size(); i++) {
            int idx=-1, serverIndex;
            //Find which server to add this to by looking at the OID
            //example:
            //     suffix == 2
            //        oid == 1.3.6.1.4.1.42.1.60.5.1.2.4.1.1
            //serverIndex == 4                         ^
            SNMPValue snmpVal = (SNMPValue)names.get(i);
            String oid = snmpVal.getOID();

            for (int j=0; j<suffix; j++) {
                idx = oid.lastIndexOf(".");
                if (idx == -1) {
                    String msg = 
                        "Error parsing OID='" + oid + "', " +
                        "suffix=" + suffix + " too short";
                    throw new PluginException(msg);
                }
                oid = oid.substring(0, idx);
            }

            String parsed = oid;
            if ((idx = oid.lastIndexOf('.')) != -1) {
                parsed = oid.substring(idx+1);
            }

            try {
                serverIndex = Integer.parseInt(parsed);
            } catch (NumberFormatException e) {
                String msg =
                    "Error parsing oid: " + snmpVal.getOID() +
                    "(idx=" + idx + ", oid=" + oid +
                    ", parsed=" + parsed + ")" +
                    ": " + e;
                throw new PluginException(msg, e);
            }

            // SNMP starts indexes at 1, not zero
            ServerResource server =
                (ServerResource)servers.get(serverIndex-1);

            String id = snmpVal.toString();

            ServiceResource service = new ServiceResource();
            service.setType(getServerType() + " " + type);
            //not using setServiceName to avoid 'Admin' in the name
            service.setName(server.getName() + " " + type + " " + id);

            server.addService(service);

            if (server.getProductConfig() == null) {
                getLog().info("no product config for " + server.getName());
                continue;
            }

            ConfigResponse config = new ConfigResponse();

            //inherit the product config from our parent server.
            
            if (type.equals(VHOST_NAME)) {
                config.merge(server.getProductConfig(), false);
            }
            else if (type.equals(THREAD_POOL_NAME)) {
                config.setValue(iPlanetDetector.PROP_THRPOOL_ID, id);
            }

            service.setProductConfig(config);
            service.setMeasurementConfig();
        }
    }

    public static void main(String[] args) {
        String path = findServerProcess();
        System.out.println(path);
    }
}

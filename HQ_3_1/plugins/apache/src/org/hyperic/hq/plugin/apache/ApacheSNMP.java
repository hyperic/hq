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

package org.hyperic.hq.plugin.apache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

public class ApacheSNMP {

    static final String COLUMN_VHOST_NAME = "wwwServiceName";
    static final String COLUMN_VHOST_PORT = "wwwServiceProtocol";
    static final String COLUMN_VHOST_DESC = "wwwServiceDescription";
    static final String COLUMN_VHOST_ADM  = "wwwServiceContact";
    static final String TCP_PROTO_ID      = "1.3.6.1.2.1.6.";
    private static final int TCP_PROTO_ID_LEN = TCP_PROTO_ID.length();
    private static HashMap configCache = null;

    private SNMPClient client = new SNMPClient();

    public class Server {
        String name;
        String port;
        String description;
        String version;
        String admin;

        public String toString() {
            return this.name + ":" + this.port;
        }
    }

    static class ConfigFile {
        long lastModified;
        String port;
        String listen = "127.0.0.1";

        public String toString() {
            return this.listen + ":" + this.port;
        }
    }

    public List getServers(Properties config) throws SNMPException {
        return getServers(new ConfigResponse(config));
    }

    /**
     * Find configured virtual servers using SNMP.
     */
    public List getServers(ConfigResponse config) throws SNMPException {
        SNMPSession session;
        List names, ports, admins;
        List servers = new ArrayList();
        String description = null, version = null;

        try {
            session = client.getSession(config);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP session: " 
                                    + e.getMessage(), e);
        }
        try {
            names = session.getBulk(COLUMN_VHOST_NAME);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_NAME +
                                    ": " + e.getMessage(), e);
        }

        try {
            ports = session.getBulk(COLUMN_VHOST_PORT);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_PORT +
                                    ": " + e.getMessage(), e);
        }
        
        try {
            admins = session.getBulk(COLUMN_VHOST_ADM);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_ADM +
                                    ": " + e.getMessage(), e);
        }
        
        try {
            //just get the first, they are all the same.
            SNMPValue desc = session.getNextValue(COLUMN_VHOST_DESC);
            if (desc != null) {
                description = desc.toString();
                StringTokenizer tok = new StringTokenizer(description);
                final String ap = "Apache/";
                while (tok.hasMoreTokens()) {
                    String component = tok.nextToken();
                    if (component.startsWith(ap)) {
                        version = component.substring(ap.length());
                        break;
                    }
                }
            }
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP value: " 
                                    + COLUMN_VHOST_DESC +
                                    ": " + e.getMessage(), e);
        }

        for (int i=0; i<names.size(); i++) {
            Server server = new Server();

            server.port =
                ports.get(i).toString().substring(TCP_PROTO_ID_LEN);

            server.name = names.get(i).toString();

            server.admin = admins.get(i).toString();

            server.description = description;
            server.version = version;

            servers.add(server);
        }

        return servers;
    }

    static ConfigFile getConfig(String file) throws IOException {
        if (configCache == null) {
            configCache = new HashMap();
        }

        ConfigFile config = (ConfigFile)configCache.get(file);

        long lastModified = new File(file).lastModified();

        if ((config == null) || (lastModified != config.lastModified)) {
            config = new ConfigFile();
            config.lastModified = lastModified;
            parse(file, config);
            configCache.put(file, config);
        }

        return config;
    }

    private static void parse(String file, ConfigFile config)
        throws IOException {

        String line;
        BufferedReader reader =
            new BufferedReader(new FileReader(file));

        final String portToken = "agentaddress";

        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(portToken)) {
                    config.port =
                        line.substring(portToken.length()).trim();
                    int ix = config.port.indexOf('@');
                    if (ix != -1) {
                        config.listen = config.port.substring(ix+1);
                        config.port = config.port.substring(0, ix);
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Get the default SNMP configuration properties.
     */
    public static Properties getConfigProperties() {
        return getConfigProperties(null);
    }

    /**
     * Get the default SNMP configuration properties overriding the port.
     */
    public static Properties getConfigProperties(ConfigFile config) {
        Properties props = new Properties();

        if (config != null) {
            props.setProperty(SNMPClient.PROP_PORT, config.port);
            props.setProperty(SNMPClient.PROP_IP, config.listen);
        }

        return props;
    }
}

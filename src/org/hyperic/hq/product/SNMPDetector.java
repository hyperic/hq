/*
 * 'SNMPDetector.java' NOTE: This copyright does *not* cover user programs that
 * use HQ program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development Kit or
 * the Hyperic Client Development Kit - this is merely considered normal use of
 * the program, and does *not* fall under the heading of "derived work".
 * Copyright (C) [2004, 2005, 2006, 2007, 2008, 2009], Hyperic, Inc. This file
 * is part of HQ. HQ is free software; you can redistribute it and/or modify it
 * under the terms version 2 of the GNU General Public License as published by
 * the Free Software Foundation. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.util.config.ConfigResponse;

/*
 * Generic SNMP Detector intended for pure-xml plugins that extend the Network
 * Device platform or servers with builtin SNMP management such as squid.
 */
public class SNMPDetector
    extends DaemonDetector
{
    private static final Log log = LogFactory.getLog(SNMPDetector.class.getName());

    static final String SNMP_INDEX_NAME = SNMPMeasurementPlugin.PROP_INDEX_NAME;
    static final String SNMP_DESCRIPTION = "snmpDescription";

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        String indexName = getTypeProperty(SNMP_INDEX_NAME);

        if (indexName != null) {
            log.debug("Looking for servers with " + indexName);

            return discoverServices(platformConfig, getTypeInfo().getName());
        }

        return super.getServerResources(platformConfig);
    }

    protected List discoverServices(ConfigResponse config) throws PluginException {
        return discoverServices(config, null);
    }

    protected List discoverServices(ConfigResponse config, String type) throws PluginException {
        log.debug("discoverServices(" + config + ")");

        String[] keys = getCustomPropertiesSchema().getOptionNames();

        ConfigResponse cprops = new ConfigResponse();

        SNMPSession session;

        try {
            session = new SNMPClient().getSession(config);

            // Custom properties discovery for the server...
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                if (SNMPClient.getOID(key) == null) {
                    log.debug("Cannot resolve '" + key + "'");

                    continue;
                }

                try {
                    cprops.setValue(key, session.getSingleValue(key).toString());
                } catch (SNMPException e) {
                    log.warn("Error getting '" + key + "': " + e.getMessage());
                }
            }

            setCustomProperties(cprops);

            if (type == null) {
                // Discover services for existings server...
                return discoverServices(this, config, session);
            } else {
                // Discover SNMP services as server types...
                return discoverServers(this, config, session, type);
            }
        } catch (SNMPException e) {
            String msg = "Error discovering services for " + getTypeInfo() + ": " + e;

            log.error(msg, e);

            return null;
        } finally {
            session = null;
        }
    }

    public static List discoverServices(ServerDetector plugin, ConfigResponse parentConfig, SNMPSession session) throws PluginException
    {
        List services = new ArrayList();

        Map servicePlugins = plugin.getServiceInventoryPlugins();

        if (servicePlugins == null) {
            return services;
        }

        for (Iterator it = servicePlugins.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            String type = (String) entry.getKey();

            // String name = (String)entry.getValue ( );
            services.addAll(discoverServices(plugin, parentConfig, session, type));
        }

        return services;
    }

    public static List discoverServices(ServerDetector plugin,
                                        ConfigResponse parentConfig,
                                        SNMPSession session,
                                        String type) throws PluginException
    {
        return discoverServices(plugin, parentConfig, session, type, true);
    }

    public static List discoverServers(ServerDetector plugin,
                                       ConfigResponse parentConfig,
                                       SNMPSession session,
                                       String type) throws PluginException
    {
        return discoverServices(plugin, parentConfig, session, type, false);
    }

    private static List discoverServices(ServerDetector plugin,
                                         ConfigResponse parentConfig,
                                         SNMPSession session,
                                         String type,
                                         boolean isServiceDiscovery) throws PluginException
    {
        List services = new ArrayList();

        String typeName = plugin.getTypeNameProperty(type);
        String indexName = plugin.getTypeProperty(type, SNMP_INDEX_NAME);
        String descrName = plugin.getTypeProperty(type, SNMP_DESCRIPTION);

        if (indexName == null) {
            String msg = "No " + SNMP_INDEX_NAME + " defined for service autoinventory of " + type;

            log.error(msg);

            return services;
        }

        List column;

        try {
            column = session.getColumn(indexName);
        } catch (SNMPException e) {
            String msg = "Error getting " + SNMP_INDEX_NAME + "=" + indexName + ": " + e;

            log.error(msg);

            return services;
        }

        log.debug("Found " + column.size() + " " + type + " services using " + indexName);

        boolean hasDescriptions = false;

        List descriptions = null;

        if (descrName != null) {
            try {
                descriptions = session.getColumn(descrName);
            } catch (SNMPException e) {
                String msg = "Error getting " + SNMP_DESCRIPTION + "=" + descrName + ": " + e;

                log.warn(msg);
            }

            if ((descriptions != null) && (descriptions.size() == column.size())) {
                hasDescriptions = true;
            }
        }

        String[] keys = plugin.getCustomPropertiesSchema(type).getOptionNames();

        HashMap cpropColumns = new HashMap();

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];

            try {
                cpropColumns.put(key, session.getColumn(key));
            } catch (SNMPException e) {
                log.warn("Error getting '" + key + "': " + e.getMessage());
            }
        }

        for (int i = 0; i < column.size(); i++) {
            ConfigResponse config = new ConfigResponse();
            ConfigResponse cprops = new ConfigResponse();

            String indexValue = column.get(i).toString().trim();
            String resourceDescr = null;

            config.setValue(SNMPMeasurementPlugin.PROP_INDEX_VALUE, indexValue);

            for (int j = 0; j < keys.length; j++) {
                String key = keys[j];

                List data = (List) cpropColumns.get(key);

                if ((data == null) || data.isEmpty()) {
                    continue;
                }

                String val = data.get(i).toString().trim();

                cprops.setValue(key, val);
            }

            if (hasDescriptions) {
                resourceDescr = descriptions.get(i).toString();
            }

            String resourceName = typeName + " " + indexValue;
            String autoName = plugin.formatAutoInventoryName(type, parentConfig, config, cprops);

            if (isServiceDiscovery) {
                ServiceResource service = new ServiceResource();

                service.setType(type);

                if (autoName == null) {
                    service.setServiceName(resourceName);
                } else {
                    service.setName(autoName);
                }

                service.setProductConfig(config);

                // Required to auto-enable metric...
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);

                if (resourceDescr != null) {
                    service.setDescription(resourceDescr);
                }

                services.add(service);
            } else {
                ServerResource server = new ServerResource();

                server.setType(type);

                if (autoName == null) {
                    server.setName(getPlatformName() + " " + resourceName);
                } else {
                    server.setName(autoName);
                }

                server.setInstallPath("/");
                server.setIdentifier(server.getName());

                server.setProductConfig(config);

                // Required to auto-enable metric...
                server.setMeasurementConfig();
                server.setCustomProperties(cprops);

                if (resourceDescr != null) {
                    server.setDescription(resourceDescr);
                }

                services.add(server);
            }
        }

        return services;
    }
}

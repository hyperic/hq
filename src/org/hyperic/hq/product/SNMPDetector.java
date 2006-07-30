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

/**
 * Generic SNMP Detector intended for pure-xml plugins
 * that extend the Network Device platform or servers
 * with builtin SNMP management such as squid. 
 */
public class SNMPDetector extends DaemonDetector {

    private static final Log log =
        LogFactory.getLog(SNMPDetector.class.getName());

    static final String SNMP_INDEX_NAME =
        SNMPMeasurementPlugin.PROP_INDEX_NAME;
    static final String SNMP_DESCRIPTION = "snmpDescription";

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String indexName = getTypeProperty(SNMP_INDEX_NAME);
        if (indexName != null) {
            log.debug("Looking for servers with " + indexName);
            return discoverServices(platformConfig,
                                    getTypeInfo().getName());
        }

        return super.getServerResources(platformConfig);
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        return discoverServices(config, null);
    }

    protected List discoverServices(ConfigResponse config, String type)
        throws PluginException {

        log.debug("discoverServices(" + config + ")");
        SNMPSession session;
        try {
            session = new SNMPClient().getSession(config);
            if (type == null) {
                //discover services for existings server
                return discoverServices(this, config, session);
            }
            else {
                //discover SNMP services as server types
                return discoverServers(this, config, session, type);
            }
        } catch (SNMPException e) {
            String msg =
                "Error discovering services for " + getTypeInfo() +
                ": " + e;
            log.error(msg, e);
            return null;
        } finally {
            session = null;
        }
    }

    public static List discoverServices(ServerDetector plugin,
                                        ConfigResponse parentConfig,
                                        SNMPSession session)
        throws PluginException {

        List services = new ArrayList();

        Map servicePlugins = plugin.getServiceInventoryPlugins();
        if (servicePlugins == null) {
            return services;
        }

        for (Iterator it=servicePlugins.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            String type = (String)entry.getKey();
            //String name = (String)entry.getValue();
            services.addAll(discoverServices(plugin, parentConfig, session, type));
        }

        return services;
    }

    public static List discoverServices(ServerDetector plugin,
                                        ConfigResponse parentConfig,
                                        SNMPSession session,
                                        String type)
        throws PluginException {

        return discoverServices(plugin, parentConfig, session, type, true);
    }

    public static List discoverServers(ServerDetector plugin,
                                       ConfigResponse parentConfig,
                                       SNMPSession session,
                                       String type)
        throws PluginException {

        return discoverServices(plugin, parentConfig, session, type, false);
    }
    
    private static List discoverServices(ServerDetector plugin,
                                         ConfigResponse parentConfig,
                                         SNMPSession session,
                                         String type,
                                         boolean isServiceDiscovery)
        throws PluginException {

        List services = new ArrayList();

        String typeName = plugin.getTypeNameProperty(type);
        String indexName = plugin.getTypeProperty(type, SNMP_INDEX_NAME);
        String descrName = plugin.getTypeProperty(type, SNMP_DESCRIPTION);

        if (indexName == null) {
            String msg =
                "No " + SNMP_INDEX_NAME +
                " defined for service autoinventory of " + type;
                log.error(msg);
                return services;
            }
    
        List column;
        try {
            column = session.getColumn(indexName);
        } catch (SNMPException e) {
            String msg =
                "Error getting " + SNMP_INDEX_NAME + "=" + indexName +
                ": " + e;
            log.error(msg);
            return services;
        }

        log.debug("Found " + column.size() + " " + type +
                  " services using " + indexName);
    
        boolean hasDescriptions = false;
        List descriptions = null;
                
        if (descrName != null) {
            try {
                descriptions = session.getColumn(descrName);
            } catch (SNMPException e) {
                String msg =
                    "Error getting " + SNMP_DESCRIPTION + "=" + descrName +
                    ": " + e;
                log.warn(msg);
            }
            if ((descriptions != null) && (descriptions.size() == column.size())) {
                hasDescriptions = true;
            }
        }
    
        String[] keys =
            plugin.getCustomPropertiesSchema(type).getOptionNames();
        HashMap cpropColumns = new HashMap();
    
        for (int i=0; i<keys.length; i++) {
            String key = keys[i];
            try {
                cpropColumns.put(key, session.getColumn(key));
            } catch (SNMPException e) {
                log.warn("Error getting '" + key + "': " +
                         e.getMessage());
            }
        }
    
        for (int i=0; i<column.size(); i++) {
            ConfigResponse config = new ConfigResponse();
            ConfigResponse cprops = new ConfigResponse();
            String indexValue = column.get(i).toString().trim();
            String resourceDescr = null;

            config.setValue(SNMPMeasurementPlugin.PROP_INDEX_VALUE,
                            indexValue);

            for (int j=0; j<keys.length; j++) {
                String key = keys[j];
                List data = (List)cpropColumns.get(key);
                if (data == null) {
                    continue;
                }
                String val = data.get(i).toString().trim();
                cprops.setValue(key, val);
            }
            if (hasDescriptions) {
                resourceDescr = descriptions.get(i).toString();
            }

            String resourceName =
                plugin.formatAutoInventoryName(type,
                                               parentConfig,
                                               config,
                                               cprops);

            if (resourceName == null) {
                resourceName = typeName + " " + indexValue;
            }

            if (isServiceDiscovery) {
                ServiceResource service = new ServiceResource();
                service.setType(plugin, typeName);
                service.setServiceName(resourceName);

                service.setProductConfig(config);
                //required to auto-enable metric
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                service.setDescription(resourceDescr);

                services.add(service);
            }
            else {
                ServerResource server = new ServerResource();
                server.setType(type);
                server.setName(getPlatformName() + " " + resourceName);
                server.setInstallPath("/"); //XXX
                server.setIdentifier(server.getName());

                server.setProductConfig(config);
                //required to auto-enable metric
                server.setMeasurementConfig();
                server.setCustomProperties(cprops);
                server.setDescription(resourceDescr);

                services.add(server);
            }
        }
    
        return services;
    }
}

/*
 * 'NetworkDeviceDetector.java' NOTE: This copyright does *not* cover user
 * programs that use HQ program services by normal system calls through the
 * application program interfaces provided as part of the Hyperic Plug-in
 * Development Kit or the Hyperic Client Development Kit - this is merely
 * considered normal use of the program, and does *not* fall under the heading
 * of "derived work". Copyright (C) [2004, 2005, 2006, 2007, 2008, 2009],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.plugin.netdevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PlatformServiceDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.SNMPDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

import org.hyperic.util.config.ConfigResponse;

public class NetworkDeviceDetector
    extends PlatformServiceDetector
{
    private static final String SVC_NAME = "Interface";
    private static final String PROP_IF = SVC_NAME.toLowerCase();
    static final String PROP_IF_IX = PROP_IF + ".index";
    static final String IF_DESCR = "ifDescr";
    static final String IF_NAME = "ifName";
    private static final String IF_MAC = "ifPhysAddress";
    private static final String IP_IF_IX = "ipAdEntIfIndex";
    private static final String IP_NETMASK = "ipAdEntNetMask";
    private static final String PROP_IP = "ipaddress";
    private static final String PROP_NETMASK = "netmask";
    private static final String[] FILTER_PROPS = { PROP_IP, PROP_NETMASK };

    private SNMPSession session;

    public List discoverServices(ConfigResponse serverConfig) throws PluginException {
        Log log = getLog();

        List services = new ArrayList();

        openSession(serverConfig);

        if (log.isDebugEnabled()) {
            log.debug("Using snmp config=" + serverConfig);
        }

        services.addAll(discoverInterfaces(serverConfig));

        List extServices = SNMPDetector.discoverServices(this, serverConfig, this.session);
        services.addAll(extServices);

        closeSession();

        return services;
    }

    protected boolean hasInterfaceService() {
        String type = getServiceTypeName(SVC_NAME);

        ProductPluginManager manager = (ProductPluginManager) getManager().getParent();

        MeasurementPlugin plugin = manager.getMeasurementPlugin(type);

        if (plugin == null) {
            // Interface service is not defined...
            return false;
        } else {
            // Check that ifMtu cprop exists, if so assume standard IF-MIB
            // interface...
            return plugin.getCustomPropertiesSchema().getOption("ifMtu") != null;
        }
    }

    protected List discoverInterfaces(ConfigResponse serverConfig) throws PluginException {
        Log log = getLog();

        List services = new ArrayList();

        String type = getServiceTypeName(SVC_NAME);

        if (!hasInterfaceService()) {
            log.debug("Skipping discovery of " + type);

            return services;
        }

        String[] keys = getCustomPropertiesSchema(type).getOptionNames();

        HashMap cpropColumns = new HashMap();

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];

            if (Arrays.binarySearch(FILTER_PROPS, key) != -1) {
                continue;
            }

            try {
                cpropColumns.put(key, getIfColumn(key));
            } catch (PluginException e) {
                log.warn("Error getting '" + key + "': " + e.getMessage());
            }
        }

        String columnName = serverConfig.getValue(PROP_IF_IX);

        if (columnName == null) {
            columnName = IF_DESCR;
        }

        Map interfaces = getIfColumn(columnName);

        log.debug("Found " + interfaces.size() + " interfaces using " + columnName);

        String descrColumn = columnName.equals(IF_DESCR) ? IF_NAME : IF_DESCR;

        Map descriptions;

        try {
            descriptions = getIfColumn(descrColumn);
        } catch (PluginException e) {
            descriptions = new HashMap();

            String msg = "Error getting descriptions from " + descrColumn + ": " + e;

            log.warn(msg);
        }

        List ip_if_ix = getColumn(IP_IF_IX);

        HashMap ips = new HashMap();

        HashMap netmasks = new HashMap();

        final String IF_IX_OID = SNMPClient.getOID(IP_IF_IX) + ".";
        final String NETMASK_OID = SNMPClient.getOID(IP_NETMASK) + ".";

        String ip, netmask;

        for (int i = 0; i < ip_if_ix.size(); i++) {
            SNMPValue value = (SNMPValue) ip_if_ix.get(i);

            String oid = value.getOID();
            String ix = value.toString();

            if (oid.startsWith(IF_IX_OID)) {
                ip = oid.substring(IF_IX_OID.length());

                ips.put(ix, ip);

                try {
                    netmask = this.session.getSingleValue(NETMASK_OID + ip).toString();

                    netmasks.put(ix, netmask);
                } catch (SNMPException e) {
                    log.debug("Failed to get netmask for " + ip);
                }
            }
        }

        for (Iterator it = interfaces.entrySet().iterator(); it.hasNext();) {
            ConfigResponse config = new ConfigResponse();
            ConfigResponse cprops = new ConfigResponse();

            Map.Entry entry = (Map.Entry) it.next();

            String ix = (String) entry.getKey();
            String name = (String) entry.getValue();
            String mac = null;

            ServiceResource service = createServiceResource(SVC_NAME);

            config.setValue(PROP_IF, name);
            config.setValue(PROP_IF_IX, columnName);
            service.setProductConfig(config);

            // required to auto-enable metric
            service.setMeasurementConfig();

            for (int j = 0; j < keys.length; j++) {
                String key = keys[j];

                Map data = (Map) cpropColumns.get(key);

                if (data == null) {
                    continue;
                }

                String val = (String) data.get(ix);

                if (val == null) {
                    continue;
                }

                cprops.setValue(key, val);

                if (key.equals(IF_MAC)) {
                    mac = val;
                }
            }

            ip = (String) ips.get(ix);

            netmask = (String) netmasks.get(ix);

            if (ip == null) {
                ip = "0.0.0.0";
            }

            if (netmask == null) {
                netmask = "0.0.0.0";
            }

            cprops.setValue(PROP_IP, ip);
            cprops.setValue(PROP_NETMASK, netmask);

            service.setCustomProperties(cprops);

            // Might be more than 1 interface w/ the same name,
            // so append the mac address to make it unique...
            name = name + " " + SVC_NAME;

            if ((mac != null) && !mac.equals("0:0:0:0:0:0")) {
                name += " (" + mac + ")";
            }

            service.setServiceName(name);

            Object obj = descriptions.get(ix);

            if (obj != null) {
                service.setDescription(obj.toString());
            }

            services.add(service);
        }

        return services;
    }

    static SNMPSession getSession(ConfigResponse config) throws PluginException {
        try {
            return new SNMPClient().getSession(config);
        } catch (SNMPException e) {
            throw new PluginException("Error getting SNMP session: " + e.getMessage(), e);
        }
    }

    // These could be in a base class of some sort...
    protected void openSession(ConfigResponse config) throws PluginException {
        this.session = getSession(config);
    }

    protected void closeSession() {
        this.session = null;
    }

    private String getIfIndex(SNMPValue val) {
        String oid = val.getOID();

        int last = oid.lastIndexOf('.');

        return oid.substring(last + 1);
    }

    protected Map getIfColumn(String name) throws PluginException {
        Map map = new LinkedHashMap();

        List column = getColumn(name);

        for (int i = 0; i < column.size(); i++) {
            SNMPValue ent = (SNMPValue) column.get(i);

            String ix = getIfIndex(ent);
            String val;

            if (name.equals(IF_MAC)) {
                val = ent.toPhysAddressString();
            } else {
                val = ent.toString().trim();
            }

            map.put(ix, val);
        }

        return map;
    }

    protected List getColumn(String name) throws PluginException {
        try {
            return this.session.getColumn(name);
        } catch (SNMPException e) {
            throw new PluginException("Error getting SNMP column: " + name + ":" + e, e);
        }
    }

    // Use platform.name instead of the generic type name...
    protected String getServerName(ConfigResponse config) {
        String fqdn = config.getValue(ProductPlugin.PROP_PLATFORM_FQDN);
        String name = config.getValue(ProductPlugin.PROP_PLATFORM_NAME);

        return fqdn + " " + name;
    }

    public List getServerResources(ConfigResponse config) {
        Log log = getLog();

        if (log.isDebugEnabled()) {
            log.debug("Testing snmp config=" + config);
        }

        if (config.getValue(SNMPClient.PROP_IP) == null) {
            log.debug("snmp config incomplete, defering server creation");

            return null;
        }

        try {
            getSession(config).getSingleValue("sysName");

        } catch (Exception e) {
            // Wait till we have valid snmp config at the platform level...
            log.debug("snmp config invalid, defering server creation");

            return null;
        }

        log.debug("snmp config valid, creating server");

        return super.getServerResources(config);
    }

    protected ServerResource getServer(ConfigResponse config) {
        ServerResource server = super.getServer(config);

        server.setName(getServerName(config));

        return server;
    }
}

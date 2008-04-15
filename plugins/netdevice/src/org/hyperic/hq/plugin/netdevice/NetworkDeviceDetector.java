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

package org.hyperic.hq.plugin.netdevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.config.ConfigResponse;

public class NetworkDeviceDetector extends PlatformServiceDetector {

    private static final String SVC_NAME   = "Interface";
    private static final String PROP_IF    = SVC_NAME.toLowerCase();
    static final String PROP_IF_IX = PROP_IF + ".index";
    static final String IF_DESCR   = "ifDescr";
    static final String IF_NAME    = "ifName";
    private static final String MAC_COLUMN = "ifPhysAddress";
    private static final String IP_IF_IX   = "ipAdEntIfIndex";
    private static final String IP_NETMASK = "ipAdEntNetMask";

    private static final String PROP_IP      = "ipaddress";
    private static final String PROP_NETMASK = "netmask";
    private static final String[] FILTER_PROPS = {
        PROP_IP, PROP_NETMASK
    };

    private SNMPSession session;

    private Object get(String name, List column, int ix) {
        int size = column.size();
        if (ix >= size) {
            getLog().warn(name + " column index [" + ix + "] " +
                          "out-of-bounds [" + size + "]");
            return null;
        }
        return column.get(ix);
    }

    public List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        Log log = getLog();

        List services = new ArrayList();

        openSession(serverConfig);

        if (log.isDebugEnabled()) {
            log.debug("Using snmp config=" + serverConfig);
        }

        services.addAll(discoverInterfaces(serverConfig));

        List extServices =
            SNMPDetector.discoverServices(this,
                                          serverConfig,
                                          this.session);
        services.addAll(extServices);

        closeSession();

        return services;
    }

    protected boolean hasInterfaceService() {
        String type = getServiceTypeName(SVC_NAME);
        ProductPluginManager manager =
            (ProductPluginManager)getManager().getParent();
        MeasurementPlugin plugin =
            manager.getMeasurementPlugin(type);
        if (plugin == null) {
            //Interface service is not defined
            return false;
        }
        else {
            //Check that ifMtu cprop exists, if so assume standard IF-MIB interface
            return plugin.getCustomPropertiesSchema().getOption("ifMtu") != null;
        }
    }

    protected List discoverInterfaces(ConfigResponse serverConfig)
        throws PluginException {

        Log log = getLog();
        List services = new ArrayList();

        String type = getServiceTypeName(SVC_NAME);

        if (!hasInterfaceService()) {
            log.debug("Skipping discovery of " + type);
            return services;
        }

        String[] keys =
            getCustomPropertiesSchema(type).getOptionNames();
        HashMap cpropColumns = new HashMap();
        for (int i=0; i<keys.length; i++) {
            String key = keys[i];
            if (Arrays.binarySearch(FILTER_PROPS, key) != -1) {
                continue;
            }
            try {
                cpropColumns.put(key, getColumn(key));
            } catch (PluginException e) {
                log.warn("Error getting '" + key + "': " +
                         e.getMessage());
            }
        }

        String columnName = serverConfig.getValue(PROP_IF_IX);
        if (columnName == null) {
            columnName = IF_DESCR;
        }
        List interfaces = getColumn(columnName);
        log.debug("Found " + interfaces.size() + " interfaces using " +
                  columnName);

        String descrColumn =
            columnName.equals(IF_DESCR) ? IF_NAME : IF_DESCR;
        List descriptions;
        
        try {
            descriptions = getColumn(descrColumn);
        } catch (PluginException e) {
            descriptions = null;
            String msg =
                "Error getting descriptions from " +
                descrColumn + ": " + e;
            log.warn(msg);
        }

        boolean hasDescriptions = 
            (descriptions != null) && (descriptions.size() == interfaces.size());

        List ip_if_ix = getColumn(IP_IF_IX);
        IntHashMap ips = new IntHashMap();
        IntHashMap netmasks = new IntHashMap();
        final String IF_IX_OID = SNMPClient.getOID(IP_IF_IX) + ".";
        final String NETMASK_OID = SNMPClient.getOID(IP_NETMASK) + ".";
        String ip, netmask;

        for (int i=0; i<ip_if_ix.size(); i++) {
            SNMPValue value = (SNMPValue)ip_if_ix.get(i);
            String oid = value.getOID();

            int ix = Integer.parseInt(value.toString()) - 1;
            if (oid.startsWith(IF_IX_OID)) {
                ip = oid.substring(IF_IX_OID.length());
                ips.put(ix, ip);
                try {
                    netmask =
                        this.session.getSingleValue(NETMASK_OID + ip).toString();
                    netmasks.put(ix, netmask);
                } catch (SNMPException e) {
                    log.debug("Failed to get netmask for " + ip);
                }
            }
        }

        for (int i=0; i<interfaces.size(); i++) {
            ConfigResponse config = new ConfigResponse();
            ConfigResponse cprops = new ConfigResponse();
            String name = interfaces.get(i).toString().trim();
            String mac = null;

            ServiceResource service = createServiceResource(SVC_NAME);

            config.setValue(PROP_IF, name);
            config.setValue(PROP_IF_IX, columnName);
            service.setProductConfig(config);
            //required to auto-enable metric
            service.setMeasurementConfig();

            for (int j=0; j<keys.length; j++) {
                String key = keys[j];
                List data = (List)cpropColumns.get(key);
                if (data == null) {
                    continue;
                }
                String val;
                Object obj = get(key, data, i);
                if (obj == null) {
                    continue;
                }
                if (key.equals(MAC_COLUMN)) {
                    mac = val = ((SNMPValue)obj).toPhysAddressString();
                }
                else {
                    val = obj.toString();
                }
                cprops.setValue(key, val);
            }

            ip = (String)ips.get(i);
            netmask = (String)netmasks.get(i);
            if (ip == null) {
                ip = "0.0.0.0";
            }
            if (netmask == null) {
                netmask = "0.0.0.0";
            }
            cprops.setValue(PROP_IP, ip);
            cprops.setValue(PROP_NETMASK, netmask);

            service.setCustomProperties(cprops);

            //might be more than 1 interface w/ the same name
            //so append the mac address to make it unique
            name = name + " " + SVC_NAME;
            if ((mac != null) && !mac.equals("0:0:0:0:0:0")) {
                name += " (" + mac + ")";
            }
            service.setServiceName(name);
            if (hasDescriptions) {
                Object obj = get(IF_DESCR, descriptions, i);
                if (obj != null) {
                    service.setDescription(obj.toString());
                }
            }
            services.add(service);
        }

        return services;
    }

    static SNMPSession getSession(ConfigResponse config)
        throws PluginException {

        try {
            return new SNMPClient().getSession(config);
        } catch(SNMPException e) {
            throw new PluginException("Error getting SNMP session: " +
                                      e.getMessage(), e);
        }
    }
    
    //XXX these could be in a base class of some sort
    protected void openSession(ConfigResponse config) throws PluginException {
        this.session = getSession(config);
    }
    
    protected void closeSession() {
        this.session = null;
    }
    
    protected List getColumn(String name) throws PluginException {
        try {
            return this.session.getColumn(name);
        } catch(SNMPException e) {
            throw new PluginException("Error getting SNMP column: " +
                                      name+ ":" + e, e);
        }
    }
    
    //use platform.name instead of the generic type name
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
            //wait till we have valid snmp config at the platform level
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

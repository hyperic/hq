/*
 *============================================================================
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of version 2.1 of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *============================================================================
 * Copyright (C) 2007 XenSource Inc.
 *============================================================================
 */
package com.xensource.xenapi;

import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.xmlrpc.XmlRpcException;

/**
 * A physical network interface (note separate VLANs are represented as several PIFs)
 *
 * @author XenSource Inc.
 */
public class PIF extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private PIF(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * PIF instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<PIF>> cache = 
        new HashMap<String,SoftReference<PIF>>();

    protected static synchronized PIF getInstFromRef(String ref) {
        if(PIF.cache.containsKey(ref)) {
            PIF instance = 
                PIF.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        PIF instance = new PIF(ref);
        PIF.cache.put(ref, new SoftReference<PIF>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a PIF
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "device", this.device);
            print.printf("%1$20s: %2$s\n", "network", this.network);
            print.printf("%1$20s: %2$s\n", "host", this.host);
            print.printf("%1$20s: %2$s\n", "MAC", this.MAC);
            print.printf("%1$20s: %2$s\n", "MTU", this.MTU);
            print.printf("%1$20s: %2$s\n", "VLAN", this.VLAN);
            print.printf("%1$20s: %2$s\n", "metrics", this.metrics);
            print.printf("%1$20s: %2$s\n", "physical", this.physical);
            print.printf("%1$20s: %2$s\n", "currentlyAttached", this.currentlyAttached);
            print.printf("%1$20s: %2$s\n", "ipConfigurationMode", this.ipConfigurationMode);
            print.printf("%1$20s: %2$s\n", "IP", this.IP);
            print.printf("%1$20s: %2$s\n", "netmask", this.netmask);
            print.printf("%1$20s: %2$s\n", "gateway", this.gateway);
            print.printf("%1$20s: %2$s\n", "DNS", this.DNS);
            print.printf("%1$20s: %2$s\n", "bondSlaveOf", this.bondSlaveOf);
            print.printf("%1$20s: %2$s\n", "bondMasterOf", this.bondMasterOf);
            print.printf("%1$20s: %2$s\n", "VLANMasterOf", this.VLANMasterOf);
            print.printf("%1$20s: %2$s\n", "VLANSlaveOf", this.VLANSlaveOf);
            print.printf("%1$20s: %2$s\n", "management", this.management);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a PIF.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("device", this.device);
            map.put("network", this.network);
            map.put("host", this.host);
            map.put("MAC", this.MAC);
            map.put("MTU", this.MTU);
            map.put("VLAN", this.VLAN);
            map.put("metrics", this.metrics);
            map.put("physical", this.physical);
            map.put("currently_attached", this.currentlyAttached);
            map.put("ip_configuration_mode", this.ipConfigurationMode);
            map.put("IP", this.IP);
            map.put("netmask", this.netmask);
            map.put("gateway", this.gateway);
            map.put("DNS", this.DNS);
            map.put("bond_slave_of", this.bondSlaveOf);
            map.put("bond_master_of", this.bondMasterOf);
            map.put("VLAN_master_of", this.VLANMasterOf);
            map.put("VLAN_slave_of", this.VLANSlaveOf);
            map.put("management", this.management);
            map.put("other_config", this.otherConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * machine-readable name of the interface (e.g. eth0)
         */
        public String device;
        /**
         * virtual network to which this pif is connected
         */
        public Network network;
        /**
         * physical machine to which this pif is connected
         */
        public Host host;
        /**
         * ethernet MAC address of physical interface
         */
        public String MAC;
        /**
         * MTU in octets
         */
        public Long MTU;
        /**
         * VLAN tag for all traffic passing through this interface
         */
        public Long VLAN;
        /**
         * metrics associated with this PIF
         */
        public PIFMetrics metrics;
        /**
         * true if this represents a physical network interface
         */
        public Boolean physical;
        /**
         * true if this interface is online
         */
        public Boolean currentlyAttached;
        /**
         * Sets if and how this interface gets an IP address
         */
        public Types.IpConfigurationMode ipConfigurationMode;
        /**
         * IP address
         */
        public String IP;
        /**
         * IP netmask
         */
        public String netmask;
        /**
         * IP gateway
         */
        public String gateway;
        /**
         * IP address of DNS servers to use
         */
        public String DNS;
        /**
         * indicates which bond this interface is part of
         */
        public Bond bondSlaveOf;
        /**
         * indicates this PIF represents the results of a bond
         */
        public Set<Bond> bondMasterOf;
        /**
         * indicates wich VLAN this interface receives untagged traffic from
         */
        public VLAN VLANMasterOf;
        /**
         * indicates which VLANs this interface transmits tagged traffic to
         */
        public Set<VLAN> VLANSlaveOf;
        /**
         * indicates whether the control software is listening for connections on this interface
         */
        public Boolean management;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given PIF.
     *
     * @return all fields from the object
     */
    public PIF.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIFRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the PIF instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static PIF getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given PIF.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the device field of the given PIF.
     *
     * @return value of the field
     */
    public String getDevice(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_device";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the network field of the given PIF.
     *
     * @return value of the field
     */
    public Network getNetwork(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_network";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toNetwork(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the host field of the given PIF.
     *
     * @return value of the field
     */
    public Host getHost(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_host";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHost(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the MAC field of the given PIF.
     *
     * @return value of the field
     */
    public String getMAC(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_MAC";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the MTU field of the given PIF.
     *
     * @return value of the field
     */
    public Long getMTU(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_MTU";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VLAN field of the given PIF.
     *
     * @return value of the field
     */
    public Long getVLAN(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the metrics field of the given PIF.
     *
     * @return value of the field
     */
    public PIFMetrics getMetrics(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIFMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the physical field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getPhysical(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_physical";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBoolean(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the currently_attached field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getCurrentlyAttached(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_currently_attached";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBoolean(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the ip_configuration_mode field of the given PIF.
     *
     * @return value of the field
     */
    public Types.IpConfigurationMode getIpConfigurationMode(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_ip_configuration_mode";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toIpConfigurationMode(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the IP field of the given PIF.
     *
     * @return value of the field
     */
    public String getIP(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_IP";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the netmask field of the given PIF.
     *
     * @return value of the field
     */
    public String getNetmask(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_netmask";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the gateway field of the given PIF.
     *
     * @return value of the field
     */
    public String getGateway(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_gateway";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the DNS field of the given PIF.
     *
     * @return value of the field
     */
    public String getDNS(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_DNS";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the bond_slave_of field of the given PIF.
     *
     * @return value of the field
     */
    public Bond getBondSlaveOf(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_bond_slave_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBond(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the bond_master_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<Bond> getBondMasterOf(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_bond_master_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfBond(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VLAN_master_of field of the given PIF.
     *
     * @return value of the field
     */
    public VLAN getVLANMasterOf(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_VLAN_master_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVLAN(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VLAN_slave_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<VLAN> getVLANSlaveOf(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_VLAN_slave_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVLAN(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the management field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getManagement(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_management";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBoolean(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given PIF.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the other_config field of the given PIF.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the other_config field of the given PIF.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given PIF.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a VLAN interface from an existing physical interface
     * @deprecated
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param host physical machine to which this PIF is connected
     * @param VLAN VLAN tag for the new interface
     * @return The reference of the created PIF object
     */
   @Deprecated public static PIF createVLAN(Connection c, String device, Network network, Host host, Long VLAN) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "PIF.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIF(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VLAN_TAG_INVALID")) {
                throw new Types.VlanTagInvalid((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy the PIF object (provided it is a VLAN interface)
     * @deprecated
     *
     */
   @Deprecated public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.PifIsPhysical {
        String method_call = "PIF.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("PIF_IS_PHYSICAL")) {
                throw new Types.PifIsPhysical((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Reconfigure the IP address settings for this interface
     *
     * @param mode whether to use dynamic/static/no-assignment
     * @param IP the new IP address
     * @param netmask the new netmask
     * @param gateway the new gateway
     * @param DNS the new DNS settings
     */
    public void reconfigureIp(Connection c, Types.IpConfigurationMode mode, String IP, String netmask, String gateway, String DNS) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.reconfigure_ip";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode), Marshalling.toXMLRPC(IP), Marshalling.toXMLRPC(netmask), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Scan for physical interfaces on a host and create PIF objects to represent them
     *
     * @param host The host on which to scan
     */
    public static void scan(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.scan";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a PIF object matching a particular network interface
     *
     * @param host The host on which the interface exists
     * @param MAC The MAC address of the interface
     * @param device The device name to use for the interface
     * @return The reference of the created PIF object
     */
    public static PIF introduce(Connection c, Host host, String MAC, String device) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(MAC), Marshalling.toXMLRPC(device)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy the PIF object matching a particular network interface
     *
     */
    public void forget(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a list of all the PIFs known to the system.
     *
     * @return references to all objects
     */
    public static Set<PIF> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of PIF references to PIF records for all PIFs known to the system.
     *
     * @return records of all objects
     */
    public static Map<PIF, PIF.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfPIFPIFRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
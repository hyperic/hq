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
import java.util.HashSet;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Pool-wide information
 *
 * @author XenSource Inc.
 */
public class Pool extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private Pool(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * Pool instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<Pool>> cache = 
        new HashMap<String,SoftReference<Pool>>();

    protected static synchronized Pool getInstFromRef(String ref) {
        if(Pool.cache.containsKey(ref)) {
            Pool instance = 
                Pool.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        Pool instance = new Pool(ref);
        Pool.cache.put(ref, new SoftReference<Pool>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a Pool
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "master", this.master);
            print.printf("%1$20s: %2$s\n", "defaultSR", this.defaultSR);
            print.printf("%1$20s: %2$s\n", "suspendImageSR", this.suspendImageSR);
            print.printf("%1$20s: %2$s\n", "crashDumpSR", this.crashDumpSR);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a pool.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("master", this.master == null ? com.xensource.xenapi.Host.getInstFromRef("OpaqueRef:NULL") : this.master);
            map.put("default_SR", this.defaultSR == null ? com.xensource.xenapi.SR.getInstFromRef("OpaqueRef:NULL") : this.defaultSR);
            map.put("suspend_image_SR", this.suspendImageSR == null ? com.xensource.xenapi.SR.getInstFromRef("OpaqueRef:NULL") : this.suspendImageSR);
            map.put("crash_dump_SR", this.crashDumpSR == null ? com.xensource.xenapi.SR.getInstFromRef("OpaqueRef:NULL") : this.crashDumpSR);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * Short name
         */
        public String nameLabel;
        /**
         * Description
         */
        public String nameDescription;
        /**
         * The host that is pool master
         */
        public Host master;
        /**
         * Default SR for VDIs
         */
        public SR defaultSR;
        /**
         * The SR in which VDIs for suspend images are created
         */
        public SR suspendImageSR;
        /**
         * The SR in which VDIs for crash dumps are created
         */
        public SR crashDumpSR;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given pool.
     *
     * @return all fields from the object
     */
    public Pool.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPoolRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the pool instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Pool getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPool(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given pool.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_uuid";
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
     * Get the name_label field of the given pool.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_name_label";
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
     * Get the name_description field of the given pool.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_name_description";
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
     * Get the master field of the given pool.
     *
     * @return value of the field
     */
    public Host getMaster(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_master";
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
     * Get the default_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getDefaultSR(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_default_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the suspend_image_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getSuspendImageSR(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_suspend_image_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the crash_dump_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getCrashDumpSR(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_crash_dump_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_other_config";
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
     * Set the name_label field of the given pool.
     *
     * @param nameLabel New value to set
     */
    public void setNameLabel(Connection c, String nameLabel) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nameLabel)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the name_description field of the given pool.
     *
     * @param nameDescription New value to set
     */
    public void setNameDescription(Connection c, String nameDescription) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nameDescription)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the default_SR field of the given pool.
     *
     * @param defaultSR New value to set
     */
    public void setDefaultSR(Connection c, SR defaultSR) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_default_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(defaultSR)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the suspend_image_SR field of the given pool.
     *
     * @param suspendImageSR New value to set
     */
    public void setSuspendImageSR(Connection c, SR suspendImageSR) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_suspend_image_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(suspendImageSR)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the crash_dump_SR field of the given pool.
     *
     * @param crashDumpSR New value to set
     */
    public void setCrashDumpSR(Connection c, SR crashDumpSR) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_crash_dump_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(crashDumpSR)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the other_config field of the given pool.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given pool.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given pool.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.remove_from_other_config";
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
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     * @return Task
     */
    public static Task joinAsync(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.JoiningHostCannotContainSharedSrs {
        String method_call = "Async.pool.join";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("JOINING_HOST_CANNOT_CONTAIN_SHARED_SRS")) {
                throw new Types.JoiningHostCannotContainSharedSrs();
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     */
    public static void join(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.JoiningHostCannotContainSharedSrs {
        String method_call = "pool.join";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("JOINING_HOST_CANNOT_CONTAIN_SHARED_SRS")) {
                throw new Types.JoiningHostCannotContainSharedSrs();
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     * @return Task
     */
    public static Task joinForceAsync(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.join_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     */
    public static void joinForce(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.join_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct a pool master to eject a host from the pool
     *
     * @param host The host to eject
     * @return Task
     */
    public static Task ejectAsync(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.eject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct a pool master to eject a host from the pool
     *
     * @param host The host to eject
     */
    public static void eject(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.eject";
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
     * Instruct host that's currently a slave to transition to being master
     *
     */
    public static void emergencyTransitionToMaster(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.emergency_transition_to_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct a slave already in a pool that the master has changed
     *
     * @param masterAddress The hostname of the master
     */
    public static void emergencyResetMaster(Connection c, String masterAddress) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.emergency_reset_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct a pool master, M, to try and contact its slaves and, if slaves are in emergency mode, reset their master address to M.
     *
     * @return Task
     */
    public static Task recoverSlavesAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.recover_slaves";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Instruct a pool master, M, to try and contact its slaves and, if slaves are in emergency mode, reset their master address to M.
     *
     * @return list of hosts whose master address were succesfully reset
     */
    public static Set<Host> recoverSlaves(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.recover_slaves";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfHost(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create PIFs, mapping a network to the same physical interface/VLAN on each host
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return Task
     */
    public static Task createVLANAsync(Connection c, String device, Network network, Long VLAN) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "Async.pool.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VLAN_TAG_INVALID")) {
                throw new Types.VlanTagInvalid((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create PIFs, mapping a network to the same physical interface/VLAN on each host
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return The references of the created PIF objects
     */
    public static Set<PIF> createVLAN(Connection c, String device, Network network, Long VLAN) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "pool.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPIF(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VLAN_TAG_INVALID")) {
                throw new Types.VlanTagInvalid((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Turn on High Availability mode
     *
     * @return Task
     */
    public static Task enableHaAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.enable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Turn on High Availability mode
     *
     */
    public static void enableHa(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.enable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Turn off High Availability mode
     *
     * @return Task
     */
    public static Task disableHaAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.disable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Turn off High Availability mode
     *
     */
    public static void disableHa(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.disable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Forcibly synchronise the database now
     *
     * @return Task
     */
    public static Task syncDatabaseAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.sync_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Forcibly synchronise the database now
     *
     */
    public static void syncDatabase(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.sync_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Perform an orderly handover of the role of master to the referenced host.
     *
     * @param host The host who should become the new master
     * @return Task
     */
    public static Task designateNewMasterAsync(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.pool.designate_new_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Perform an orderly handover of the role of master to the referenced host.
     *
     * @param host The host who should become the new master
     */
    public static void designateNewMaster(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.designate_new_master";
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
     * Return a list of all the pools known to the system.
     *
     * @return references to all objects
     */
    public static Set<Pool> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPool(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of pool references to pool records for all pools known to the system.
     *
     * @return records of all objects
     */
    public static Map<Pool, Pool.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfPoolPoolRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
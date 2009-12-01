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
 * 
 *
 * @author XenSource Inc.
 */
public class Bond extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private Bond(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * Bond instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<Bond>> cache = 
        new HashMap<String,SoftReference<Bond>>();

    protected static synchronized Bond getInstFromRef(String ref) {
        if(Bond.cache.containsKey(ref)) {
            Bond instance = 
                Bond.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        Bond instance = new Bond(ref);
        Bond.cache.put(ref, new SoftReference<Bond>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a Bond
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "master", this.master);
            print.printf("%1$20s: %2$s\n", "slaves", this.slaves);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a Bond.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("master", this.master == null ? com.xensource.xenapi.PIF.getInstFromRef("OpaqueRef:NULL") : this.master);
            map.put("slaves", this.slaves == null ? new HashSet<PIF>() : this.slaves);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * The bonded interface
         */
        public PIF master;
        /**
         * The interfaces which are part of this bond
         */
        public Set<PIF> slaves;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given Bond.
     *
     * @return all fields from the object
     */
    public Bond.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBondRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the Bond instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Bond getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBond(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given Bond.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_uuid";
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
     * Get the master field of the given Bond.
     *
     * @return value of the field
     */
    public PIF getMaster(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the slaves field of the given Bond.
     *
     * @return value of the field
     */
    public Set<PIF> getSlaves(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_slaves";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given Bond.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_other_config";
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
     * Set the other_config field of the given Bond.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given Bond.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given Bond.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.remove_from_other_config";
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
     * Create an interface bond
     *
     * @param network Network to add the bonded PIF to
     * @param members PIFs to add to this bond
     * @param MAC The MAC address to use on the bond itself. If this parameter is the empty string then the bond will inherit its MAC address from the first of the specified 'members'
     * @return Task
     */
    public static Task createAsync(Connection c, Network network, Set<PIF> members, String MAC) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.Bond.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(members), Marshalling.toXMLRPC(MAC)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create an interface bond
     *
     * @param network Network to add the bonded PIF to
     * @param members PIFs to add to this bond
     * @param MAC The MAC address to use on the bond itself. If this parameter is the empty string then the bond will inherit its MAC address from the first of the specified 'members'
     * @return The reference of the created Bond object
     */
    public static Bond create(Connection c, Network network, Set<PIF> members, String MAC) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(members), Marshalling.toXMLRPC(MAC)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBond(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy an interface bond
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.Bond.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy an interface bond
     *
     */
    public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.destroy";
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
     * Return a list of all the Bonds known to the system.
     *
     * @return references to all objects
     */
    public static Set<Bond> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfBond(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of Bond references to Bond records for all Bonds known to the system.
     *
     * @return records of all objects
     */
    public static Map<Bond, Bond.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Bond.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfBondBondRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
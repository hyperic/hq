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
 * A console
 *
 * @author XenSource Inc.
 */
public class Console extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private Console(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * Console instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<Console>> cache = 
        new HashMap<String,SoftReference<Console>>();

    protected static synchronized Console getInstFromRef(String ref) {
        if(Console.cache.containsKey(ref)) {
            Console instance = 
                Console.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        Console instance = new Console(ref);
        Console.cache.put(ref, new SoftReference<Console>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a Console
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "protocol", this.protocol);
            print.printf("%1$20s: %2$s\n", "location", this.location);
            print.printf("%1$20s: %2$s\n", "VM", this.VM);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a console.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("protocol", this.protocol == null ? Types.ConsoleProtocol.UNRECOGNIZED : this.protocol);
            map.put("location", this.location == null ? "" : this.location);
            map.put("VM", this.VM == null ? com.xensource.xenapi.VM.getInstFromRef("OpaqueRef:NULL") : this.VM);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * the protocol used by this console
         */
        public Types.ConsoleProtocol protocol;
        /**
         * URI for the console service
         */
        public String location;
        /**
         * VM to which this console is attached
         */
        public VM VM;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given console.
     *
     * @return all fields from the object
     */
    public Console.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toConsoleRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the console instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Console getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toConsole(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new console instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, Console.Record record) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.console.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new console instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static Console create(Connection c, Console.Record record) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toConsole(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy the specified console instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.console.destroy";
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
     * Destroy the specified console instance.
     *
     */
    public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.destroy";
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
     * Get the uuid field of the given console.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_uuid";
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
     * Get the protocol field of the given console.
     *
     * @return value of the field
     */
    public Types.ConsoleProtocol getProtocol(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_protocol";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toConsoleProtocol(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the location field of the given console.
     *
     * @return value of the field
     */
    public String getLocation(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_location";
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
     * Get the VM field of the given console.
     *
     * @return value of the field
     */
    public VM getVM(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_VM";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given console.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_other_config";
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
     * Set the other_config field of the given console.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given console.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given console.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.remove_from_other_config";
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
     * Return a list of all the consoles known to the system.
     *
     * @return references to all objects
     */
    public static Set<Console> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfConsole(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of console references to console records for all consoles known to the system.
     *
     * @return records of all objects
     */
    public static Map<Console, Console.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "console.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfConsoleConsoleRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
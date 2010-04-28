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
 * A virtual TPM device
 *
 * @author XenSource Inc.
 */
public class VTPM extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private VTPM(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * VTPM instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<VTPM>> cache = 
        new HashMap<String,SoftReference<VTPM>>();

    protected static synchronized VTPM getInstFromRef(String ref) {
        if(VTPM.cache.containsKey(ref)) {
            VTPM instance = 
                VTPM.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        VTPM instance = new VTPM(ref);
        VTPM.cache.put(ref, new SoftReference<VTPM>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a VTPM
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "VM", this.VM);
            print.printf("%1$20s: %2$s\n", "backend", this.backend);
            return writer.toString();
        }

        /**
         * Convert a VTPM.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("VM", this.VM == null ? com.xensource.xenapi.VM.getInstFromRef("OpaqueRef:NULL") : this.VM);
            map.put("backend", this.backend == null ? com.xensource.xenapi.VM.getInstFromRef("OpaqueRef:NULL") : this.backend);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * the virtual machine
         */
        public VM VM;
        /**
         * the domain where the backend is located
         */
        public VM backend;
    }

    /**
     * Get a record containing the current state of the given VTPM.
     *
     * @return all fields from the object
     */
    public VTPM.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVTPMRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the VTPM instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VTPM getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVTPM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new VTPM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VTPM.Record record) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.VTPM.create";
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
     * Create a new VTPM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VTPM create(Connection c, VTPM.Record record) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVTPM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy the specified VTPM instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.VTPM.destroy";
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
     * Destroy the specified VTPM instance.
     *
     */
    public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.destroy";
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
     * Get the uuid field of the given VTPM.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.get_uuid";
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
     * Get the VM field of the given VTPM.
     *
     * @return value of the field
     */
    public VM getVM(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.get_VM";
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
     * Get the backend field of the given VTPM.
     *
     * @return value of the field
     */
    public VM getBackend(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VTPM.get_backend";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
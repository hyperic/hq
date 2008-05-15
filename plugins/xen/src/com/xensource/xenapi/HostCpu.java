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
 * A physical CPU
 *
 * @author XenSource Inc.
 */
public class HostCpu extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private HostCpu(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * HostCpu instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<HostCpu>> cache = 
        new HashMap<String,SoftReference<HostCpu>>();

    protected static synchronized HostCpu getInstFromRef(String ref) {
        if(HostCpu.cache.containsKey(ref)) {
            HostCpu instance = 
                HostCpu.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        HostCpu instance = new HostCpu(ref);
        HostCpu.cache.put(ref, new SoftReference<HostCpu>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a HostCpu
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "host", this.host);
            print.printf("%1$20s: %2$s\n", "number", this.number);
            print.printf("%1$20s: %2$s\n", "vendor", this.vendor);
            print.printf("%1$20s: %2$s\n", "speed", this.speed);
            print.printf("%1$20s: %2$s\n", "modelname", this.modelname);
            print.printf("%1$20s: %2$s\n", "family", this.family);
            print.printf("%1$20s: %2$s\n", "model", this.model);
            print.printf("%1$20s: %2$s\n", "stepping", this.stepping);
            print.printf("%1$20s: %2$s\n", "flags", this.flags);
            print.printf("%1$20s: %2$s\n", "features", this.features);
            print.printf("%1$20s: %2$s\n", "utilisation", this.utilisation);
            return writer.toString();
        }

        /**
         * Convert a host_cpu.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("host", this.host);
            map.put("number", this.number);
            map.put("vendor", this.vendor);
            map.put("speed", this.speed);
            map.put("modelname", this.modelname);
            map.put("family", this.family);
            map.put("model", this.model);
            map.put("stepping", this.stepping);
            map.put("flags", this.flags);
            map.put("features", this.features);
            map.put("utilisation", this.utilisation);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * the host the CPU is in
         */
        public Host host;
        /**
         * the number of the physical CPU within the host
         */
        public Long number;
        /**
         * the vendor of the physical CPU
         */
        public String vendor;
        /**
         * the speed of the physical CPU
         */
        public Long speed;
        /**
         * the model name of the physical CPU
         */
        public String modelname;
        /**
         * the family (number) of the physical CPU
         */
        public Long family;
        /**
         * the model number of the physical CPU
         */
        public Long model;
        /**
         * the stepping of the physical CPU
         */
        public String stepping;
        /**
         * the flags of the physical CPU (a decoded version of the features field)
         */
        public String flags;
        /**
         * the physical CPU feature bitmap
         */
        public String features;
        /**
         * the current CPU utilisation
         */
        public Double utilisation;
    }

    /**
     * Get a record containing the current state of the given host_cpu.
     *
     * @return all fields from the object
     */
    public HostCpu.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHostCpuRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the host_cpu instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static HostCpu getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHostCpu(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_uuid";
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
     * Get the host field of the given host_cpu.
     *
     * @return value of the field
     */
    public Host getHost(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_host";
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
     * Get the number field of the given host_cpu.
     *
     * @return value of the field
     */
    public Long getNumber(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_number";
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
     * Get the vendor field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getVendor(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_vendor";
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
     * Get the speed field of the given host_cpu.
     *
     * @return value of the field
     */
    public Long getSpeed(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_speed";
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
     * Get the modelname field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getModelname(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_modelname";
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
     * Get the family field of the given host_cpu.
     *
     * @return value of the field
     */
    public Long getFamily(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_family";
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
     * Get the model field of the given host_cpu.
     *
     * @return value of the field
     */
    public Long getModel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_model";
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
     * Get the stepping field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getStepping(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_stepping";
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
     * Get the flags field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getFlags(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_flags";
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
     * Get the features field of the given host_cpu.
     *
     * @return value of the field
     */
    public String getFeatures(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_features";
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
     * Get the utilisation field of the given host_cpu.
     *
     * @return value of the field
     */
    public Double getUtilisation(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toDouble(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a list of all the host_cpus known to the system.
     *
     * @return references to all objects
     */
    public static Set<HostCpu> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfHostCpu(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of host_cpu references to host_cpu records for all host_cpus known to the system.
     *
     * @return records of all objects
     */
    public static Map<HostCpu, HostCpu.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_cpu.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfHostCpuHostCpuRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
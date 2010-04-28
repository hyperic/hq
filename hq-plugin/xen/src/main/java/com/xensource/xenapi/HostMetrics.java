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
 * The metrics associated with a host
 *
 * @author XenSource Inc.
 */
public class HostMetrics extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private HostMetrics(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * HostMetrics instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<HostMetrics>> cache = 
        new HashMap<String,SoftReference<HostMetrics>>();

    protected static synchronized HostMetrics getInstFromRef(String ref) {
        if(HostMetrics.cache.containsKey(ref)) {
            HostMetrics instance = 
                HostMetrics.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        HostMetrics instance = new HostMetrics(ref);
        HostMetrics.cache.put(ref, new SoftReference<HostMetrics>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a HostMetrics
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "memoryTotal", this.memoryTotal);
            print.printf("%1$20s: %2$s\n", "memoryFree", this.memoryFree);
            print.printf("%1$20s: %2$s\n", "live", this.live);
            print.printf("%1$20s: %2$s\n", "lastUpdated", this.lastUpdated);
            return writer.toString();
        }

        /**
         * Convert a host_metrics.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("memory_total", this.memoryTotal == null ? 0 : this.memoryTotal);
            map.put("memory_free", this.memoryFree == null ? 0 : this.memoryFree);
            map.put("live", this.live == null ? false : this.live);
            map.put("last_updated", this.lastUpdated == null ? new Date(0) : this.lastUpdated);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * Host's total memory (bytes)
         */
        public Long memoryTotal;
        /**
         * Host's free memory (bytes)
         */
        public Long memoryFree;
        /**
         * Pool master thinks this host is live
         */
        public Boolean live;
        /**
         * Time at which this information was last updated
         */
        public Date lastUpdated;
    }

    /**
     * Get a record containing the current state of the given host_metrics.
     *
     * @return all fields from the object
     */
    public HostMetrics.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHostMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the host_metrics instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static HostMetrics getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHostMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given host_metrics.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_uuid";
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
     * Get the memory/total field of the given host_metrics.
     *
     * @return value of the field
     */
    public Long getMemoryTotal(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_memory_total";
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
     * Get the memory/free field of the given host_metrics.
     *
     * @return value of the field
     */
    public Long getMemoryFree(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_memory_free";
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
     * Get the live field of the given host_metrics.
     *
     * @return value of the field
     */
    public Boolean getLive(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_live";
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
     * Get the last_updated field of the given host_metrics.
     *
     * @return value of the field
     */
    public Date getLastUpdated(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_last_updated";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toDate(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a list of all the host_metrics instances known to the system.
     *
     * @return references to all objects
     */
    public static Set<HostMetrics> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfHostMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of host_metrics references to host_metrics records for all host_metrics instances known to the system.
     *
     * @return records of all objects
     */
    public static Map<HostMetrics, HostMetrics.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "host_metrics.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfHostMetricsHostMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
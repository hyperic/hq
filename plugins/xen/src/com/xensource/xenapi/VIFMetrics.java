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
 * The metrics associated with a virtual network device
 *
 * @author XenSource Inc.
 */
public class VIFMetrics extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private VIFMetrics(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * VIFMetrics instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<VIFMetrics>> cache = 
        new HashMap<String,SoftReference<VIFMetrics>>();

    protected static synchronized VIFMetrics getInstFromRef(String ref) {
        if(VIFMetrics.cache.containsKey(ref)) {
            VIFMetrics instance = 
                VIFMetrics.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        VIFMetrics instance = new VIFMetrics(ref);
        VIFMetrics.cache.put(ref, new SoftReference<VIFMetrics>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a VIFMetrics
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "ioReadKbs", this.ioReadKbs);
            print.printf("%1$20s: %2$s\n", "ioWriteKbs", this.ioWriteKbs);
            print.printf("%1$20s: %2$s\n", "lastUpdated", this.lastUpdated);
            return writer.toString();
        }

        /**
         * Convert a VIF_metrics.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("io_read_kbs", this.ioReadKbs);
            map.put("io_write_kbs", this.ioWriteKbs);
            map.put("last_updated", this.lastUpdated);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * Read bandwidth (KiB/s)
         */
        public Double ioReadKbs;
        /**
         * Write bandwidth (KiB/s)
         */
        public Double ioWriteKbs;
        /**
         * Time at which this information was last updated
         */
        public Date lastUpdated;
    }

    /**
     * Get a record containing the current state of the given VIF_metrics.
     *
     * @return all fields from the object
     */
    public VIFMetrics.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVIFMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the VIF_metrics instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VIFMetrics getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVIFMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given VIF_metrics.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_uuid";
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
     * Get the io/read_kbs field of the given VIF_metrics.
     *
     * @return value of the field
     */
    public Double getIoReadKbs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_io_read_kbs";
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
     * Get the io/write_kbs field of the given VIF_metrics.
     *
     * @return value of the field
     */
    public Double getIoWriteKbs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_io_write_kbs";
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
     * Get the last_updated field of the given VIF_metrics.
     *
     * @return value of the field
     */
    public Date getLastUpdated(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_last_updated";
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
     * Return a list of all the VIF_metrics instances known to the system.
     *
     * @return references to all objects
     */
    public static Set<VIFMetrics> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVIFMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of VIF_metrics references to VIF_metrics records for all VIF_metrics instances known to the system.
     *
     * @return records of all objects
     */
    public static Map<VIFMetrics, VIFMetrics.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VIF_metrics.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfVIFMetricsVIFMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
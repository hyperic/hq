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
 * The metrics associated with a VM
 *
 * @author XenSource Inc.
 */
public class VMMetrics extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private VMMetrics(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * VMMetrics instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<VMMetrics>> cache = 
        new HashMap<String,SoftReference<VMMetrics>>();

    protected static synchronized VMMetrics getInstFromRef(String ref) {
        if(VMMetrics.cache.containsKey(ref)) {
            VMMetrics instance = 
                VMMetrics.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        VMMetrics instance = new VMMetrics(ref);
        VMMetrics.cache.put(ref, new SoftReference<VMMetrics>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a VMMetrics
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "memoryActual", this.memoryActual);
            print.printf("%1$20s: %2$s\n", "VCPUsNumber", this.VCPUsNumber);
            print.printf("%1$20s: %2$s\n", "VCPUsUtilisation", this.VCPUsUtilisation);
            print.printf("%1$20s: %2$s\n", "VCPUsCPU", this.VCPUsCPU);
            print.printf("%1$20s: %2$s\n", "VCPUsParams", this.VCPUsParams);
            print.printf("%1$20s: %2$s\n", "VCPUsFlags", this.VCPUsFlags);
            print.printf("%1$20s: %2$s\n", "state", this.state);
            print.printf("%1$20s: %2$s\n", "startTime", this.startTime);
            print.printf("%1$20s: %2$s\n", "installTime", this.installTime);
            print.printf("%1$20s: %2$s\n", "lastUpdated", this.lastUpdated);
            return writer.toString();
        }

        /**
         * Convert a VM_metrics.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("memory_actual", this.memoryActual);
            map.put("VCPUs_number", this.VCPUsNumber);
            map.put("VCPUs_utilisation", this.VCPUsUtilisation);
            map.put("VCPUs_CPU", this.VCPUsCPU);
            map.put("VCPUs_params", this.VCPUsParams);
            map.put("VCPUs_flags", this.VCPUsFlags);
            map.put("state", this.state);
            map.put("start_time", this.startTime);
            map.put("install_time", this.installTime);
            map.put("last_updated", this.lastUpdated);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * Guest's actual memory (bytes)
         */
        public Long memoryActual;
        /**
         * Current number of VCPUs
         */
        public Long VCPUsNumber;
        /**
         * Utilisation for all of guest's current VCPUs
         */
        public Map<Long, Double> VCPUsUtilisation;
        /**
         * VCPU to PCPU map
         */
        public Map<Long, Long> VCPUsCPU;
        /**
         * The live equivalent to VM.VCPUs_params
         */
        public Map<String, String> VCPUsParams;
        /**
         * CPU flags (blocked,online,running)
         */
        public Map<Long, Set<String>> VCPUsFlags;
        /**
         * The state of the guest, eg blocked, dying etc
         */
        public Set<String> state;
        /**
         * Time at which this VM was last booted
         */
        public Date startTime;
        /**
         * Time at which the VM was installed
         */
        public Date installTime;
        /**
         * Time at which this information was last updated
         */
        public Date lastUpdated;
    }

    /**
     * Get a record containing the current state of the given VM_metrics.
     *
     * @return all fields from the object
     */
    public VMMetrics.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the VM_metrics instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VMMetrics getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given VM_metrics.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_uuid";
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
     * Get the memory/actual field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Long getMemoryActual(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_memory_actual";
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
     * Get the VCPUs/number field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Long getVCPUsNumber(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_VCPUs_number";
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
     * Get the VCPUs/utilisation field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Map<Long, Double> getVCPUsUtilisation(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_VCPUs_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfLongDouble(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VCPUs/CPU field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Map<Long, Long> getVCPUsCPU(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_VCPUs_CPU";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfLongLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VCPUs/params field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getVCPUsParams(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_VCPUs_params";
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
     * Get the VCPUs/flags field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Map<Long, Set<String>> getVCPUsFlags(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_VCPUs_flags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfLongSetOfString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the state field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Set<String> getState(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_state";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the start_time field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Date getStartTime(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_start_time";
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
     * Get the install_time field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Date getInstallTime(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_install_time";
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
     * Get the last_updated field of the given VM_metrics.
     *
     * @return value of the field
     */
    public Date getLastUpdated(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_last_updated";
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
     * Return a list of all the VM_metrics instances known to the system.
     *
     * @return references to all objects
     */
    public static Set<VMMetrics> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVMMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of VM_metrics references to VM_metrics records for all VM_metrics instances known to the system.
     *
     * @return records of all objects
     */
    public static Map<VMMetrics, VMMetrics.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_metrics.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfVMMetricsVMMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
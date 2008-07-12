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
 * The metrics reported by the guest (as opposed to inferred from outside)
 *
 * @author XenSource Inc.
 */
public class VMGuestMetrics extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private VMGuestMetrics(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * VMGuestMetrics instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<VMGuestMetrics>> cache = 
        new HashMap<String,SoftReference<VMGuestMetrics>>();

    protected static synchronized VMGuestMetrics getInstFromRef(String ref) {
        if(VMGuestMetrics.cache.containsKey(ref)) {
            VMGuestMetrics instance = 
                VMGuestMetrics.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        VMGuestMetrics instance = new VMGuestMetrics(ref);
        VMGuestMetrics.cache.put(ref, new SoftReference<VMGuestMetrics>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a VMGuestMetrics
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "osVersion", this.osVersion);
            print.printf("%1$20s: %2$s\n", "PVDriversVersion", this.PVDriversVersion);
            print.printf("%1$20s: %2$s\n", "PVDriversUpToDate", this.PVDriversUpToDate);
            print.printf("%1$20s: %2$s\n", "memory", this.memory);
            print.printf("%1$20s: %2$s\n", "disks", this.disks);
            print.printf("%1$20s: %2$s\n", "networks", this.networks);
            print.printf("%1$20s: %2$s\n", "other", this.other);
            print.printf("%1$20s: %2$s\n", "lastUpdated", this.lastUpdated);
            return writer.toString();
        }

        /**
         * Convert a VM_guest_metrics.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("os_version", this.osVersion == null ? new HashMap<String, String>() : this.osVersion);
            map.put("PV_drivers_version", this.PVDriversVersion == null ? new HashMap<String, String>() : this.PVDriversVersion);
            map.put("PV_drivers_up_to_date", this.PVDriversUpToDate == null ? false : this.PVDriversUpToDate);
            map.put("memory", this.memory == null ? new HashMap<String, String>() : this.memory);
            map.put("disks", this.disks == null ? new HashMap<String, String>() : this.disks);
            map.put("networks", this.networks == null ? new HashMap<String, String>() : this.networks);
            map.put("other", this.other == null ? new HashMap<String, String>() : this.other);
            map.put("last_updated", this.lastUpdated == null ? new Date(0) : this.lastUpdated);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * version of the OS
         */
        public Map<String, String> osVersion;
        /**
         * version of the PV drivers
         */
        public Map<String, String> PVDriversVersion;
        /**
         * true if the PV drivers appear to be up to date
         */
        public Boolean PVDriversUpToDate;
        /**
         * free/used/total memory
         */
        public Map<String, String> memory;
        /**
         * disk configuration/free space
         */
        public Map<String, String> disks;
        /**
         * network configuration
         */
        public Map<String, String> networks;
        /**
         * anything else
         */
        public Map<String, String> other;
        /**
         * Time at which this information was last updated
         */
        public Date lastUpdated;
    }

    /**
     * Get a record containing the current state of the given VM_guest_metrics.
     *
     * @return all fields from the object
     */
    public VMGuestMetrics.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMGuestMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the VM_guest_metrics instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VMGuestMetrics getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMGuestMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_uuid";
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
     * Get the os_version field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getOsVersion(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_os_version";
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
     * Get the PV_drivers_version field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getPVDriversVersion(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_PV_drivers_version";
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
     * Get the PV_drivers_up_to_date field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Boolean getPVDriversUpToDate(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_PV_drivers_up_to_date";
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
     * Get the memory field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getMemory(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_memory";
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
     * Get the disks field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getDisks(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_disks";
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
     * Get the networks field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getNetworks(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_networks";
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
     * Get the other field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Map<String, String> getOther(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_other";
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
     * Get the last_updated field of the given VM_guest_metrics.
     *
     * @return value of the field
     */
    public Date getLastUpdated(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_last_updated";
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
     * Return a list of all the VM_guest_metrics instances known to the system.
     *
     * @return references to all objects
     */
    public static Set<VMGuestMetrics> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVMGuestMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of VM_guest_metrics references to VM_guest_metrics records for all VM_guest_metrics instances known to the system.
     *
     * @return records of all objects
     */
    public static Map<VMGuestMetrics, VMGuestMetrics.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM_guest_metrics.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfVMGuestMetricsVMGuestMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
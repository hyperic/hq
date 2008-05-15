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
 * The metrics associated with a physical network interface
 *
 * @author XenSource Inc.
 */
public class PIFMetrics extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private PIFMetrics(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * PIFMetrics instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<PIFMetrics>> cache = 
        new HashMap<String,SoftReference<PIFMetrics>>();

    protected static synchronized PIFMetrics getInstFromRef(String ref) {
        if(PIFMetrics.cache.containsKey(ref)) {
            PIFMetrics instance = 
                PIFMetrics.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        PIFMetrics instance = new PIFMetrics(ref);
        PIFMetrics.cache.put(ref, new SoftReference<PIFMetrics>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a PIFMetrics
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "ioReadKbs", this.ioReadKbs);
            print.printf("%1$20s: %2$s\n", "ioWriteKbs", this.ioWriteKbs);
            print.printf("%1$20s: %2$s\n", "carrier", this.carrier);
            print.printf("%1$20s: %2$s\n", "vendorId", this.vendorId);
            print.printf("%1$20s: %2$s\n", "vendorName", this.vendorName);
            print.printf("%1$20s: %2$s\n", "deviceId", this.deviceId);
            print.printf("%1$20s: %2$s\n", "deviceName", this.deviceName);
            print.printf("%1$20s: %2$s\n", "speed", this.speed);
            print.printf("%1$20s: %2$s\n", "duplex", this.duplex);
            print.printf("%1$20s: %2$s\n", "pciBusPath", this.pciBusPath);
            print.printf("%1$20s: %2$s\n", "lastUpdated", this.lastUpdated);
            return writer.toString();
        }

        /**
         * Convert a PIF_metrics.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("io_read_kbs", this.ioReadKbs);
            map.put("io_write_kbs", this.ioWriteKbs);
            map.put("carrier", this.carrier);
            map.put("vendor_id", this.vendorId);
            map.put("vendor_name", this.vendorName);
            map.put("device_id", this.deviceId);
            map.put("device_name", this.deviceName);
            map.put("speed", this.speed);
            map.put("duplex", this.duplex);
            map.put("pci_bus_path", this.pciBusPath);
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
         * Report if the PIF got a carrier or not
         */
        public Boolean carrier;
        /**
         * Report vendor ID
         */
        public String vendorId;
        /**
         * Report vendor name
         */
        public String vendorName;
        /**
         * Report device ID
         */
        public String deviceId;
        /**
         * Report device name
         */
        public String deviceName;
        /**
         * Speed of the link (if available)
         */
        public Long speed;
        /**
         * Full duplex capability of the link (if available)
         */
        public Boolean duplex;
        /**
         * PCI bus path of the pif (if available)
         */
        public String pciBusPath;
        /**
         * Time at which this information was last updated
         */
        public Date lastUpdated;
    }

    /**
     * Get a record containing the current state of the given PIF_metrics.
     *
     * @return all fields from the object
     */
    public PIFMetrics.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIFMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the PIF_metrics instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static PIFMetrics getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPIFMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_uuid";
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
     * Get the io/read_kbs field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Double getIoReadKbs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_io_read_kbs";
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
     * Get the io/write_kbs field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Double getIoWriteKbs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_io_write_kbs";
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
     * Get the carrier field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Boolean getCarrier(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_carrier";
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
     * Get the vendor_id field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getVendorId(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_vendor_id";
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
     * Get the vendor_name field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getVendorName(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_vendor_name";
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
     * Get the device_id field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getDeviceId(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_device_id";
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
     * Get the device_name field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getDeviceName(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_device_name";
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
     * Get the speed field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Long getSpeed(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_speed";
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
     * Get the duplex field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Boolean getDuplex(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_duplex";
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
     * Get the pci_bus_path field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public String getPciBusPath(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_pci_bus_path";
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
     * Get the last_updated field of the given PIF_metrics.
     *
     * @return value of the field
     */
    public Date getLastUpdated(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_last_updated";
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
     * Return a list of all the PIF_metrics instances known to the system.
     *
     * @return references to all objects
     */
    public static Set<PIFMetrics> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPIFMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of PIF_metrics references to PIF_metrics records for all PIF_metrics instances known to the system.
     *
     * @return records of all objects
     */
    public static Map<PIFMetrics, PIFMetrics.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "PIF_metrics.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfPIFMetricsPIFMetricsRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
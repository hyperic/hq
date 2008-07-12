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
 * A storage repository
 *
 * @author XenSource Inc.
 */
public class SR extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private SR(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * SR instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<SR>> cache = 
        new HashMap<String,SoftReference<SR>>();

    protected static synchronized SR getInstFromRef(String ref) {
        if(SR.cache.containsKey(ref)) {
            SR instance = 
                SR.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        SR instance = new SR(ref);
        SR.cache.put(ref, new SoftReference<SR>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a SR
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "allowedOperations", this.allowedOperations);
            print.printf("%1$20s: %2$s\n", "currentOperations", this.currentOperations);
            print.printf("%1$20s: %2$s\n", "VDIs", this.VDIs);
            print.printf("%1$20s: %2$s\n", "PBDs", this.PBDs);
            print.printf("%1$20s: %2$s\n", "virtualAllocation", this.virtualAllocation);
            print.printf("%1$20s: %2$s\n", "physicalUtilisation", this.physicalUtilisation);
            print.printf("%1$20s: %2$s\n", "physicalSize", this.physicalSize);
            print.printf("%1$20s: %2$s\n", "type", this.type);
            print.printf("%1$20s: %2$s\n", "contentType", this.contentType);
            print.printf("%1$20s: %2$s\n", "shared", this.shared);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "smConfig", this.smConfig);
            return writer.toString();
        }

        /**
         * Convert a SR.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("allowed_operations", this.allowedOperations == null ? new HashSet<Types.StorageOperations>() : this.allowedOperations);
            map.put("current_operations", this.currentOperations == null ? new HashMap<String, Types.StorageOperations>() : this.currentOperations);
            map.put("VDIs", this.VDIs == null ? new HashSet<VDI>() : this.VDIs);
            map.put("PBDs", this.PBDs == null ? new HashSet<PBD>() : this.PBDs);
            map.put("virtual_allocation", this.virtualAllocation == null ? 0 : this.virtualAllocation);
            map.put("physical_utilisation", this.physicalUtilisation == null ? 0 : this.physicalUtilisation);
            map.put("physical_size", this.physicalSize == null ? 0 : this.physicalSize);
            map.put("type", this.type == null ? "" : this.type);
            map.put("content_type", this.contentType == null ? "" : this.contentType);
            map.put("shared", this.shared == null ? false : this.shared);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("sm_config", this.smConfig == null ? new HashMap<String, String>() : this.smConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * a human-readable name
         */
        public String nameLabel;
        /**
         * a notes field containg human-readable description
         */
        public String nameDescription;
        /**
         * list of the operations allowed in this state. This list is advisory only and the server state may have changed by the time this field is read by a client.
         */
        public Set<Types.StorageOperations> allowedOperations;
        /**
         * links each of the running tasks using this object (by reference) to a current_operation enum which describes the nature of the task.
         */
        public Map<String, Types.StorageOperations> currentOperations;
        /**
         * all virtual disks known to this storage repository
         */
        public Set<VDI> VDIs;
        /**
         * describes how particular hosts can see this storage repository
         */
        public Set<PBD> PBDs;
        /**
         * sum of virtual_sizes of all VDIs in this storage repository (in bytes)
         */
        public Long virtualAllocation;
        /**
         * physical space currently utilised on this storage repository (in bytes). Note that for sparse disk formats, physical_utilisation may be less than virtual_allocation
         */
        public Long physicalUtilisation;
        /**
         * total physical size of the repository (in bytes)
         */
        public Long physicalSize;
        /**
         * type of the storage repository
         */
        public String type;
        /**
         * the type of the SR's content, if required (e.g. ISOs)
         */
        public String contentType;
        /**
         * true if this SR is (capable of being) shared between multiple hosts
         */
        public Boolean shared;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * SM dependent data
         */
        public Map<String, String> smConfig;
    }

    /**
     * Get a record containing the current state of the given SR.
     *
     * @return all fields from the object
     */
    public SR.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSRRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the SR instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static SR getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get all the SR instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<SR> getByNameLabel(Connection c, String label) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given SR.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_uuid";
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
     * Get the name/label field of the given SR.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_name_label";
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
     * Get the name/description field of the given SR.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_name_description";
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
     * Get the allowed_operations field of the given SR.
     *
     * @return value of the field
     */
    public Set<Types.StorageOperations> getAllowedOperations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfStorageOperations(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the current_operations field of the given SR.
     *
     * @return value of the field
     */
    public Map<String, Types.StorageOperations> getCurrentOperations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_current_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringStorageOperations(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VDIs field of the given SR.
     *
     * @return value of the field
     */
    public Set<VDI> getVDIs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_VDIs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVDI(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PBDs field of the given SR.
     *
     * @return value of the field
     */
    public Set<PBD> getPBDs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_PBDs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPBD(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the virtual_allocation field of the given SR.
     *
     * @return value of the field
     */
    public Long getVirtualAllocation(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_virtual_allocation";
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
     * Get the physical_utilisation field of the given SR.
     *
     * @return value of the field
     */
    public Long getPhysicalUtilisation(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_physical_utilisation";
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
     * Get the physical_size field of the given SR.
     *
     * @return value of the field
     */
    public Long getPhysicalSize(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_physical_size";
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
     * Get the type field of the given SR.
     *
     * @return value of the field
     */
    public String getType(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_type";
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
     * Get the content_type field of the given SR.
     *
     * @return value of the field
     */
    public String getContentType(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_content_type";
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
     * Get the shared field of the given SR.
     *
     * @return value of the field
     */
    public Boolean getShared(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_shared";
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
     * Get the other_config field of the given SR.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_other_config";
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
     * Get the sm_config field of the given SR.
     *
     * @return value of the field
     */
    public Map<String, String> getSmConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_sm_config";
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
     * Set the name/label field of the given SR.
     *
     * @param label New value to set
     */
    public void setNameLabel(Connection c, String label) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the name/description field of the given SR.
     *
     * @param description New value to set
     */
    public void setNameDescription(Connection c, String description) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(description)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the other_config field of the given SR.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given SR.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given SR.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.remove_from_other_config";
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
     * Set the sm_config field of the given SR.
     *
     * @param smConfig New value to set
     */
    public void setSmConfig(Connection c, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_sm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the sm_config field of the given SR.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToSmConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.add_to_sm_config";
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
     * Remove the given key and its corresponding value from the sm_config field of the given SR.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromSmConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.remove_from_sm_config";
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
     * Create a new Storage Repository and introduce it into the managed system, creating both SR record and PBD record to attach it to current host (with specified device_config parameters)
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param physicalSize The physical size of the new storage repository
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param shared True if the SR (is capable of) being shared by multiple hosts
     * @param smConfig Storage backend specific configuration options
     * @return Task
     */
    public static Task createAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException,
       Types.SrUnknownDriver {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioCreateAsync(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, shared);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiCreateAsync(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, shared, smConfig);
        }
    }



    private static Task rioCreateAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrUnknownDriver {
        String method_call = "Async.SR.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_UNKNOWN_DRIVER")) {
                throw new Types.SrUnknownDriver((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    private static Task miamiCreateAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrUnknownDriver {
        String method_call = "Async.SR.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_UNKNOWN_DRIVER")) {
                throw new Types.SrUnknownDriver((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new Storage Repository and introduce it into the managed system, creating both SR record and PBD record to attach it to current host (with specified device_config parameters)
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param physicalSize The physical size of the new storage repository
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param shared True if the SR (is capable of) being shared by multiple hosts
     * @param smConfig Storage backend specific configuration options
     * @return The reference of the newly created Storage Repository.
     */
    public static SR create(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException,
       Types.SrUnknownDriver {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioCreate(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, shared);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiCreate(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, shared, smConfig);
        }
    }



    private static SR rioCreate(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrUnknownDriver {
        String method_call = "SR.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_UNKNOWN_DRIVER")) {
                throw new Types.SrUnknownDriver((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    private static SR miamiCreate(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrUnknownDriver {
        String method_call = "SR.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_UNKNOWN_DRIVER")) {
                throw new Types.SrUnknownDriver((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Introduce a new Storage Repository into the managed system
     *
     * @param uuid The uuid assigned to the introduced SR
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param shared True if the SR (is capable of) being shared by multiple hosts
     * @param smConfig Storage backend specific configuration options
     * @return Task
     */
    public static Task introduceAsync(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioIntroduceAsync(c, uuid, nameLabel, nameDescription, type, contentType, shared);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiIntroduceAsync(c, uuid, nameLabel, nameDescription, type, contentType, shared, smConfig);
        }
    }



    private static Task rioIntroduceAsync(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    private static Task miamiIntroduceAsync(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Introduce a new Storage Repository into the managed system
     *
     * @param uuid The uuid assigned to the introduced SR
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param shared True if the SR (is capable of) being shared by multiple hosts
     * @param smConfig Storage backend specific configuration options
     * @return The reference of the newly introduced Storage Repository.
     */
    public static SR introduce(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioIntroduce(c, uuid, nameLabel, nameDescription, type, contentType, shared);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiIntroduce(c, uuid, nameLabel, nameDescription, type, contentType, shared, smConfig);
        }
    }



    private static SR rioIntroduce(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    private static SR miamiIntroduce(Connection c, String uuid, String nameLabel, String nameDescription, String type, String contentType, Boolean shared, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(shared), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new Storage Repository on disk
     * @deprecated
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param physicalSize The physical size of the new storage repository
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param smConfig Storage backend specific configuration options
     * @return Task
     */
   @Deprecated public static Task makeAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioMakeAsync(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiMakeAsync(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, smConfig);
        }
    }



   @Deprecated private static Task rioMakeAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.make";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

   @Deprecated private static Task miamiMakeAsync(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.make";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new Storage Repository on disk
     * @deprecated
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param physicalSize The physical size of the new storage repository
     * @param nameLabel The name of the new storage repository
     * @param nameDescription The description of the new storage repository
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param contentType The type of the new SRs content, if required (e.g. ISOs)
     * @param smConfig Storage backend specific configuration options
     * @return The uuid of the newly created Storage Repository.
     */
   @Deprecated public static String make(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       Types.VersionException,
       XmlRpcException {

        if(c.rioConnection){
            if (smConfig.isEmpty()){
                return rioMake(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType);
            } else {
                throw new Types.VersionException("smConfig parameter must be empty map for Rio (legacy XenServer) host");
            }
        } else {
            return miamiMake(c, host, deviceConfig, physicalSize, nameLabel, nameDescription, type, contentType, smConfig);
        }
    }



   @Deprecated private static String rioMake(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.make";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

   @Deprecated private static String miamiMake(Connection c, Host host, Map<String, String> deviceConfig, Long physicalSize, String nameLabel, String nameDescription, String type, String contentType, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.make";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(physicalSize), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(contentType), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy specified SR, removing SR-record from database and remove SR from disk. (In order to affect this operation the appropriate device_config is read from the specified SR's PBD on current host)
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrHasPbd {
        String method_call = "Async.SR.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_HAS_PBD")) {
                throw new Types.SrHasPbd((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy specified SR, removing SR-record from database and remove SR from disk. (In order to affect this operation the appropriate device_config is read from the specified SR's PBD on current host)
     *
     */
    public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrHasPbd {
        String method_call = "SR.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_HAS_PBD")) {
                throw new Types.SrHasPbd((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Removing specified SR-record from database, without attempting to remove SR from disk
     *
     * @return Task
     */
    public Task forgetAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrHasPbd {
        String method_call = "Async.SR.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_HAS_PBD")) {
                throw new Types.SrHasPbd((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Removing specified SR-record from database, without attempting to remove SR from disk
     *
     */
    public void forget(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SrHasPbd {
        String method_call = "SR.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SR_HAS_PBD")) {
                throw new Types.SrHasPbd((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a set of all the SR types supported by the system
     *
     * @return the supported SR types
     */
    public static Set<String> getSupportedTypes(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_supported_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Refreshes the list of VDIs associated with an SR
     *
     * @return Task
     */
    public Task scanAsync(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.scan";
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
     * Refreshes the list of VDIs associated with an SR
     *
     */
    public void scan(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.scan";
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
     * Perform a backend-specific scan, using the given device_config.  If the device_config is complete, then this will return a list of the SRs present of this type on the device, if any.  If the device_config is partial, then a backend-specific scan will be performed, returning results that will guide the user in improving the device_config.
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param smConfig Storage backend specific configuration options
     * @return Task
     */
    public static Task probeAsync(Connection c, Host host, Map<String, String> deviceConfig, String type, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.probe";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Perform a backend-specific scan, using the given device_config.  If the device_config is complete, then this will return a list of the SRs present of this type on the device, if any.  If the device_config is partial, then a backend-specific scan will be performed, returning results that will guide the user in improving the device_config.
     *
     * @param host The host to create/make the SR on
     * @param deviceConfig The device config string that will be passed to backend SR driver
     * @param type The type of the SR; used to specify the SR backend driver to use
     * @param smConfig Storage backend specific configuration options
     * @return An XML fragment containing the scan results.  These are specific to the scan being performed, and the backend.
     */
    public static String probe(Connection c, Host host, Map<String, String> deviceConfig, String type, Map<String, String> smConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.probe";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(deviceConfig), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the shared flag on the SR
     *
     * @param value True if the SR is shared
     * @return Task
     */
    public Task setSharedAsync(Connection c, Boolean value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.set_shared";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the shared flag on the SR
     *
     * @param value True if the SR is shared
     */
    public void setShared(Connection c, Boolean value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_shared";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's physical_size field
     *
     * @param value The new value of the SR's physical_size
     * @return Task
     */
    public Task setPhysicalSizeAsync(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.set_physical_size";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's physical_size field
     *
     * @param value The new value of the SR's physical_size
     */
    public void setPhysicalSize(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_physical_size";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's virtual_allocation field
     *
     * @param value The new value of the SR's virtual_allocation
     * @return Task
     */
    public Task setVirtualAllocationAsync(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.set_virtual_allocation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's virtual_allocation field
     *
     * @param value The new value of the SR's virtual_allocation
     */
    public void setVirtualAllocation(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_virtual_allocation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's physical_utilisation field
     *
     * @param value The new value of the SR's physical utilisation
     * @return Task
     */
    public Task setPhysicalUtilisationAsync(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.SR.set_physical_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Sets the SR's physical_utilisation field
     *
     * @param value The new value of the SR's physical utilisation
     */
    public void setPhysicalUtilisation(Connection c, Long value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.set_physical_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a list of all the SRs known to the system.
     *
     * @return references to all objects
     */
    public static Set<SR> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfSR(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of SR references to SR records for all SRs known to the system.
     *
     * @return records of all objects
     */
    public static Map<SR, SR.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "SR.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfSRSRRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
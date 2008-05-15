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
 * Pool-wide patches
 *
 * @author XenSource Inc.
 */
public class PoolPatch extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private PoolPatch(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * PoolPatch instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<PoolPatch>> cache = 
        new HashMap<String,SoftReference<PoolPatch>>();

    protected static synchronized PoolPatch getInstFromRef(String ref) {
        if(PoolPatch.cache.containsKey(ref)) {
            PoolPatch instance = 
                PoolPatch.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        PoolPatch instance = new PoolPatch(ref);
        PoolPatch.cache.put(ref, new SoftReference<PoolPatch>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a PoolPatch
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "version", this.version);
            print.printf("%1$20s: %2$s\n", "size", this.size);
            print.printf("%1$20s: %2$s\n", "poolApplied", this.poolApplied);
            print.printf("%1$20s: %2$s\n", "hostPatches", this.hostPatches);
            print.printf("%1$20s: %2$s\n", "afterApplyGuidance", this.afterApplyGuidance);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a pool_patch.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("name_label", this.nameLabel);
            map.put("name_description", this.nameDescription);
            map.put("version", this.version);
            map.put("size", this.size);
            map.put("pool_applied", this.poolApplied);
            map.put("host_patches", this.hostPatches);
            map.put("after_apply_guidance", this.afterApplyGuidance);
            map.put("other_config", this.otherConfig);
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
         * Patch version number
         */
        public String version;
        /**
         * Size of the patch
         */
        public Long size;
        /**
         * This patch should be applied across the entire pool
         */
        public Boolean poolApplied;
        /**
         * This hosts this patch is applied to.
         */
        public Set<HostPatch> hostPatches;
        /**
         * What the client should do after this patch has been applied.
         */
        public Set<Types.AfterApplyGuidance> afterApplyGuidance;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given pool_patch.
     *
     * @return all fields from the object
     */
    public PoolPatch.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPoolPatchRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the pool_patch instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static PoolPatch getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toPoolPatch(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get all the pool_patch instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<PoolPatch> getByNameLabel(Connection c, String label) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPoolPatch(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given pool_patch.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_uuid";
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
     * Get the name/label field of the given pool_patch.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_name_label";
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
     * Get the name/description field of the given pool_patch.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_name_description";
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
     * Get the version field of the given pool_patch.
     *
     * @return value of the field
     */
    public String getVersion(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_version";
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
     * Get the size field of the given pool_patch.
     *
     * @return value of the field
     */
    public Long getSize(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_size";
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
     * Get the pool_applied field of the given pool_patch.
     *
     * @return value of the field
     */
    public Boolean getPoolApplied(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_pool_applied";
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
     * Get the host_patches field of the given pool_patch.
     *
     * @return value of the field
     */
    public Set<HostPatch> getHostPatches(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_host_patches";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfHostPatch(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the after_apply_guidance field of the given pool_patch.
     *
     * @return value of the field
     */
    public Set<Types.AfterApplyGuidance> getAfterApplyGuidance(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_after_apply_guidance";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfAfterApplyGuidance(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given pool_patch.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_other_config";
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
     * Set the other_config field of the given pool_patch.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given pool_patch.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given pool_patch.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.remove_from_other_config";
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
     * Apply the selected patch to a host and return its output
     *
     * @param host The host to apply the patch too
     * @return the output of the patch application process
     */
    public String apply(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.apply";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Apply the selected patch to all hosts in the pool and return a map of host_ref -> patch output
     *
     */
    public void poolApply(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.pool_apply";
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
     * Removes the patch's files from all hosts in the pool, but does not remove the database entries
     *
     */
    public void clean(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.clean";
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
     * Return a list of all the pool_patchs known to the system.
     *
     * @return references to all objects
     */
    public static Set<PoolPatch> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfPoolPatch(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of pool_patch references to pool_patch records for all pool_patchs known to the system.
     *
     * @return records of all objects
     */
    public static Map<PoolPatch, PoolPatch.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "pool_patch.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfPoolPatchPoolPatchRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
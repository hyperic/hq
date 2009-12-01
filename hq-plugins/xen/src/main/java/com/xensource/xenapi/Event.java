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
 * Asynchronous event registration and handling
 *
 * @author XenSource Inc.
 */
public class Event extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private Event(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * Event instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<Event>> cache = 
        new HashMap<String,SoftReference<Event>>();

    protected static synchronized Event getInstFromRef(String ref) {
        if(Event.cache.containsKey(ref)) {
            Event instance = 
                Event.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        Event instance = new Event(ref);
        Event.cache.put(ref, new SoftReference<Event>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a Event
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "id", this.id);
            print.printf("%1$20s: %2$s\n", "timestamp", this.timestamp);
            print.printf("%1$20s: %2$s\n", "clazz", this.clazz);
            print.printf("%1$20s: %2$s\n", "operation", this.operation);
            print.printf("%1$20s: %2$s\n", "ref", this.ref);
            print.printf("%1$20s: %2$s\n", "objUuid", this.objUuid);
            print.printf("%1$20s: %2$s\n", "snapshot", this.snapshot);
            return writer.toString();
        }

        /**
         * Convert a event.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("id", this.id == null ? 0 : this.id);
            map.put("timestamp", this.timestamp == null ? new Date(0) : this.timestamp);
            map.put("class", this.clazz == null ? "" : this.clazz);
            map.put("operation", this.operation == null ? Types.EventOperation.UNRECOGNIZED : this.operation);
            map.put("ref", this.ref == null ? "" : this.ref);
            map.put("obj_uuid", this.objUuid == null ? "" : this.objUuid);
            map.put("snapshot", this.snapshot);
            return map;
        }

        /**
         * An ID, monotonically increasing, and local to the current session
         */
        public Long id;
        /**
         * The time at which the event occurred
         */
        public Date timestamp;
        /**
         * The name of the class of the object that changed
         */
        public String clazz;
        /**
         * The operation that was performed
         */
        public Types.EventOperation operation;
        /**
         * A reference to the object that changed
         */
        public String ref;
        /**
         * The uuid of the object that changed
         */
        public String objUuid;
        /**
         * The record of the database object that was added, changed or deleted
         * (the actual type will be VM.Record, VBD.Record or similar)
         */
        public Object snapshot;
    }

    /**
     * Registers this session with the event system.  Specifying the empty list will register for all classes.
     *
     * @param classes register for events for the indicated classes
     * @return Task
     */
    public static Task registerAsync(Connection c, Set<String> classes) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.event.register";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(classes)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Registers this session with the event system.  Specifying the empty list will register for all classes.
     *
     * @param classes register for events for the indicated classes
     */
    public static void register(Connection c, Set<String> classes) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "event.register";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(classes)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Unregisters this session with the event system
     *
     * @param classes remove this session's registration for the indicated classes
     * @return Task
     */
    public static Task unregisterAsync(Connection c, Set<String> classes) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "Async.event.unregister";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(classes)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toTask(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Unregisters this session with the event system
     *
     * @param classes remove this session's registration for the indicated classes
     */
    public static void unregister(Connection c, Set<String> classes) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "event.unregister";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(classes)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Blocking call which returns a (possibly empty) batch of events
     *
     * @return the batch of events
     */
    public static Set<Event.Record> next(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SessionNotRegistered,
       Types.EventsLost {
        String method_call = "event.next";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfEventRecord(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SESSION_NOT_REGISTERED")) {
                throw new Types.SessionNotRegistered((String) error[1]);
            }
            if(error[0].equals("EVENTS_LOST")) {
                throw new Types.EventsLost();
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return the ID of the next event to be generated by the system
     *
     * @return the event ID
     */
    public static Long getCurrentId(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "event.get_current_id";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

}
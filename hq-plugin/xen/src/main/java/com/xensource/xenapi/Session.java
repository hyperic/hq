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
 * A session
 *
 * @author XenSource Inc.
 */
public class Session extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private Session(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * Session instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<Session>> cache = 
        new HashMap<String,SoftReference<Session>>();

    protected static synchronized Session getInstFromRef(String ref) {
        if(Session.cache.containsKey(ref)) {
            Session instance = 
                Session.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        Session instance = new Session(ref);
        Session.cache.put(ref, new SoftReference<Session>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a Session
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "thisHost", this.thisHost);
            print.printf("%1$20s: %2$s\n", "thisUser", this.thisUser);
            print.printf("%1$20s: %2$s\n", "lastActive", this.lastActive);
            print.printf("%1$20s: %2$s\n", "pool", this.pool);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            return writer.toString();
        }

        /**
         * Convert a session.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("this_host", this.thisHost == null ? com.xensource.xenapi.Host.getInstFromRef("OpaqueRef:NULL") : this.thisHost);
            map.put("this_user", this.thisUser == null ? com.xensource.xenapi.User.getInstFromRef("OpaqueRef:NULL") : this.thisUser);
            map.put("last_active", this.lastActive == null ? new Date(0) : this.lastActive);
            map.put("pool", this.pool == null ? false : this.pool);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * Currently connected host
         */
        public Host thisHost;
        /**
         * Currently connected user
         */
        public User thisUser;
        /**
         * Timestamp for last time session was active
         */
        public Date lastActive;
        /**
         * True if this session relates to a intra-pool login, false otherwise
         */
        public Boolean pool;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
    }

    /**
     * Get a record containing the current state of the given session.
     *
     * @return all fields from the object
     */
    public Session.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSessionRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the session instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Session getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSession(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given session.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_uuid";
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
     * Get the this_host field of the given session.
     *
     * @return value of the field
     */
    public Host getThisHost(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_this_host";
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
     * Get the this_user field of the given session.
     *
     * @return value of the field
     */
    public User getThisUser(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_this_user";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toUser(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the last_active field of the given session.
     *
     * @return value of the field
     */
    public Date getLastActive(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_last_active";
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
     * Get the pool field of the given session.
     *
     * @return value of the field
     */
    public Boolean getPool(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_pool";
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
     * Get the other_config field of the given session.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.get_other_config";
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
     * Set the other_config field of the given session.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.set_other_config";
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
     * Add the given key-value pair to the other_config field of the given session.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.add_to_other_config";
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
     * Remove the given key and its corresponding value from the other_config field of the given session.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.remove_from_other_config";
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
     * Attempt to authenticate the user, returning a session reference if successful
     *
     * @param uname Username for login.
     * @param pwd Password for login.
     * @param version Client API version.
     * @return reference of newly created session
     */
    public static Session loginWithPassword(Connection c, String uname, String pwd, String version) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.SessionAuthenticationFailed {
        String method_call = "session.login_with_password";
        Object[] method_params = {Marshalling.toXMLRPC(uname), Marshalling.toXMLRPC(pwd), Marshalling.toXMLRPC(version)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSession(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("SESSION_AUTHENTICATION_FAILED")) {
                throw new Types.SessionAuthenticationFailed();
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Log out of a session
     *
     */
    public static void logout(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.logout";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Change the account password; if your session is authenticated with root priviledges then the old_pwd is validated and the new_pwd is set regardless
     *
     * @param oldPwd Old password for account
     * @param newPwd New password for account
     */
    public static void changePassword(Connection c, String oldPwd, String newPwd) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "session.change_password";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(oldPwd), Marshalling.toXMLRPC(newPwd)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

}
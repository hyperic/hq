/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.agent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.GenericValueMap;

/**
 * An object representing key/value pairs to be passed to, and returned from,
 * remote method calls.
 * 
 * This object provides a way for values to be sent and received in a way which
 * abstracts the serialization or protocol implementation.
 */

public class AgentRemoteValue implements GenericValueMap, Externalizable {

    private static final long serialVersionUID = 644682754857767836L;

    private static Log _log = LogFactory.getLog(AgentRemoteValue.class);

    private Map<String, String> vals = new LinkedHashMap<String, String>();;

    private static final int MAX_VALUE_SIZE = 65535;

    private static final String CHUNK_PREFIX = "-chunk";

    /**
     * Create a new AgentRemoteValue object with default innards.
     */

    public AgentRemoteValue() {
    }

    /**
     * Create a new AgentRemoteValue object with some default key/value pairs.
     * 
     * @param keyvals an array of arrays containing 2 elements. The first
     *        element is the key, and the second is its associated value.
     * 
     * @throws IllegalArgumentException indicating the passed array contained
     *         sub-arrays of size != 2.
     */

    public AgentRemoteValue(String keyvals[][]) throws IllegalArgumentException {
        for (int i = 0; i < keyvals.length; i++) {
            if (keyvals[i].length != 2) {
                throw new IllegalArgumentException("Arg index " + i + "didn't contain 2 values");
            }
            this.setValue(keyvals[i][0], keyvals[i][1]);
        }
    }

    /**
     * Setup a key/value pair.
     * 
     * @param key Key to assign the value to
     * @param val Value to assign to the key
     */
    public void setValue(String key, String val) {
        if (key == null || val == null) {
            _log.warn("Invalid key/value found.  Key='" + key + "' value='" + val + "'");
            return;
        }

        this.vals.put(key, val);
    }

    /**
     * Retrieve a value based on the key.
     * 
     * @param key Key for which to get the value.
     * 
     * @return the value of a previously set key
     */

    public String getValue(String key) {
        return (String) this.vals.get(key);
    }

    /**
     * Get a value, interpreted as a long integer.
     * 
     * @param key Key for which to get the value.
     * 
     * @return The value for the key 'key', as a long value.
     * 
     * @throws AgentRemoteException if the value cannot be interpted as a long.
     */

    public long getValueAsLong(String key) throws AgentRemoteException {
        String val = this.getValue(key);

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException exc) {
            throw new AgentRemoteException("Value is not a long, '" + val + "'");
        }
    }

    /**
     * Get a value, interpreted as a double.
     * 
     * @param key Key for which to get the value.
     * 
     * @return The value for the key 'key', as a double.
     * 
     * @throws AgentRemoteException if the value cannot be interpted as double.
     */

    public double getValueAsDouble(String key) throws AgentRemoteException {
        String val = this.getValue(key);

        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException exc) {
            throw new AgentRemoteException("Value is not a double, '" + val + "'");
        }
    }

    /**
     * Get a value, interpreted as an integer.
     * 
     * @param key Key for which to get the value.
     * 
     * @return The value for the key 'key', as an integer value.
     * 
     * @throws AgentRemoteException if the value cannot be interpted as an int.
     */

    public int getValueAsInt(String key) throws AgentRemoteException {
        String val = this.getValue(key);

        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException exc) {
            throw new AgentRemoteException("Value is not an integer, '" + val + "'");
        }
    }

    public void toStream(DataOutput os) throws IOException {
        for (Entry<String, String> entry : this.vals.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            // check if value is too large for writeUTF
            if (val.length() > MAX_VALUE_SIZE) {
                writeChunkedValues(key, val, os);
            } else {
                os.writeUTF(key);
                os.writeUTF(val);
            }
        }

        os.writeUTF(new String(""));
    }

    private void writeChunkedValues(String key, String val, DataOutput os) throws IOException {
        // now chunk the values
        int num = 0;
        while ((num * MAX_VALUE_SIZE) < val.length()) {
            int start = num * MAX_VALUE_SIZE;
            int end = ((start + MAX_VALUE_SIZE) > val.length()) ? val.length() : start +
                                                                                 MAX_VALUE_SIZE;

            String chunk = val.substring(start, end);
            os.writeUTF(CHUNK_PREFIX +"."+ key + "." + num);
            os.writeUTF(chunk);
            num++;
        }
    }

    public Set getKeys() {
        return this.vals.keySet();
    }

    public static AgentRemoteValue fromStream(DataInput is) throws IOException {
        Map<String, String> chunkedValues = new LinkedHashMap<String, String>();
        AgentRemoteValue res = new AgentRemoteValue();
        String key, val;

        key = is.readUTF();
        while (key.length() != 0) {
            val = is.readUTF();
            if (Pattern.matches("^"+CHUNK_PREFIX+"[.].*[.](\\d)", key)) {
                chunkedValues.put(key, val);
            } else {
                res.setValue(key, val);
            }
            key = is.readUTF();
        }

        for (Entry<String, String> entry : getUnchunkedValues(chunkedValues).entrySet()) {
            res.setValue(entry.getKey(), entry.getValue());
        }

        return res;
    }

    private static Map<String, String> getUnchunkedValues(Map<String, String> chunkedValues) {
        Map<String, String> unchunked = new LinkedHashMap<String, String>();
        for (Entry<String, String> chunkedEntry : chunkedValues.entrySet()) {
            String chunkParamKey = chunkedEntry.getKey().substring(CHUNK_PREFIX.length()+1);
            String paramKey = chunkParamKey.substring(0, chunkParamKey.indexOf("."));
            if (unchunked.containsKey(paramKey)) {
                unchunked.put(paramKey, unchunked.get(paramKey).concat(chunkedEntry.getValue()));
            } else {
                unchunked.put(paramKey, chunkedEntry.getValue());
            }
        }
        return unchunked;

    }

    public String toString() {
        return this.vals.toString();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        AgentRemoteValue res = fromStream(in);
        this.vals = res.vals;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        toStream(out);
    }
}

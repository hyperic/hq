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

package org.hyperic.hq.agent.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.GenericValueMap;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class ConfigStorage {

    public static final String PROP_TYPE      = "entity.type";
    public static final String PROP_ID        = "entity.id";
    public static final String PROP_TYPE_NAME = "entity.typeName";
    public static final String CONFIG_PREFIX  = "config.";
    public static final String NO_PREFIX      = "";
        
    private static Log log =
        LogFactory.getLog(ConfigStorage.class.getName());

    private AgentStorageProvider storage;
    private String keylistName;
    private String rootPrefix;

    public ConfigStorage(AgentStorageProvider storage,
                         String keylistName,
                         String storagePrefix) {

        this.storage = storage;
        this.keylistName = keylistName;
        this.rootPrefix = storagePrefix;
    }

    public Key getKey(AgentRemoteValue args) {
        return new Key(args.getValue(PROP_TYPE),
                       args.getValue(PROP_ID),
                       args.getValue(PROP_TYPE_NAME));
    }

    private String getKeys() {
        String keys = this.storage.getValue(this.keylistName);
        if (keys == null) {
            keys = "";
        }
        return keys;
    }
    
    public Map load() {
        Map configs = new HashMap();
        StringTokenizer tok =
            new StringTokenizer(getKeys(), Key.DELIM);

        while (tok.hasMoreTokens()) {
            Key key = new Key(tok.nextToken());
            ConfigResponse cr = new ConfigResponse();

            // read config from agent storage
            copy(key.storagePrefix, this.storage, NO_PREFIX, cr);

            configs.put(key, cr);

            if (log.isDebugEnabled()) {
                log.debug("Successfully restored config from storage: " +
                          key + "-->" + cr);
            }
        }

        return configs;
    }

    public ConfigResponse put(Key key, AgentRemoteValue args)
        throws AgentStorageException {        

        ConfigResponse cr = new ConfigResponse();

        copy(CONFIG_PREFIX, args, NO_PREFIX, cr);

        put(key, cr);

        return cr;
    }

    public void put(Key key, ConfigResponse cr) 
        throws AgentStorageException {        

        // store config in agent storage
        copy(NO_PREFIX, cr, key.storagePrefix, this.storage);

        // if this key doesn't yet exist in out master list, add it
        String keys = getKeys();

        String entry = key.getEntry();
        log.debug("Searching for '" + entry + "' " +
                  "within '" + keys + "'");

        if (keys.indexOf(entry) == -1) {
            keys += entry;
            this.storage.setValue(this.keylistName, keys);
        }
        // Flush writes to agent storage
        this.storage.flush();

        if (log.isDebugEnabled()) {
            log.debug("Successfully stored config into storage: " +
                      key + "-->" + cr);
        }
    }

    public void remove(Key key)
        throws AgentStorageException {
        
        String keys = getKeys();
        int len = keys.length();

        keys = StringUtil.replace(keys, key.getEntry(), "");

        if (len == keys.length()) {
            log.debug("Remove failed, key not found: " + key);
            return;
        }

        this.storage.setValue(this.keylistName, keys);
        this.storage.flush();

        log.debug("Removed from storage: " + key);

        // With the key nuked from the keylist entry, it shouldn't matter
        // (functionally) that the keys for the entity will hang around in
        // the agent db because they'll never be seen.  in the long run, we
        // should find some reliable way to make sure they're removed 
        // properly.
    }

    /**
     * For all keys in the keySet that begin with prefix,
     * copy the corresponding key/value pairs from the srcMap to the
     * destination map.
     * @param srcPrefix Properties in the source map that begin with this
     * prefix will be copied.
     * @param src The source map to search for matching keys.
     * @param destPrefix This prefix will be prepended to the key names
     * that are copied.  This is prepended after the srcPrefix has
     * been stripped.
     * @param dest The destination map to copy key/value pairs to.
     * @return The destination map.
     */
    public static GenericValueMap copy(String srcPrefix,
                                       GenericValueMap src,
                                       String destPrefix,
                                       GenericValueMap dest) {
    
        int srcPrefixLen = srcPrefix.length();

        //copy keys to avoid possible ConcurrentModificationException
        ArrayList keys = new ArrayList();
        keys.addAll(src.getKeys());

        for (int i=0; i<keys.size(); i++) {
            String key = (String)keys.get(i);
    
            if (key.startsWith(srcPrefix)) {
                String destKey =
                    destPrefix + key.substring(srcPrefixLen); 
                dest.setValue(destKey, src.getValue(key));
            }
        }
    
        return dest;
    }

    public class Key {
        private static final String DELIM = "|";
        private int type, id;
        private String typeName;
        private String storagePrefix;
        private String key;
        private String appdefKey;

        private void init(String type, String id, String typeName) {
            //XXX catch NFE
            init(Integer.parseInt(type),
                 Integer.parseInt(id),
                 typeName);
        }

        private void init(int type, int id, String typeName) {
            this.type = type;
            this.id = id;
            this.typeName = typeName;

            this.appdefKey =
                this.type + "-" + this.id + "-";

            this.key =
                this.appdefKey + this.typeName; 

            this.storagePrefix =
                rootPrefix + "." + this.key + ".";
        }

        private Key(String type, String id, String typeName) {
            init(type, id, typeName);
        }

        private Key(String key) {
            String val = key;

            int ix = val.indexOf("-");
            if (ix == -1) {
                throw new IllegalArgumentException("Invalid key format: " + key);
            }
            String type = val.substring(0, ix);

            val = val.substring(ix+1);
            ix = val.indexOf("-");
            if (ix == -1) {
                throw new IllegalArgumentException("Invalid key format: " + key);
            }
            String id = val.substring(0, ix);

            String typeName = val.substring(ix+1);

            init(type, id, typeName);
        }

        private String getEntry() {
            return this.key + DELIM;
        }

        public int getType() {
            return this.type;
        }

        public int getId() {
            return this.id;
        }

        public String getTypeName() {
            return this.typeName;
        }

        public int hashCode() {
            //same as AppdefEntityID.hashCode()
            return this.type * this.id;
        }

        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key k = (Key)o;
                
                return ((k.type == this.type) &&
                        (k.id == this.id));
            }
            else {
                return false;
            }
        }

        public String toString() {
            return this.key;
        }
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.common.server.session;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is an in-memory map of HQ server settings
 */
@Repository
@Transactional
public class ServerConfigCacheImpl implements ServerConfigCache {
    
    private final Cache _cache;
    private final Object _cacheLock = new Object();
    private ConfigPropertyDAO configPropertyDAO;
    
    public static final String CACHENAME = "ServerConfigCache";
   
    @Autowired
    public ServerConfigCacheImpl(ConfigPropertyDAO configPropertyDAO) {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
        this.configPropertyDAO = configPropertyDAO;
    }

    @Transactional(readOnly=true)
    public String getProperty(String key) {        
        String val = null;
        Element el = _cache.get(key);
        
        if (el != null) {
            val = (String) el.getObjectValue();
        } else {
            loadConfig();
            if (_cache.get(key) != null) {
                val = getProperty(key);
            }
        }
        return val;
    }

    @Transactional(readOnly=true)    
    public Boolean getBooleanProperty(String key) {
        Boolean bool = null;
        String prop = getProperty(key);
        
        if (prop != null) {
            bool = Boolean.valueOf(prop);
        }
        return bool;
    }

    public void put(String key, String value) {
        Element el = new Element(key, value);
                                                          
        synchronized (_cacheLock) {
            _cache.put(el);
        }
    }
    
    public void remove(String key) {
        synchronized (_cacheLock) {
            _cache.remove(key);
        }
    }

    private void loadConfig() {
        try {
            Properties config = getConfig();
            String key = null;
            
            synchronized (_cacheLock) {
                for (Enumeration e = config.propertyNames(); e.hasMoreElements() ;) {
                    key = (String) e.nextElement();
                    put(key, config.getProperty(key));
                }
            }
        } catch (ConfigPropertyException e) {
            throw new SystemException(e);
        }
    }

   
    
    /**
     * Get the "root" server configuration, that means those keys that have the
     * NULL prefix.
     * @return Properties
     * 
     */
    @Transactional(readOnly=true)
    public Properties getConfig() throws ConfigPropertyException {
        return getConfig(null);
    }

    /**
     * Get the server configuration
     * @param prefix The prefix of the configuration to retrieve.
     * @return Properties
     * 
     */
    @Transactional(readOnly=true)
    public Properties getConfig(String prefix) throws ConfigPropertyException {

        Collection<ConfigProperty> allProps = getProps(prefix);
        Properties props = new Properties();

        for (ConfigProperty configProp : allProps) {
            String key = configProp.getKey();
            // Check if the key has a value
            if (configProp.getValue() != null && configProp.getValue().length() != 0) {
                props.setProperty(key, configProp.getValue());
            } else {
                // Use defaults
                if (configProp.getDefaultValue() != null) {
                    props.setProperty(key, configProp.getDefaultValue());
                } else {
                    // Otherwise return an empty key. We dont want to
                    // prune any keys from the config.
                    props.setProperty(key, "");
                }
            }
        }

        return props;

    }

    @Transactional(readOnly=true)
    public Collection<ConfigProperty> getProps(String prefix) {
        if (prefix == null) {
            return configPropertyDAO.findAll();
        } else {
            return configPropertyDAO.findByPrefix(prefix);
        }
    }

}
